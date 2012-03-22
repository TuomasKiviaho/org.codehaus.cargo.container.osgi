package org.codehaus.cargo.container.osgi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.configuration.AbstractLocalConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class OsgiLocalConfiguration extends AbstractLocalConfiguration {

	private static final ConfigurationCapability CAPABILITY = new OsgiConfigurationCapability();

	public OsgiLocalConfiguration() {
		super(null);
		String port = System.getProperty("org.osgi.service.http.port");
		if (port != null) {
			this.setProperty(ServletPropertySet.PORT, port);
		}
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
		Framework framework = embeddedLocalContainer.getBundle();
		framework.init();
		List<Deployable> deployables = this.getDeployables();
		Map<Long, Deployable> bundles = new LinkedHashMap<Long, Deployable>(
				deployables.size());
		for (Deployable deployable : deployables) {
			try {
				Bundle bundle = embeddedLocalContainer
						.installBundle(deployable);
				long bundleId = bundle.getBundleId();
				bundles.put(bundleId, deployable);
			} catch (BundleException e) {
				throw new DeployableException(deployable.toString(), e);
			}
		}
		BundleContext bundleContext = framework.getBundleContext();
		for (long bundleId : bundles.keySet()) {
			Bundle bundle = bundleContext.getBundle(bundleId);
			try {
				OsgiEmbeddedLocalContainer.start(bundle);
			} catch (BundleException e) {
				Deployable deployable = bundles.get(bundleId);
				throw new DeployableException(deployable.toString(), e);
			}
		}
	}

}
