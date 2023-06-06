package net.nicoll.boot.config.diff;

import java.io.IOException;
import java.util.List;

import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDiffGenerator}.
 *
 * @author Stephane Nicoll
 */
public class ConfigDiffGeneratorTests {

	@Test
	public void diffRepositories() throws IOException {
		ConfigurationMetadataLoader loader = configure("repository/sample-one-1.0.json",
				"repository/sample-one-2.0.json");
		ConfigDiffGenerator configDiffGenerator = new ConfigDiffGenerator(loader);
		ConfigDiffResult diff = configDiffGenerator.generateDiff("1.0", "2.0");
		assertThat(diff).isNotNull();
		assertThat(diff.getLeftVersion()).isEqualTo("1.0");
		assertThat(diff.getRightVersion()).isEqualTo("2.0");
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> equal = diff.getPropertiesDiffFor(ConfigDiffType.EQUALS);
		assertThat(equal).hasSize(1);
		assertProperty(equal.get(0).right(), "test.equal", String.class, "test");
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> added = diff.getPropertiesDiffFor(ConfigDiffType.ADD);
		assertThat(added).hasSize(1);
		assertProperty(added.get(0).right(), "test.add", String.class, "new");
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> delete = diff.getPropertiesDiffFor(ConfigDiffType.DELETE);
		assertThat(delete).anySatisfy((entry) -> assertProperty(entry.left(), "test.delete", String.class, "delete"))
			.anySatisfy((entry) -> assertProperty(entry.right(), "test.delete.deprecated", String.class, "delete"))
			.hasSize(2);
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> deprecate = diff
			.getPropertiesDiffFor(ConfigDiffType.DEPRECATE);
		assertThat(deprecate).hasSize(1);
		assertProperty(deprecate.get(0).left(), "test.deprecate", String.class, "wrong");
		assertProperty(deprecate.get(0).right(), "test.deprecate", String.class, "wrong");
	}

	private void assertProperty(ConfigurationMetadataProperty property, String id, Class<?> type, Object defaultValue) {
		assertThat(property).isNotNull();
		assertThat(property.getId()).isEqualTo(id);
		assertThat(property.getType()).isEqualTo(type.getName());
		assertThat(property.getDefaultValue()).isEqualTo(defaultValue);
	}

	private ConfigurationMetadataLoader configure(String left, String right) {
		try {
			ConfigurationMetadataLoader loader = mock(ConfigurationMetadataLoader.class);
			given(loader.loadRepository("1.0")).willReturn(load(left));
			given(loader.loadRepository("2.0")).willReturn(load(right));
			return loader;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ConfigurationMetadataRepository load(String path) throws Exception {
		return ConfigurationMetadataRepositoryJsonBuilder.create(new ClassPathResource(path).getInputStream()).build();
	}

}
