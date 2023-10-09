package org.vividus.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Hack for updating final static field of external library
 */
public final class FieldUtils2
{
    private static final VarHandle MODIFIERS;

    static
    {
        try {
            var lookup = MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup());
            MODIFIERS = lookup.findVarHandle(Field.class, "modifiers", int.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    private FieldUtils2()
    {
    }

    public static void setFinalStatic(Field field, Object value)
    {
//        var emptyElementDataField = ArrayList.class.getDeclaredField("EMPTY_ELEMENTDATA");
        // make field non-final
        MODIFIERS.set(field, field.getModifiers() & ~Modifier.FINAL);

        // set field to new value
        try
        {
            field.setAccessible(true);
            field.set(null, value);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }

        //        var list = new ArrayList<>(0);
//
//        // println uses toString(), and ArrayList.toString() indirectly relies on 'size'
//        var sizeField = ArrayList.class.getDeclaredField("size");
//        sizeField.setAccessible(true);
//        sizeField.set(list, 2); // the new "empty element data" has a length of 2
//
//        System.out.println(list);
    }
}
