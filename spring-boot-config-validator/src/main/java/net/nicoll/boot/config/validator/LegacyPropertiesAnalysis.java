package net.nicoll.boot.config.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Describes the outcome of the environment analysis.
 *
 * @author Stephane Nicoll
 */
class LegacyPropertiesAnalysis {

	private final Map<String, PropertySourceAnalysis> content = new LinkedHashMap<>();

	public boolean isEmpty() {
		for (Map.Entry<String, PropertySourceAnalysis> entry : this.content.entrySet()) {
			if (!entry.getValue().handledProperties.isEmpty()) {
				return false;
			}
			if (!entry.getValue().notHandledProperties.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	public Set<String> getPropertyNames() {
		return Collections.unmodifiableSet(content.keySet());
	}

	public List<LegacyProperty> getHandledProperties(String propertySourceName) {
		PropertySourceAnalysis entry = this.content.get(propertySourceName);
		return (entry != null ? entry.handledProperties : null);
	}

	public List<LegacyProperty> getNotHandledProperties(String propertySourceName) {
		PropertySourceAnalysis entry = this.content.get(propertySourceName);
		return (entry != null ? entry.notHandledProperties : null);
	}

	void register(String name, List<LegacyProperty> handledProperties,
			List<LegacyProperty> notHandledProperties) {
		List<LegacyProperty> handled = (handledProperties != null
				? new ArrayList<>(handledProperties) : Collections.emptyList());
		List<LegacyProperty> notHandled = (notHandledProperties != null
				? new ArrayList<>(notHandledProperties) : Collections.emptyList());
		this.content.put(name, new PropertySourceAnalysis(handled, notHandled));
	}

	private static class PropertySourceAnalysis {

		private final List<LegacyProperty> handledProperties;

		private final List<LegacyProperty> notHandledProperties;

		public PropertySourceAnalysis(List<LegacyProperty> handledProperties,
				List<LegacyProperty> notHandledProperties) {
			this.handledProperties = handledProperties;
			this.notHandledProperties = notHandledProperties;
		}

	}
}
