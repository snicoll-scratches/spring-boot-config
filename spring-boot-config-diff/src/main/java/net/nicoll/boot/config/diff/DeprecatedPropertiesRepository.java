/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Dumps error keys for a given version, making sure they're actually reporting a key that
 * exists in the previous version.
 *
 * @author Stephane Nicoll
 */
public class DeprecatedPropertiesRepository {

	public static void main(String[] args) throws Exception {
		String previous = "1.5.9.RELEASE";
		String current = "2.0.0.BUILD-SNAPSHOT";

		AetherDependencyResolver dependencyResolver = AetherDependencyResolver.withAllRepositories();
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(dependencyResolver);
		ConfigurationMetadataRepository repository = loader.loadRepository(current);
		ConfigurationMetadataRepository previousRepository = loader.loadRepository(previous);

		StringBuilder sb = new StringBuilder();
		List<String> keys = repository.getAllProperties()
			.entrySet()
			.stream()
			.filter(e -> e.getValue().isDeprecated()
					&& Deprecation.Level.ERROR == e.getValue().getDeprecation().getLevel())
			.map(Map.Entry::getKey)
			.sorted()
			.toList();
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

		System.out.println(sb);
	}

}
