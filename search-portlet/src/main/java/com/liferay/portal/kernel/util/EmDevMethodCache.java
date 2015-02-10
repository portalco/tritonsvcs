package com.liferay.portal.kernel.util;

import java.lang.reflect.Method;

/**
 * I've created this class to avoid protected restriction for method get
 * 
 * @author akakunin
 * 
 */
public class EmDevMethodCache extends MethodCache {

    public static Method get(Class<?> declaringClass, String methodName,
            Class<?>... parameterTypes)
        throws NoSuchMethodException {
        return get(new MethodKey(declaringClass, methodName, parameterTypes));
    }
}
