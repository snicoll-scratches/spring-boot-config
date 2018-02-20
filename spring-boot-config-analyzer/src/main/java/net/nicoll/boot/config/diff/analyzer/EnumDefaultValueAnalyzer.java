package net.nicoll.boot.config.diff.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
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

	private static final List<String> EXCLUDES = Arrays.asList(
			"management.server.ssl.client-auth", // no default
			"server.ssl.client-auth", // no default
			"spring.artemis.mode", // no default
			"spring.cache.type", // no default
			"spring.data.cassandra.consistency-level", // no default
			"spring.data.cassandra.serial-consistency-level", // no default
			"spring.gson.field-naming-policy", // no default
			"spring.gson.long-serialization-policy", // no default
			"spring.jackson.default-property-inclusion", // no default
			"spring.jms.listener.acknowledge-mode", // no default
			"spring.jms.template.delivery-mode", // no default
			"spring.jooq.sql-dialect", // no default
			"spring.jpa.database", // no default
			"spring.kafka.listener.ack-mode", // no default
			"spring.main.web-application-type", // no default
			"spring.mvc.message-codes-resolver-format", // no default
			"spring.rabbitmq.listener.direct.acknowledge-mode", // no default
			"spring.rabbitmq.listener.simple.acknowledge-mode", // no default
			"spring.session.store-type" // no default
	);

	private static final Logger logger = LoggerFactory.getLogger(EnumDefaultValueAnalyzer.class);


	public static void main(String[] args) throws Exception {
		ConfigurationMetadataLoader loader =
				new ConfigurationMetadataLoader(AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.loadRepository("2.0.0.BUILD-SNAPSHOT");
		List<ConfigurationMetadataGroup> groups = MetadataUtils.sortGroups(repo.getAllGroups().values());
		List<ConfigurationMetadataProperty> matchingProperties = new ArrayList<>();
		List<String> excludes = new ArrayList<>(EXCLUDES);
		for (ConfigurationMetadataGroup group : groups) {
			List<ConfigurationMetadataProperty> properties =
					MetadataUtils.sortProperties(group.getProperties().values());
			for (ConfigurationMetadataProperty property : properties) {
				if (property.getDefaultValue() == null && isEnum(property.getType())) {
					if (excludes.contains(property.getId())) {
						excludes.remove(property.getId());
						System.out.println("Validate that " + property.getId()
								+ " has still no default value.");
					}
					else {
						matchingProperties.add(property);
					}
				}
			}
		}
		matchingProperties.sort(Comparator.comparing(ConfigurationMetadataProperty::getId));
		StringBuilder sb = new StringBuilder();
		if (!excludes.isEmpty()) {
			sb.append(NEW_LINE).append(NEW_LINE);
			sb.append("WARNING: excludes list is not up to date. The following "
					+ "properties no longer exist:").append(NEW_LINE);
			for (String exclude : excludes) {
				sb.append("\t").append(exclude).append(NEW_LINE);
			}
		}
		sb.append(NEW_LINE).append(NEW_LINE);
		if (matchingProperties.isEmpty()) {
			sb.append("All other enums have default values");
		}
		else {
			for (ConfigurationMetadataProperty property : matchingProperties) {
				sb.append("  {").append(NEW_LINE);
				sb.append("    \"name\": \"").append(property.getId()).append("\",")
						.append(NEW_LINE).append("    \"defaultValue\": ")
						.append("TODO").append(NEW_LINE).append("  },")
						.append(NEW_LINE);
			}
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
