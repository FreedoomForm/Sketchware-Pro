package com.sketchware.ai.util;

import java.lang.reflect.Method;

/**
 * Reflection helpers for invoking Sketchware-Pro's obfuscated singleton
 * methods (jC.a/b/c/d, lC.b, etc.) without compile-time coupling.
 *
 * <p>This is a safety net: it lets the AI tools invoke singletons whose
 * exact method signatures may vary between Sketchware-Pro versions.
 * Direct method calls would be cleaner but risk compile errors when
 * the obfuscated API shifts.
 */
public final class SketchwareApi {

    private SketchwareApi() {}

    /** Reflectively invoke a static method that returns a singleton, e.g. {@code jC.a(scId)}. */
    public static Object invokeStatic(String className, String methodName, Object... args) {
        try {
            Class<?> cls = Class.forName(className);
            Method m = findMethod(cls, methodName, args);
            if (m == null) throw new NoSuchMethodException(className + "." + methodName);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke " + className + "." + methodName + ": " + t.getMessage(), t);
        }
    }

    /** Reflectively invoke an instance method on a singleton. */
    public static Object invoke(Object instance, String methodName, Object... args) {
        if (instance == null) throw new RuntimeException("Cannot invoke " + methodName + " on null instance");
        try {
            Method m = findMethod(instance.getClass(), methodName, args);
            if (m == null) throw new NoSuchMethodException(instance.getClass().getName() + "." + methodName);
            m.setAccessible(true);
            return m.invoke(instance, args);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke " + instance.getClass().getName() + "." + methodName + ": " + t.getMessage(), t);
        }
    }

    /** Find a method by name + arg count (types are auto-coerced). */
    private static Method findMethod(Class<?> cls, String name, Object[] args) {
        for (Method m : cls.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterTypes().length != args.length) continue;
            // Check arg compatibility
            boolean ok = true;
            for (int i = 0; i < args.length; i++) {
                Class<?> expected = m.getParameterTypes()[i];
                if (args[i] == null) continue;
                Class<?> actual = args[i].getClass();
                if (!expected.isAssignableFrom(actual)
                        && !isAssignable(expected, actual)) {
                    ok = false;
                    break;
                }
            }
            if (ok) return m;
        }
        // Try superclass
        Class<?> sup = cls.getSuperclass();
        if (sup != null) return findMethod(sup, name, args);
        return null;
    }

    private static boolean isAssignable(Class<?> expected, Class<?> actual) {
        if (expected.isPrimitive()) {
            if (expected == int.class) return actual == Integer.class || actual == int.class;
            if (expected == long.class) return actual == Long.class || actual == long.class;
            if (expected == boolean.class) return actual == Boolean.class || actual == boolean.class;
            if (expected == double.class) return actual == Double.class || actual == double.class;
            if (expected == float.class) return actual == Float.class || actual == float.class;
        }
        return expected.isAssignableFrom(actual);
    }
}
