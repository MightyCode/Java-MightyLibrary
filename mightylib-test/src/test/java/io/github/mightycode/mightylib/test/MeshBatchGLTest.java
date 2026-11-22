package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.debug.GraphRenderer;
import io.github.mightycode.mightylib.graphics.debug.MatrixRenderer;
import io.github.mightycode.mightylib.graphics.debug.NodesRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static io.github.mightycode.mightylib.test.BatchPictures.*;
import static org.junit.jupiter.api.Assertions.*;

// A 2D MeshRenderer whose mesh keeps its data on the CPU is batched: the same picture as drawn alone
@MightySceneTest(config = SceneRunnerTest.Config.class)
class MeshBatchGLTest {
    // A textured quad turned, of a mesh of the game (positions, texture coordinates, indices)
    private static MeshRenderer texturedQuad(Pattern pattern, boolean keepCpuData) {
        MeshRenderer renderer = new MeshRenderer(EngineResources.TEXTURE_2D, true);
        Mesh mesh = renderer.getMesh();
        if (keepCpuData)
            mesh.keepCpuData();
        mesh.setEbo(new int[]{0, 1, 2, 0, 2, 3});
        mesh.addVboFloat(new float[]{0, 0, 0, 1, 1, 1, 1, 0}, 2, Mesh.STATIC_STORE);
        mesh.addVboFloat(new float[]{0.1f, 0.2f, 0.1f, 0.9f, 0.8f, 0.9f, 0.8f, 0.2f}, 2, Mesh.STATIC_STORE);
        renderer.setMainTextureChannel(pattern);
        renderer.setScale(new Vector3f(150, 110, 1));
        return renderer;
    }

    @Test
    void theBatchedMeshesAreTheMeshesDrawnAlone(SceneRunner runner) {
        inScene(runner, scene -> {
            Pattern pattern;
            MeshRenderer quad, alone;
            NodesRenderer nodes;
            GraphRenderer graph;
            MatrixRenderer matrix;
            try (Views.Scope ignored = Views.enter(scene)) {
                Camera2D camera = scene.camera2D();
                pattern = new Pattern();
                quad = texturedQuad(pattern, true);
                quad.setX(500);
                quad.setY(300);
                quad.setRotationZ(0.6f);
                alone = texturedQuad(pattern, false);
                alone.setX(700);
                alone.setY(100);

                List<Vector2f> points = new ArrayList<>();
                for (int i = 0; i < 12; ++i)
                    points.add(new Vector2f(60 + i * 70, 450 + (i % 3) * 40));
                nodes = new NodesRenderer(camera);
                nodes.setNodeSize(9);
                nodes.init(points);
                nodes.updateNodes(points, new Vector4f(), new Vector4f(), 1f);

                SortedMap<Vector2f, Long> ordered = new TreeMap<>((a, b) -> Float.compare(a.x, b.x));
                Map<Vector2f, List<Vector2f>> connections = new HashMap<>();
                for (int i = 0; i < points.size(); ++i) {
                    ordered.put(points.get(i), (long) i);
                    List<Vector2f> neighbours = new ArrayList<>();
                    if (i > 0)
                        neighbours.add(points.get(i - 1));
                    if (i > 1)
                        neighbours.add(points.get(i - 2));
                    connections.put(points.get(i), neighbours);
                }
                graph = new GraphRenderer(camera);
                graph.setColor(new Color4f(0.2f, 0.4f, 1f, 0.8f));
                graph.setEdgeSize(4);
                graph.init(ordered, connections);
                graph.updateRoads(ordered, connections, new Vector4f(), new Vector4f(), 1f);

                int[][] values = new int[6][8];
                for (int y = 0; y < values.length; ++y) {
                    for (int x = 0; x < values[y].length; ++x)
                        values[y][x] = x * 7 + y * 3;
                }
                matrix = new MatrixRenderer(camera, 48);
                matrix.updateRenderer(values, 0, 64, new Vector4f(40, 40, 360, 280));
            }
            runner.frames(5);

            try (Views.Scope ignored = Views.enter(scene); Capture capture = new Capture(scene)) {
                capture.assertEveryModeDrawsTheSame(scene, () -> {
                    matrix.display();
                    graph.display();
                    nodes.display();
                    quad.display();
                    alone.display();
                });
                // The mesh without copy on the CPU is drawn alone, the others batched
                assertEquals(4, scene.batch2D().lastFrameDraws());
                assertTrue(scene.batch2D().lastFrameBarrierCauses().containsKey(MeshRenderer.class.getName()));

                matrix.unload();
                graph.unload();
                nodes.unload();
                quad.unload();
                alone.unload();
                pattern.delete();
            }
        });
    }
}
