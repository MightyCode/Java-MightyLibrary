package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.EBlendMode;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// A Material shared by renderers: references, version, part of the sort key
@MightySceneTest(config = SceneRunnerTest.Config.class)
class MaterialTest {
    // A texture of the test, not a GL element (not released by the material)
    private static final class FakeTexture implements IRenderTextureBindable {
        @Override public void bindRenderTexture(int position) {}
        @Override public int getWidth() { return 1; }
        @Override public int getHeight() { return 1; }
        @Override public int getRenderTextureId() { return 0; }
    }

    private static void inScene(SceneRunner runner, Runnable test) {
        runner.frames(1);
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            test.run();
        }
    }

    @Test
    void eachChangeIncrementsTheVersion(SceneRunner runner) {
        inScene(runner, () -> {
            Material material = new Material(EngineResources.COLOR_SHAPE_2D);
            material.retain();

            Consumer<Runnable> changes = change -> {
                int before = material.version();
                change.run();
                assertTrue(material.version() > before);
            };
            changes.accept(() -> material.addShaderValue("value", Float.class, 1f));
            changes.accept(() -> material.updateShaderValue("value", 2f));
            changes.accept(() -> material.removeShaderValue("value"));
            changes.accept(() -> material.setBlendMode(EBlendMode.ADDITIVE));
            changes.accept(() -> material.setDepthTest(true));
            changes.accept(() -> material.setDepthWrite(true));
            changes.accept(() -> material.setFaceCulling(true));
            changes.accept(() -> material.setMainTexture(new FakeTexture()));
            changes.accept(() -> material.setTextureChannel(new FakeTexture(), 2));

            // Nothing to remove: no change
            int before = material.version();
            material.removeShaderValue("value");
            assertEquals(before, material.version());

            material.release();
        });
    }

    @Test
    void theSortKeyPartIsTheSameForTheSameState(SceneRunner runner) {
        inScene(runner, () -> {
            Material first = new Material(EngineResources.COLOR_SHAPE_2D);
            Material second = new Material(EngineResources.COLOR_SHAPE_2D);
            first.retain();
            second.retain();

            assertEquals(first.sortKeyPart(), second.sortKeyPart());
            // Values are not in the key
            first.addShaderValue("value", Float.class, 1f);
            assertEquals(first.sortKeyPart(), second.sortKeyPart());

            second.setBlendMode(EBlendMode.ADDITIVE);
            assertNotEquals(first.sortKeyPart(), second.sortKeyPart());
            first.setBlendMode(EBlendMode.ADDITIVE);
            assertEquals(first.sortKeyPart(), second.sortKeyPart());

            FakeTexture texture = new FakeTexture();
            first.setMainTexture(texture);
            assertNotEquals(first.sortKeyPart(), second.sortKeyPart());
            second.setMainTexture(texture);
            assertEquals(first.sortKeyPart(), second.sortKeyPart());
            second.setMainTexture(new FakeTexture());
            assertNotEquals(first.sortKeyPart(), second.sortKeyPart());
            second.setMainTexture(texture);

            first.setDepthTest(true);
            assertNotEquals(first.sortKeyPart(), second.sortKeyPart());

            first.release();
            second.release();
        });
    }

    @Test
    void setMaterialRetainsTheNewOneBeforeReleasingTheOld(SceneRunner runner) {
        inScene(runner, () -> {
            MeshRenderer renderer = new MeshRenderer(EngineResources.COLOR_SHAPE_2D, false);
            Material own = renderer.material();
            Shader shader = own.getShader();
            assertEquals(1, own.references());

            Material shared = new Material(EngineResources.COLOR_SHAPE_2D);
            MeshRenderer other = new MeshRenderer(EngineResources.COLOR_SHAPE_2D, false);
            other.setMaterial(shared);
            renderer.setMaterial(shared);

            // The previous material is freed, the program it shared with the new one stays loaded
            assertTrue(own.isFreed());
            assertEquals(2, shared.references());
            assertSame(shader, shared.getShader());
            assertTrue(shader.isLoaded());

            // A change of the shared material is seen by both renderers
            shared.setBlendMode(EBlendMode.MULTIPLY);
            assertEquals(EBlendMode.MULTIPLY, renderer.getBlendMode());
            assertEquals(EBlendMode.MULTIPLY, other.getBlendMode());

            renderer.unload();
            assertEquals(1, shared.references());
            other.unload();
            assertTrue(shared.isFreed());
        });
    }

    @Test
    void aMaterialOrMeshRetainedByNobodyIsCountedAsOrphan(SceneRunner runner) {
        runner.frames(1);
        Map<String, Integer> before = runner.leakSnapshot();
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            Material material = new Material(EngineResources.COLOR_SHAPE_2D);
            Mesh mesh = new Mesh(false);
            mesh.addVboFloat(Mesh.COMMON_VERTEX_POSITION_CHANNEL, new float[]{0, 0, 1, 0, 0, 1}, 2, Mesh.STATIC_STORE);

            Map<String, Integer> orphans = runner.leakSnapshot();
            assertEquals(before.get("retained.orphanMaterial") + 1, orphans.get("retained.orphanMaterial"));
            assertEquals(before.get("retained.orphanMesh") + 1, orphans.get("retained.orphanMesh"));

            CommandResult result = runner.execute("gl materials");
            assertTrue(result.isSuccess(), result.getMessage());
            assertEquals(orphans.get("retained.orphanMaterial"), result.getData().getInt("orphanMaterials"));
            assertEquals(orphans.get("retained.orphanMesh"), result.getData().getInt("orphanMeshes"));

            // Drawn by a renderer: no longer an orphan, and freed with it
            MeshRenderer renderer = new MeshRenderer(mesh, material);
            Map<String, Integer> drawn = runner.leakSnapshot();
            assertEquals(before.get("retained.orphanMaterial"), drawn.get("retained.orphanMaterial"));
            assertEquals(before.get("retained.orphanMesh"), drawn.get("retained.orphanMesh"));
            renderer.unload();
        }
        runner.assertNoLeak(before);
    }

    @Test
    void theValuesOfTheRendererAreItsOwn(SceneRunner runner) {
        inScene(runner, () -> {
            Material shared = new Material(EngineResources.COLOR_SHAPE_2D);
            shared.addShaderValue("common", Float.class, 1f);
            MeshRenderer first = new MeshRenderer(EngineResources.COLOR_SHAPE_2D, false);
            first.setMaterial(shared);
            first.addShaderValue("common", Float.class, 2f);

            assertTrue(first.hasShaderValue("common"));
            assertEquals(2f, first.getShaderValue("common").getObject());
            assertEquals(1f, shared.getShaderValue("common").getObject());

            first.removeShaderValue("common");
            assertFalse(first.hasShaderValue("common"));
            // The value of the material is the one sent again
            assertEquals(1f, first.getShaderValue("common").getObject());
            assertThrows(IllegalArgumentException.class, () -> first.updateShaderValue("common", 3f));

            first.unload();
        });
    }
}
