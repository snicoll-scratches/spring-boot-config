package net.nicoll.boot.config.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation;

/**
 * Dumps error keys for a given version, making sure they're actually reporting
 * a key that exists in the previous version.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedPropertiesRepository {

	public static void main(String[] args) throws Exception {
		String previous = "1.5.8.RELEASE";
		String current = "2.0.0.BUILD-SNAPSHOT";

		AetherDependencyResolver dependencyResolver = AetherDependencyResolver
				.withAllRepositories();
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(
				dependencyResolver);
		ConfigurationMetadataRepository repository = loader.loadRepository(current);
		ConfigurationMetadataRepository previousRepository = loader.loadRepository(previous);

		StringBuilder sb = new StringBuilder();
		List<String> keys = repository.getAllProperties().entrySet().stream()
				.filter(e -> e.getValue().isDeprecated()
						&& Deprecation.Level.ERROR == e.getValue().getDeprecation().getLevel())
				.map(Map.Entry::getKey).sorted().collect(Collectors.toList());
		List<String> invalidKeys = new ArrayList<>();
		keys.forEach(k -> {
			sb.append(k);
			ConfigurationMetadataProperty property = repository.getAllProperties().get(k);
			String replacement = property.getDeprecation().getReplacement();
			if (replacement != null) {
				sb.append(" --> ").append(replacement);
			}
			String reason = property.getDeprecation().getReason();
			if (reason != null) {
				sb.append(" - ").append(reason);
			}
			sb.append(String.format("%n"));

			if (previousRepository.getAllProperties().get(k) == null) {
				invalidKeys.add(k);
			}
		});

		if (!invalidKeys.isEmpty()) {
			sb.append(String.format("%nThe following invalid keys were found (unknown in %s)%n", previous));
			invalidKeys.forEach(k -> sb.append(String.format("%s%n", k)));
		}

		System.out.println(sb.toString());
	}

}
