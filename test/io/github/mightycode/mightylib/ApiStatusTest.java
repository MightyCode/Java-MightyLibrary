package io.github.mightycode.mightylib;

import io.github.mightycode.mightylib.resources.Resources;
import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the API statuses (docs/architecture.md, "API publique"): every package of mightylib declares its status
 * in its package-info (@API of apiguardian), a type or a member only declares a status less stable than the one
 * around it, and the demo, which uses the library as another project would, uses no INTERNAL type or package.
 */
class ApiStatusTest {
    private static final String ROOT = "io.github.mightycode.mightylib";
    private static final Path DEMO_SOURCES = Paths.get("src/io/github/mightycode/mightylibdemo");
    private static final Pattern QUALIFIED_NAME =
            Pattern.compile("\\bio\\.github\\.mightycode\\.mightylib(?:\\.\\w+)+(?:\\.\\*)?");

    private static Path classesFolder() throws URISyntaxException {
        return Paths.get(Resources.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }

    private static List<String> classNames() throws IOException, URISyntaxException {
        Path folder = classesFolder();
        try (Stream<Path> files = Files.walk(folder.resolve(ROOT.replace('.', '/')))) {
            return files
                    .map(file -> folder.relativize(file).toString().replace('\\', '/'))
                    .filter(name -> name.endsWith(".class"))
                    .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
                    .collect(Collectors.toList());
        }
    }

    // Not initialized: no static block runs (GL, OpenAL)
    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, ApiStatusTest.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static String packageOf(String className) {
        return className.substring(0, className.lastIndexOf('.'));
    }

    // Higher = more stable
    private static int rank(API.Status status) {
        switch (status) {
            case INTERNAL: return 0;
            case DEPRECATED: return 1;
            case EXPERIMENTAL: return 2;
            case MAINTAINED: return 3;
            default: return 4;
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

    @Test
    void everyPackageDeclaresItsStatus() throws Exception {
        Set<String> missing = new TreeSet<>();
        for (String name : classNames()) {
            if (!name.endsWith("package-info") && packageStatus(packageOf(name)) == null)
                missing.add(packageOf(name));
        }

        assertTrue(missing.isEmpty(), "Packages without a package-info annotated with @API:\n"
                + String.join("\n", missing));
    }

    @Test
    void annotationsAreNeverMoreStableThanAroundThem() throws Exception {
        List<String> problems = new ArrayList<>();
        for (String name : classNames()) {
            if (name.endsWith("package-info"))
                continue;
            Class<?> type = load(name);
            if (type == null || type.isSynthetic())
                continue;

            API.Status own = declared(type);
            Class<?> enclosing = type.getEnclosingClass();
            API.Status around = enclosing != null ? status(enclosing) : packageStatus(type.getPackageName());
            if (own != null && around != null && rank(own) > rank(around))
                problems.add(name + ": " + own + " in " + around);

            API.Status typeStatus = status(type);
            List<AnnotatedElement> members = new ArrayList<>();
            members.addAll(List.of(type.getDeclaredMethods()));
            members.addAll(List.of(type.getDeclaredConstructors()));
            members.addAll(List.of(type.getDeclaredFields()));
            for (AnnotatedElement member : members) {
                API.Status memberStatus = declared(member);
                if (memberStatus != null && typeStatus != null && rank(memberStatus) > rank(typeStatus))
                    problems.add(name + "." + member + ": " + memberStatus + " in " + typeStatus);
            }
        }

        assertTrue(problems.isEmpty(), "Statuses more stable than their type or package:\n"
                + String.join("\n", problems));
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
        String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
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
        List<String> problems = new ArrayList<>();
        try (Stream<Path> files = Files.walk(DEMO_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).collect(Collectors.toList()))
                problems.addAll(internalUses(file));
        }

        assertTrue(problems.isEmpty(), "The demo uses INTERNAL types or packages:\n" + String.join("\n", problems));
    }

    @Test
    void internalUsesAreDetected() throws Exception {
        Path file = Files.createTempFile("ApiStatusTest", ".java");
        try {
            Files.write(file, List.of(
                    "import io.github.mightycode.mightylib.console.ConsoleLog;",
                    "import static io.github.mightycode.mightylib.graphics.GLContext.current;",
                    "import io.github.mightycode.mightylib.utils.enginecommand.general.*;",
                    "import io.github.mightycode.mightylib.scenes.Scene;",
                    "class X { io.github.mightycode.mightylib.resources.BatchLoader loader; }"),
                    StandardCharsets.UTF_8);

            List<String> uses = internalUses(file);
            assertEquals(4, uses.size(), uses.toString());
        } finally {
            Files.delete(file);
        }
    }
}
