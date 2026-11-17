package io.github.mightycode.mightylib.resources.source;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathSourceTest {
    @TempDir
    Path folder;

    private static String read(IResourceSource source, String path) throws IOException {
        try (InputStream stream = source.open(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> names(List<SourceEntry> entries) {
        return entries.stream().map(SourceEntry::name).collect(Collectors.toList());
    }

    // A jar with its folder entries, as Maven writes it
    private Path writeJar() throws IOException {
        Path jar = folder.resolve("plugin.jar");
        try (OutputStream file = Files.newOutputStream(jar); ZipOutputStream zip = new ZipOutputStream(file)) {
            for (String entry : List.of("assets/", "assets/plugin/", "assets/plugin/sub/")) {
                zip.putNextEntry(new ZipEntry(entry));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("assets/plugin/a.txt"));
            zip.write("jar a".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("assets/plugin/sub/b.txt"));
            zip.write("jar bb".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return jar;
    }

    private static URLClassLoader loaderOf(Path jar) throws IOException {
        return new URLClassLoader(new URL[]{jar.toUri().toURL()}, null);
    }

    @Test
    void folderOfTheClasspath() throws IOException {
        ClasspathSource source = new ClasspathSource(getClass().getClassLoader(), "sources/classpath");

        assertTrue(source.isPresent());
        assertEquals("classpath:sources/classpath/", source.name());
        assertEquals("classpath a", read(source, "a.txt"));
        assertEquals(11, source.size("sub/b.txt"));
        assertEquals(List.of("a.txt", "sub"), names(source.list("")));
        // In the IDE or target/test-classes: on the disk
        assertTrue(source.localFile("a.txt").isPresent());
    }

    @Test
    void jar() throws IOException {
        try (URLClassLoader loader = loaderOf(writeJar())) {
            ClasspathSource source = new ClasspathSource(loader, "assets/plugin/");
            try {
                assertTrue(source.isPresent());
                assertEquals("jar a", read(source, "a.txt"));
                assertEquals(6, source.size("sub/b.txt"));
                assertEquals(-1, source.size("missing.txt"));
                assertEquals(List.of("a.txt", "sub"), names(source.list("")));
                assertTrue(source.list("").get(1).isFolder());
                assertEquals("sub/b.txt", source.list("sub").get(0).path());
                assertTrue(source.isFolder("sub"));
                assertTrue(source.localFile("a.txt").isEmpty());
                assertThrows(IllegalArgumentException.class, () -> source.open("../plugin/a.txt"));
            } finally {
                source.close();
            }
        }
    }

    @Test
    void absentPrefixIsEmpty() throws IOException {
        try (URLClassLoader loader = loaderOf(writeJar())) {
            ClasspathSource source = new ClasspathSource(loader, "assets/other/");

            assertFalse(source.isPresent());
            assertFalse(source.exists("a.txt"));
            assertTrue(source.list("").isEmpty());
            assertEquals(-1, source.size("a.txt"));
            assertTrue(source.localFile("a.txt").isEmpty());
            source.close();
        }
    }

    @Test
    void reusesAnOpenedFileSystemWithoutClosingIt() throws IOException {
        Path jar = writeJar();
        URI uri = URI.create("jar:" + jar.toUri());
        try (FileSystem opened = FileSystems.newFileSystem(uri, Map.of()); URLClassLoader loader = loaderOf(jar)) {
            ClasspathSource source = new ClasspathSource(loader, "assets/plugin");
            assertEquals("jar a", read(source, "a.txt"));

            source.close();
            // Opened by the test: still open
            assertTrue(opened.isOpen());
            assertEquals("jar a", read(source, "a.txt"));
        }
    }

    @Test
    void closesTheFileSystemItOpened() throws IOException {
        Path jar = writeJar();
        try (URLClassLoader loader = loaderOf(jar)) {
            new ClasspathSource(loader, "assets/plugin").close();

            // Closed: a new source opens it again
            ClasspathSource again = new ClasspathSource(loader, "assets/plugin");
            assertEquals("jar a", read(again, "a.txt"));
            again.close();
        }
    }

    @Test
    void needsAPrefix() {
        assertThrows(IllegalArgumentException.class, () -> new ClasspathSource(getClass().getClassLoader(), ""));
        assertThrows(IllegalArgumentException.class,
                () -> new ClasspathSource(getClass().getClassLoader(), "../assets"));
    }
}
