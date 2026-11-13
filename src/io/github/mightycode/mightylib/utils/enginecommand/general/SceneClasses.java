package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.Logger;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Scenes found on the class path below a package (folder or jar), named relative to it ("Platformer2D.PlatformerBox2D"),
 * the way the command scene accepts them. Searched once per package: classes are read without being initialized.
 */
final class SceneClasses {
    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private SceneClasses() {}

    static List<String> Find(String scenesPackage) {
        if (scenesPackage == null)
            return new ArrayList<>();

        return CACHE.computeIfAbsent(scenesPackage, SceneClasses::search);
    }

    private static List<String> search(String scenesPackage) {
        List<String> classNames = new ArrayList<>();
        String folder = scenesPackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null)
            loader = SceneClasses.class.getClassLoader();

        try {
            Enumeration<URL> urls = loader.getResources(folder);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url.getProtocol().equals("file"))
                    listFolder(Paths.get(url.toURI()), scenesPackage, classNames);
                else if (url.getProtocol().equals("jar"))
                    listJar(((JarURLConnection) url.openConnection()).getJarFile(), folder, classNames);
            }
        } catch (IOException | URISyntaxException | RuntimeException e) {
            Logger.LogException("Scenes of " + scenesPackage + " can't be listed", e);
        }

        List<String> scenes = new ArrayList<>();
        for (String className : classNames) {
            if (isScene(className, loader))
                scenes.add(className.substring(scenesPackage.length() + 1));
        }

        return scenes;
    }

    private static void listFolder(Path root, String scenesPackage, List<String> classNames) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(file -> file.toString().endsWith(".class")).forEach(file -> {
                String relative = root.relativize(file).toString().replace('\\', '/');
                classNames.add(scenesPackage + "." + relative.substring(0, relative.length() - ".class".length()).replace('/', '.'));
            });
        }
    }

    private static void listJar(JarFile jar, String folder, List<String> classNames) {
        for (Enumeration<JarEntry> entries = jar.entries(); entries.hasMoreElements(); ) {
            String name = entries.nextElement().getName();
            if (name.startsWith(folder + "/") && name.endsWith(".class"))
                classNames.add(name.substring(0, name.length() - ".class".length()).replace('/', '.'));
        }
    }

    private static boolean isScene(String className, ClassLoader loader) {
        // Inner and anonymous classes are not created by the command
        if (className.contains("$"))
            return false;

        try {
            Class<?> type = Class.forName(className, false, loader);
            return Scene.class.isAssignableFrom(type) && !Modifier.isAbstract(type.getModifiers());
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
