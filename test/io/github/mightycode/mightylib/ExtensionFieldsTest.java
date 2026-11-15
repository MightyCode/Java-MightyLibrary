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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the extension contract (docs/architecture.md, "API publique"): only the classes chosen to be inherited
 * (ExtensibleClasses) are open, every other public concrete class is final; the open ones (chosen or extended by
 * the library) have no protected field, their state is reached through chosen and documented methods (a subclass cannot break their invariants,
 * and the fields can change without breaking the subclasses of other projects).
 */
class ExtensionFieldsTest {
    private static final String ROOT = "io/github/mightycode/mightylib/";

    // Not initialized: no static block runs (GL, OpenAL)
    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, false, ExtensionFieldsTest.class.getClassLoader());
    }

    private static List<String> classNames() throws IOException, URISyntaxException {
        Path folder = Paths.get(Resources.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (Stream<Path> files = Files.walk(folder.resolve(ROOT))) {
            return files
                    .map(file -> folder.relativize(file).toString().replace('\\', '/'))
                    .filter(name -> name.endsWith(".class") && !name.endsWith("package-info.class"))
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

    @Test
    void noProtectedField() throws Exception {
        List<String> problems = new ArrayList<>();

        // The classes extended by the library too: a project can extend them as well (public constructor)
        for (List<String> list : List.of(ExtensibleClasses.NAMES, ExtensibleClasses.EXTENDED_BY_THE_LIBRARY)) {
            for (String name : list) {
                for (Field field : load(name).getDeclaredFields()) {
                    if (Modifier.isProtected(field.getModifiers()) && !field.isSynthetic())
                        problems.add(name + "." + field.getName());
                }
            }
        }

        assertTrue(problems.isEmpty(), "Protected fields in open classes (use methods):\n"
                + String.join("\n", problems));
    }

    @Test
    void publicConcreteClassesAreFinal() throws Exception {
        Set<String> open = new HashSet<>(ExtensibleClasses.NAMES);
        open.addAll(ExtensibleClasses.EXTENDED_BY_THE_LIBRARY);

        List<String> problems = new ArrayList<>();
        for (String name : classNames()) {
            Class<?> type = load(name);
            int modifiers = type.getModifiers();
            if (!isPublicApi(type) || type.isInterface() || type.isEnum() || type.isAnnotation()
                    || Modifier.isAbstract(modifiers) || Modifier.isFinal(modifiers) || open.contains(name))
                continue;

            problems.add(name);
        }

        assertTrue(problems.isEmpty(), "Public concrete classes neither final nor in ExtensibleClasses:\n"
                + String.join("\n", problems));
    }

    @Test
    void listedClassesExistAndAreOpen() throws Exception {
        List<String> problems = new ArrayList<>();
        for (List<String> list : List.of(ExtensibleClasses.NAMES, ExtensibleClasses.EXTENDED_BY_THE_LIBRARY)) {
            for (String name : list) {
                Class<?> type = load(name);
                if (Modifier.isFinal(type.getModifiers()) || !Modifier.isPublic(type.getModifiers()))
                    problems.add(name);
            }
        }

        assertTrue(problems.isEmpty(), "Listed as open but final or not public:\n" + String.join("\n", problems));
    }
}
