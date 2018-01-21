package org.springframework.boot.configurationalayzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.configurationalayzer.LegacyPropertiesAnalyzer;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.SimpleConfigurationMetadataRepository;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link LegacyPropertiesAnalyzer}.
 *
 * @author Stephane Nicoll
 */
public class LegacyPropertiesAnalyzerTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	public void reportIsNullWithNoMatchingKeys() {
		String report = createReport(new SimpleConfigurationMetadataRepository());
		assertThat(report).isNull();
	}

	@Test
	public void replacementKeysAreRemapped() throws IOException {
		MutablePropertySources propertySources = this.environment.getPropertySources();
		PropertySource<?> one = loadPropertySource("one", "config/config-one.properties");
		PropertySource<?> two = loadPropertySource("two", "config/config-two.properties");
		propertySources.addFirst(one);
		propertySources.addAfter("one", two);
		assertThat(propertySources).hasSize(3);
		createReport(loadRepository("metadata/sample-metadata.json"));
		assertThat(mapToNames(propertySources)).containsExactly("one",
				"migrate-two", "two", "mockProperties");
		assertMappedProperty(propertySources.get("migrate-two"),
				"test.two", "another", getOrigin(two, "wrong.two"));
	}

	@Test
	public void reportWithOrigin() throws IOException {
		this.environment.getPropertySources().addFirst(loadPropertySource("test",
				"config/sample-error.properties"));
		String report = createReport(loadRepository("metadata/sample-metadata.json"));
		assertThat(report).isNotNull();
		assertThat(report).containsSequence("Property source 'test'",
				"line 000 wrong.one", "line 003 wrong.two");
		assertThat(report).contains("wrong.one - reason: This is no longer supported.");
		assertThat(report).contains("wrong.two -> test.two");
		System.out.println(report); // TODO
	}

	private List<String> mapToNames(PropertySources sources) {
		List<String> names = new ArrayList<>();
		for (PropertySource<?> source : sources) {
			names.add(source.getName());
		}
		return names;
	}

	@SuppressWarnings("unchecked")
	private Origin getOrigin(PropertySource<?> propertySource, String name) {
		return ((OriginLookup<String>) propertySource).getOrigin(name);
	}

	private void assertMappedProperty(PropertySource<?> propertySource, String name, Object value, Origin origin) {
		assertThat(propertySource.containsProperty(name)).isTrue();
		assertThat(propertySource.getProperty(name)).isEqualTo(value);
		if (origin != null) {
			assertThat(propertySource).isInstanceOf(OriginLookup.class);
			assertThat(((OriginLookup<Object>) propertySource).getOrigin(name))
					.isEqualTo(origin);
		}
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

	private String createReport(ConfigurationMetadataRepository repository) {
		return new LegacyPropertiesAnalyzer(repository, this.environment)
				.analyseAndCreateReport();
	}

}
