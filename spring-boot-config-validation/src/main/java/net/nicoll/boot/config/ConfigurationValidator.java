package net.nicoll.boot.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SpringBootApplication
public class ConfigurationValidator implements CommandLineRunner {

	private static final Log logger = LogFactory.getLog(ConfigurationValidator.class);

	@Autowired
	@Qualifier("advertizedProperties")
	private Properties advertizedProperties;

	@Autowired
	private ConfigurationMetadataRepository configurationMetadataRepository;

	private Map<String, ConfigurationMetadataProperty> items = new HashMap<>();

	private Map<String, ConfigurationMetadataGroup> groups = new HashMap<>();

	@PostConstruct
	public void initialize() {
		this.items = this.configurationMetadataRepository.getAllProperties();
		this.groups = this.configurationMetadataRepository.getAllGroups();
	}


	@Override
	public void run(String... args) throws Exception {
		List<String> found = new ArrayList<>();
		List<String> undocumented = new ArrayList<>();
		List<String> unresolved = new ArrayList<>();
		List<String> deprecated = new ArrayList<>();
		Map<String, List<String>> groups = new LinkedHashMap<>();

		// Generate relax names for all properties
		List<ConfigKeyCandidates> advertized = advertizedProperties.keySet()
				.stream().map(item -> new ConfigKeyCandidates((String) item))
				.collect(Collectors.toList());

		// Check advertized properties
		for (ConfigKeyCandidates propertyItem : advertized) {
			String key = getDocumentedKey(propertyItem);
			if (key != null) {
				found.add(key);
			}
			else {
				unresolved.add(propertyItem.item);
			}
		}

		// Check non advertized properties
		for (String key : this.items.keySet()) {
			if (!found.contains(key)) {
				String value = key;
				ConfigurationMetadataProperty candidate = this.items.get(key);
				Deprecation deprecation = candidate.getDeprecation();
				if (deprecation != null) {
					value += " (deprecated";
					if (deprecation.getReplacement() != null) {
						value += " see " + deprecation.getReplacement();
					}
					value += ")";
					deprecated.add(value);
				}
				else {
					undocumented.add(value);
				}
			}
		}

		// Check all the ".*" properties and match against the undocumented ones
		for (String key : unresolved) {
			if (key.endsWith(".*")) {
				String group = key.substring(0, key.length() - 2);
				List<String> matching = new ArrayList<>();
				undocumented.removeIf(item -> {
					if (item.startsWith(group)) {
						matching.add(item);
						return true;
					}
					return false;
				});
				groups.put(group, matching);
			}
		}
		groups.keySet().forEach(item -> unresolved.remove(item + ".*"));


		StringBuilder sb = new StringBuilder("\n");
		sb.append("Configuration key statistics").append("\n");
		sb.append("Advertized keys: ").append(advertizedProperties.size()).append("\n");
		sb.append("Repository items: ").append(this.items.size()).append("\n");
		sb.append("Matching items: ").append(found.size()).append("\n");
		sb.append("Unresolved items (found in documentation but not in generated metadata): ").append(unresolved.size()).append("\n");
		sb.append("Groups (group defined in the documentation but not each individual elements): ").append(groups.size()).append("\n");
		sb.append("Undocumented items (found in generated metadata but not in documentation): ").append(undocumented.size()).append("\n");
		sb.append("Deprecated items (found in generated metadata but not in documentation): ").append(deprecated.size()).append("\n");
		sb.append("\n");
		sb.append("\n");
		if (!unresolved.isEmpty()) {
			sb.append("Unresolved items").append("\n");
			sb.append("----------------").append("\n");
			Collections.sort(unresolved);
			for (String id : unresolved) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}
		if (!groups.isEmpty()) {
			sb.append("Groups").append("\n");
			sb.append("----------------").append("\n");
			for (Map.Entry<String, List<String>> group : groups.entrySet()) {
				sb.append(group.getKey()).append(" with ")
						.append(group.getValue().size()).append(" elements").append("\n");
			}
			sb.append("\n");
		}
		if (!undocumented.isEmpty()) {
			sb.append("Undocumented items").append("\n");
			sb.append("--------------------").append("\n");
			List<String> ids = new ArrayList<>(undocumented);
			Collections.sort(ids);
			for (String id : ids) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}
		if (!deprecated.isEmpty()) {
			sb.append("Deprecated items").append("\n");
			sb.append("--------------------").append("\n");
			List<String> ids = new ArrayList<>(deprecated);
			Collections.sort(ids);
			for (String id : ids) {
				sb.append(id).append("\n");
			}
			sb.append("\n");
		}

		logger.info(sb.toString());
	}

	private String getDocumentedKey(ConfigKeyCandidates candidates) {
		for (String candidate : candidates) {
			boolean hasKey = this.items.containsKey(candidate);
			if (hasKey) {
				return candidate;
			}
		}
		return null;
	}


	@Bean
	public ConfigurationMetadataRepository configurationMetadataRepository() throws Exception {
		ConfigurationMetadataLoader loader =
				new ConfigurationMetadataLoader(AetherDependencyResolver.withAllRepositories());
		return loader.loadRepository("2.0.0.BUILD-SNAPSHOT");
	}

	@Bean
	public PropertiesFactoryBean advertizedProperties() {
		PropertiesFactoryBean factory = new PropertiesFactoryBean();
		factory.setLocation(new PathMatchingResourcePatternResolver()
				.getResource("classpath:advertized.properties"));
		return factory;
	}

	public static void main(String[] args) {
		SpringApplication.run(ConfigurationValidator.class, args);
	}

	private static class ConfigKeyCandidates implements Iterable<String> {
		private final String item;

		private final Set<String> values;

		private ConfigKeyCandidates(String item) {
			this.item = item;
			this.values = initialize(item);
		}

		@Override
		public Iterator<String> iterator() {
			return this.values.iterator();
		}

		private static Set<String> initialize(String item) {
			String itemToUse = item;
			if (itemToUse.endsWith(".*")) {
				itemToUse = itemToUse.substring(0, itemToUse.length() - 2);
			}

			Set<String> values = new LinkedHashSet<String>();
			int i = itemToUse.lastIndexOf('.');
			if (i == -1) {
				for (String o : new RelaxedNames(itemToUse)) {
					values.add(o);
				}
			}
			else {
				String prefix = itemToUse.substring(0, i + 1);
				String suffix = itemToUse.substring(i + 1, itemToUse.length());
				for (String value : new RelaxedNames(suffix)) {
					values.add(prefix + value);
				}
			}
			return values;
		}
	}

}
