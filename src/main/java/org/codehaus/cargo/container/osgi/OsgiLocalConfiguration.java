package org.codehaus.cargo.container.osgi;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.spi.configuration.AbstractLocalConfiguration;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class OsgiLocalConfiguration extends AbstractLocalConfiguration {

	private static final ConfigurationCapability CAPABILITY = new OsgiConfigurationCapability();

	public OsgiLocalConfiguration() {
		super(null);
	}

	public ConfigurationCapability getCapability() {
		return CAPABILITY;
	}

	public ConfigurationType getType() {
		return ConfigurationType.RUNTIME;
	}

	@Override
	protected void doConfigure(LocalContainer container)
			throws BundleException, SecurityException {
		OsgiEmbeddedLocalContainer embeddedLocalContainer = (OsgiEmbeddedLocalContainer) container;
		Framework framework = embeddedLocalContainer.getFramework();
		framework.init();
	}

}
