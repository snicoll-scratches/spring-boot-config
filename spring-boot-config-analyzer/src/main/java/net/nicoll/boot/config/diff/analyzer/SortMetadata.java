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
		return StreamSupport.stream(
				Spliterators.spliteratorUnknownSize(elements.elements(), Spliterator.ORDERED), false)
				.sorted(Comparator.comparing(o -> o.get("name").asText())).collect(Collectors.toList());
	}

}
