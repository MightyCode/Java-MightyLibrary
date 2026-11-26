package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.EBatch2DMode;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.Scene;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

// A mesh that keeps its data on the CPU sends its float vbos at the draw that reads them (Mesh.bind), once for all the
// updates in between, and never when it is only drawn by the 2D batch
@MightySceneTest(config = SceneRunnerTest.Config.class)
class MeshUploadGLTest {
    private static final float[] SQUARE = {0, 0, 0, 1, 1, 1, 1, 0};
    private static final float[] SLANTED = {0, 0, 0.3f, 1, 1, 0.7f, 1, 0};
    private static final float[] JUNK = {0.5f, 0.5f, 0.2f, 0.1f, 0.9f, 0.3f, 0.4f, 0.8f};

    private static MeshRenderer texturedQuad(Pattern pattern, boolean keepCpuData, float[] positions) {
        MeshRenderer renderer = new MeshRenderer(EngineResources.TEXTURE_2D, true);
        Mesh mesh = renderer.getMesh();
        if (keepCpuData)
            mesh.keepCpuData();
        mesh.setEbo(new int[]{0, 1, 2, 0, 2, 3});
        mesh.addVboFloat(positions, 2, Mesh.DYNAMIC_STORE);
        mesh.addVboFloat(new float[]{0.1f, 0.2f, 0.1f, 0.9f, 0.8f, 0.9f, 0.8f, 0.2f}, 2, Mesh.STATIC_STORE);
        renderer.setMainTextureChannel(pattern);
        renderer.setScale(new Vector3f(150, 110, 1));
        renderer.setX(500);
        renderer.setY(300);
        renderer.setRotationZ(0.4f);
        return renderer;
    }

    // The bytes the drawing sent to buffers of the GPU (meshes and batch)
    private static long uploadedBy(Runnable drawing) {
        EngineStats.endSecond(1, 1, 0, 0);
        drawing.run();
        EngineStats.endSecond(1, 1, 0, 0);
        return EngineStats.toJson().getLong("bufferUploadBytesPerSecond");
    }

    // The picture of the last drawAndUpload
    private ByteBuffer picture;

    private long drawAndUpload(Capture capture, Scene scene, MeshRenderer renderer) {
        return uploadedBy(() -> picture = capture.draw(scene, renderer::display));
    }

    private static void assertSamePicture(ByteBuffer expected, ByteBuffer actual, int tolerance) {
        assertTrue(countDifferent(expected, actual) <= tolerance, compare(expected, actual));
    }

    @Test
    void aMeshDrawnAloneSendsItsLastValuesOnce(SceneRunner runner) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Pattern pattern = new Pattern();
                MeshRenderer expected = texturedQuad(pattern, false, SLANTED);
                MeshRenderer updated = texturedQuad(pattern, true, SQUARE);
                Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                drawAndUpload(capture, scene, expected);
                ByteBuffer wanted = picture;

                // Two updates before the draw: nothing sent until it, then the last values once
                long beforeDraw = uploadedBy(() -> {
                    updated.getMesh().updateVbo(JUNK, 0);
                    updated.getMesh().updateVbo(SLANTED, 0);
                });
                assertEquals(0, beforeDraw, "an update only writes the copy on the CPU");
                assertEquals(SLANTED.length * Float.BYTES, drawAndUpload(capture, scene, updated));
                assertSamePicture(wanted, picture, 0);
                assertEquals(0, drawAndUpload(capture, scene, updated), "up to date: nothing sent again");

                // Parts: the union of their ranges sent once, with the last values
                updated.getMesh().updateVbo(SQUARE, 0);
                drawAndUpload(capture, scene, updated);
                updated.getMesh().updateSubVbo(new float[]{0.9f, 0.9f}, 0, 2 * Float.BYTES);
                updated.getMesh().updateSubVbo(new float[]{0.3f, 1}, 0, 2 * Float.BYTES);
                updated.getMesh().updateSubVbo(new float[]{1, 0.7f}, 0, 4 * Float.BYTES);
                assertEquals(4 * Float.BYTES, drawAndUpload(capture, scene, updated));
                assertSamePicture(wanted, picture, 0);

                // Without copy on the CPU: sent at the update
                assertEquals(JUNK.length * Float.BYTES, uploadedBy(() -> expected.getMesh().updateVbo(JUNK, 0)));

                expected.unload();
                updated.unload();
                pattern.delete();
            }
        });
    }

    @Test
    void aMeshDrawnByTheBatchSendsNothing(SceneRunner runner) {
        inScene(runner, scene -> {
            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                Pattern pattern = new Pattern();
                MeshRenderer expected = texturedQuad(pattern, false, SLANTED);
                MeshRenderer updated = texturedQuad(pattern, true, SQUARE);
                Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                drawAndUpload(capture, scene, expected);
                ByteBuffer wanted = picture;

                // What the batch sends for this quad, the mesh up to date
                Views.engine().setBatch2DMode(EBatch2DMode.ORPHAN);
                long batchOnly = drawAndUpload(capture, scene, updated);
                assertTrue(batchOnly > 0, "the batch sends its vertices");

                // Updated twice then batched: the batch reads the copy, the mesh sends nothing
                updated.getMesh().updateVbo(JUNK, 0);
                updated.getMesh().updateVbo(SLANTED, 0);
                assertEquals(batchOnly, drawAndUpload(capture, scene, updated));
                assertSamePicture(wanted, picture, capture.size.x * capture.size.y * 4 / 2000);
                assertEquals(batchOnly, drawAndUpload(capture, scene, updated));

                // Drawn alone later: the vbo still waiting is sent then
                Views.engine().setBatch2DMode(EBatch2DMode.OFF);
                assertEquals(SLANTED.length * Float.BYTES, drawAndUpload(capture, scene, updated));
                assertSamePicture(wanted, picture, 0);

                expected.unload();
                updated.unload();
                pattern.delete();
            }
        });
    }
}
