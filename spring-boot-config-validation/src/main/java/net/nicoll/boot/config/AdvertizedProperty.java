package net.nicoll.boot.config;

/**
 * Describe a documented property.
 *
 * @author Stephane Nicoll
 */
class AdvertizedProperty {

	private final String key;

	private final String defaultValue;

	private final String description;

	AdvertizedProperty(String key, String defaultValue, String description) {
		this.key = key;
		this.defaultValue = defaultValue;
		this.description = description;
	}

	/**
	 * Return the key of the property matching a metadata entry.
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Return the documented default value or {@code null} if there isn't any
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Return the documentation of the property (can be {@code null} though it is very
	 * unusual).
	 */
	public String getDescription() {
		return this.description;
	}

}
