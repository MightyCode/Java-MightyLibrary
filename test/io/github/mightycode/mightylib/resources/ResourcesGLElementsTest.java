package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.shader.FallbackShader;
import io.github.mightycode.mightylib.graphics.shader.FallbackShaderData;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The GL elements held by the batches: one reference per data listed (urgent for a loaded batch, not for a preloaded
 * one), none for "gpu": false or a dependency, all given back by unload, cancelPreload or a new description.
 */
class ResourcesGLElementsTest extends ResourcesTestBase {
    // Counts the references by data name, like GLResources would
    static class FakeSink implements IGLElementSink {
        final Map<String, int[]> references = new HashMap<>();
        final Map<String, GLElement> elements = new HashMap<>();

        @Override
        public GLElement acquire(DataType data, boolean urgent) {
            int[] counts = references.computeIfAbsent(data.getDataName(), name -> new int[2]);
            ++counts[urgent ? 0 : 1];
            return elements.computeIfAbsent(data.getDataName(), name -> new GLElement() {
                @Override
                public boolean load(int remainingMilliseconds) {
                    return true;
                }

                @Override
                public void unload(int remainingMilliseconds) {}

                @Override
                public String getName() {
                    return name;
                }
            });
        }

        // Names of the data given to dataReloaded, in order
        final java.util.List<String> reloadedData = new java.util.ArrayList<>();

        @Override
        public void dataReloaded(DataType data) {
            reloadedData.add(data.getDataName());
        }

        @Override
        public void release(GLElement element, boolean urgent) {
            int[] counts = references.get(element.getName());
            --counts[urgent ? 0 : 1];
            assertTrue(counts[0] >= 0 && counts[1] >= 0, "released more than acquired: " + element.getName());
        }

        int urgent(String name) {
            int[] counts = references.get(name);
            return counts == null ? 0 : counts[0];
        }

        int preload(String name) {
            int[] counts = references.get(name);
            return counts == null ? 0 : counts[1];
        }

        void assertAllReleased() {
            for (Map.Entry<String, int[]> entry : references.entrySet()) {
                assertEquals(0, entry.getValue()[0], entry.getKey());
                assertEquals(0, entry.getValue()[1], entry.getKey());
            }
        }
    }

    private FakeSink sink;

    @BeforeEach
    void setSink() {
        sink = new FakeSink();
        resources.setGLElementSink(sink);
    }

    private void batchWithoutGPU(String name, String sent, String notSent) throws IOException {
        String content = "{\"batchs\":["
                + "{\"type\":\"Fake\",\"files\":[\"" + sent + "\"],\"regex\":[]},"
                + "{\"type\":\"Fake\",\"files\":[\"" + notSent + "\"],\"regex\":[],\"gpu\":false}]}";
        Files.write(folder.resolve("batchs").resolve(name + ".batch"), content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void loadedBatchHoldsUrgentElementsUntilUnloaded() throws Exception {
        fake(THREAD_CONTEXT, "a");
        fake(THREAD_CONTEXT, "b");
        batch("level", "a", "b");
        start();

        resources.loadBatch("level");
        pump();
        assertEquals(1, sink.urgent("a"));
        assertEquals(1, sink.urgent("b"));
        assertEquals(0, sink.preload("a"));

        resources.unloadBatch("level");
        pump();
        sink.assertAllReleased();
    }

    // The program of a shader is the GL element of its sources, the fallback ones give the fallback program
    @Test
    void shaderDataGivesItsProgram() {
        ShaderData data = new ShaderData("program", true, "v.glsl", "f.glsl");
        assertEquals(Shader.class, data.glElementType());
        Shader program = (Shader) data.createGLElement();
        assertSame(data, program.getData());
        assertTrue(program.isDimension2DShader());

        FallbackShaderData fallback = new FallbackShaderData("3_30");
        assertEquals(Shader.class, fallback.glElementType());
        assertInstanceOf(FallbackShader.class, fallback.createGLElement());
        assertFalse(fallback.hasSource());
    }

    // A reload that failed on a missing include: the include is watched too, creating it reloads the shader
    @Test
    void aShaderWatchesTheFilesOfAFailedReload() {
        ShaderData data = new ShaderData("program", true, "v.glsl", "f.glsl");
        ShaderData copy = new ShaderData("program", true, "v.glsl", "f.glsl");
        copy.setSourcePaths(List.of("v.glsl", "missing.glsl"));

        ((DataType) data).reloadFailed(copy);
        assertEquals(List.of("v.glsl", "f.glsl", "missing.glsl"), data.sourcePaths());
    }

    // The sink tries again the elements that failed with the previous version: told of each reload, not of a first load
    @Test
    void theSinkIsToldWhenADataIsReloaded() throws Exception {
        FakeData apart = fake(THREAD_CONTEXT, "apart");
        FakeData inPlace = fake(THREAD_CONTEXT, "inPlace");
        inPlace.reloadApart = false;
        batch("level", "apart", "inPlace");
        start();

        resources.loadBatch("level");
        pump();
        assertTrue(sink.reloadedData.isEmpty());

        resources.reload(apart);
        resources.reload(inPlace);
        pump();
        assertTrue(sink.reloadedData.contains("apart"));
        assertTrue(sink.reloadedData.contains("inPlace"));
        assertEquals(2, sink.reloadedData.size());
        assertConsistent();
    }

    @Test
    void gpuFalseAndDependenciesAreNotSent() throws Exception {
        FakeData sent = fake(THREAD_CONTEXT, "sent");
        fake(THREAD_CONTEXT, "cpuOnly");
        sent.dependencyToAdd = fake(THREAD_CONTEXT, "dependency");
        batchWithoutGPU("level", "sent", "cpuOnly");
        start();

        resources.loadBatch("level");
        pump();
        assertEquals(1, sink.urgent("sent"));
        assertFalse(sink.references.containsKey("cpuOnly"));
        assertFalse(sink.references.containsKey("dependency"));

        resources.unloadBatch("level");
        pump();
        sink.assertAllReleased();
    }

    @Test
    void preloadedBatchHoldsElementsWithoutUrgency() throws Exception {
        fake(THREAD_CONTEXT, "a");
        batch("next", "a");
        start();

        resources.preloadBatch("next");
        pump();
        assertEquals(0, sink.urgent("a"));
        assertEquals(1, sink.preload("a"));

        // Entering the scene: loaded for real, then the preload is cancelled (SceneManager.changeScene)
        resources.loadBatch("next");
        resources.cancelPreloadBatch("next");
        pump();
        assertEquals(1, sink.urgent("a"));
        assertEquals(0, sink.preload("a"));

        resources.unloadBatch("next");
        pump();
        sink.assertAllReleased();
    }

    @Test
    void newDescriptionAcquiresAddedAndReleasesRemoved() throws Exception {
        fake(THREAD_CONTEXT, "a");
        fake(THREAD_CONTEXT, "b");
        fake(THREAD_CONTEXT, "c");
        batch("level", "a", "b");
        start();
        resources.loadBatch("level");
        pump();

        batch("level", "b", "c");
        resources.reloadAllOfType(BatchResources.class);
        pump();
        assertEquals(0, sink.urgent("a"));
        assertEquals(1, sink.urgent("b"));
        assertEquals(1, sink.urgent("c"));

        resources.unloadBatch("level");
        pump();
        sink.assertAllReleased();
    }

    @Test
    void withoutSinkNothingIsSent() throws Exception {
        resources.setGLElementSink(null);
        FakeData a = fake(THREAD_CONTEXT, "a");
        batch("level", "a");
        start();

        resources.loadBatch("level");
        pump();
        assertTrue(a.isLoaded());
        assertTrue(sink.references.isEmpty());

        resources.unloadBatch("level");
        pump();
        assertConsistent();
    }
}
