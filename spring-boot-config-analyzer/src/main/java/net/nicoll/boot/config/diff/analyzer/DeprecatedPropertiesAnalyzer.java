package net.nicoll.boot.config.diff.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.nicoll.boot.config.diff.ConfigDiffEntry;
import net.nicoll.boot.config.diff.ConfigDiffGenerator;
import net.nicoll.boot.config.diff.ConfigDiffResult;
import net.nicoll.boot.config.diff.ConfigDiffType;
import net.nicoll.boot.config.diff.SentenceExtractor;
import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * Analyze deprecated configuration, making sure that the replacement refers to an
 * existing property.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedPropertiesAnalyzer {

	public static void main(String[] args) throws Exception {
		String from = "2.7.5";
		String to = "3.0.0-SNAPSHOT";

		AetherDependencyResolver dependencyResolver = AetherDependencyResolver.withAllRepositories();
		ConfigDiffGenerator configDiffGenerator = new ConfigDiffGenerator(dependencyResolver);
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(dependencyResolver);
		ConfigDiffResult diff = configDiffGenerator.generateDiff(from, to);
		DeprecatedPropertiesReporter reporter = new DeprecatedPropertiesReporter(diff, loader.loadRepository(to));
		System.out.println(reporter.getReport());
	}

	private static class DeprecatedPropertiesReporter {

		private final ConfigDiffResult diff;

		private final ConfigurationMetadataRepository repository;

		DeprecatedPropertiesReporter(ConfigDiffResult diff, ConfigurationMetadataRepository repository) {
			this.diff = diff;
			this.repository = repository;
		}

		public String getReport() {
			List<String> valid = new ArrayList<>();
			List<String> invalid = new ArrayList<>();
			List<String> errors = new ArrayList<>();
			List<ConfigDiffEntry<ConfigurationMetadataProperty>> properties = diff
					.getPropertiesDiffFor(ConfigDiffType.DEPRECATE).stream().filter(this::isSupported).toList();

			properties.stream().filter(this::hasReplacement).forEach(e -> {
				ConfigurationMetadataProperty current = e.right();
				String candidate = current.getDeprecation().getReplacement();
				ConfigurationMetadataProperty replacement = getReplacementMetadata(candidate);
				if (replacement != null) {
					valid.add(current.getId() + " replaced by " + candidate);
				}
				else {
					invalid.add(current.getId() + " with invalid replacement " + candidate);
				}
			});

			properties.stream().filter(e -> !hasReplacement(e)).forEach(e -> {
				ConfigurationMetadataProperty current = e.right();
				errors.add(String.format("%s - %s", current.getId(), current.getDeprecation().getReason() != null
						? SentenceExtractor.getFirstSentence(current.getDeprecation().getReason()) : "none"));
			});
			StringBuilder message = new StringBuilder();
			message.append(String.format("Found %d deprecated properties%n", properties.size()));
			message.append(String.format("\t%d have a valid replacement%n", valid.size()));
			message.append(String.format("\t%d have an invalid replacement and must be fixed%n", invalid.size()));
			message.append(String.format("\t%d have no replacement%n", errors.size()));
			if (!invalid.isEmpty()) {
				message.append(String.format("%n%n"));
				message.append(String.format("Invalid replacements%n"));
				Collections.sort(invalid);
				invalid.forEach(e -> message.append(String.format("\t%s%n", e)));
			}
			if (!errors.isEmpty()) {
				message.append(String.format("%n%n"));
				message.append(String.format("Entries with no replacement%n"));
				Collections.sort(errors);
				errors.forEach(e -> message.append(String.format("\t%s%n", e)));
			}
			if (!valid.isEmpty()) {
				message.append(String.format("%n%n"));
				message.append(String.format("Entries with valid replacement%n"));
				Collections.sort(valid);
				valid.forEach(e -> message.append(String.format("\t%s%n", e)));
			}

			return message.toString();
		}

		private ConfigurationMetadataProperty getReplacementMetadata(String candidate) {
			ConfigurationMetadataProperty replacement = this.repository.getAllProperties().get(candidate);
			if (replacement != null) {
				return replacement;
			}
			return findMapReplacement(candidate);
		}

		private ConfigurationMetadataProperty findMapReplacement(String candidate) {
			int lastDot = candidate.lastIndexOf('.');
			if (lastDot != -1) {
				String mapCandidate = candidate.substring(0, lastDot);
				ConfigurationMetadataProperty property = this.repository.getAllProperties().get(mapCandidate);
				if (property != null) {
					String type = property.getType();
					if (type != null && type.startsWith(Map.class.getName())) {
						return property;
					}
				}
				else {
					return findMapReplacement(mapCandidate);
				}
			}
			return null;
		}

		private boolean isSupported(ConfigDiffEntry<ConfigurationMetadataProperty> e) {
			return e.right().getDeprecation().getLevel() == Level.WARNING;
		}

		private boolean hasReplacement(ConfigDiffEntry<ConfigurationMetadataProperty> e) {
			return e.right().getDeprecation().getReplacement() != null;
		}

	}

}
