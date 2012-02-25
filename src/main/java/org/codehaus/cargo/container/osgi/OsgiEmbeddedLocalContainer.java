package org.codehaus.cargo.container.osgi;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import javassist.util.proxy.RuntimeSupport;

import org.codehaus.cargo.container.ContainerCapability;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.spi.AbstractEmbeddedLocalContainer;
import org.objenesis.ObjenesisHelper;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OsgiEmbeddedLocalContainer extends AbstractEmbeddedLocalContainer {

	private static class MethodHandlerImpl implements MethodHandler {

		private static <T> T[] proxy(Class<? extends T> componentType,
				Object[] target) {
			@SuppressWarnings("unchecked")
			T[] proxy = (T[]) Array.newInstance(componentType, target.length);
			for (int i = 0; i < target.length; i++) {
				proxy[i] = OsgiEmbeddedLocalContainer.proxy(componentType,
						target[i]);
			}
			return proxy;
		}

		private Object target;

		public MethodHandlerImpl(Object target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method proxyMethod, Method method,
				Object[] arguments) throws Throwable {
			Method targetMethod = method;
			if (targetMethod == null) {
				String descriptor = RuntimeSupport.makeDescriptor(proxyMethod);
				String name = proxyMethod.getName();
				targetMethod = RuntimeSupport.findMethod(this.target, name,
						descriptor);
			}
			Class<?> declaringClass = targetMethod.getDeclaringClass();
			int modifiers = declaringClass.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				targetMethod.setAccessible(true);
			}
			Object result;
			try {
				result = targetMethod.invoke(target, arguments);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
			if (result != null) {
				Class<?> returnType = proxyMethod.getReturnType();
				if (!returnType.isInstance(result)) {
					Class<?> componentType = returnType.getComponentType();
					result = componentType == null ? OsgiEmbeddedLocalContainer
							.proxy(returnType, result) : proxy(componentType,
							(Object[]) result);
				}
			}
			return result;
		}

	}

	private static <T> T proxy(Class<? extends T> type, Object target) {
		ProxyFactory proxyFactory = new ProxyFactory();
		if (type.isInterface()) {
			Class<?>[] interfaces = new Class<?>[] { type };
			proxyFactory.setInterfaces(interfaces);
		} else {
			proxyFactory.setSuperclass(type);
		}
		Class<?> proxyClass = proxyFactory.createClass();
		ProxyObject proxyObject = (ProxyObject) ObjenesisHelper
				.newInstance(proxyClass);
		MethodHandler methodHandler = new MethodHandlerImpl(target);
		proxyObject.setHandler(methodHandler);
		@SuppressWarnings("unchecked")
		T proxy = (T) proxyObject;
		return proxy;
	}

	private Framework framework;

	public OsgiEmbeddedLocalContainer(LocalConfiguration localConfiguration) {
		super(localConfiguration);
	}

	@Override
	public void setClassLoader(ClassLoader classLoader) {
		super.setClassLoader(classLoader);
		LocalConfiguration configuration = this.getConfiguration();
		Map<String, String> properties = configuration.getProperties();
		try {
			Class<?> service = Class.forName(FrameworkFactory.class.getName(),
					true, classLoader);
			ServiceLoader<?> frameworkFactories = ServiceLoader.load(service,
					classLoader);
			Iterator<?> iterator = frameworkFactories.iterator();
			FrameworkFactory frameworkFactory = proxy(FrameworkFactory.class,
					iterator.next());
			framework = frameworkFactory.newFramework(properties);
		} catch (NoSuchElementException e) {
			throw new ContainerException(FrameworkFactory.class.getName(), e);
		} catch (ClassNotFoundException e) {
			throw new ContainerException(FrameworkFactory.class.getName(), e);
		}
	}

	public Framework getFramework() {
		return framework;
	}

	public String getId() {
		return OsgiFactoryRegistry.CONTAINER_ID;
	}

	public String getName() {
		Dictionary<?, ?> headers = framework.getHeaders();
		String name = (String) headers.get(Constants.BUNDLE_NAME);
		return name;
	}

	public ContainerCapability getCapability() {
		return OsgiContainerCapability.INSTANCE;
	}

	@Override
	protected void doStart() throws BundleException, SecurityException {
		framework.start();
	}

	@Override
	protected void doStop() throws BundleException, SecurityException,
			InterruptedException {
		framework.stop();
		long timeout = this.getTimeout();
		framework.waitForStop(timeout);
	}

}
