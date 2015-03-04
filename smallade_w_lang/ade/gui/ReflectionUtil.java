/**
 * Agent Development Environment (ADE)
 *
 * @version 1.0
 * @author Matthias Scheutz
 * 
 * Copyright 1997-2013 Matthias Scheutz
 * All rights reserved. Do not copy and use without permission.
 * For questions contact Matthias Scheutz at mscheutz@gmail.com
 */
package ade.gui;

import ade.ADEGlobals;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReflectionUtil {

    public static Method findMethod(String methodName, Object[] args, Method[] availableMethods)
            throws NoSuchMethodException {
        Class<?>[] remoteParamTypes = ADEGlobals.getClassArrayNonPrimitive(args);

        for (Method eachMethod : availableMethods) {
            if (methodMatches(eachMethod, methodName, remoteParamTypes)) {
                return eachMethod;
            }
        }

        throw new NoSuchMethodException("Could not find the appropriate method, "
                + "even after converting primitives and following a widening convention");
    }

    private static boolean methodMatches(Method method, String desiredMethodName, Class<?>[] remoteParamTypes) {
        if (!method.getName().equals(desiredMethodName)) {
            return false;
        }

        Class<?>[] expectedTypes = ADEGlobals.getClassArrayNonPrimitive(method.getParameterTypes());

        return arrayTypesAreTheSame(remoteParamTypes, expectedTypes);
    }

    private static boolean arrayTypesAreTheSame(Class<?>[] argTypes, Class<?>[] expectedTypes) {
        if (argTypes.length != expectedTypes.length) {
            return false;
        }

        for (int i = 0; i < argTypes.length; i++) {
            // "widen" parameters
            if (!expectedTypes[i].isAssignableFrom(argTypes[i])) {
                return false;
            }
        }

        // if arrays are of same length, arg types are assignable from expected types:
        return true;
    }

    public static Constructor<?> findConstructor(Class<?> classType, Object[] allParams)
            throws NoSuchMethodException {
        Class<?>[] argTypes = ADEGlobals.getClassArrayNonPrimitive(allParams);

        Constructor<?>[] possibleConstructors = classType.getConstructors();
        for (Constructor<?> eachConstructor : possibleConstructors) {
            if (constructorMatches(eachConstructor, argTypes)) {
                return eachConstructor;
            }
        }

        throw new NoSuchMethodException("Could not find the appropriate constructor, "
                + "even after converting primitives and following a widening convention");
    }

    private static boolean constructorMatches(Constructor<?> eachConstructor,
            Class<?>[] argTypes) {
        Class<?>[] constructorTypes = ADEGlobals.getClassArrayNonPrimitive(eachConstructor.getParameterTypes());
        return arrayTypesAreTheSame(argTypes, constructorTypes);
    }
}
