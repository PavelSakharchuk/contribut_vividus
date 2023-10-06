package org.vividus.util;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

/**
 * Hack for updating final static field of external library
 */
public final class FieldUtils
{
    private static Unsafe unsafe;

    static
    {
        try
        {
            final Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new RuntimeException("The 'theUnsafe' field is missed", e);
        }
    }

    private FieldUtils()
    {
    }

    public static void setFinalStatic(Field field, Object value)
    {
        Object fieldBase = unsafe.staticFieldBase(field);
        long fieldOffset = unsafe.staticFieldOffset(field);

        unsafe.putObject(fieldBase, fieldOffset, value);
    }
}
