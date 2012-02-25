package org.codehaus.cargo.container.osgi;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.deployable.DeployableType;

public class OsgiContainerCapability implements ContainerCapability {

	public static final ContainerCapability INSTANCE = new OsgiContainerCapability();
	
	public OsgiContainerCapability() {
		super();
	}

	public boolean supportsDeployableType(DeployableType type) { 
		return DeployableType.BUNDLE.equals(type);
	}

}
