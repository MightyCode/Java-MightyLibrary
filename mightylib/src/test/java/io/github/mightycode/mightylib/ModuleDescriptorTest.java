package io.github.mightycode.mightylib;

import org.apiguardian.api.API;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.nio.file.Files;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guard of the module descriptor of mightylib (module-info.java, docs/architecture.md, "API publique"), read from the
 * compiled classes while the tests run on the classpath.
 */
class ModuleDescriptorTest {
    private static ModuleDescriptor descriptor() throws Exception {
        try (InputStream input = Files.newInputStream(ApiStatusTest.classesFolder().resolve("module-info.class"))) {
            return ModuleDescriptor.read(input);
        }
    }

    @Test
    void name() throws Exception {
        assertEquals("io.github.mightycode.mightylib", descriptor().name());
    }

    // Exported, to everyone: exactly the packages that are not INTERNAL
    @Test
    void exportsExactlyThePackagesThatAreNotInternal() throws Exception {
        Set<String> expected = new TreeSet<>();
        for (String name : ApiStatusTest.classNames()) {
            String packageName = ApiStatusTest.packageOf(name);
            if (ApiStatusTest.packageStatus(packageName) != API.Status.INTERNAL)
                expected.add(packageName);
        }

        ModuleDescriptor descriptor = descriptor();
        Set<String> exported = descriptor.exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(expected, exported);
        assertTrue(descriptor.exports().stream().noneMatch(ModuleDescriptor.Exports::isQualified),
                "No qualified export: an internal package stays closed to every other module");
    }

    // The resources (assets/mightylib/) are found without opening any package (see module-info.java)
    @Test
    void opensNothing() throws Exception {
        ModuleDescriptor descriptor = descriptor();
        assertFalse(descriptor.isOpen());
        assertTrue(descriptor.opens().isEmpty(), descriptor.opens().toString());
    }

    // The modules whose types the public API exposes are read by every module that reads the engine
    @Test
    void exposedModulesAreTransitive() throws Exception {
        Set<String> transitive = descriptor().requires().stream()
                .filter(requires -> requires.modifiers().contains(ModuleDescriptor.Requires.Modifier.TRANSITIVE))
                .map(ModuleDescriptor.Requires::name)
                .collect(Collectors.toCollection(TreeSet::new));
        assertEquals(new TreeSet<>(Set.of("java.desktop", "jbox2d.library", "org.joml", "org.json")), transitive);
    }

    // The statuses are annotations only: the module runs without apiguardian
    @Test
    void apiguardianIsOptional() throws Exception {
        ModuleDescriptor.Requires apiguardian = descriptor().requires().stream()
                .filter(requires -> requires.name().equals("org.apiguardian.api"))
                .findFirst()
                .orElseThrow();
        assertTrue(apiguardian.modifiers().contains(ModuleDescriptor.Requires.Modifier.STATIC));
    }
}
