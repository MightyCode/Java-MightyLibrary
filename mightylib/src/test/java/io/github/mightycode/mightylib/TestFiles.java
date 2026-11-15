package io.github.mightycode.mightylib;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The files of the tests of the library, read from the classpath (the test resources of the module): independent of
 * the working directory of Maven or of the IDE. A file missing on purpose (missing.ogg) is resolved from the folder of
 * the test files, so it is still missing there.
 */
public final class TestFiles {
    private TestFiles() {}

    /** The file at this path relative to the test resources ("sounds/selectogg.ogg"). */
    public static Path path(String name) {
        URL url = TestFiles.class.getClassLoader().getResource(name);
        return url != null ? toPath(url) : root().resolve(name);
    }

    /** Same as path, as a string for the APIs that take one. */
    public static String file(String name) {
        return path(name).toString();
    }

    // The folder the test resources are copied into: the one of the compiled tests
    private static Path root() {
        return toPath(TestFiles.class.getProtectionDomain().getCodeSource().getLocation());
    }

    private static Path toPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Test file not on the file system: " + url, e);
        }
    }
}
