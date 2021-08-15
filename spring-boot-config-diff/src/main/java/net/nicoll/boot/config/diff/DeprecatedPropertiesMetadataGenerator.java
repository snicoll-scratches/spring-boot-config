package net.nicoll.boot.config.diff;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Generates a JSON structure for all keys that were removed between two versions. This
 * allows to use the new deprecation level feature that allows to flag removed properties
 * for IDE inspection.
 * <p>
 * This generator tries to guess what the replacement could be using an approximate
 * algorithm so that the generated list must be reviewed manually.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedPropertiesMetadataGenerator {

	public static void main(String[] args) throws Exception {
		String from = "2.6.0-M1";
		String to = "2.6.0-SNAPSHOT";

		AetherDependencyResolver dependencyResolver = AetherDependencyResolver
				.withAllRepositories();
		ConfigDiffResult configDiffResult = new ConfigDiffGenerator(dependencyResolver)
				.generateDiff(from, to);
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(
				dependencyResolver);
		ConfigurationMetadataRepository repository = loader.loadRepository(to);

		DeprecatedPropertyJsonFormatter formatter = new DeprecatedPropertyJsonFormatter(
				repository);

		System.out.println(formatter.formatDiff(configDiffResult));
	}


	private static class DeprecatedPropertyJsonFormatter implements ConfigDiffFormatter {

		private final List<String> knownExcludes = Arrays.asList("security.oauth2",
				"spring.datasource.dbcp", "spring.datasource.hikari", "spring.mobile",
				"spring.social");

		private final ConfigurationMetadataRepository repository;

		private DeprecatedPropertyJsonFormatter(
				ConfigurationMetadataRepository repository) {
			this.repository = repository;
		}

		@Override
		public String formatDiff(ConfigDiffResult result) throws IOException {
			List<ConfigDiffEntry<ConfigurationMetadataProperty>> deleted
					= result.getPropertiesDiffFor(ConfigDiffType.DELETE);


			List<DeprecatedItem> items = deleted.stream()
					.filter(this::isValidCandidate)
					.map(this::toDeprecatedItem)
					.collect(Collectors.toList());

			MultiValueMap<String, DeprecatedItem> groups = new LinkedMultiValueMap<>();
			items.forEach(item -> groups.add(detectGroup(item.getName()), item));


			StringBuilder sb = new StringBuilder();
			ObjectMapper mapper = new ObjectMapper();
			for (Map.Entry<String, List<DeprecatedItem>> e : groups.entrySet()) {
				List<DeprecatedItem> groupItems = e.getValue();
				groupItems.sort(Comparator.comparing(DeprecatedItem::getName));
				sb.append(String.format("Add to `%s`%n%n", e.getKey()));
				sb.append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(groupItems));
				//groupItems.forEach(i -> sb.append(String.format("%s%n", i.getName())));
				sb.append(String.format("%n"));
			}
			return sb.toString();
		}

		private boolean isValidCandidate(ConfigDiffEntry<ConfigurationMetadataProperty> entry) {
			if (isAlreadyDeprecated(entry)) {
				return false;
			}
			String id = entry.getLeft().getId();
			for (String knownExclude : this.knownExcludes) {
				if (id.startsWith(knownExclude)) {
					return false;
				}
			}
			return true;
		}

		private boolean isAlreadyDeprecated(
				ConfigDiffEntry<ConfigurationMetadataProperty> entry) {
			return (entry.getLeft().isDeprecated()
					&& entry.getLeft().getDeprecation().getLevel().equals(Deprecation.Level.ERROR)
					&& entry.getRight() == null);
		}

		private DeprecatedItem toDeprecatedItem(
				ConfigDiffEntry<ConfigurationMetadataProperty> e) {
			return new DeprecatedItem(e.getLeft(),
					detectReplacement(e.getLeft()), detectReason(e.getLeft()));
		}

		private String detectReplacement(ConfigurationMetadataProperty property) {
			String[] parts = property.getId().split("\\.");
			if (parts.length == 2) {
				List<ConfigurationMetadataProperty> properties = extractSimilarKey(property.getId());
				if (properties.size() == 1) {
					return properties.get(0).getId();
				}
			}
			if (parts.length > 2) {
				String attempt = parts[parts.length - 2] + "." + parts[parts.length - 1];
				List<ConfigurationMetadataProperty> properties = extractSimilarKey(attempt);
				if (properties.size() == 1) {
					return properties.get(0).getId();
				}
			}
			List<ConfigurationMetadataProperty> properties = extractSimilarKey(parts[parts.length - 1]);
			if (properties.size() == 1) {
				return properties.get(0).getId();
			}
			return null;
		}

		private String detectReason(ConfigurationMetadataProperty property) {
			String id = property.getId();
			// TODO: add reason for well known keys

			return null;
		}

		private List<ConfigurationMetadataProperty> extractSimilarKey(String part) {
			return this.repository.getAllProperties().values().stream()
					.filter(p -> p.getId().endsWith(part))
					.collect(Collectors.toList());
		}

		private String detectGroup(String name) {
			if (name.startsWith("endpoints") || name.startsWith("management")) {
				return "spring-boot-actuator";
			}
			if (name.startsWith("spring.devtools")) {
				return "spring-boot-devtools";
			}
			return "spring-boot-autoconfigure";
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class DeprecatedItem {

		private final String name;

		private final String type;

		private final Deprecation deprecation;

		public DeprecatedItem(ConfigurationMetadataProperty property,
				String replacement, String reason) {
			this.name = property.getId();
			this.type = property.getType();
			this.deprecation = new Deprecation(replacement, reason);
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
		}

		public Deprecation getDeprecation() {
			return this.deprecation;
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Deprecation {
			private final String replacement;

			private final String reason;

			public Deprecation(String replacement, String reason) {
				this.replacement = replacement;
				this.reason = reason;
			}

			public String getLevel() {
				return "error";
			}

			public String getReplacement() {
				return this.replacement;
			}

			public String getReason() {
				return this.reason;
			}
		}
	}
}
