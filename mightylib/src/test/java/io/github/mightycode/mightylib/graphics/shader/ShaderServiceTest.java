package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // ---- Defines ----

    private int reloads;

    private ShaderService counting(Path root) throws IOException {
        MainThreadTestAccess.setCurrentAsMain();
        writeList(root, "{\"shaders\": {}}");
        return new ShaderService(ResourceSources.of(new FolderSource(root)), () -> ++reloads);
    }

    // Globals sorted by name, then those of the entry, which win over a global of the same name
    @Test
    void preludeSortsTheGlobalsThenWritesTheEntry(@TempDir Path root) throws IOException {
        ShaderService service = counting(root);
        service.setDefine("ZED", "3");
        service.setDefine("ALPHA", "");
        service.setDefine("SHARED", "global");

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("TINT", "vec3(0.2, 0.6, 1.0)");
        entry.put("SHARED", "entry");
        entry.put("B", "");

        assertEquals("#version 330 core\n#define ALPHA\n#define ZED 3\n"
                + "#define B\n#define SHARED entry\n#define TINT vec3(0.2, 0.6, 1.0)\n", service.prelude(entry));
        assertEquals("#version 330 core\n#define ALPHA\n#define SHARED global\n#define ZED 3\n",
                service.prelude(Collections.emptyMap()));
    }

    @Test
    void invalidDefinesAreRefused(@TempDir Path root) throws IOException {
        ShaderService service = counting(root);

        assertThrows(IllegalArgumentException.class, () -> service.setDefine("1ABC", "1"));
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("A-B", "1"));
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("", "1"));
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("GL_MINE", "1"));
        // A line break would slip a directive into the prelude
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("A", "1\n#version 450"));
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("A", "1\r"));
        assertThrows(IllegalArgumentException.class, () -> service.setDefine("A", null));
        assertTrue(service.defines().isEmpty());
        assertEquals(0, reloads);

        service.checkDefine("_ok_9", "");
    }

    // Every change reads the shaders again, the same value or an unknown name does nothing
    @Test
    void onlyAChangeReloadsTheShaders(@TempDir Path root) throws IOException {
        ShaderService service = counting(root);

        service.setDefine("A", "1");
        assertEquals(1, reloads);
        service.setDefine("A", "1");
        assertEquals(1, reloads);
        service.setDefine("A", "2");
        assertEquals(2, reloads);
        service.removeDefine("NONE");
        assertEquals(2, reloads);
        service.removeDefine("A");
        assertEquals(3, reloads);
        assertTrue(service.defines().isEmpty());
    }

    // What a worker read stays the same while the main thread changes the defines
    @Test
    void definesAreAnImmutableSnapshot(@TempDir Path root) throws IOException {
        ShaderService service = counting(root);
        service.setDefine("B", "1");
        service.setDefine("A", "");

        Map<String, String> snapshot = service.defines();
        service.setDefine("C", "2");
        service.removeDefine("B");

        assertEquals(List.of("A", "B"), List.copyOf(snapshot.keySet()));
        assertEquals("1", snapshot.get("B"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("D", ""));
        assertEquals(List.of("A", "C"), List.copyOf(service.defines().keySet()));
    }

    // Two entries on the same files, with different defines: two shaders
    @Test
    void entryDefinesAreKeptByTheShaderData() {
        JSONObject plain = new JSONObject("{\"files\": [\"v.glsl\", \"f.glsl\"], \"tags\": [\"2D\"]}");
        JSONObject tinted = new JSONObject("{\"files\": [\"v.glsl\", \"f.glsl\"], \"tags\": [\"2D\"],"
                + "\"defines\": {\"TINT\": \"vec3(0.2, 0.6, 1.0)\", \"FLAG\": \"\", \"COUNT\": 3}}");

        assertTrue(ShaderLoader.entryDefines(plain).isEmpty());
        Map<String, String> defines = ShaderLoader.entryDefines(tinted);
        assertEquals(Map.of("TINT", "vec3(0.2, 0.6, 1.0)", "FLAG", "", "COUNT", "3"), defines);

        ShaderData data = new ShaderData("tinted", true, defines, "shaders/v.glsl", "shaders/f.glsl");
        assertEquals(List.of("COUNT", "FLAG", "TINT"), List.copyOf(data.getDefines().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> data.getDefines().put("X", ""));
        assertTrue(new ShaderData("plain", true, "shaders/v.glsl", "shaders/f.glsl").getDefines().isEmpty());
    }

    // null, an array or an object: no value, refused when the shader is read (that shader alone fails)
    @Test
    void entryDefinesWithoutTextValueAreKeptAsNull(@TempDir Path root) throws IOException {
        JSONObject entry = new JSONObject("{\"defines\": {\"N\": null, \"A\": [1], \"O\": {}, \"T\": true}}");

        Map<String, String> defines = ShaderLoader.entryDefines(entry);
        assertNull(defines.get("N"));
        assertNull(defines.get("A"));
        assertNull(defines.get("O"));
        assertTrue(defines.containsKey("N") && defines.containsKey("A") && defines.containsKey("O"));
        assertEquals("true", defines.get("T"));

        ShaderService service = counting(root);
        assertThrows(IllegalArgumentException.class, () -> service.checkDefine("N", defines.get("N")));
    }

    @Test
    void definesChangeOnTheMainThreadOnly(@TempDir Path root) throws Exception {
        ShaderService service = counting(root);
        Throwable[] error = new Throwable[1];
        Thread other = new Thread(() -> {
            try {
                service.setDefine("A", "1");
            } catch (Throwable e) {
                error[0] = e;
            }
        });
        other.start();
        other.join();

        assertInstanceOf(IllegalStateException.class, error[0]);
        assertTrue(service.defines().isEmpty());
    }
}
