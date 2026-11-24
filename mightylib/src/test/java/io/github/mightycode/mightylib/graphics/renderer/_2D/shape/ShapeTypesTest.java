package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.internal.ShapeTypes;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

// The numbers of the shapes in the common batch program: the same in Java (ShapeTypes) and in GLSL
// (utils/shapeTypes2D.glsl); the value each renderer writes is checked by the pixels (VectorShapeBatchGLTest)
class ShapeTypesTest {
    private static final Pattern DEFINE = Pattern.compile("^\\s*#define\\s+SHAPE_TYPE_(\\w+)\\s+(-?\\d+)\\s*$",
            Pattern.MULTILINE);

    private static Map<String, Integer> glslTypes() throws IOException {
        try (InputStream in = ShapeTypesTest.class.getClassLoader()
                .getResourceAsStream("assets/mightylib/shaders/utils/shapeTypes2D.glsl")) {
            assertNotNull(in, "utils/shapeTypes2D.glsl of the engine");
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Integer> types = new HashMap<>();
            Matcher matcher = DEFINE.matcher(text);
            while (matcher.find())
                assertNull(types.put(matcher.group(1), Integer.parseInt(matcher.group(2))), matcher.group(1));
            return types;
        }
    }

    private static Map<String, Integer> javaTypes() throws IllegalAccessException {
        Map<String, Integer> types = new HashMap<>();
        for (Field field : ShapeTypes.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class)
                types.put(field.getName(), field.getInt(null));
        }
        return types;
    }

    @Test
    void theJavaAndGlslNumbersAreTheSame() throws IOException, IllegalAccessException {
        Map<String, Integer> java = javaTypes();
        assertEquals(8, java.size(), java.toString());
        assertEquals(java, glslTypes());
    }

    @Test
    void theNumbersAreUniqueAndNoneIsZero() throws IllegalAccessException {
        Map<String, Integer> java = javaTypes();
        assertEquals(0, ShapeTypes.NONE);
        assertEquals(java.size(), new HashSet<>(java.values()).size(), java.toString());
    }

    // Each vector shape of the engine of the common program says its kind (the hexagon: the regular polygon); the
    // geometric shapes and the points have their own formats, a shape of a project keeps NONE
    @Test
    void eachShapeOfTheEngineGivesItsKind() throws NoSuchMethodException {
        Class<?>[] shapes = {RoundedRectangleRenderer.class, CircleRenderer.class, EllipseRenderer.class,
                TriangleRenderer.class, ArcRenderer.class, RegularPolygonRenderer.class, HexagonRenderer.class,
                GridRenderer.class};
        for (Class<?> shape : shapes)
            assertEquals(int.class, shape.getDeclaredMethod("shapeType").getReturnType(), shape.getName());

        Method base = VectorShape2DRenderer.class.getDeclaredMethod("shapeType");
        assertFalse(Modifier.isPublic(base.getModifiers()) || Modifier.isProtected(base.getModifiers()),
                "a project can't give itself a kind of the common program");
        for (Class<?> other : new Class<?>[]{PointsRenderer.class, GeometricShape2DRenderer.class})
            assertThrows(NoSuchMethodException.class, () -> other.getDeclaredMethod("shapeType"), other.getName());
    }
}
