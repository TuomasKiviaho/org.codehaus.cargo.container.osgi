package org.codehaus.cargo.container.osgi;

import java.io.InputStream;
import java.util.Dictionary;
import java.util.Map;

import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.spi.deployer.AbstractEmbeddedLocalDeployer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class OsgiEmbeddedLocalDeployer
    extends AbstractEmbeddedLocalDeployer
{

    public OsgiEmbeddedLocalDeployer( EmbeddedLocalContainer embeddedLocalContainer )
    {
        super( embeddedLocalContainer );
        OsgiEmbeddedLocalContainer container = this.getContainer();
        Bundle bundle = container.getBundle();
        BundleContext bundleContext = bundle.getBundleContext();
        if ( bundleContext == null )
        {
            LocalConfiguration configuration = embeddedLocalContainer.getConfiguration();
            configuration.configure( embeddedLocalContainer );
        }
    }

    @Override
    protected OsgiEmbeddedLocalContainer getContainer()
    {
        return (OsgiEmbeddedLocalContainer) super.getContainer();
    }

    protected Bundle findBundle( Deployable deployable )
    {
        OsgiEmbeddedLocalContainer container = this.getContainer();
        Bundle framework = container.getBundle();
        BundleContext bundleContext = framework.getBundleContext();
        Bundle[] candidateBundles = bundleContext.getBundles();
        String location = OsgiEmbeddedLocalContainer.getLocation( deployable );
        Bundle bundle = null;
        for ( Bundle candidateBundle : candidateBundles )
        {
            String candidateLocation = candidateBundle.getLocation();
            if ( candidateLocation.equals( location ) )
            {
                bundle = candidateBundle;
                break;
            }
        }
        return bundle;
    }

    @Override
    public void deploy( Deployable deployable )
    {
        OsgiEmbeddedLocalContainer container = this.getContainer();
        try
        {
            container.installBundle( deployable );
        }
        catch ( BundleException e )
        {
            throw new DeployableException( deployable.toString(), e );
        }
    }

    @Override
    public void start( Deployable deployable )
    {
        Bundle bundle = findBundle( deployable );
        if ( bundle != null )
        {
            try
            {
                OsgiEmbeddedLocalContainer.start( bundle );
            }
            catch ( BundleException e )
            {
                throw new DeployableException( deployable.toString(), e );
            }
        }
    }

    @Override
    public void stop( Deployable deployable )
    {
        Bundle bundle = findBundle( deployable );
        if ( bundle != null )
        {
            try
            {
                Dictionary<?, ?> headers = bundle.getHeaders();
                String fragmentHost = (String) headers.get( Constants.FRAGMENT_HOST );
                if ( fragmentHost == null )
                {
                    bundle.stop();
                }
            }
            catch ( BundleException e )
            {
                throw new DeployableException( deployable.toString(), e );
            }
        }
    }

    @Override
    public void undeploy( Deployable deployable )
    {
        Bundle bundle = findBundle( deployable );
        if ( bundle != null )
        {
            try
            {
                bundle.uninstall();
            }
            catch ( BundleException e )
            {
                throw new DeployableException( deployable.toString(), e );
            }
        }
    }

    @Override
    public void redeploy( Deployable deployable )
    {
        Bundle bundle = findBundle( deployable );
        if ( bundle != null )
        {
            OsgiEmbeddedLocalContainer container = this.getContainer();
            Map.Entry<String, InputStream> entry = container.getInputStream( deployable );
            InputStream inputStream = entry.getValue();
            try
            {
                if ( inputStream == null )
                {
                    bundle.update();
                }
                else
                {
                    bundle.update( inputStream );
                }
            }
            catch ( BundleException e )
            {
                throw new DeployableException( deployable.toString(), e );
            }
        }
    }

}
