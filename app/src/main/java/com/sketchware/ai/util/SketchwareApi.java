package com.sketchware.ai.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        Class<?> cls;
        try {
            cls = Class.forName(className);
        } catch (Throwable t) {
            throw new RuntimeException("SketchwareApi.invokeStatic: class not found: "
                    + className + " (method " + methodName + ", " + describeArgs(args) + ")", t);
        }
        Method m = findMethod(cls, methodName, args);
        if (m == null) {
            throw new RuntimeException("SketchwareApi.invokeStatic: no static method " + methodName
                    + "(" + describeArgs(args) + ") on " + className
                    + ". Declared methods with matching name: " + listMatchingMethods(cls, methodName));
        }
        try {
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            throw new RuntimeException("SketchwareApi.invokeStatic: " + className + "." + methodName
                    + "(" + describeArgs(args) + ") threw: "
                    + cause.getClass().getSimpleName()
                    + (cause.getMessage() != null ? ": " + cause.getMessage() : ""), cause);
        }
    }

    /** Reflectively invoke an instance method on a singleton. */
    public static Object invoke(Object instance, String methodName, Object... args) {
        if (instance == null) {
            throw new RuntimeException("SketchwareApi.invoke: cannot invoke " + methodName
                    + "(" + describeArgs(args) + ") on null instance");
        }
        Class<?> cls = instance.getClass();
        Method m = findMethod(cls, methodName, args);
        if (m == null) {
            throw new RuntimeException("SketchwareApi.invoke: no method " + methodName
                    + "(" + describeArgs(args) + ") on " + cls.getName()
                    + ". Declared methods with matching name: " + listMatchingMethods(cls, methodName));
        }
        try {
            m.setAccessible(true);
            return m.invoke(instance, args);
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            throw new RuntimeException("SketchwareApi.invoke: " + cls.getName() + "." + methodName
                    + "(" + describeArgs(args) + ") threw: "
                    + cause.getClass().getSimpleName()
                    + (cause.getMessage() != null ? ": " + cause.getMessage() : ""), cause);
        }
    }

    /**
     * Reflectively read a field from an object. Tries the getter
     * {@code getX()} first (with the capitalised field name), then falls back
     * to direct field access (searching superclasses).
     */
    public static Object readField(Object bean, String fieldName) {
        if (bean == null) return null;
        // Try getter first.
        if (fieldName != null && !fieldName.isEmpty()) {
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            try {
                Method m = findMethod(bean.getClass(), getter);
                if (m != null) {
                    m.setAccessible(true);
                    return m.invoke(bean);
                }
            } catch (Throwable ignored) {
                // Fall through to direct field access.
            }
        }
        // Direct field access (search superclasses).
        Class<?> cls = bean.getClass();
        while (cls != null) {
            try {
                java.lang.reflect.Field f = cls.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(bean);
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    /** Find a method by name + arg count (types are auto-coerced). */
    private static Method findMethod(Class<?> cls, String name, Object[] args) {
        if (args == null) args = new Object[0];
        // Search declared methods (and superclass chain).
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != args.length) continue;
                if (argsCompatible(m.getParameterTypes(), args)) return m;
            }
        }
        // Search interface methods (a public method declared on an interface
        // might not appear in getDeclaredMethods of a subclass).
        for (Class<?> i : collectInterfaces(cls)) {
            for (Method m : i.getMethods()) {
                if (!m.getName().equals(name)) continue;
                if (m.getParameterTypes().length != args.length) continue;
                if (argsCompatible(m.getParameterTypes(), args)) return m;
            }
        }
        return null;
    }

    /** Convenience overload for findMethod with no args. */
    private static Method findMethod(Class<?> cls, String name) {
        return findMethod(cls, name, new Object[0]);
    }

    private static List<Class<?>> collectInterfaces(Class<?> cls) {
        List<Class<?>> result = new ArrayList<>();
        java.util.Set<Class<?>> seen = new java.util.HashSet<>();
        while (cls != null) {
            for (Class<?> i : cls.getInterfaces()) {
                if (seen.add(i)) {
                    result.add(i);
                    collectInterfacesRecursive(i, seen, result);
                }
            }
            cls = cls.getSuperclass();
        }
        return result;
    }

    private static void collectInterfacesRecursive(Class<?> i, java.util.Set<Class<?>> seen, List<Class<?>> out) {
        for (Class<?> sub : i.getInterfaces()) {
            if (seen.add(sub)) {
                out.add(sub);
                collectInterfacesRecursive(sub, seen, out);
            }
        }
    }

    private static boolean argsCompatible(Class<?>[] expected, Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) continue;
            Class<?> actual = args[i].getClass();
            if (!expected[i].isAssignableFrom(actual) && !isAssignable(expected[i], actual)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAssignable(Class<?> expected, Class<?> actual) {
        if (expected.isPrimitive()) {
            if (expected == int.class) return actual == Integer.class;
            if (expected == long.class) return actual == Long.class;
            if (expected == boolean.class) return actual == Boolean.class;
            if (expected == double.class) return actual == Double.class;
            if (expected == float.class) return actual == Float.class;
            if (expected == short.class) return actual == Short.class;
            if (expected == byte.class) return actual == Byte.class;
            if (expected == char.class) return actual == Character.class;
        }
        return expected.isAssignableFrom(actual);
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(args[i] == null ? "null" : args[i].getClass().getSimpleName());
        }
        return sb.toString();
    }

    private static String listMatchingMethods(Class<?> cls, String name) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name)) continue;
                if (count > 0) sb.append(", ");
                sb.append(m.getName()).append("(")
                  .append(Arrays.toString(m.getParameterTypes())).append(")");
                count++;
                if (count >= 5) break;
            }
            if (count >= 5) break;
        }
        if (count == 0) sb.append("(none)");
        return sb.toString();
    }
}
