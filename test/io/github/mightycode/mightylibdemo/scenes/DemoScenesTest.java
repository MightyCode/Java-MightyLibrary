package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylibdemo.scenes.Platformer2D.PlatformerBox2D;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// Every scene of the demo is registered (DemoScenes), under the identifier the scripts and the docs use
class DemoScenesTest {
    // The concrete scenes of the sources of the demo, loading screens excepted (reflection is fine in a test)
    private static Set<Class<?>> sceneClasses() throws IOException {
        Path root = Paths.get(SceneHistory.SCENES_DIRECTORY);
        String scenesPackage = DemoScenes.class.getPackageName();
        Set<Class<?>> scenes = new HashSet<>();
        List<Path> sources;
        try (Stream<Path> files = Files.walk(root)) {
            sources = files.filter(file -> file.toString().endsWith(".java")).collect(Collectors.toList());
        }

        for (Path source : sources) {
            String relative = root.relativize(source).toString().replace('\\', '/');
            String className = scenesPackage + "." + relative.substring(0, relative.length() - ".java".length())
                    .replace('/', '.');
            Class<?> type;
            try {
                type = Class.forName(className, false, DemoScenesTest.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new AssertionError("No class for " + source, e);
            }

            if (Scene.class.isAssignableFrom(type) && !TemplateSceneLoading.class.isAssignableFrom(type)
                    && !Modifier.isAbstract(type.getModifiers()))
                scenes.add(type);
        }

        return scenes;
    }

    @Test
    void everySceneOfTheDemoIsRegistered() throws IOException {
        Set<Class<?>> registered = DemoScenes.ALL.stream().map(DemoScenes.Entry::type).collect(Collectors.toSet());
        Set<String> missing = new TreeSet<>();
        for (Class<?> type : sceneClasses()) {
            if (!registered.contains(type))
                missing.add(type.getSimpleName());
        }

        assertTrue(missing.isEmpty(), "Scenes to add to DemoScenes: " + missing);
        assertEquals(DemoScenes.ALL.size(), registered.size(), "A scene is listed twice in DemoScenes");
    }

    // The relative class name in lower case: "scene Scene2D" and "scene Platformer2D.PlatformerBox2D" still work
    @Test
    void identifierIsTheRelativeClassName() {
        assertEquals("demo:scene2d", DemoScenes.IdOf(Scene2D.class).toString());
        assertEquals("demo:platformer2d.platformerbox2d",
                DemoScenes.IdOf(PlatformerBox2D.class).toString());
        assertEquals("Platformer2D.PlatformerBox2D",
                DemoScenes.RelativeName(PlatformerBox2D.class));
        assertEquals(String.class.getName(), DemoScenes.RelativeName(String.class));
    }

    @Test
    void everySceneHasADescription() {
        for (DemoScenes.Entry<?> entry : DemoScenes.ALL)
            assertFalse(entry.description().isEmpty(), entry.type().getName());
    }
}
