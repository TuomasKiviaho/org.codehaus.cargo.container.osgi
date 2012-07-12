package org.codehaus.cargo.container.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class ServiceTracker<S, T> extends org.osgi.util.tracker.ServiceTracker<S, T>
{

    private Class<S> objectClass;

    public ServiceTracker(BundleContext bundleContext, final Class<S> objectClass,
        ServiceTrackerCustomizer<S, T> serviceTrackerCustomizer)
    {
        super(bundleContext, objectClass.getName(), serviceTrackerCustomizer);
        this.objectClass = objectClass;
    }

    @Override
    public T addingService(ServiceReference<S> serviceReference)
    {
        T service = super.addingService(serviceReference);
        @SuppressWarnings("unchecked")
        T proxy = (T) Proxy.newInstance(this.objectClass, service);
        return proxy;
    }

}
