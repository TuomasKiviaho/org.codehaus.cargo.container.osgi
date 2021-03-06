package org.codehaus.cargo.container.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;
import org.codehaus.cargo.util.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OsgiEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer implements
    BundleReference
{

    private Framework framework;

    public OsgiEmbeddedLocalContainer(LocalConfiguration localConfiguration)
    {
        super(localConfiguration);
    }

    @Override
    public void setClassLoader(ClassLoader classLoader)
    {
        super.setClassLoader(classLoader);
        FrameworkFactory frameworkFactory;
        try
        {
            Class< ? > service =
                Class.forName(FrameworkFactory.class.getName(), true, classLoader);
            ServiceLoader< ? > frameworkFactories = ServiceLoader.load(service, classLoader);
            Iterator< ? > iterator = frameworkFactories.iterator();
            ClassLoader containerClassLoader = this.getClass().getClassLoader();
            frameworkFactory =
                Proxy.newInstance(containerClassLoader, FrameworkFactory.class, classLoader,
                    iterator.next());
        }
        catch (NoSuchElementException e)
        {
            throw new ContainerException(FrameworkFactory.class.getName(), e);
        }
        catch (ClassNotFoundException e)
        {
            throw new ContainerException(FrameworkFactory.class.getName(), e);
        }
        LocalConfiguration configuration = this.getConfiguration();
        Map<String, String> configurationProperties = configuration.getProperties();
        Map<String, String> properties = new HashMap<String, String>(configurationProperties);
        for (Iterator<String> iterator = properties.keySet().iterator(); iterator.hasNext();)
        {
            String propertyName = iterator.next();
            if (propertyName.startsWith("cargo."))
            {
                iterator.remove();
            }
        }
        String port = properties.get("org.osgi.service.http.port");
        if (port == null)
        {
            port = configurationProperties.get(ServletPropertySet.PORT);
            properties.put("org.osgi.service.http.port", port);
        }
        else
        {
            configurationProperties.put(ServletPropertySet.PORT, port);
        }
        framework = frameworkFactory.newFramework(properties);
    }

    public Framework getBundle()
    {
        return framework;
    }

    public String getId()
    {
        return OsgiFactoryRegistry.CONTAINER_ID;
    }

    public String getName()
    {
        Dictionary< ? , ? > headers = framework.getHeaders();
        String name = (String) headers.get(Constants.BUNDLE_NAME);
        return name;
    }

    public ContainerCapability getCapability()
    {
        return OsgiContainerCapability.INSTANCE;
    }

    @Override
    protected void doStart() throws BundleException, SecurityException
    {
        ClassLoader classLoader = this.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            framework.start();
            Logger logger = this.getLogger();
            String category = this.getClass().getName();
            logger.info(String.format("%4s|%-11s|%s", "ID", "State", "Level", "Name"), category);
            Map<Integer, String> states = new LinkedHashMap<Integer, String>(6);
            states.put(Bundle.UNINSTALLED, "UNINSTALLED");
            states.put(Bundle.INSTALLED, "INSTALLED");
            states.put(Bundle.RESOLVED, "RESOLVED");
            states.put(Bundle.STARTING, "STARTING");
            states.put(Bundle.STOPPING, "STOPPING");
            states.put(Bundle.ACTIVE, "ACTIVE");
            BundleContext bundleContext = framework.getBundleContext();
            Bundle[] bundles = bundleContext.getBundles();
            for (Bundle bundle : bundles)
            {
                long bundleId = bundle.getBundleId();
                int bundleState = bundle.getState();
                String state = null;
                for (Map.Entry<Integer, String> entry : states.entrySet())
                {
                    int key = entry.getKey();
                    if ((key & bundleState) > 0)
                    {
                        state = entry.getValue();
                        break;
                    }
                }
                String symbolicName = bundle.getSymbolicName();
                Version version = bundle.getVersion();
                String message =
                    String.format("%4d|%-11s|%s (%s)", bundleId, state, symbolicName,
                        version.toString());
                logger.info(message, category);
            }
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
    }

    @Override
    protected void doStop() throws BundleException, SecurityException, InterruptedException
    {
        ClassLoader classLoader = this.getClassLoader();
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try
        {
            framework.stop();
        }
        finally
        {
            thread.setContextClassLoader(contextClassLoader);
        }
        long timeout = this.getTimeout();
        framework.waitForStop(timeout);
    }

}
