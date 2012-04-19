package org.codehaus.cargo.container.osgi;

import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationCapability;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.configuration.AbstractLocalConfiguration;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

public class OsgiLocalConfiguration
    extends AbstractLocalConfiguration
{

    private static final ConfigurationCapability CAPABILITY = new OsgiConfigurationCapability();

    public OsgiLocalConfiguration()
    {
        super( null );
        String port = System.getProperty( "org.osgi.service.http.port" );
        if ( port != null )
        {
            this.setProperty( ServletPropertySet.PORT, port );
        }
    }

    public ConfigurationCapability getCapability()
    {
        return CAPABILITY;
    }

    public ConfigurationType getType()
    {
        return ConfigurationType.RUNTIME;
    }

    @Override
    protected void doConfigure( LocalContainer container )
        throws BundleException, SecurityException
    {
        OsgiEmbeddedLocalContainer embeddedLocalContainer = (OsgiEmbeddedLocalContainer) container;
        Framework framework = embeddedLocalContainer.getBundle();
        framework.init();
    }

}
