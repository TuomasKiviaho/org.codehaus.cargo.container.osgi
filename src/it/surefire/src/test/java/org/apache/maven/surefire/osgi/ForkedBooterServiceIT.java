package org.apache.maven.surefire.osgi;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class ForkedBooterServiceIT extends TestCase
{

    public void test()
    {
        Bundle bundle = FrameworkUtil.getBundle(Booter.class);
        Assert.assertNotNull(FrameworkUtil.class.getSimpleName(), bundle);
        BundleContext bundleContext = bundle.getBundleContext();
        @SuppressWarnings("rawtypes")
        ServiceReference<Class> serviceReference = bundleContext.getServiceReference(Class.class);
        @SuppressWarnings("unchecked")
        Class< ? extends Booter> service = bundleContext.getService(serviceReference);
        Assert.assertNotNull(Booter.class.getName(), service);
    }

}
