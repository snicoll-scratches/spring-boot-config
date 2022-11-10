package net.nicoll.boot.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;

/**
 * @author Stephane Nicoll
 */
public abstract class MetadataUtils {

	public static final Comparator<ConfigurationMetadataGroup> GROUP_COMPARATOR = new GroupComparator();

	public static final Comparator<ConfigurationMetadataProperty> PROPERTY_COMPARATOR = new PropertyComparator();

	public static List<ConfigurationMetadataGroup> sortGroups(Collection<ConfigurationMetadataGroup> groups) {
		List<ConfigurationMetadataGroup> result = new ArrayList<>(groups);
		result.sort(GROUP_COMPARATOR);
		return result;
	}

	public static List<ConfigurationMetadataProperty> sortProperties(
			Collection<ConfigurationMetadataProperty> properties) {
		List<ConfigurationMetadataProperty> result = new ArrayList<>(properties);
		result.sort(PROPERTY_COMPARATOR);
		return result;
	}

	private static class GroupComparator implements Comparator<ConfigurationMetadataGroup> {

		@Override
		public int compare(ConfigurationMetadataGroup o1, ConfigurationMetadataGroup o2) {
			if (ConfigurationMetadataRepository.ROOT_GROUP.equals(o1.getId())) {
				return -1;
			}
			if (ConfigurationMetadataRepository.ROOT_GROUP.equals(o2.getId())) {
				return 1;
			}
			return o1.getId().compareTo(o2.getId());
		}

	}

	private static class PropertyComparator implements Comparator<ConfigurationMetadataProperty> {

		@Override
		public int compare(ConfigurationMetadataProperty o1, ConfigurationMetadataProperty o2) {
			return o1.getId().compareTo(o2.getId());
		}

	}

}
