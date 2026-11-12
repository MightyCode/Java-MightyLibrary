package io.github.mightycode.mightylib;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the extension contract of the classes meant to be inherited: no protected field, their state is
 * reached through chosen and documented methods (a subclass cannot break their invariants, and the fields can
 * change without breaking the subclasses of other projects). The list grows with the objective
 * "Héritage et API publique" (docs/current.md).
 */
class ExtensionFieldsTest {
    private static final List<String> EXTENSIBLE_CLASSES = List.of(
            "io.github.mightycode.mightylib.graphics.renderer.Mesh",
            "io.github.mightycode.mightylib.graphics.renderer.Renderer",
            "io.github.mightycode.mightylib.graphics.renderer.MeshRenderer",
            "io.github.mightycode.mightylib.graphics.renderer._2D.shape.Shape2DRenderer"
    );

    @Test
    void noProtectedField() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        List<String> problems = new ArrayList<>();

        for (String name : EXTENSIBLE_CLASSES) {
            // Not initialized: no static block runs (GL, OpenAL)
            Class<?> type = Class.forName(name, false, loader);

            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isProtected(field.getModifiers()) && !field.isSynthetic())
                    problems.add(name + "." + field.getName());
            }
        }

        assertTrue(problems.isEmpty(), "Protected fields in extensible classes (use methods):\n"
                + String.join("\n", problems));
    }
}
