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

package net.nicoll.boot.config.diff.analyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 * Simple utility to order existing metadata according to their names.
 *
 * @author Stephane Nicoll
 */
public class SortMetadata {

	public static void main(String[] args) throws IOException {
		Resource resource = new ClassPathResource("test.json");
		String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.reader().readTree(json);
		sortArray(root, "groups");
		sortArray(root, "properties");
		sortArray(root, "hints");
		System.out.println(root);
	}

	private static void sortArray(JsonNode root, String name) {
		JsonNode node = root.get(name);
		if (node != null) {
			ArrayNode elements = (ArrayNode) node;
			int originalSize = elements.size();
			List<JsonNode> sortedElements = sortByName(elements);
			elements.removeAll();
			elements.addAll(sortedElements);
			if (elements.size() != originalSize) {
				throw new IllegalArgumentException("Oops");
			}
		}
	}

	private static List<JsonNode> sortByName(ArrayNode elements) {
		return StreamSupport
			.stream(Spliterators.spliteratorUnknownSize(elements.elements(), Spliterator.ORDERED), false)
			.sorted(Comparator.comparing(o -> o.get("name").asText()))
			.collect(Collectors.toList());
	}

}
