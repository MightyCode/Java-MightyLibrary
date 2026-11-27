package io.github.mightycode.mightylib.test;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// The rules of the public API of the engine, for the kit: EXPERIMENTAL package, final classes, no public field
// but constants
class KitApiTest {
    private static List<Class<?>> kitClasses() throws IOException, URISyntaxException, ClassNotFoundException {
        Path folder = Paths.get(MightyTest.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve(MightyTest.class.getPackage().getName().replace('.', '/'));
        List<String> names;
        try (Stream<Path> files = Files.list(folder)) {
            names = files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".class") && !name.equals("package-info.class"))
                    .map(name -> name.substring(0, name.length() - ".class".length()))
                    .collect(Collectors.toList());
        }

        List<Class<?>> classes = new ArrayList<>();
        for (String name : names)
            classes.add(Class.forName(MightyTest.class.getPackage().getName() + "." + name));
        return classes;
    }

    @Test
    void thePackageIsExperimental() {
        API api = MightyTest.class.getPackage().getAnnotation(API.class);
        assertNotNull(api);
        assertEquals(API.Status.EXPERIMENTAL, api.status());
    }

    // A public type added to the kit is a change of its API: add it here on purpose
    @Test
    void thePublicTypesAreTheKnownOnes() throws Exception {
        List<String> names = kitClasses().stream()
                .filter(type -> Modifier.isPublic(type.getModifiers()) && !type.isMemberClass())
                .map(Class::getSimpleName).sorted().collect(Collectors.toList());
        assertEquals(List.of("CapturedImage", "EmptyTestScene", "GLUnavailableException", "ImageComparison",
                "ImageTolerance", "MightyEngineExtension", "MightyEngineTest", "MightySceneExtension",
                "MightySceneTest", "MightyTest", "ReferenceImages", "RendererKey", "SceneRunner", "TestEngine"),
                names);
    }

    @Test
    void thePublicClassesAreFinalWithoutPublicField() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Class<?> type : kitClasses()) {
            if (!Modifier.isPublic(type.getModifiers()) || type.isInterface() || type.isAnnotation())
                continue;
            if (!Modifier.isFinal(type.getModifiers()))
                problems.add(type.getSimpleName() + " is not final");
            for (Field field : type.getDeclaredFields()) {
                // Constants (property names) are part of the API
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()))
                    continue;
                if (Modifier.isPublic(field.getModifiers()) || Modifier.isProtected(field.getModifiers()))
                    problems.add(type.getSimpleName() + "." + field.getName() + " is visible");
            }
        }

        assertEquals(List.of(), problems);
    }
}
