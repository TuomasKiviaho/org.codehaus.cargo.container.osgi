package org.codehaus.cargo.container.osgi;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import javassist.util.proxy.RuntimeSupport;

import org.objenesis.ObjenesisHelper;

public class Proxy {

	private static class MethodHandlerImpl implements MethodHandler {

		private static <T> T[] proxy(Class<? extends T> componentType,
				Object[] target) {
			@SuppressWarnings("unchecked")
			T[] proxy = (T[]) Array.newInstance(componentType, target.length);
			for (int i = 0; i < target.length; i++) {
				proxy[i] = Proxy.newInstance(componentType, target[i]);
			}
			return proxy;
		}

		private Object target;

		public MethodHandlerImpl(Object target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method proxyMethod, Method method,
				Object[] arguments) throws Throwable {
			String descriptor = RuntimeSupport.makeDescriptor(proxyMethod);
			String name = proxyMethod.getName();
			Method targetMethod = RuntimeSupport.findMethod(this.target, name,
					descriptor);
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
				if (!(returnType.isPrimitive() || returnType.isInstance(result))) {
					Class<?> componentType = returnType.getComponentType();
					result = componentType == null ? Proxy.newInstance(
							returnType, result) : proxy(componentType,
							(Object[]) result);
				}
			}
			return result;
		}

	}

	public static <T> T newInstance(Class<? extends T> type, Object target) {
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

	public static boolean isProxyClass(Class<?> type) {
		return ProxyFactory.isProxyClass(type);
	}

}