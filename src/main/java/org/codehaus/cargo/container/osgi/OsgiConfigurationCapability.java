package org.codehaus.cargo.container.osgi;

import java.util.Map;

import org.codehaus.cargo.container.spi.configuration.AbstractConfigurationCapability;

public class OsgiConfigurationCapability extends AbstractConfigurationCapability {

	public OsgiConfigurationCapability() {
		super();
	}

	@Override
	protected Map<String, Boolean> getPropertySupportMap() {
		return this.defaultSupportsMap;
	}

}
