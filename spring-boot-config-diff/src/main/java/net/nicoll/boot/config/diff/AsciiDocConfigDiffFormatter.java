/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.nicoll.boot.metadata.ConsoleMetadataFormatter;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.util.ObjectUtils;

/**
 * Renders the diff in asciidoc format.
 *
 * @author Stephane Nicoll
 */
public class AsciiDocConfigDiffFormatter extends AbstractConfigDiffFormatter {

	@Override
	public String formatDiff(ConfigDiffResult result) {
		StringBuilder out = new StringBuilder();
		out.append(String.format("Configuration properties change between `%s` and " + "`%s`%n",
				result.getLeftVersion(), result.getRightVersion()));
		out.append(System.lineSeparator());
		out.append(String.format("== Deprecated in `%s`%n", result.getRightVersion()));
		appendDeprecatedProperties(out, result);
		out.append(System.lineSeparator());
		out.append(String.format("== New in `%s`%n", result.getRightVersion()));
		appendAddedProperties(out, result);
		out.append(System.lineSeparator());
		out.append(String.format("== Removed in `%s`%n", result.getRightVersion()));
		appendRemovedProperties(out, result);
		return out.toString();
	}

	private void appendDeprecatedProperties(StringBuilder out, ConfigDiffResult result) {
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> properties = sortProperties(
				result.getPropertiesDiffFor(ConfigDiffType.DEPRECATE), false).stream()
						.filter(this::isDeprecatedInRelease).collect(Collectors.toList());
		if (ObjectUtils.isEmpty(properties)) {
			out.append(String.format("None.%n"));
		}
		else {
			out.append(String.format("|======================%n"));
			out.append(String.format("|Key  |Replacement |Reason%n"));
			properties.forEach(diff -> {
				ConfigurationMetadataProperty property = diff.right();
				appendDeprecatedProperty(out, property);
			});
			out.append(String.format("|======================%n"));
		}
		out.append(String.format("%n%n"));
	}

	private boolean isDeprecatedInRelease(ConfigDiffEntry<ConfigurationMetadataProperty> diff) {
		return diff.right().getDeprecation() != null
				&& Deprecation.Level.ERROR != diff.right().getDeprecation().getLevel();
	}

	private void appendAddedProperties(StringBuilder out, ConfigDiffResult result) {
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> properties = sortProperties(
				result.getPropertiesDiffFor(ConfigDiffType.ADD), false);
		if (ObjectUtils.isEmpty(properties)) {
			out.append(String.format("None.%n"));
		}
		else {
			out.append(String.format("|======================%n"));
			out.append(String.format("|Key  |Default value |Description%n"));
			properties.forEach(diff -> appendRegularProperty(out, diff.right()));
			out.append(String.format("|======================%n"));
		}
		out.append(String.format("%n%n"));
	}

	private void appendRemovedProperties(StringBuilder out, ConfigDiffResult result) {
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> properties = getRemovedProperties(result);
		if (ObjectUtils.isEmpty(properties)) {
			out.append(String.format("None.%n"));
		}
		else {
			out.append(String.format("|======================%n"));
			out.append(String.format("|Key  |Replacement |Reason%n"));
			properties.forEach(diff -> {
				if (diff.right() != null) {
					appendDeprecatedProperty(out, diff.right());
				}
				else {
					appendDeprecatedProperty(out, diff.left());
				}
			});
			out.append(String.format("|======================%n"));
		}
	}

	private List<ConfigDiffEntry<ConfigurationMetadataProperty>> getRemovedProperties(ConfigDiffResult result) {
		List<ConfigDiffEntry<ConfigurationMetadataProperty>> properties = new ArrayList<>(
				result.getPropertiesDiffFor(ConfigDiffType.DELETE));
		properties.addAll(result.getPropertiesDiffFor(ConfigDiffType.DEPRECATE).stream()
				.filter(p -> !isDeprecatedInRelease(p)).toList());
		return sortProperties(properties, null);
	}

	private void appendRegularProperty(StringBuilder out, ConfigurationMetadataProperty property) {
		out.append("|`").append(property.getId()).append("` |");
		if (property.getDefaultValue() != null) {
			out.append("`").append(ConsoleMetadataFormatter.defaultValueToString(property.getDefaultValue()))
					.append("`");
		}
		out.append(" |");
		if (property.getDescription() != null) {
			out.append(property.getShortDescription());
		}
		out.append(System.lineSeparator());
	}

	private void appendDeprecatedProperty(StringBuilder out, ConfigurationMetadataProperty property) {
		Deprecation deprecation = (property.getDeprecation() != null) ? property.getDeprecation() : new Deprecation();
		out.append("|`").append(property.getId()).append("` |");
		if (deprecation.getReplacement() != null) {
			out.append("`").append(deprecation.getReplacement()).append("`");
		}
		out.append(" |");
		if (deprecation.getReason() != null) {
			out.append(SentenceExtractor.getFirstSentence(deprecation.getReason()));
		}
		out.append(System.lineSeparator());
	}

}
