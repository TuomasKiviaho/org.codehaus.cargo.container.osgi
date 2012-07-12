package org.codehaus.cargo.container.osgi;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.RuntimeSupport;

import org.objenesis.ObjenesisHelper;

public class Proxy
{

    private static class MethodHandlerImpl implements MethodHandler
    {

        private static <T> T proxy(ClassLoader proxyClassLoader, Class< ? extends T> type,
            ClassLoader classLoader, Object target)
        {
            @SuppressWarnings("unchecked")
            T proxy = (T) target;
            if (target != null && !type.isPrimitive())
            {
                Class< ? > targetType = target.getClass();
                if (targetType.isArray())
                {
                    targetType = targetType.getComponentType();
                    Class< ? > componentType = type.getComponentType();
                    @SuppressWarnings("unchecked")
                    T array =
                        (T) (ProxyFactory.isProxyClass(targetType) ? unproxy(proxyClassLoader,
                            componentType, classLoader, (Object[]) target) : targetType
                            .isPrimitive() || type.isInstance(target) ? target : proxy(
                            proxyClassLoader, componentType, classLoader, (Object[]) target));
                    proxy = array;
                }
                else if (ProxyFactory.isProxyClass(targetType))
                {
                    proxy = unproxy(proxyClassLoader, type, classLoader, target);
                }
                else if (!type.isInstance(target))
                {
                    proxy = Proxy.newInstance(proxyClassLoader, type, classLoader, target);
                }
            }
            return proxy;
        }

        private static <T> T[] proxy(ClassLoader proxyClassLoader,
            Class< ? extends T> componentType, ClassLoader classLoader, Object[] target)
        {
            @SuppressWarnings("unchecked")
            T[] proxy = (T[]) Array.newInstance(componentType, target.length);
            for (int i = 0; i < target.length; i++)
            {
                proxy[i] =
                    Proxy.newInstance(proxyClassLoader, componentType, classLoader, target[i]);
            }
            return proxy;
        }

        private static <T> T unproxy(ClassLoader classLoader, Class< ? extends T> type,
            ClassLoader proxyClassLoader, Object proxy)
        {
            MethodHandler methodHandler =
                ProxyFactory.getHandler((javassist.util.proxy.Proxy) proxy);
            @SuppressWarnings("unchecked")
            T target =
                methodHandler instanceof MethodHandlerImpl
                    ? (T) ((MethodHandlerImpl) methodHandler).target : Proxy.newInstance(
                        classLoader, type, proxyClassLoader, proxy);
            return target;
        }

        private static <T> T[] unproxy(ClassLoader classLoader,
            Class< ? extends T> componentType, ClassLoader proxyClassLoader, Object[] proxies)
        {
            @SuppressWarnings("unchecked")
            T[] targets = (T[]) Array.newInstance(componentType, proxies.length);
            for (int i = 0; i < proxies.length; i++)
            {
                targets[i] = unproxy(classLoader, componentType, proxyClassLoader, proxies[i]);
            }
            return targets;
        }

        private ClassLoader classLoader;

        private Object target;

        public MethodHandlerImpl(ClassLoader classLoader, Object target)
        {
            this.classLoader = classLoader;
            this.target = target;
        }

        public Object invoke(Object proxy, Method proxyMethod, Method method, Object[] arguments)
            throws Throwable
        {
            Method targetMethod;
            {
                String descriptor = RuntimeSupport.makeDescriptor(proxyMethod);
                String name = proxyMethod.getName();
                try
                {
                    targetMethod = RuntimeSupport.findMethod(this.target, name, descriptor);
                }
                catch (RuntimeException e)
                {
                    try
                    {
                        targetMethod =
                            RuntimeSupport.findSuperMethod(this.target, name, descriptor);
                    }
                    catch (RuntimeException e2)
                    {
                        throw new AssertionError(e);
                    }
                }
                Class< ? > declaringClass = targetMethod.getDeclaringClass();
                int modifiers = declaringClass.getModifiers();
                if (!Modifier.isPublic(modifiers))
                {
                    targetMethod.setAccessible(true);
                }
            }
            Class< ? > declaringClass = proxyMethod.getDeclaringClass();
            ClassLoader classLoader = declaringClass.getClassLoader();
            Class< ? >[] parameterTypes = targetMethod.getParameterTypes();
            Object[] parameters = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++)
            {
                Class< ? > parameterType = parameterTypes[i];
                Object argument = arguments[i];
                parameters[i] = proxy(this.classLoader, parameterType, classLoader, argument);
            }
            Object result;
            try
            {
                result = targetMethod.invoke(target, parameters);
            }
            catch (IllegalArgumentException e)
            {
                throw new AssertionError(e);
            }
            catch (InvocationTargetException e)
            {
                throw e.getCause();
            }
            Class< ? > returnType = proxyMethod.getReturnType();
            return proxy(classLoader, returnType, this.classLoader, result);
        }
    }

    private static class ProxyFactoryImpl extends ProxyFactory
    {

        private ClassLoader classLoader;

        public ProxyFactoryImpl(ClassLoader classLoader)
        {
            this.classLoader = new ClassLoader(classLoader)
            {

                @Override
                protected Class< ? > findClass(String name) throws ClassNotFoundException
                {
                    Class<javassist.util.proxy.Proxy> proxyClass =
                        javassist.util.proxy.Proxy.class;
                    Package proxyPackage = proxyClass.getPackage();
                    return name.startsWith(proxyPackage.getName()) ? proxyClass.getClassLoader()
                        .loadClass(name) : super.findClass(name);
                }

                @Override
                public int hashCode()
                {
                    ClassLoader parent = this.getParent();
                    return parent == null ? System.identityHashCode(null) : parent.hashCode();
                }

                @Override
                public boolean equals(Object object)
                {
                    boolean equals = this.getClass().isInstance(object);
                    if (equals)
                    {
                        ClassLoader parent1 = this.getParent();
                        ClassLoader parent2 = ((ClassLoader) object).getParent();
                        equals = parent1 == parent2 || parent1 != null && parent1.equals(parent2);
                    }
                    return equals;
                }

            };

        }

        @Override
        protected ClassLoader getClassLoader()
        {
            return this.classLoader;
        }

    }

    public static <T> T newInstance(ClassLoader proxyClassLoader, Class< ? extends T> type,
        ClassLoader classLoader, Object target)
    {
        ProxyFactory proxyFactory = new ProxyFactoryImpl(proxyClassLoader);
        if (type.isInterface())
        {
            Class< ? >[] interfaces = new Class< ? >[] {type};
            proxyFactory.setInterfaces(interfaces);
        }
        else
        {
            proxyFactory.setSuperclass(type);
        }
        Class< ? > proxyClass;
        try
        {
            proxyClass = proxyFactory.createClass();
        }
        catch (RuntimeException e)
        {
            throw new AssertionError(e);
        }
        catch (NoClassDefFoundError e)
        {
            throw new AssertionError(e);
        }
        @SuppressWarnings("unchecked")
        T instance = (T) ObjenesisHelper.newInstance(proxyClass);
        javassist.util.proxy.Proxy proxy = (javassist.util.proxy.Proxy) instance;
        MethodHandler methodHandler = new MethodHandlerImpl(classLoader, target);
        proxy.setHandler(methodHandler);
        return instance;
    }

    public static <T> T newInstance(Class< ? extends T> type, Object target)
    {
        ClassLoader proxyClassLoader = type.getClassLoader();
        Class< ? extends Object> targetClass = target.getClass();
        ClassLoader classLoader = targetClass.getClassLoader();
        return newInstance(proxyClassLoader, type, classLoader, target);
    }

}
