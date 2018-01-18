package net.nicoll.boot.config.validator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * An {@link ApplicationListener} that inspects the {@link ConfigurableEnvironment
 * environment} for configuration keys that are no longer managed by Spring Boot.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ConfigurationValidatorListener
		implements ApplicationListener<ApplicationPreparedEvent> {

	private static final Log logger = LogFactory.getLog(ConfigurationValidatorListener.class);

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		ConfigurationMetadataRepository repository = loadRepository();
		ConfigurableEnvironment environment =
				event.getApplicationContext().getEnvironment();
		ConfigurationPropertiesAnalyzer validator = new ConfigurationPropertiesAnalyzer(
				repository, environment);
		String report = validator.createMatchingPropertiesReport(p ->
				p.getDeprecation() != null
						&& p.getDeprecation().getLevel() == Deprecation.Level.ERROR);
		if (report != null) {
			logger.error(report);
		}
		else {
			logger.info("No error found in the environment");
		}
	}

	private ConfigurationMetadataRepository loadRepository() {
		try {
			ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
			for (InputStream inputStream : getResources()) {
				builder.withJsonResource(inputStream);
			}
			return builder.build();
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load metadata", ex);
		}
	}

	private List<InputStream> getResources() throws IOException {
		Resource[] resources = new PathMatchingResourcePatternResolver()
				.getResources("classpath*:/META-INF/spring-configuration-metadata.json");
		List<InputStream> result = new ArrayList<>();
		for (Resource resource : resources) {
			result.add(resource.getInputStream());
		}
		return result;
	}

}