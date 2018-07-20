package net.nicoll.boot.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.StringUtils;

/**
 * Analyze the appendix and generate a {@link AdvertizedPropertiesAnalysis}.
 *
 * @author Stephane Nicoll
 */
class AdvertizedPropertiesAnalyzer {

	private static final Logger logger = LoggerFactory.getLogger(AdvertizedPropertiesAnalyzer.class);

	private final Properties advertizedProperties;

	private final Map<String, ConfigurationMetadataProperty> items;

	AdvertizedPropertiesAnalyzer(Properties advertizedProperties,
			ConfigurationMetadataRepository repository) {
		this.advertizedProperties = advertizedProperties;
		this.items = repository.getAllProperties();
	}

	public AdvertizedPropertiesAnalysis analyze() {
		// Generate relax names for all properties
		List<ConfigKeyCandidates> advertized = advertizedProperties.keySet()
				.stream().map(item -> new ConfigKeyCandidates((String) item))
				.collect(Collectors.toList());

		Map<String, AdvertizedProperty> resolvedProperties = new LinkedHashMap<>();
		Map<String, AdvertizedProperty> unresolvedProperties = new LinkedHashMap<>();
		// Check advertized properties
		for (ConfigKeyCandidates propertyItem : advertized) {
			String key = getDocumentedKey(propertyItem);
			if (key != null) {
				resolvedProperties.put(key, toAdvertizedProperty(propertyItem.item, key));
			}
			else {
				unresolvedProperties.put(propertyItem.item,
						toAdvertizedProperty(propertyItem.item, propertyItem.item));
			}
		}
		return new AdvertizedPropertiesAnalysis(resolvedProperties, unresolvedProperties);
	}

	private String getDocumentedKey(ConfigKeyCandidates candidates) {
		String candidate = candidates.configurationPropertyName.toString();
		boolean hasKey = this.items.containsKey(candidate);
		if (hasKey) {
			return candidate;
		}
		return null;
	}

	private AdvertizedProperty toAdvertizedProperty(String key, String canonicalKey) {
		String rawValue = this.advertizedProperties.getProperty(key);
		String[] split = rawValue.split("#", 2);
		if (split.length != 2) {
			logger.warn(String.format("Unusual value for key '%s': %s", key, rawValue));
			return new AdvertizedProperty(canonicalKey, null, null);
		}
		else {
			String defaultValue = split[0].trim();
			String description = split[1].trim();
			return new AdvertizedProperty(canonicalKey,
					(StringUtils.hasText(defaultValue) ? defaultValue : null),
					(StringUtils.hasText(description) ? description : null));
		}
	}

	private static class ConfigKeyCandidates {

		private final String item;

		private final ConfigurationPropertyName configurationPropertyName;

		private ConfigKeyCandidates(String item) {
			this.item = item;
			this.configurationPropertyName = initialize(item);
		}

		private static ConfigurationPropertyName initialize(String item) {
			String itemToUse = item;
			if (itemToUse.endsWith(".*")) {
				itemToUse = itemToUse.substring(0, itemToUse.length() - 2);
			}
			return ConfigurationPropertyName.of(itemToUse);
		}
	}

}
