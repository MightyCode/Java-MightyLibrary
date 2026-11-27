package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

// The series of reference images in a temporary folder: missing, required, image missing, difference, update
class ReferenceImagesTest {
    private static final String KEY = "linux-mesa-llvmpipe-intel";

    @TempDir
    Path folder;
    // The classpath of the test (the copy of the resources), the sources, the failures
    private Path classpath;
    private Path sources;
    private Path failures;

    @BeforeEach
    void folders() {
        classpath = folder.resolve("classes");
        sources = folder.resolve("sources");
        failures = folder.resolve("failures");
    }

    private ReferenceImages references(boolean update, boolean require) {
        return new ReferenceImages(KEY, resource -> {
            Path file = classpath.resolve(resource);
            try {
                return Files.exists(file) ? file.toUri().toURL() : null;
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }, sources, failures, update, require);
    }

    private static CapturedImage filled(int rgb) {
        int[] pixels = new int[8 * 4];
        Arrays.fill(pixels, rgb);
        return CapturedImage.of(8, 4, pixels);
    }

    // A reference of the series on the classpath
    private void reference(String name, CapturedImage image) throws IOException {
        image.writePng(classpath.resolve(ReferenceImages.RESOURCES + KEY).resolve(name + ".png"));
    }

    private Path failure(String file) {
        return failures.resolve(KEY).resolve(file);
    }

    @Test
    void noSeriesSkipsTheTest() {
        TestAbortedException e = assertThrows(TestAbortedException.class,
                () -> references(false, false).check(filled(0x336699), "Scene-default", ImageTolerance.STRICT));
        assertTrue(e.getMessage().contains(KEY) && e.getMessage().contains("references.py --update"),
                e.getMessage());
    }

    @Test
    void noSeriesFailsWhenTheReferencesAreRequired() throws IOException {
        AssertionError e = assertThrows(AssertionError.class,
                () -> references(false, true).check(filled(0x336699), "Scene-default", ImageTolerance.STRICT));
        assertTrue(e.getMessage().contains(KEY), e.getMessage());
        // Kept to start the series from the CI
        assertEquals(0x336699, CapturedImage.read(failure("Scene-default.actual.png")).rgb(0, 0));
    }

    @Test
    void noSeriesWritesNothingWhenSkipped() {
        assertThrows(TestAbortedException.class,
                () -> references(false, false).check(filled(0x336699), "Scene-default", ImageTolerance.STRICT));
        assertFalse(Files.exists(failure("Scene-default.actual.png")));
    }

    @Test
    void anImageMissingInTheSeriesFailsWithTheCapture() throws IOException {
        reference("Other", filled(0));
        AssertionError e = assertThrows(AssertionError.class,
                () -> references(false, false).check(filled(0x336699), "Scene-default", ImageTolerance.STRICT));
        assertTrue(e.getMessage().contains("Scene-default"), e.getMessage());
        assertEquals(0x336699, CapturedImage.read(failure("Scene-default.actual.png")).rgb(0, 0));
    }

    @Test
    void theSameImagePasses() throws IOException {
        reference("Scene-default", filled(0x336699));
        // Within STRICT: 2 levels
        references(false, true).check(filled(0x346799), "Scene-default", ImageTolerance.STRICT);
        assertFalse(Files.exists(failure("Scene-default.actual.png")));
    }

    @Test
    void aDifferenceFailsWithTheThreeImages() throws IOException {
        reference("Scene-default", filled(0x336699));
        CapturedImage changed = filled(0x336699);
        int[] pixels = new int[8 * 4];
        for (int y = 0; y < 4; ++y) {
            for (int x = 0; x < 8; ++x)
                pixels[x + y * 8] = x == 5 && y == 2 ? 0xFFFFFF : changed.rgb(x, y);
        }
        CapturedImage actual = CapturedImage.of(8, 4, pixels);

        AssertionError e = assertThrows(AssertionError.class,
                () -> references(false, false).check(actual, "Scene-default", ImageTolerance.STRICT));
        assertTrue(e.getMessage().contains("1 different pixels") && e.getMessage().contains("5,2 1x1"),
                e.getMessage());
        assertTrue(e.getMessage().contains(failures.resolve(KEY).toAbsolutePath().toString()), e.getMessage());
        assertEquals(0xFFFFFF, CapturedImage.read(failure("Scene-default.actual.png")).rgb(5, 2));
        assertEquals(0x336699, CapturedImage.read(failure("Scene-default.expected.png")).rgb(5, 2));
        assertEquals(0xFF0000, CapturedImage.read(failure("Scene-default.diff.png")).rgb(5, 2));

        // Fixed: the images of the failure are deleted
        references(false, false).check(filled(0x336699), "Scene-default", ImageTolerance.STRICT);
        assertFalse(Files.exists(failure("Scene-default.diff.png")));
    }

    @Test
    void anUpdateWritesTheReferenceAndPasses() throws IOException {
        String log = printed(() -> references(true, true).check(filled(0x336699), "Scene-default",
                ImageTolerance.STRICT));
        Path written = sources.resolve(KEY).resolve("Scene-default.png");
        assertEquals(0x336699, CapturedImage.read(written).rgb(3, 3));
        assertTrue(log.contains("--Reference updated " + KEY + "/Scene-default (new)"), log);

        // Within the tolerance: kept as it is
        long modified = Files.getLastModifiedTime(written).toMillis();
        log = printed(() -> references(true, true).check(filled(0x346799), "Scene-default", ImageTolerance.STRICT));
        assertTrue(log.contains("--Reference unchanged " + KEY + "/Scene-default"), log);
        assertEquals(0x336699, CapturedImage.read(written).rgb(3, 3));
        assertEquals(modified, Files.getLastModifiedTime(written).toMillis());

        // Changed: replaced, the number of pixels said
        log = printed(() -> references(true, true).check(filled(0x000000), "Scene-default", ImageTolerance.STRICT));
        assertTrue(log.contains("--Reference updated " + KEY + "/Scene-default (32 different pixels)"), log);
        assertEquals(0, CapturedImage.read(written).rgb(3, 3));
    }

    @Test
    void aBadNameOrAToleranceWithoutReasonIsRefused() throws IOException {
        reference("Scene-default", filled(0x336699));
        assertThrows(IllegalArgumentException.class,
                () -> references(false, false).check(filled(0), "../Scene", ImageTolerance.STRICT));
        assertThrows(IllegalArgumentException.class, () -> references(false, false).check(filled(0x336699),
                "Scene-default", ImageTolerance.STRICT.withDifferentPixels(3)));
    }

    // The entry of the tests reads its properties: key, folder of the update, update, references required
    @Test
    void thePublicEntryFollowsTheProperties() throws IOException {
        String key = "kit-properties-test";
        String[] properties = {RendererKey.PROPERTY, ReferenceImages.FOLDER_PROPERTY, ReferenceImages.UPDATE_PROPERTY,
                ReferenceImages.REQUIRE_PROPERTY};
        String[] previous = new String[properties.length];
        for (int i = 0; i < properties.length; ++i)
            previous[i] = System.getProperty(properties[i]);
        Path failure = ReferenceImages.FAILURE_FOLDER.resolve(key);

        try {
            System.setProperty(RendererKey.PROPERTY, key);
            System.setProperty(ReferenceImages.FOLDER_PROPERTY, sources.toString());
            System.setProperty(ReferenceImages.UPDATE_PROPERTY, "true");
            // The CI gives it to every test
            System.setProperty(ReferenceImages.REQUIRE_PROPERTY, "false");
            printed(() -> ReferenceImages.assertMatches(filled(0x336699), "Scene-default"));
            assertEquals(0x336699, CapturedImage.read(sources.resolve(key).resolve("Scene-default.png")).rgb(0, 0));

            // No series of this key on the classpath: skipped, or failed when required
            System.setProperty(ReferenceImages.UPDATE_PROPERTY, "false");
            assertThrows(TestAbortedException.class,
                    () -> ReferenceImages.assertMatches(filled(0x336699), "Scene-default"));
            System.setProperty(ReferenceImages.REQUIRE_PROPERTY, "true");
            AssertionError e = assertThrows(AssertionError.class,
                    () -> ReferenceImages.assertMatches(filled(0x336699), "Scene-default"));
            assertTrue(e.getMessage().contains(key), e.getMessage());
            assertTrue(Files.exists(failure.resolve("Scene-default.actual.png")));
        } finally {
            for (int i = 0; i < properties.length; ++i) {
                if (previous[i] == null)
                    System.clearProperty(properties[i]);
                else
                    System.setProperty(properties[i], previous[i]);
            }
            Files.deleteIfExists(failure.resolve("Scene-default.actual.png"));
            Files.deleteIfExists(failure);
        }
    }

    private static String printed(Runnable action) {
        PrintStream previous = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(previous);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
