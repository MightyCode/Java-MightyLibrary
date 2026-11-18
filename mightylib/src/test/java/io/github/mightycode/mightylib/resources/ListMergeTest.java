package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.internal.BatchLoader;
import io.github.mightycode.mightylib.resources.internal.BatchResources;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.resources.texture.TextureDataLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// The lists merged by entry across the sources: textures.json, fonts.json, shaders.json, batches
class ListMergeTest {
    @TempDir
    Path low;
    @TempDir
    Path high;

    private ResourceSources sources() {
        return ResourceSources.of(new FolderSource(low), new FolderSource(high));
    }

    private static void write(Path root, String path, String content) throws IOException {
        Path file = root.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String texture(String file, String quality) {
        return "{\"file\": \"" + file + "\", \"qualityType\": \"" + quality + "\"}";
    }

    @Test
    void texturesOfAHigherSourceAreAddedOrReplaceThoseOfTheSameName() throws IOException {
        write(low, "textures/textures.json", "{\"textures\": {\"a\": " + texture("a.png", "realistic")
                + ", \"b\": " + texture("b.png", "realistic") + "}}");
        write(high, "textures/textures.json", "{\"textures\": {\"gui\": {\"b\": " + texture("b2.png", "pixelart")
                + "}, \"c\": " + texture("c.png", "realistic") + "}}");

        TextureDataLoader loader = LoaderTestAccess.attached(new TextureDataLoader(), sources());
        Map<String, DataType> data = new HashMap<>();
        loader.create(data);

        assertEquals(Set.of("a", "b", "c"), data.keySet());
        assertEquals("textures/a.png", ((TextureData) data.get("a")).path());
        assertEquals("textures/gui/b2.png", ((TextureData) data.get("b")).path());
        assertEquals("textures/c.png", ((TextureData) data.get("c")).path());
    }

    @Test
    void anEntryIsReplacedAsAWholeAndAddedOnes() {
        JSONObject lowFonts = new JSONObject("{\"fonts\": {\"arial\": {\"texture\": \"arial\", \"info\": \"arial.fnt\"},"
                + " \"mono\": {\"texture\": \"mono\", \"info\": \"mono.fnt\"}}}");
        JSONObject highFonts = new JSONObject("{\"fonts\": {\"arial\": {\"texture\": \"arial2\", \"info\": \"a2.fnt\"},"
                + " \"title\": {\"texture\": \"title\", \"info\": \"title.fnt\"}}}");

        JSONObject fonts = FileMethods.mergeEntries(List.of(lowFonts, highFonts), "fonts").getJSONObject("fonts");
        assertEquals(Set.of("arial", "mono", "title"), fonts.keySet());
        assertEquals("a2.fnt", fonts.getJSONObject("arial").getString("info"));

        // A shader replaced keeps nothing of the lower one (its tags included)
        JSONObject lowShaders = new JSONObject("{\"shaders\": {\"s\": {\"files\": [\"a.glsl\", \"b.glsl\"],"
                + " \"tags\": [\"2D\"], \"path\": \"\"}}, \"other\": 1}");
        JSONObject highShaders = new JSONObject("{\"shaders\": {\"s\": {\"files\": [\"c.glsl\", \"d.glsl\"],"
                + " \"tags\": [\"3D\"]}, \"t\": {\"files\": [], \"tags\": []}}, \"other\": 2}");

        JSONObject shaders = FileMethods.mergeEntries(List.of(lowShaders, highShaders), "shaders");
        assertEquals(2, shaders.getInt("other"));
        assertEquals(Set.of("s", "t"), shaders.getJSONObject("shaders").keySet());
        JSONObject s = shaders.getJSONObject("shaders").getJSONObject("s");
        assertEquals("c.glsl", s.getJSONArray("files").getString(0));
        assertFalse(s.has("path"));
    }

    @Test
    void layersAreReadInTheirOwnSource() throws IOException {
        write(low, "list.json", "{\"from\": \"low\"}");
        write(high, "list.json", "{\"from\": \"high\"}");
        write(low, "onlyLow.json", "{}");

        List<JSONObject> layers = FileMethods.readJSONObjectLayers(sources(), "list.json");
        assertEquals("low", layers.get(0).getString("from"));
        assertEquals("high", layers.get(1).getString("from"));
        assertEquals(1, FileMethods.readJSONObjectLayers(sources(), "onlyLow.json").size());
        assertThrows(ResourceException.class, () -> FileMethods.readJSONObjectLayers(sources(), "missing.json"));

        write(high, "bad.json", "{");
        write(low, "bad.json", "{}");
        ResourceException e = assertThrows(ResourceException.class,
                () -> FileMethods.readJSONObjectLayers(sources(), "bad.json"));
        assertTrue(e.getMessage().contains(high.getFileName().toString()), e.getMessage());
    }

    private static String batch(String entries) {
        return "{\"batchs\": [" + entries + "]}";
    }

    private static String entry(String type, String files, String regex) {
        return "{\"type\": \"" + type + "\", \"files\": [" + files + "], \"regex\": [" + regex + "]}";
    }

    @Test
    void theListsOfABatchAreJoinedWithoutDuplicateAndEveryLayerIsReadAgain() throws IOException {
        write(low, "batchs/level.batch", batch(entry("TextureData", "\"a\", \"b\"", "\"tile.*\"")));
        write(high, "batchs/level.batch", batch(entry("TextureData", "\"b\", \"c\"", "\"tile.*\"") + ", "
                + entry("JSONFile", "\"level\"", "")));

        BatchLoader loader = LoaderTestAccess.attached(new BatchLoader(), sources());
        BatchResources level = new BatchResources("level", "batchs/level.batch");
        loader.initWithFile(level);

        JSONArray merged = level.getObject().getJSONArray("batchs");
        assertEquals(2, merged.length());
        JSONObject textures = merged.getJSONObject(0);
        assertEquals("TextureData", textures.getString("type"));
        assertEquals(List.of("a", "b", "c"), textures.getJSONArray("files").toList());
        assertEquals(List.of("tile.*"), textures.getJSONArray("regex").toList());
        assertEquals(List.of("level"), merged.getJSONObject(1).getJSONArray("files").toList());

        // The lower layer changes: read again, the batch gets it
        write(low, "batchs/level.batch", batch(entry("TextureData", "\"d\"", "")));
        loader.initWithFile(level);
        assertEquals(List.of("d", "b", "c"),
                level.getObject().getJSONArray("batchs").getJSONObject(0).getJSONArray("files").toList());
    }

    @Test
    void aBatchInOneSourceIsReadAsIs() throws IOException {
        write(low, "batchs/one.batch", batch(entry("TextureData", "\"a\"", "")));

        BatchLoader loader = LoaderTestAccess.attached(new BatchLoader(), sources());
        BatchResources one = new BatchResources("one", "batchs/one.batch");
        loader.initWithFile(one);

        assertEquals(List.of("a"),
                one.getObject().getJSONArray("batchs").getJSONObject(0).getJSONArray("files").toList());
    }
}
