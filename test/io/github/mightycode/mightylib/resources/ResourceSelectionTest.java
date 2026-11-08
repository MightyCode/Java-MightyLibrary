package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** Selection written as words (commands), reload and detail of a selection. */
class ResourceSelectionTest {
    @TempDir
    Path folder;

    private String previousFolder;
    private Resources resources;
    private FakeData grass, sand, stone;

    @BeforeEach
    void setUp() throws IOException {
        MainThreadTestAccess.setCurrentAsMain();
        previousFolder = Resources.FOLDER;
        Resources.FOLDER = folder.toString().replace('\\', '/') + "/";
        Files.createDirectories(folder.resolve("batchs"));

        FakeLoader fakeLoader = new FakeLoader();
        JSONLoader jsonLoader = new JSONLoader();
        resources = new Resources(new Resources.BatchResourcesMethod("unused"), 2,
                Arrays.asList(fakeLoader, jsonLoader, new BatchLoader(jsonLoader)));

        grass = new FakeData(THREAD_CONTEXT, "grass");
        sand = new FakeData(THREAD_CONTEXT, "tile_sand");
        stone = new FakeData(THREAD_CONTEXT, "tile_stone");
        fakeLoader.add(grass);
        fakeLoader.add(sand);
        fakeLoader.add(stone);

        Files.write(folder.resolve("batchs").resolve("level.batch"),
                "{\"batchs\":[{\"type\":\"Fake\",\"files\":[\"grass\",\"tile_sand\",\"tile_stone\"],\"regex\":[]}]}"
                        .getBytes(StandardCharsets.UTF_8));
        resources.init();
        resources.loadAllOfType(BatchResources.class);
        resources.loadBatch("level");
        pump();
    }

    @AfterEach
    void tearDown() {
        Resources.FOLDER = previousFolder;
    }

    private void pump() throws IOException {
        long end = System.currentTimeMillis() + 10_000;
        do {
            resources.process(2);
            MainThreadTestAccess.processAll();
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        } while (!resources.finishedCurrentLoading() && System.currentTimeMillis() < end);
    }

    @Test
    void parseBuildsTheSelectionFromWords() {
        ResourceSelection selection = ResourceSelection.parse(resources, new String[]{"cmd", "Fake", "grass,x", "re:tile_.*"}, 1);
        assertTrue(selection.matches(FakeData.class, grass));
        assertTrue(selection.matches(FakeData.class, sand));

        assertTrue(ResourceSelection.parse(resources, new String[]{"cmd", "all"}, 1).matches(FakeData.class, grass));
        assertFalse(ResourceSelection.parse(resources, new String[]{"cmd", "all", "re:tile_.*"}, 1)
                .matches(FakeData.class, grass));
    }

    @Test
    void parseRejectsWhatIsWrong() {
        assertThrows(IllegalArgumentException.class, () -> ResourceSelection.parse(resources, new String[]{"cmd"}, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceSelection.parse(resources, new String[]{"cmd", "Nope"}, 1));
        assertThrows(IllegalArgumentException.class, () -> ResourceSelection.parse(resources, new String[]{"cmd", "all", "re:(["}, 1));
    }

    @Test
    void reloadOnlyTheSelectedData() throws IOException {
        int grassVersion = grass.getVersion();
        int sandVersion = sand.getVersion();

        assertEquals(2, resources.reload(ResourceSelection.parse(resources, new String[]{"cmd", "Fake", "re:tile_.*"}, 1)));
        pump();

        assertEquals(grassVersion, grass.getVersion());
        assertEquals(sandVersion + 1, sand.getVersion());
        assertEquals(sandVersion + 1, stone.getVersion());
    }

    @Test
    void reloadingNothingIsNotAnError() {
        assertEquals(0, resources.reload(ResourceSelection.parse(resources, new String[]{"cmd", "Fake", "nothing"}, 1)));
    }

    @Test
    void describeListsTheSelectedData() {
        JSONObject result = resources.describe(ResourceSelection.parse(resources, new String[]{"cmd", "Fake", "grass"}, 1));

        assertEquals(1, result.getInt("count"));
        JSONObject item = result.getJSONArray("data").getJSONObject(0);
        assertEquals("grass", item.getString("name"));
        assertEquals("loaded", item.getString("state"));
        assertFalse(item.getJSONArray("references").isEmpty());
    }
}
