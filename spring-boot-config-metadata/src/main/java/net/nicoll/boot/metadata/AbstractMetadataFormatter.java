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

package net.nicoll.boot.metadata;

import java.util.Collection;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractMetadataFormatter {

	protected static final String NEW_LINE = System.getProperty("line.separator");

	protected List<ConfigurationMetadataGroup> sortGroups(Collection<ConfigurationMetadataGroup> groups) {
		return MetadataUtils.sortGroups(groups);
	}

	protected List<ConfigurationMetadataProperty> sortProperties(Collection<ConfigurationMetadataProperty> properties) {
		return MetadataUtils.sortProperties(properties);
	}

}
