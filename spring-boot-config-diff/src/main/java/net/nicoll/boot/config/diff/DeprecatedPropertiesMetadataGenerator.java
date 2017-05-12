package net.nicoll.boot.config.diff;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nicoll.boot.config.loader.AetherDependencyResolver;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

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
		ConfigDiffResult configDiffResult = new ConfigDiffGenerator(
				AetherDependencyResolver.withAllRepositories()).generateDiff(
				"1.5.4.BUILD-SNAPSHOT", "2.0.0.BUILD-SNAPSHOT");

		DeprecatedPropertyJsonFormatter formatter = new DeprecatedPropertyJsonFormatter();

		System.out.println(formatter.formatDiff(configDiffResult));
	}


	private static class DeprecatedPropertyJsonFormatter implements ConfigDiffFormatter {

		@Override
		public String formatDiff(ConfigDiffResult result) throws IOException {
			List<ConfigDiffEntry<ConfigurationMetadataProperty>> deleted
					= result.getPropertiesDiffFor(ConfigDiffType.DELETE);


			List<DeprecatedItem> items = deleted.stream()
					.map(e -> new DeprecatedItem(e.getLeft(), null))
					.collect(Collectors.toList());
			items.sort(Comparator.comparing(DeprecatedItem::getName));

			ObjectMapper mapper = new ObjectMapper();
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(items);
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
				String replacement) {
			this.name = property.getId();
			this.type = property.getType();
			this.description = property.getDescription();
			this.defaultValue = property.getDefaultValue();
			this.deprecation = new Deprecation(replacement);
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

		public boolean isDeprecated() {
			return true;
		}


		public Deprecation getDeprecation() {
			return this.deprecation;
		}

		@JsonInclude(JsonInclude.Include.NON_NULL)
		public static class Deprecation {
			private final String replacement;

			public Deprecation(String replacement) {
				this.replacement = replacement;
			}

			public String getLevel() {
				return "error";
			}

			public String getReplacement() {
				return this.replacement;
			}
		}
	}
}
