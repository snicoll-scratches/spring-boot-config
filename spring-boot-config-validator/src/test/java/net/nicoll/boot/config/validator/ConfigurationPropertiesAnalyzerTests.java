package net.nicoll.boot.config.validator;

import java.io.IOException;
import java.util.function.Predicate;

import org.junit.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.configurationmetadata.SimpleConfigurationMetadataRepository;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link ConfigurationPropertiesAnalyzer}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationPropertiesAnalyzerTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	public void reportIsNullWithNoMatchingKeys() {
		String report = createReport(new SimpleConfigurationMetadataRepository(),
				deprecatedFilter());
		assertThat(report).isNull();
	}

	@Test
	public void reportWithOrigin() throws IOException {
		this.environment.getPropertySources().addFirst(loadPropertySource("test",
				"config/sample-error.properties"));
		String report = createReport(loadRepository("metadata/sample-metadata.json"),
				deprecatedFilter());
		assertThat(report).isNotNull();
		assertThat(report).containsSequence("Property source 'test'",
				"line 000 wrong.one", "line 003 wrong.two");
		assertThat(report).contains("wrong.one - reason: This is no longer supported.");
		assertThat(report).contains("wrong.two -> test.two");
		System.out.println(report);
	}

	private Predicate<ConfigurationMetadataProperty> deprecatedFilter() {
		return p -> p.getDeprecation() != null
				&& p.getDeprecation().getLevel() == Deprecation.Level.ERROR;
	}

	private PropertySource<?> loadPropertySource(String name, String path)
			throws IOException {
		ClassPathResource resource = new ClassPathResource(path);
		PropertySource<?> propertySource = new PropertiesPropertySourceLoader()
				.load(name, resource, null);
		assertThat(propertySource).isNotNull();
		return propertySource;
	}

	private ConfigurationMetadataRepository loadRepository(String... content) {
		try {
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			for (String path : content) {
				Resource resource = new ClassPathResource(path);
				builder.withJsonResource(resource.getInputStream());
			}
			return builder.build();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load metadata", ex);
		}
	}

	private String createReport(ConfigurationMetadataRepository repository,
			Predicate<ConfigurationMetadataProperty> filter) {
		return new ConfigurationPropertiesAnalyzer(repository, this.environment)
				.createMatchingPropertiesReport(filter);
	}

}
