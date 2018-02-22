package net.nicoll.boot.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Provide a report matching the appendix with the actual metadata.
 *
 * @author Stephane Nicoll
 */
class ConfigurationAppendixReporter {

	private static final Logger logger = LoggerFactory.getLogger(ConfigurationAppendixReporter.class);

	private final AdvertizedPropertiesAnalysis analysis;

	private Map<String, ConfigurationMetadataProperty> items;

	public ConfigurationAppendixReporter(AdvertizedPropertiesAnalysis analysis,
			ConfigurationMetadataRepository repository) {
		this.analysis = analysis;
		this.items = repository.getAllProperties();
	}

	public String getReport() {
		List<String> found = new ArrayList<>(
				this.analysis.getResolvedProperties().keySet());
		List<String> undocumented = new ArrayList<>();
		List<String> unresolved = new ArrayList<>(
				this.analysis.getUnresolvedProperties().keySet());
		List<String> mismatch = new ArrayList<>();
		List<String> deprecated = new ArrayList<>();
		Map<String, List<String>> groups = new LinkedHashMap<>();

		// Check non advertized properties and mismatch
		for (String key : this.items.keySet()) {
			if (!found.contains(key)) {
				String value = key;
				ConfigurationMetadataProperty candidate = this.items.get(key);
				Deprecation deprecation = candidate.getDeprecation();
				if (deprecation != null) {
					value += " (deprecated";
					if (deprecation.getReplacement() != null) {
						value += " see " + deprecation.getReplacement();
					}
					value += ")";
					deprecated.add(value);
				}
				else {
					undocumented.add(value);
				}
			}
			else if (!ignoreMismatchKey(key)) {
				StringBuilder sb = new StringBuilder(key);
				ConfigurationMetadataProperty metadata = this.items.get(key);
				AdvertizedProperty advertizedProperty = this.analysis
						.getResolvedProperties().get(key);
				String expectedDefaultValue = determineDefaultValue(metadata.getDefaultValue());
				if (!isDefaultValueSimilar(expectedDefaultValue,
						advertizedProperty.getDefaultValue())) {
					sb.append(String.format("%n\tWrong default value%n"));
					sb.append(String.format("\t\texpected: '%s'%n",
							expectedDefaultValue));
					sb.append(String.format("\t\tgot:      '%s'%n",
							advertizedProperty.getDefaultValue()));
				}
				String expectedDescription = sanitizeDescription(
						metadata.getDescription());
				if (!isDescriptionSimilar(key, expectedDescription,
						advertizedProperty.getDescription())) {
					sb.append(String.format("%n\tWrong description%n"));
					sb.append(String.format("\t\texpected: '%s'%n", expectedDescription));
					sb.append(String.format("\t\tgot:      '%s'%n",
							advertizedProperty.getDescription()));
				}
				String message = sb.toString();
				if (!message.equals(key)) {
					mismatch.add(String.format("%s%n", message));
				}
			}
		}

		// Check all the ".*" properties and match against the undocumented ones
		for (String key : unresolved) {
			if (key.endsWith(".*")) {
				String group = key.substring(0, key.length() - 2);
				List<String> matching = new ArrayList<>();
				undocumented.removeIf(item -> {
					if (item.startsWith(group)) {
						matching.add(item);
						return true;
					}
					return false;
				});
				groups.put(group, matching);
			}
		}
		groups.keySet().forEach(item -> unresolved.remove(item + ".*"));


		StringBuilder sb = new StringBuilder("\n");
		sb.append("Configuration key statistics").append("\n");
		sb.append("Advertized keys: ").append(this.analysis.propertiesCount()).append("\n");
		sb.append("Repository items: ").append(this.items.size()).append("\n");
		sb.append("Matching items: ").append(found.size()).append("\n");
		sb.append("Unresolved items (found in documentation but not in generated metadata): ").append(unresolved.size()).append("\n");
		sb.append("Groups (group defined in the documentation but not each individual elements): ").append(groups.size()).append("\n");
		sb.append("Undocumented items (found in generated metadata but not in documentation): ").append(undocumented.size()).append("\n");
		sb.append("Deprecated items (found in generated metadata but not in documentation): ").append(deprecated.size()).append("\n");
		sb.append("Mismatch items (description or default value not matching): ").append(mismatch.size()).append("\n");
		sb.append("\n");
		sb.append("\n");
		if (!unresolved.isEmpty()) {
			sb.append("Unresolved items").append("\n");
			sb.append("----------------").append("\n");
			Collections.sort(unresolved);
			for (String id : unresolved) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}
		if (!groups.isEmpty()) {
			sb.append("Groups").append("\n");
			sb.append("----------------").append("\n");
			for (Map.Entry<String, List<String>> group : groups.entrySet()) {
				sb.append(group.getKey()).append(" with ")
						.append(group.getValue().size()).append(" elements").append("\n");
			}
			sb.append("\n");
		}
		if (!undocumented.isEmpty()) {
			sb.append("Undocumented items").append("\n");
			sb.append("--------------------").append("\n");
			List<String> ids = new ArrayList<>(undocumented);
			Collections.sort(ids);
			for (String id : ids) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}
		if (!deprecated.isEmpty()) {
			sb.append("Deprecated items").append("\n");
			sb.append("--------------------").append("\n");
			List<String> ids = new ArrayList<>(deprecated);
			Collections.sort(ids);
			for (String id : ids) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}
		if (!mismatch.isEmpty()) {
			sb.append("Mismatch items").append("\n");
			sb.append("--------------------").append("\n");
			List<String> ids = new ArrayList<>(mismatch);
			Collections.sort(ids);
			for (String id : ids) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	// Nested class in a separate module (no documentation available)
	private static final List<String> NON_MANAGED_GROUPS = Arrays.asList(
			"management.server.ssl.", "server.compression.", "server.http2.",
			"server.servlet.jsp.", "server.servlet.session.", "server.ssl.");

	// Metadata generated from third party class
	private static final List<String> THIRD_PARTY_GROUPS = Arrays.asList(
			"spring.jta.atomikos.connectionfactory.", "spring.jta.atomikos.datasource.",
			"spring.jta.bitronix.connectionfactory.", "spring.jta.bitronix.datasource.");

	private boolean ignoreMismatchKey(String key) {
		for (String group : NON_MANAGED_GROUPS) {
			if (key.startsWith(group)) {
				return true;
			}
		}
		for (String group : THIRD_PARTY_GROUPS) {
			if (key.startsWith(group)) {
				return true;
			}
		}
		return false;
	}

	private boolean isDefaultValueSimilar(String expected, String actual) {
		return expected == null && actual == null
				|| expected != null && expected.equals(actual);
	}

	private String determineDefaultValue(Object value) {
		if (ObjectUtils.isEmpty(value)) {
			return null;
		}
		if (value.getClass().isArray()) {
			return StringUtils.arrayToCommaDelimitedString((Object[]) value);
		}
		if (value instanceof Collection) {
			return StringUtils.collectionToCommaDelimitedString((Collection<?>) value);
		}
		return value.toString();
	}

	private boolean isDescriptionSimilar(String key, String expected, String actual) {
		if (ObjectUtils.nullSafeEquals(expected, actual)) {
			return true;
		}
		if (expected != null && actual != null) {
			if (expected.startsWith(actual) && actual.endsWith(".")) {
				logger.info(String.format("Shortened description for '%s%n\t%s%n\t%s%n",
						key, expected, actual));
				return true;
			}
		}
		return false;
	}

	private static String sanitizeDescription(String text) {
		if (text == null) {
			return null;
		}
		return removeSpaceBetweenLine(text);
	}

	private static String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

}
