package net.nicoll.boot.config.diff;

import java.util.List;

import net.nicoll.boot.config.diff.support.AetherDependencyResolver;
import net.nicoll.boot.config.diff.support.ConfigurationMetadataRepositoryLoader;
import net.nicoll.boot.metadata.MetadataUtils;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.util.StringUtils;

public class AppendixGenerator {

	static final String NEW_LINE = System.getProperty("line.separator");


	public static void main(String[] args) throws Exception {
		ConfigurationMetadataRepositoryLoader loader =
				new ConfigurationMetadataRepositoryLoader(AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.load("1.3.0.BUILD-SNAPSHOT");

		List<ConfigurationMetadataGroup> groups = MetadataUtils.sortGroups(repo.getAllGroups().values());
		StringBuilder sb = new StringBuilder();
		for (ConfigurationMetadataGroup group : groups) {
			sb.append("# ").append(group.getId()).append(NEW_LINE);
			List<ConfigurationMetadataProperty> properties =
					MetadataUtils.sortProperties(group.getProperties().values());
			for (ConfigurationMetadataProperty property : properties) {
				sb.append(property.getId()).append("=");
				if (property.getDefaultValue() != null) {
					sb.append(defaultValueToString(property.getDefaultValue()));
				}
				sb.append(" # ").append(cleanDescription(property.getShortDescription()))
						.append(NEW_LINE);
			}
			sb.append(NEW_LINE);
		}

		System.out.println(sb.toString());


	}

	private static String defaultValueToString(Object defaultValue) {
		if (defaultValue instanceof Object[]) {
			return StringUtils.arrayToCommaDelimitedString((Object[]) defaultValue);
		}
		else {
			return defaultValue.toString();
		}
	}

	private static String cleanDescription(String description) {
		if (description == null) {
			return "";
		}
		description = Character.toLowerCase(description.charAt(0)) + description.substring(1);
		if (description.endsWith(".")) {
			return description.substring(0, description.length() -1);
		}
		return description;
	}
}
