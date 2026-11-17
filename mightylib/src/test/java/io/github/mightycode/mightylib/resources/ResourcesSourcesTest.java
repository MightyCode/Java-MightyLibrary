package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONFile;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.resources.source.FolderSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The sources of the resources belong to each Resources, given at its construction
class ResourcesSourcesTest {
    @TempDir
    Path first;
    @TempDir
    Path second;

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
    }

    private static Resources create(Path folder, String jsonName) throws IOException {
        Files.write(folder.resolve(jsonName + ".json"), "{}".getBytes(StandardCharsets.UTF_8));
        ResourceSources sources = ResourceSources.builder()
                .add(ResourceSources.GAME_OWNER, new FolderSource(folder)).build();
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1, sources,
                List.of(new JSONLoader()));
        resources.init();
        return resources;
    }

    @Test
    void twoResourcesOnTwoStacksDontSeeEachOther() throws IOException {
        Resources a = create(first, "onlyInFirst");
        Resources b = create(second, "onlyInSecond");

        assertEquals(List.of("onlyInFirst"), a.getResourceNames(JSONFile.class));
        assertEquals(List.of("onlyInSecond"), b.getResourceNames(JSONFile.class));
        assertNotSame(a.sources(), b.sources());
        assertTrue(a.sources().exists("onlyInFirst.json"));
        assertFalse(a.sources().exists("onlyInSecond.json"));
    }

    @Test
    void describeSourcesShowsTheLayersOfAPath() throws IOException {
        Files.writeString(first.resolve("a.json"), "{}");
        Files.writeString(second.resolve("a.json"), "{ }");
        FolderSource low = new FolderSource(first);
        FolderSource high = new FolderSource(second);
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1,
                ResourceSources.builder().add(ResourceSources.GAME_OWNER, low).add("mod", high).build(), List.of());

        JSONObject stack = resources.describeSources(null);
        assertEquals(2, stack.getJSONArray("sources").length());
        assertEquals("game", stack.getJSONArray("sources").getJSONObject(0).getString("kind"));
        assertEquals("plugin", stack.getJSONArray("sources").getJSONObject(1).getString("kind"));
        assertTrue(stack.getJSONArray("sources").getJSONObject(1).getBoolean("watchable"));

        JSONObject path = resources.describeSources("./a.json");
        assertEquals("a.json", path.getString("path"));
        assertEquals(high.name(), path.getString("servedBy"));
        assertEquals(2, path.getJSONArray("layers").length());
        assertEquals(3, path.getJSONArray("layers").getJSONObject(1).getLong("size"));
        assertFalse(resources.describeSources("missing.json").has("servedBy"));
    }

    @Test
    void folderEndsWithASlashAndUsesForwardSlashes() {
        Resources resources = new Resources(new Resources.AllResourcesMethod(), 1, "some\\folder", List.of());
        assertEquals("some/folder/", resources.folder());

        resources = new Resources(new Resources.AllResourcesMethod(), 1, "other/", List.of());
        assertEquals("other/", resources.folder());
    }
}
