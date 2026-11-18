package io.github.mightycode.mightylibdemo;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The demo uses the library as another project would: no INTERNAL type or package of mightylib (docs/architecture.md,
 * "API publique"). Reads the sources of the demo (imports, static and on demand imports, qualified names) and the
 * statuses of the library by reflection (@API of apiguardian, kept at run time).
 */
class DemoApiUsageTest {
    // From the folder of the module, the working directory of its tests
    private static final Path DEMO_SOURCES = Paths.get("src/main/java/io/github/mightycode/mightylibdemo");
    private static final Pattern QUALIFIED_NAME =
            Pattern.compile("\\bio\\.github\\.mightycode\\.mightylib(?:\\.\\w+)+(?:\\.\\*)?");

    // Not initialized: no static block runs (GL, OpenAL)
    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, DemoApiUsageTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static API.Status declared(AnnotatedElement element) {
        API api = element.getAnnotation(API.class);
        return api == null ? null : api.status();
    }

    private static API.Status packageStatus(String packageName) {
        Class<?> info = load(packageName + ".package-info");
        return info == null ? null : declared(info);
    }

    // The status of a type: its own, else the one of its enclosing type, else its package's
    private static API.Status status(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getEnclosingClass()) {
            API.Status own = declared(current);
            if (own != null)
                return own;
        }
        return packageStatus(type.getPackageName());
    }

    // The library type named by a qualified name, possibly followed by a member (static import): the longest
    // prefix that is a class, nested classes included
    private static Class<?> typeNamed(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        for (int end = parts.length; end > 1; end--) {
            for (int nested = end - 1; nested > 0; nested--) {
                String name = String.join(".", List.of(parts).subList(0, nested))
                        + "." + String.join("$", List.of(parts).subList(nested, end));
                Class<?> type = load(name);
                if (type != null)
                    return type;
            }
        }
        return null;
    }

    private static List<String> internalUses(Path file) throws IOException {
        List<String> uses = new ArrayList<>();
        String source = Files.readString(file);
        Matcher matcher = QUALIFIED_NAME.matcher(source);
        while (matcher.find()) {
            String name = matcher.group();
            if (name.endsWith(".*")) {
                String target = name.substring(0, name.length() - 2);
                Class<?> type = typeNamed(target);
                API.Status status = type != null ? status(type) : packageStatus(target);
                if (status == API.Status.INTERNAL)
                    uses.add(file.getFileName() + ": " + name);
                continue;
            }

            Class<?> type = typeNamed(name);
            if (type != null && Modifier.isPublic(type.getModifiers()) && status(type) == API.Status.INTERNAL)
                uses.add(file.getFileName() + ": " + name);
        }
        return uses;
    }

    @Test
    void demoUsesNoInternalApi() throws Exception {
        assertTrue(Files.isDirectory(DEMO_SOURCES), "Sources of the demo not found: " + DEMO_SOURCES.toAbsolutePath());

        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DEMO_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList()))
                problems.addAll(internalUses(file));
        }

        assertTrue(problems.isEmpty(), "The demo uses INTERNAL types or packages:\n" + String.join("\n", problems));
    }

    @Test
    void internalUsesAreDetected() throws Exception {
        Path file = Files.createTempFile("DemoApiUsageTest", ".java");
        try {
            Files.write(file, List.of(
                    "import io.github.mightycode.mightylib.console.internal.ConsoleLog;",
                    "import static io.github.mightycode.mightylib.graphics.internal.GLContext.current;",
                    "import io.github.mightycode.mightylib.utils.enginecommand.internal.*;",
                    "import io.github.mightycode.mightylib.scenes.Scene;",
                    "class X { io.github.mightycode.mightylib.resources.internal.BatchLoader loader; }"),
                    StandardCharsets.UTF_8);

            List<String> uses = internalUses(file);
            assertEquals(4, uses.size(), uses.toString());
        } finally {
            Files.delete(file);
        }
    }
}
