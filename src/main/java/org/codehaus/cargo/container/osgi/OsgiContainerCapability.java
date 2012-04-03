package org.codehaus.cargo.container.osgi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.deployable.DeployableType;

public class OsgiContainerCapability
    implements ContainerCapability
{

    public static final ContainerCapability INSTANCE = new OsgiContainerCapability();

    private Set<DeployableType> deployableTypes;

    public OsgiContainerCapability()
    {
        this.deployableTypes =
            new HashSet<DeployableType>( Arrays.asList( DeployableType.BUNDLE, DeployableType.toType( "jar" ) ) );
    }

    public boolean supportsDeployableType( DeployableType deployableType )
    {
        return this.deployableTypes.contains( deployableType );
    }

}
