package org.codehaus.cargo.container.osgi;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.generic.AbstractFactoryRegistry;
import org.codehaus.cargo.generic.ContainerCapabilityFactory;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationCapabilityFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.generic.deployer.DeployerFactory;
import org.codehaus.cargo.generic.packager.PackagerFactory;

public class OsgiFactoryRegistry extends AbstractFactoryRegistry {

	public static final String CONTAINER_ID = "osgi";

	public OsgiFactoryRegistry() {
		super();
	}

	@Override
	protected void register(DeployableFactory deployableFactory) {
		return;
	}

	@Override
	protected void register(
			ConfigurationCapabilityFactory configurationCapabilityFactory) {
		configurationCapabilityFactory.registerConfigurationCapability(
				CONTAINER_ID, ContainerType.EMBEDDED,
				ConfigurationType.RUNTIME, OsgiConfigurationCapability.class);
	}

	@Override
	protected void register(ConfigurationFactory configurationFactory) {
		configurationFactory.registerConfiguration(CONTAINER_ID,
				ContainerType.EMBEDDED, ConfigurationType.RUNTIME,
				OsgiLocalConfiguration.class);
	}

	@Override
	protected void register(DeployerFactory deployerFactory) {
		deployerFactory.registerDeployer(CONTAINER_ID, DeployerType.EMBEDDED,
				OsgiEmbeddedLocalDeployer.class);
	}

	@Override
	protected void register(PackagerFactory packagerFactory) {
		return;
	}

	@Override
	protected void register(ContainerFactory containerFactory) {
		containerFactory.registerContainer(CONTAINER_ID,
				ContainerType.EMBEDDED, OsgiEmbeddedLocalContainer.class);
	}

	@Override
	protected void register(
			ContainerCapabilityFactory containerCapabilityFactory) {
		containerCapabilityFactory.registerContainerCapability(CONTAINER_ID,
				OsgiContainerCapability.class);
	}

}
