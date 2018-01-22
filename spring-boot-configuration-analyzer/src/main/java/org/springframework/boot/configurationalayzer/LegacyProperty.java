package org.springframework.boot.configurationalayzer;

import java.util.Comparator;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.TextResourceOrigin;

/**
 * Description of a legacy property.
 *
 * @author Stephane Nicoll
 */
class LegacyProperty {

	static final LegacyPropertyComparator COMPARATOR = new LegacyPropertyComparator();

	private final ConfigurationMetadataProperty metadata;

	private final ConfigurationProperty property;

	private final Integer lineNumber;

	LegacyProperty(ConfigurationMetadataProperty metadata,
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
				return textOrigin.getLocation().getLine() + 1;
			}
		}
		return null;
	}

	public ConfigurationMetadataProperty getMetadata() {
		return this.metadata;
	}

	public ConfigurationProperty getProperty() {
		return this.property;
	}

	public Integer getLineNumber() {
		return this.lineNumber;
	}


	private static class LegacyPropertyComparator implements Comparator<LegacyProperty> {

		@Override
		public int compare(LegacyProperty p1, LegacyProperty p2) {
			return p1.getMetadata().getId().compareTo(p2.getMetadata().getId());
		}
	}

}
