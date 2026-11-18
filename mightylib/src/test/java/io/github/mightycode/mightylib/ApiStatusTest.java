package io.github.mightycode.mightylib;

import io.github.mightycode.mightylib.resources.Resources;
import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the API statuses (docs/architecture.md, "API publique"): every package of mightylib declares its status
 * in its package-info (@API of apiguardian), and a type or a member only declares a status less stable than the one
 * around it. The demo checks on its side that it uses no INTERNAL type or package (DemoApiUsageTest).
 */
class ApiStatusTest {
    private static final String ROOT = "io.github.mightycode.mightylib";

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

    // A package named "internal" is INTERNAL, and every INTERNAL package is named "internal"
    @Test
    void internalPackagesAreExactlyThoseNamedInternal() throws Exception {
        Set<String> packages = new TreeSet<>();
        for (String name : classNames()) {
            if (!name.endsWith("package-info"))
                packages.add(packageOf(name));
        }

        List<String> problems = new ArrayList<>();
        for (String packageName : packages) {
            boolean named = packageName.endsWith(".internal");
            boolean internal = packageStatus(packageName) == API.Status.INTERNAL;
            if (named != internal)
                problems.add(packageName + (named ? ": named internal but not INTERNAL" : ": INTERNAL but not named internal"));
        }

        assertTrue(problems.isEmpty(), "Packages breaking the internal naming rule:\n" + String.join("\n", problems));
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
}
