package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources.EGraphicResources;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// A Mesh drawn by two renderers: retained by each, unloaded by the release of the last one
@MightySceneTest(config = SceneRunnerTest.Config.class)
class SharedMeshTest {
    private static Mesh triangle() {
        Mesh mesh = new Mesh(false);
        mesh.addVboFloat(Mesh.COMMON_VERTEX_POSITION_CHANNEL, new float[]{0, 0, 1, 0, 0, 1}, 2, Mesh.STATIC_STORE);
        return mesh;
    }

    @Test
    void twoRenderersShareAMesh(SceneRunner runner) {
        runner.frames(1);
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            share();
        }
    }

    private static void share() {
        int vbos = GenDeleteResources.Count(EGraphicResources.VertexBufferObject);

        Mesh mesh = triangle();
        assertEquals(0, mesh.references());
        MeshRenderer first = new MeshRenderer(mesh, EngineResources.COLOR_SHAPE_2D);
        MeshRenderer second = new MeshRenderer(mesh, EngineResources.COLOR_SHAPE_2D);
        assertEquals(2, mesh.references());
        assertSame(first.getMesh(), second.getMesh());
        assertEquals(vbos + 1, GenDeleteResources.Count(EGraphicResources.VertexBufferObject));

        first.display();
        second.display();

        first.unload();
        assertEquals(1, mesh.references());
        assertEquals(1, mesh.getVboCount());
        second.display();

        second.unload();
        assertEquals(0, mesh.references());
        assertEquals(0, mesh.getVboCount());
        assertEquals(vbos, GenDeleteResources.Count(EGraphicResources.VertexBufferObject));
        assertThrows(IllegalStateException.class, mesh::retain);
    }

    @Test
    void theGameKeepsAMeshAfterItsRenderers(SceneRunner runner) {
        runner.frames(1);
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            keep();
        }
    }

    private static void keep() {
        Mesh mesh = triangle();
        mesh.retain();

        MeshRenderer renderer = new MeshRenderer(mesh, EngineResources.COLOR_SHAPE_2D);
        renderer.unload();
        assertEquals(1, mesh.getVboCount());

        assertTrue(mesh.release());
        assertEquals(0, mesh.getVboCount());
    }

    @Test
    void aMeshCreatedByItsRendererIsRetainedOnce(SceneRunner runner) {
        runner.frames(1);
        try (Views.Scope ignored = Views.enter(runner.scene())) {
            MeshRenderer renderer = new MeshRenderer(EngineResources.COLOR_SHAPE_2D, false);
            assertEquals(1, renderer.getMesh().references());
            renderer.unload();
            assertEquals(0, renderer.getMesh().references());
        }
    }
}
