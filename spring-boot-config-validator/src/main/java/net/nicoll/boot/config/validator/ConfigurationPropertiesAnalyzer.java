package net.nicoll.boot.config.validator;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.TextResourceOrigin;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 *
 * @author Stephane Nicoll
 */
class ConfigurationPropertiesAnalyzer {

	private final ConfigurationMetadataRepository metadataRepository;

	private final ConfigurableEnvironment environment;

	ConfigurationPropertiesAnalyzer(ConfigurationMetadataRepository metadataRepository,
			ConfigurableEnvironment environment) {
		this.metadataRepository = metadataRepository;
		this.environment = environment;
	}

	public String createMatchingPropertiesReport(
			Predicate<ConfigurationMetadataProperty> filter) {
		Map<String, List<ConfigurationPropertiesAnalyzer.Property>> properties = getMatchingProperties(filter);
		if (properties.isEmpty()) {
			return null;
		}
		StringBuilder message = new StringBuilder();
		message.append(String.format("%nConfiguration keys that are no longer supported "
				+ "were found in the environment%n%n"));
		properties.forEach((name, matches) -> {
			message.append(String.format("Property source '%s'%n", name));
			matches.stream().sorted(PropertyComparator.INSTANCE).forEach(match -> {
				ConfigurationMetadataProperty metadata = match.metadata;
				ConfigurationProperty property = match.property;
				message.append("\t");
				if (property.getOrigin() instanceof TextResourceOrigin) {
					message.append(String.format("line %03d ", ((TextResourceOrigin)
							property.getOrigin()).getLocation().getLine()));
				}
				else {
					message.append("          ");
				}
				message.append(String.format("%s ", property.getName()));
				if (metadata.getDeprecation().getReplacement() != null) {
					message.append(String.format("-> %s",
							metadata.getDeprecation().getReplacement()));
				}
				else if (StringUtils.hasText(metadata.getDeprecation().getReason())) {
					message.append(String.format("- reason: %s",
							metadata.getDeprecation().getReason()));
				}
				message.append(String.format("%n"));
			});
			message.append(String.format("%n%n"));
		});
		message.append("Those entries must be renamed (if an alternative exists) or "
				+ "reviewed with the provided reason.");
		return message.toString();
	}

	private Map<String, List<Property>> getMatchingProperties(
			Predicate<ConfigurationMetadataProperty> filter) {
		MultiValueMap<String, Property> result = new LinkedMultiValueMap<>();
		List<ConfigurationMetadataProperty> candidates = metadataRepository
				.getAllProperties().values().stream()
				.filter(filter)
				.collect(Collectors.toList());
		getPropertySourcesAsMap().forEach((name, source) -> {
			candidates.forEach(metadata -> {
				ConfigurationProperty configurationProperty = source.getConfigurationProperty(
						ConfigurationPropertyName.of(metadata.getId()));
				if (configurationProperty != null) {
					result.add(name, new Property(metadata, configurationProperty));
				}
			});
		});
		return result;
	}

	private Map<String, ConfigurationPropertySource> getPropertySourcesAsMap() {
		Map<String, ConfigurationPropertySource> map = new LinkedHashMap<>();
		ConfigurationPropertySources.get(this.environment);
		for (ConfigurationPropertySource source : ConfigurationPropertySources.get(this.environment)) {
			map.put(determinePropertySourceName(source), source);
		}
		return map;
	}

	private String determinePropertySourceName(ConfigurationPropertySource source) {
		if (source.getUnderlyingSource() instanceof PropertySource) {
			return ((PropertySource) source.getUnderlyingSource()).getName();
		}
		return source.getUnderlyingSource().toString(); // YOLO
	}


	private static class Property {

		private final ConfigurationMetadataProperty metadata;

		private final ConfigurationProperty property;

		private final Integer lineNumber;

		Property(ConfigurationMetadataProperty metadata,
				ConfigurationProperty property) {
			this.metadata = metadata;
			this.property = property;
			this.lineNumber = determineLineNumber(property);
		}

		private static Integer determineLineNumber(ConfigurationProperty property) {
			Origin origin = property.getOrigin();
			if (origin instanceof TextResourceOrigin) {
				TextResourceOrigin textOrigin = (TextResourceOrigin) origin;
				if (textOrigin.getLocation() != null) {
					return textOrigin.getLocation().getLine();
				}
			}
			return null;
		}

	}

	private static class PropertyComparator implements Comparator<Property> {

		private static final PropertyComparator INSTANCE =  new PropertyComparator();

		@Override
		public int compare(Property p1, Property p2) {
			if (p1.lineNumber != null && p2.lineNumber != null) {
				return p1.lineNumber.compareTo(p2.lineNumber);
			}
			return p1.metadata.getId().compareTo(p2.metadata.getId());
		}
	}

}
