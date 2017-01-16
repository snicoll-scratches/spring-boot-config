package net.nicoll.boot.config.diff.analyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import net.nicoll.boot.metadata.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Stephane Nicoll
 */
public class EnumDefaultValueAnalyzer {

	private static final String NEW_LINE = System.getProperty("line.separator");

	private static final Logger logger = LoggerFactory.getLogger(EnumDefaultValueAnalyzer.class);


	public static void main(String[] args) throws Exception {
		ConfigurationMetadataLoader loader =
				new ConfigurationMetadataLoader(AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.loadRepository("1.5.0.BUILD-SNAPSHOT");
		List<ConfigurationMetadataGroup> groups = MetadataUtils.sortGroups(repo.getAllGroups().values());
		List<ConfigurationMetadataProperty> matchingProperties = new ArrayList<>();
		for (ConfigurationMetadataGroup group : groups) {
			List<ConfigurationMetadataProperty> properties =
					MetadataUtils.sortProperties(group.getProperties().values());
			for (ConfigurationMetadataProperty property : properties) {
				if (property.getDefaultValue() == null && isEnum(property.getType())) {
					matchingProperties.add(property);
				}
			}
		}
		matchingProperties.sort(Comparator.comparing(ConfigurationMetadataProperty::getId));
		StringBuilder sb = new StringBuilder();
		for (ConfigurationMetadataProperty property : matchingProperties) {
			sb.append("  {").append(NEW_LINE);
			sb.append("    \"name\": \"").append(property.getId()).append(",")
					.append(NEW_LINE).append("    \"defaultValue\": ")
					.append("TODO").append(NEW_LINE).append("  },")
					.append(NEW_LINE);
		}
		System.out.println(sb.toString());

	}

	private static boolean isEnum(String type) {
		if (type == null) {
			return false;
		}
		if (type.startsWith("java.util") || type.startsWith("java.lang")) {
			return false;
		}
		try {
			Class<?> target = ClassUtils.forName(type,
					EnumDefaultValueAnalyzer.class.getClassLoader());
			return target.isEnum();
		}
		catch (ClassNotFoundException ex) {
			logger.info("Type {} not on classpath", type);
		}
		return false;
	}

}
