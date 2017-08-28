package net.nicoll.boot.config.diff;

import java.io.IOException;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Generates a JSON structure for all keys that were removed between two versions. This
 * allows to use the new deprecation level feature that allows to flag removed properties
 * for IDE inspection.
 * <p>
 * The generator does not try to guess what the replacement could be (the algorithm
 * would be fussy anyway). This generated list must be reviewed to add replacement keys.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedPropertiesMetadataGenerator {

	public static void main(String[] args) throws Exception {
		String from = "1.5.6.RELEASE";
		String to = "2.0.0.BUILD-SNAPSHOT";

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
					.filter(this::filterAlreadyDeprecatedItem)
					.map(e -> new DeprecatedItem(e.getLeft(),
							detectReplacement(e.getLeft()), detectReason(e.getLeft())))
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
				sb.append(String.format("%n"));
			}
			return sb.toString();
		}

		private boolean filterAlreadyDeprecatedItem(
				ConfigDiffEntry<ConfigurationMetadataProperty> entry) {
			return !entry.getLeft().isDeprecated()
					&& (entry.getRight() == null || !entry.getRight().isDeprecated());
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

		private final String description;

		private final Object defaultValue;

		private final Deprecation deprecation;

		public DeprecatedItem(ConfigurationMetadataProperty property,
				String replacement, String reason) {
			this.name = property.getId();
			this.type = property.getType();
			this.description = property.getDescription();
			this.defaultValue = property.getDefaultValue();
			this.deprecation = new Deprecation(replacement, reason);
		}

		public String getName() {
			return this.name;
		}

		public String getType() {
			return this.type;
		}

		public String getDescription() {
			return this.description;
		}

		public Object getDefaultValue() {
			return this.defaultValue;
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
