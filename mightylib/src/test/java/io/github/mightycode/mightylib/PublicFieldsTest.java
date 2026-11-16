package io.github.mightycode.mightylib;

import io.github.mightycode.mightylib.resources.Resources;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the rule "a class with logic exposes no mutable public field" (docs/bonnes-pratiques.md): no public
 * non-final field, no public static non-final field, no public final collection or map, in the public classes of
 * mightylib (network excepted). Pure data structures (no logic, no invariant) keep their public fields: listed here.
 */
class PublicFieldsTest {
    private static final String ROOT = "io/github/mightycode/mightylib/";
    private static final String EXCLUDED_PACKAGE = ROOT + "network/";

    private static final Set<String> PURE_DATA_STRUCTURES = Set.of(
            "io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo",
            "io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo",
            "io.github.mightycode.mightylib.utils.math.ID",
            "io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent$Inputs",
            "io.github.mightycode.mightylib.scenes.camera.cameraComponents.MovingCameraComponent$Inputs",
            "io.github.mightycode.mightylib.graphics.renderer._2D.shape.EllipseRenderer$ShaderEllipseProperty",
            "io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial",
            "io.github.mightycode.mightylib.graphics.lighting.materials.Material"
    );

    private static Path classesFolder() throws URISyntaxException {
        return Paths.get(Resources.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static List<String> classNames() throws IOException, URISyntaxException {
        Path folder = classesFolder();
        try (Stream<Path> files = Files.walk(folder.resolve(ROOT))) {
            return files
                    .map(file -> folder.relativize(file).toString().replace('\\', '/'))
                    .filter(name -> name.endsWith(".class") && !name.startsWith(EXCLUDED_PACKAGE))
                    .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
                    .collect(Collectors.toList());
        }
    }

    // Reachable from outside the library: the class and all its enclosing classes are public
    private static boolean isPublicApi(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            if (!Modifier.isPublic(current.getModifiers()))
                return false;
        }
        return !type.isAnonymousClass() && !type.isSynthetic();
    }

    private static String problem(Field field) {
        int modifiers = field.getModifiers();
        if (!Modifier.isPublic(modifiers) || field.isSynthetic() || field.isEnumConstant())
            return null;

        if (!Modifier.isFinal(modifiers))
            return Modifier.isStatic(modifiers) ? "public static non-final" : "public non-final";

        if (Collection.class.isAssignableFrom(field.getType()) || Map.class.isAssignableFrom(field.getType()))
            return "public final collection";

        return null;
    }

    @Test
    void noMutablePublicField() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        List<String> problems = new ArrayList<>();
        List<String> names = classNames();
        assertFalse(names.isEmpty(), "no class found in " + classesFolder());

        for (String name : names) {
            if (PURE_DATA_STRUCTURES.contains(name))
                continue;

            // Not initialized: no static block runs (GL, OpenAL)
            Class<?> type = Class.forName(name, false, loader);
            if (!isPublicApi(type))
                continue;

            for (Field field : type.getDeclaredFields()) {
                String problem = problem(field);
                if (problem != null)
                    problems.add(name + "." + field.getName() + " (" + problem + ")");
            }
        }

        assertTrue(problems.isEmpty(), "Mutable public fields (use methods, or list a pure data structure here):\n"
                + String.join("\n", problems));
    }

    @Test
    void listedDataStructuresExist() throws Exception {
        List<String> names = classNames();
        for (String name : PURE_DATA_STRUCTURES)
            assertTrue(names.contains(name), name + " no longer exists: remove it from the list");
    }
}
