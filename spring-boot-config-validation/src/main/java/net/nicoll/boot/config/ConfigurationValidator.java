package net.nicoll.boot.config;

import java.util.Properties;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SpringBootApplication
public class ConfigurationValidator {

	private static final Log logger = LogFactory.getLog(ConfigurationValidator.class);

	public static void main(String[] args) {
		SpringApplication.run(ConfigurationValidator.class, args);
	}

	@Bean
	public ApplicationRunner runner(Properties advertizedProperties,
			ConfigurationMetadataRepository repository) {
		return args -> {
			String report = new ConfigurationAppendixReporter(
					advertizedProperties, repository).getReport();
			logger.info(report);
		};
	}

	@Bean
	public ConfigurationMetadataRepository configurationMetadataRepository() throws Exception {
		ConfigurationMetadataLoader loader =
				new ConfigurationMetadataLoader(AetherDependencyResolver.withAllRepositories());
		return loader.loadRepository("2.0.0.BUILD-SNAPSHOT");
	}

	@Bean
	public PropertiesFactoryBean advertizedProperties() {
		PropertiesFactoryBean factory = new PropertiesFactoryBean();
		factory.setLocation(new PathMatchingResourcePatternResolver()
				.getResource("classpath:advertized.properties"));
		return factory;
	}

}
