/*
 * Copyright 2012-2014 the original author or authors.
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

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;

/**
 * Spike of appendix automatic generation.
 *
 * @author Stephane Nicoll
 */
public class MetadataFormatterSample {

	public static void main(String[] args) throws Exception {
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(
				AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.loadRepository("1.5.9.RELEASE");
		System.out.println(getMetadataFormatter().formatMetadata(repo));
	}

	private static MetadataFormatter getMetadataFormatter() {
		return new ConsoleMetadataFormatter();
		//return new CsvMetadataFormatter();
	}

}
