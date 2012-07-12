package org.codehaus.cargo.container.osgi;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Dictionary;
import java.util.Map;

import org.codehaus.cargo.container.EmbeddedLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableException;
import org.codehaus.cargo.container.spi.deployer.AbstractEmbeddedLocalDeployer;
import org.codehaus.cargo.util.FileHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;

public class OsgiEmbeddedLocalDeployer<T extends EmbeddedLocalContainer & BundleReference>
    extends AbstractEmbeddedLocalDeployer
{

    private static final String REFERENCE = "reference:";

    private static String getLocation(Deployable deployable)
    {
        String location = deployable.getFile();
        File file = new File(location);
        if (file.exists())
        {
            URI uri = file.toURI();
            location = REFERENCE + uri.toString();
        }
        return location;
    }

    public OsgiEmbeddedLocalDeployer(T embeddedLocalContainer)
    {
        super(embeddedLocalContainer);
        BundleReference bundleReference = this.getContainer();
        Bundle framework = bundleReference.getBundle();
        BundleContext bundleContext = framework.getBundleContext();
        if (bundleContext == null)
        {
            LocalConfiguration configuration = embeddedLocalContainer.getConfiguration();
            configuration.configure(embeddedLocalContainer);
        }
    }

    private Map.Entry<String, InputStream> getInputStream(Deployable deployable)
    {
        String location = getLocation(deployable);
        InputStream inputStream = null;
        if (!location.startsWith(REFERENCE))
        {
            FileHandler fileHandler = this.getFileHandler();
            inputStream = fileHandler.getInputStream(location);
        }
        Map.Entry<String, InputStream> entry =
            new AbstractMap.SimpleImmutableEntry<String, InputStream>(location, inputStream);
        return entry;
    }

    @Override
    protected T getContainer()
    {
        @SuppressWarnings("unchecked")
        T container = (T) super.getContainer();
        return container;
    }

    protected Bundle findBundle(Deployable deployable)
    {
        BundleReference bundleReference = this.getContainer();
        Bundle framework = bundleReference.getBundle();
        BundleContext bundleContext = framework.getBundleContext();
        Bundle[] candidateBundles = bundleContext.getBundles();
        String location = getLocation(deployable);
        Bundle bundle = null;
        for (Bundle candidateBundle : candidateBundles)
        {
            String candidateLocation = candidateBundle.getLocation();
            if (candidateLocation.equals(location))
            {
                bundle = candidateBundle;
                break;
            }
        }
        return bundle;
    }

    @Override
    public void deploy(Deployable deployable)
    {
        T container = this.getContainer();
        ClassLoader classLoader = container.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            Map.Entry<String, InputStream> entry = getInputStream(deployable);
            String location = entry.getKey();
            InputStream inputStream = entry.getValue();
            BundleReference bundleReference = this.getContainer();
            Bundle framework = bundleReference.getBundle();
            BundleContext bundleContext = framework.getBundleContext();
            try
            {
                bundleContext.installBundle(location, inputStream);
            }
            catch (BundleException e)
            {
                throw new DeployableException(deployable.toString(), e);
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void start(Deployable deployable)
    {
        T container = this.getContainer();
        ClassLoader classLoader = container.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            Bundle bundle = findBundle(deployable);
            if (bundle != null)
            {
                Dictionary< ? , ? > headers = bundle.getHeaders();
                String fragmentHost = (String) headers.get(Constants.FRAGMENT_HOST);
                if (fragmentHost == null)
                {
                    try
                    {
                        bundle.start(Bundle.START_ACTIVATION_POLICY);
                    }
                    catch (BundleException e)
                    {
                        throw new DeployableException(deployable.toString(), e);
                    }
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void stop(Deployable deployable)
    {
        T container = this.getContainer();
        ClassLoader classLoader = container.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            Bundle bundle = findBundle(deployable);
            if (bundle != null)
            {
                try
                {
                    Dictionary< ? , ? > headers = bundle.getHeaders();
                    String fragmentHost = (String) headers.get(Constants.FRAGMENT_HOST);
                    if (fragmentHost == null)
                    {
                        bundle.stop();
                    }
                }
                catch (BundleException e)
                {
                    throw new DeployableException(deployable.toString(), e);
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void undeploy(Deployable deployable)
    {
        T container = this.getContainer();
        ClassLoader classLoader = container.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            Bundle bundle = findBundle(deployable);
            if (bundle != null)
            {
                try
                {
                    bundle.uninstall();
                }
                catch (BundleException e)
                {
                    throw new DeployableException(deployable.toString(), e);
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    public void redeploy(Deployable deployable)
    {
        T container = this.getContainer();
        ClassLoader classLoader = container.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            Bundle bundle = findBundle(deployable);
            if (bundle != null)
            {
                Map.Entry<String, InputStream> entry = this.getInputStream(deployable);
                InputStream inputStream = entry.getValue();
                try
                {
                    if (inputStream == null)
                    {
                        bundle.update();
                    }
                    else
                    {
                        bundle.update(inputStream);
                    }
                }
                catch (BundleException e)
                {
                    throw new DeployableException(deployable.toString(), e);
                }
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

}
