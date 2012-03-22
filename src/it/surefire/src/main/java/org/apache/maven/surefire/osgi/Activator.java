package org.apache.maven.surefire.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

	private ServiceRegistration<?> serviceRegistration;

	public Activator() {
		super();
	}

	public void start(BundleContext bundleContext) throws IOException,
			ClassNotFoundException {
		Dictionary<String, Object> dictionary = new Hashtable<String, Object>(2);
		Class<Booter> type = Booter.class;
		String scope = type.getPackage().getName();
		dictionary.put(CommandProcessor.COMMAND_SCOPE, scope);
		String function = type.getSimpleName();
		dictionary.put(CommandProcessor.COMMAND_FUNCTION, function);
		String className = type
				.getClass().getName();
		this.serviceRegistration = bundleContext.registerService(className, type, dictionary);
	}

	public void stop(BundleContext bundleContext) {
		this.serviceRegistration.unregister();
	}

}
