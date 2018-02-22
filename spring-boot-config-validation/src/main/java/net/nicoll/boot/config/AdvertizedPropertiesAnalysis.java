package net.nicoll.boot.config;

import java.util.Map;

/**
 * Analyzis of advertized (e.g. documented) properties.
 *
 * @author Stephane Nicoll
 */
class AdvertizedPropertiesAnalysis {

	private final Map<String, AdvertizedProperty> resolvedProperties;

	private final Map<String, AdvertizedProperty> unresolvedProperties;

	AdvertizedPropertiesAnalysis(
			Map<String, AdvertizedProperty> resolvedProperties,
			Map<String, AdvertizedProperty> unresolvedProperties) {
		this.resolvedProperties = resolvedProperties;
		this.unresolvedProperties = unresolvedProperties;
	}

	/**
	 * Return the {@link AdvertizedProperty advertized properties} that have a match in
	 * the metadata.
	 */
	public Map<String, AdvertizedProperty> getResolvedProperties() {
		return this.resolvedProperties;
	}

	/**
	 * Return the {@link AdvertizedProperty advertized properties} that have no match in
	 * the metadata.
	 */
	public Map<String, AdvertizedProperty> getUnresolvedProperties() {
		return this.unresolvedProperties;
	}

	/**
	 * Return the number of documented properties.
	 */
	public int propertiesCount() {
		return this.resolvedProperties.size() + this.unresolvedProperties.size();
	}

}
