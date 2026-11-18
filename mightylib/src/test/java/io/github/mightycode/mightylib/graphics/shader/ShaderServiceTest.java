package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ShaderServiceTest {
    private static void writeList(Path root, String json) throws IOException {
        Files.createDirectories(root.resolve("shaders"));
        Files.writeString(root.resolve("shaders/shaders.json"), json);
    }

    private static ShaderService service(Path... roots) {
        FolderSource[] sources = new FolderSource[roots.length];
        for (int i = 0; i < roots.length; ++i)
            sources[i] = new FolderSource(roots[i]);

        return new ShaderService(ResourceSources.of(sources), () -> {});
    }

    // A higher source adds shaders and replaces those of the same name, entry by entry
    @Test
    void declarationsAreMergedThroughTheSources(@TempDir Path low, @TempDir Path high) throws IOException {
        writeList(low, "{\"shaders\": {"
                + "\"a\": {\"files\": [\"a/v.glsl\", \"a/f.glsl\"], \"tags\": [\"2D\"]},"
                + "\"b\": {\"files\": [\"b/v.glsl\", \"b/f.glsl\"], \"tags\": [\"3D\"]}}}");
        writeList(high, "{\"shaders\": {"
                + "\"b\": {\"files\": [\"high/v.glsl\", \"high/f.glsl\"], \"tags\": [\"2D\"]},"
                + "\"c\": {\"files\": [\"c/v.glsl\", \"c/f.glsl\"], \"tags\": []}}}");

        JSONObject declarations = service(low, high).declarations();

        assertEquals(3, declarations.length());
        assertEquals("a/v.glsl", declarations.getJSONObject("a").getJSONArray("files").getString(0));
        assertEquals("high/v.glsl", declarations.getJSONObject("b").getJSONArray("files").getString(0));
        assertTrue(declarations.has("c"));
    }

    // The engine always has one: without any, the service can't be created (the engine stops, SERVICE_FAIL)
    @Test
    void noListFails(@TempDir Path empty) {
        assertThrows(RuntimeException.class, () -> service(empty));
    }

    @Test
    void folderAndPreludeWithoutDefine(@TempDir Path root) throws IOException {
        writeList(root, "{\"shaders\": {}}");
        ShaderService service = service(root);

        assertEquals("shaders/", service.folder());
        assertEquals("#version 330 core\n", service.prelude(Collections.emptyMap()));
    }
}
