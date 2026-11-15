package io.github.mightycode.mightylib.resources.cubemap;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.main.WorkersTestAccess;
import io.github.mightycode.mightylib.resources.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The cubemaps: six faces found by folder, in the order of GL, a missing face named, a fallback. No OpenGL. */
class CubeMapLoaderTest {
    @TempDir
    Path folder;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        WorkersTestAccess.ensureThreads(2);
    }

    // Each face filled with its own gray level (index * 40), size x size
    private void faces(String cubeMap, int size, String... skipped) throws IOException {
        Path dir = folder.resolve(CubeMapLoader.FOLDER).resolve(cubeMap);
        Files.createDirectories(dir);
        List<String> names = CubeMapData.faceNames();
        for (int i = 0; i < names.size(); ++i) {
            if (List.of(skipped).contains(names.get(i)))
                continue;

            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            int gray = i * 40;
            for (int y = 0; y < size; ++y) {
                for (int x = 0; x < size; ++x)
                    image.setRGB(x, y, 0xFF000000 | gray << 16 | gray << 8 | gray);
            }
            ImageIO.write(image, "png", dir.resolve(names.get(i) + ".png").toFile());
        }
    }

    private String path(String cubeMap, String face) {
        return Resources.NormalizeFolder(folder.toString()) + CubeMapLoader.FOLDER + "/" + cubeMap + "/" + face + ".png";
    }

    private Resources load(StringBuilder errors) {
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 2, folder.toString(),
                List.of(new CubeMapLoader()));
        PrintStream previous = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            resources.init();
            resources.load();
        } finally {
            System.setErr(previous);
        }
        errors.append(captured.toString(StandardCharsets.UTF_8));
        return resources;
    }

    @Test
    void theFacesAreFoundInTheOrderOfGL() {
        assertEquals(List.of("right", "left", "top", "bottom", "front", "back"), CubeMapData.faceNames());
    }

    @Test
    void eachFolderIsACubeMapOfSixFaces() throws IOException {
        faces("sky", 4);

        CubeMapData sky = load(new StringBuilder()).getResource(CubeMapData.class, "sky");

        List<String> expected = new ArrayList<>();
        for (String face : CubeMapData.faceNames())
            expected.add(path("sky", face));
        assertEquals(expected, sky.sourcePaths());
        assertTrue(sky.isLoaded());
        assertEquals(4, sky.getFaceSize());
        for (int i = 0; i < CubeMapData.FACE_COUNT; ++i) {
            assertTrue(sky.isSourceRead(i));
            assertEquals((byte) (i * 40), sky.getFace(i).get(0), "face " + i);
        }
    }

    @Test
    void aMissingFaceIsNamed() throws IOException {
        faces("sky", 4, "top");

        StringBuilder errors = new StringBuilder();
        CubeMapData sky = load(errors).getResource(CubeMapData.class, "sky");

        assertTrue(sky.hasFailed());
        assertTrue(errors.toString().contains("missing face " + path("sky", "top")), errors.toString());
        assertTrue(sky.isSourceRead(1));
        assertFalse(sky.isSourceRead(2));
    }

    @Test
    void facesOfDifferentSizesFail() throws IOException {
        faces("sky", 4);
        BufferedImage bigger = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(bigger, "png", folder.resolve(CubeMapLoader.FOLDER).resolve("sky").resolve("front.png").toFile());

        StringBuilder errors = new StringBuilder();
        CubeMapData sky = load(errors).getResource(CubeMapData.class, "sky");

        assertTrue(sky.hasFailed());
        assertTrue(errors.toString().contains(path("sky", "front") + " is 8x8, the others are 4x4"), errors.toString());
    }

    @Test
    void theFallbackHasSixCheckerboards() {
        Resources resources = load(new StringBuilder());
        CubeMapData fallback = resources.getFallback(CubeMapData.class);

        assertInstanceOf(FallbackCubeMapData.class, fallback);
        assertTrue(fallback.isLoaded());
        assertFalse(fallback.hasSource());
        assertTrue(fallback.getFaceSize() > 0);
        for (int i = 0; i < CubeMapData.FACE_COUNT; ++i)
            assertNotNull(fallback.getFace(i));
        assertSame(fallback, resources.getResourceOrFallback(CubeMapData.class, "unknown"));
    }
}
