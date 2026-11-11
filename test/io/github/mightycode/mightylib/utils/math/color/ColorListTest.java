package io.github.mightycode.mightylib.utils.math.color;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColorListTest {
    private static List<Method> colorMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method method : ColorList.class.getDeclaredMethods())
            if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == Color4f.class && method.getParameterCount() == 0)
                methods.add(method);
        return methods;
    }

    private static Color4f call(Method method) throws IllegalAccessException, InvocationTargetException {
        return (Color4f) method.invoke(null);
    }

    @Test
    void everyColorIsOpaqueAndInRange() throws Exception {
        List<Method> methods = colorMethods();
        assertTrue(methods.size() >= 24, "the list of colours is found by reflection");

        for (Method method : methods) {
            Color4f color = call(method);
            assertEquals(1f, color.getA(), method.getName());
            for (float channel : new float[]{color.getR(), color.getG(), color.getB()})
                assertTrue(channel >= 0f && channel <= 1f, method.getName());
        }
    }

    @Test
    void everyCallGivesAFreshCopy() throws Exception {
        for (Method method : colorMethods()) {
            Color4f first = call(method);
            float red = first.getR();
            first.setR(red == 0.123f ? 0.5f : 0.123f);

            Color4f second = call(method);
            assertNotSame(first, second, method.getName());
            assertEquals(red, second.getR(), method.getName() + " is not changed by the caller");
        }
    }

    @Test
    void knownValues() {
        assertEquals(new Color4f(1, 1, 1, 1), ColorList.White());
        assertEquals(new Color4f(0, 0, 0, 1), ColorList.Black());
        assertEquals(new Color4f(1, 0, 0, 1), ColorList.Red());
        assertEquals(new Color4f(0, 1, 0, 1), ColorList.Green());
        assertEquals(new Color4f(0, 0, 1, 1), ColorList.Blue());
        assertEquals(new Color4f(0.5f, 0.5f, 0.5f, 1), ColorList.Grey());
    }

    @Test
    void greysGetDarker() {
        assertTrue(ColorList.White().getR() > ColorList.LightLightGrey().getR());
        assertTrue(ColorList.LightLightGrey().getR() > ColorList.LightGrey().getR());
        assertTrue(ColorList.LightGrey().getR() > ColorList.Grey().getR());
        assertTrue(ColorList.Grey().getR() > ColorList.DarkGrey().getR());
        assertTrue(ColorList.DarkGrey().getR() > ColorList.Black().getR());
    }

    @Test
    void theEnumOfBasicColoursIsKept() {
        assertEquals(7, EColor.values().length);
        assertEquals(EColor.Cyan, EColor.valueOf("Cyan"));
    }
}
