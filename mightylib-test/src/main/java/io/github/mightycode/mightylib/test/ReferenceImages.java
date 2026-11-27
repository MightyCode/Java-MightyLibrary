package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The images a test compares its captures to: one series per driver ({@link RendererKey}), read on the classpath of
 * the test at {@code references/<key>/<name>.png} (so in {@code src/test/resources/references/<key>/}).
 * <ul>
 * <li>No series for this key: the test is skipped, or fails with {@value #REQUIRE_PROPERTY}=true (the CI), the
 * capture written.</li>
 * <li>A series without the image: the test fails (a reference not generated), the capture written.</li>
 * <li>A difference beyond the tolerance: the test fails, and {@code <name>.actual.png}, {@code .expected.png} and
 * {@code .diff.png} are written in {@code target/references/<key>/} (from the working folder of the tests).</li>
 * <li>{@value #UPDATE_PROPERTY}=true: each capture becomes the reference of the local series, written under
 * {@code src/test/resources/references/} (or {@value #FOLDER_PROPERTY}), and the test passes. Read with git diff.</li>
 * </ul>
 */
public final class ReferenceImages {
    public static final String UPDATE_PROPERTY = "mightylib.test.updateReferences";
    public static final String REQUIRE_PROPERTY = "mightylib.test.requireReferences";
    public static final String FOLDER_PROPERTY = "mightylib.test.referencesFolder";

    static final String RESOURCES = "references/";
    static final Path DEFAULT_FOLDER = Paths.get("src", "test", "resources", "references");
    static final Path FAILURE_FOLDER = Paths.get("target", "references");
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final String UPDATE_COMMAND = "python tools/references.py --update";

    private final String key;
    // A resource of the classpath (references/<key>/...), null if absent
    private final Function<String, URL> classpath;
    // Where the update writes the series (the sources of the test resources)
    private final Path sourceFolder;
    private final Path failureFolder;
    private final boolean update;
    private final boolean require;

    ReferenceImages(String key, Function<String, URL> classpath, Path sourceFolder, Path failureFolder,
                    boolean update, boolean require) {
        this.key = key;
        this.classpath = classpath;
        this.sourceFolder = sourceFolder;
        this.failureFolder = failureFolder;
        this.update = update;
        this.require = require;
    }

    // The references of this machine, as the properties ask (main thread: the key is read from the driver)
    private static ReferenceImages fromProperties() {
        String folder = System.getProperty(FOLDER_PROPERTY);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return new ReferenceImages(RendererKey.current(), loader::getResource,
                folder == null || folder.trim().isEmpty() ? DEFAULT_FOLDER : Paths.get(folder.trim()),
                FAILURE_FOLDER, Boolean.getBoolean(UPDATE_PROPERTY), Boolean.getBoolean(REQUIRE_PROPERTY));
    }

    /** Within STRICT. */
    public static void assertMatches(CapturedImage image, String name) {
        assertMatches(image, name, ImageTolerance.STRICT);
    }

    /**
     * Compares the image to the reference of this name in the series of the machine (main thread).
     * @param name letters, digits, dots, dashes and underscores, as {@code ReferenceSprites2D-zoom}
     * @throws AssertionError if the image differs or has no reference in an existing series
     * @throws org.opentest4j.TestAbortedException if the machine has no series (unless required)
     */
    public static void assertMatches(CapturedImage image, String name, ImageTolerance tolerance) {
        fromProperties().check(image, name, tolerance);
    }

    String key() {
        return key;
    }

    void check(CapturedImage image, String name, ImageTolerance tolerance) {
        if (name == null || !NAME.matcher(name).matches())
            throw new IllegalArgumentException("A reference name has letters, digits, '.', '-' and '_': " + name);
        tolerance.requireReason();

        if (update) {
            update(image, name, tolerance);
            return;
        }

        String reference = key + "/" + name + ".png";
        if (classpath.apply(RESOURCES + key + "/") == null) {
            String message = "No reference images for the driver " + key + " (" + RESOURCES + key + "/ on the"
                    + " classpath): generate them with " + UPDATE_COMMAND + ", or give the key of a series with -D"
                    + RendererKey.PROPERTY;
            // The CI: the captures are kept to start the series from them (tools/references.py --from-ci)
            if (require)
                throw new AssertionError(message + "; the capture is in " + write(image, name + ".actual.png"));
            Assumptions.assumeTrue(false, message);
        }

        CapturedImage expected = readResource(RESOURCES + reference);
        if (expected == null) {
            Path actual = write(image, name + ".actual.png");
            throw new AssertionError("No reference " + reference + " in the series " + key + " (not generated?):"
                    + " the capture is in " + actual + "; generate it with " + UPDATE_COMMAND);
        }

        ImageComparison comparison = ImageComparison.compare(expected, image, tolerance);
        if (comparison.matches()) {
            deleteFailure(name);
            return;
        }

        write(image, name + ".actual.png");
        write(expected, name + ".expected.png");
        if (comparison.differenceImage() != null)
            write(comparison.differenceImage(), name + ".diff.png");
        else
            deleteIfExists(failureFolder.resolve(key).resolve(name + ".diff.png"));
        throw new AssertionError("The image " + name + " differs from the reference " + reference + ": "
                + comparison.describe() + "; images in " + failureFolder.resolve(key).toAbsolutePath() + " ("
                + name + ".actual.png, .expected.png, .diff.png)");
    }

    // The capture becomes the reference, unless it matches the one there
    private void update(CapturedImage image, String name, ImageTolerance tolerance) {
        Path file = sourceFolder.resolve(key).resolve(name + ".png");
        CapturedImage previous = null;
        if (Files.exists(file)) {
            try {
                previous = CapturedImage.read(file);
            } catch (IOException e) {
                // Not an image any more: replaced
                previous = null;
            }
        }

        String reference = key + "/" + name;
        if (previous == null) {
            writePng(image, file);
            System.out.println("--Reference updated " + reference + " (new)");
            return;
        }

        ImageComparison comparison = ImageComparison.compare(previous, image, tolerance);
        if (comparison.matches()) {
            System.out.println("--Reference unchanged " + reference);
            return;
        }

        writePng(image, file);
        System.out.println("--Reference updated " + reference + " (" + (comparison.isSameSize()
                ? comparison.differentPixels() + " different pixels" : comparison.describe()) + ")");
    }

    private CapturedImage readResource(String resource) {
        URL url = classpath.apply(resource);
        if (url == null)
            return null;

        try (InputStream input = url.openStream()) {
            return CapturedImage.read(input);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read the reference " + resource, e);
        }
    }

    private Path write(CapturedImage image, String fileName) {
        Path file = failureFolder.resolve(key).resolve(fileName);
        writePng(image, file);
        return file.toAbsolutePath();
    }

    private static void writePng(CapturedImage image, Path file) {
        try {
            image.writePng(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't write " + file, e);
        }
    }

    // A test that passes again leaves no image of an old failure
    private void deleteFailure(String name) {
        Path folder = failureFolder.resolve(key);
        for (String suffix : new String[]{".actual.png", ".expected.png", ".diff.png"})
            deleteIfExists(folder.resolve(name + suffix));
    }

    private static void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't delete " + file, e);
        }
    }
}
