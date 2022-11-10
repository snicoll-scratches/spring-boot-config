/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nicoll.boot.config.diff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * @author Stephane Nicoll
 */
public class ConfigDiffGenerator {

	private static final Log logger = LogFactory.getLog(ConfigDiffGenerator.class);

	private final ConfigurationMetadataLoader loader;

	protected ConfigDiffGenerator(ConfigurationMetadataLoader loader) {
		this.loader = loader;
	}

	public ConfigDiffGenerator(AetherDependencyResolver dependencyResolver) {
		this(new ConfigurationMetadataLoader(dependencyResolver));
	}

	public ConfigDiffResult generateDiff(String leftVersion, String rightVersion) throws IOException {
		ConfigurationMetadataRepository left = loader.loadRepository(leftVersion);
		ConfigurationMetadataRepository right = loader.loadRepository(rightVersion);

		ConfigDiffResult result = new ConfigDiffResult(leftVersion, rightVersion);
		diffGroup(result, left, right).diffItem(result, left, right);
		return result;
	}

	protected ConfigDiffGenerator diffGroup(ConfigDiffResult result, ConfigurationMetadataRepository left,
			ConfigurationMetadataRepository right) {
		List<String> matches = new ArrayList<>();
		Map<String, ConfigurationMetadataGroup> leftGroups = left.getAllGroups();
		Map<String, ConfigurationMetadataGroup> rightGroups = right.getAllGroups();
		for (ConfigurationMetadataGroup leftGroup : leftGroups.values()) {
			String id = leftGroup.getId();
			ConfigurationMetadataGroup rightGroup = rightGroups.get(id);
			if (rightGroup == null) {
				result.register(ConfigDiffType.DELETE, leftGroup, null);
			}
			else {
				matches.add(id);
				ConfigDiffType diffType = (equals(leftGroup, rightGroup) ? ConfigDiffType.EQUALS
						: ConfigDiffType.MODIFY);
				result.register(diffType, leftGroup, rightGroup);
			}
		}
		for (ConfigurationMetadataGroup rightGroup : rightGroups.values()) {
			if (!matches.contains(rightGroup.getId())) {
				result.register(ConfigDiffType.ADD, null, rightGroup);
			}
		}
		return this;
	}

	protected ConfigDiffGenerator diffItem(ConfigDiffResult result, ConfigurationMetadataRepository left,
			ConfigurationMetadataRepository right) {
		List<String> matches = new ArrayList<>();
		Map<String, ConfigurationMetadataProperty> leftProperties = left.getAllProperties();
		Map<String, ConfigurationMetadataProperty> rightProperties = right.getAllProperties();
		for (ConfigurationMetadataProperty leftProperty : leftProperties.values()) {
			String id = leftProperty.getId();
			ConfigurationMetadataProperty rightProperty = rightProperties.get(id);
			if (rightProperty == null) {
				matches.add(id);
				if (leftProperty.isDeprecated() && leftProperty.getDeprecation().getLevel() == Level.ERROR) {
					logger.info("Ignoring removal of ERROR deprecated property: " + id);
				}
				else {
					result.register(ConfigDiffType.DELETE, leftProperty, null);
				}
			}
			else if (rightProperty.isDeprecated() && !leftProperty.isDeprecated()) {
				matches.add(id);
				result.register(ConfigDiffType.DEPRECATE, leftProperty, rightProperty);
			}
			else if (leftProperty.isDeprecated() && leftProperty.getDeprecation().getLevel() == Level.WARNING
					&& rightProperty.isDeprecated() && rightProperty.getDeprecation().getLevel() == Level.ERROR) {
				matches.add(id);
				result.register(ConfigDiffType.DELETE, leftProperty, rightProperty);
			}
			else {
				matches.add(id);
				ConfigDiffType diffType = (equals(leftProperty, rightProperty) ? ConfigDiffType.EQUALS
						: ConfigDiffType.MODIFY);
				result.register(diffType, leftProperty, rightProperty);
			}
		}
		for (ConfigurationMetadataProperty rightProperty : rightProperties.values()) {
			if (!matches.contains(rightProperty.getId())) {
				if (rightProperty.isDeprecated()) {
					logger.info("Ignoring late addition of deprecated property: " + rightProperty.getId());
				}
				else {
					result.register(ConfigDiffType.ADD, null, rightProperty);
				}
			}
		}
		return this;
	}

	private boolean equals(ConfigurationMetadataGroup left, ConfigurationMetadataGroup right) {
		if (left.getProperties().size() != right.getProperties().size()) {
			return false;
		}
		for (ConfigurationMetadataProperty property : left.getProperties().values()) {
			if (!right.getProperties().containsKey(property.getId())) {
				return false;
			}
		}
		return true;
	}

	private boolean equals(ConfigurationMetadataProperty left, ConfigurationMetadataProperty right) {
		return true; // TODO
	}

}
