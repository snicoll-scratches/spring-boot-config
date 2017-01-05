package net.nicoll.boot.config.loader;

import java.io.InputStream;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.JsonMarshaller;

/**
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataJsonBuilder {

	private final JsonMarshaller marshaller = new JsonMarshaller();

	private final ConfigurationMetadata metadata = new ConfigurationMetadata();

	public ConfigurationMetadataJsonBuilder withJsonResource(InputStream inputStream) {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null.");
		}
		ConfigurationMetadata other = read(inputStream);
		this.metadata.merge(other);
		return this;
	}

	public ConfigurationMetadata build() {
		return this.metadata;
	}

	private ConfigurationMetadata read(InputStream in) {
		try {
			return this.marshaller.read(in);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to read configuration metadata", ex);
		}
	}

}
