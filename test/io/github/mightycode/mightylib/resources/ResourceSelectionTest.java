package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.main.MainThreadTestAccess;
import io.github.mightycode.mightylib.resources.data.JSONLoader;
import io.github.mightycode.mightylib.utils.enginecommand.CommandException;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandContext;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandParser;
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
import java.util.List;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

/** Selection written as words by the command syntax (ResourceArguments), reload and detail of a selection. */
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

    // The selection read by the syntax of "cmd <type> [names...] [--regex pattern]"
    private ResourceSelection select(String... words) {
        ResourceSelection[] read = new ResourceSelection[1];
        CommandNode root = CommandNode.root("cmd", "test").then(ResourceArguments.selection(() -> resources, "type",
                selection -> {
                    read[0] = selection;
                    return CommandResult.ok("ok");
                }));

        CommandContext context = new CommandContext("cmd", ECommandLevel.DEV, null);
        CommandParser.Parsed parsed = new CommandParser(root).parse(words, context);
        parsed.getHandler().run(parsed.getContext());
        return read[0];
    }

    private List<String> complete(String... words) {
        CommandNode root = CommandNode.root("cmd", "test").then(ResourceArguments.selection(() -> resources, "type",
                selection -> CommandResult.ok("ok")));
        return new CommandParser(root).complete(words, new CommandContext("cmd", ECommandLevel.DEV, null));
    }

    @Test
    void theSyntaxBuildsTheSelectionFromWords() {
        // A name in the list OR matching the regex
        ResourceSelection selection = select("Fake", "grass", "x", "--regex", "tile_.*");
        assertTrue(selection.matches(FakeData.class, grass));
        assertTrue(selection.matches(FakeData.class, sand));

        // The option can come before the names
        assertTrue(select("Fake", "--regex=tile_.*", "grass").matches(FakeData.class, grass));
        assertTrue(select("all").matches(FakeData.class, grass));
        assertTrue(select("fake").matches(FakeData.class, grass));
        assertFalse(select("all", "--regex", "tile_.*").matches(FakeData.class, grass));
    }

    @Test
    void theSyntaxRejectsWhatIsWrong() {
        assertThrows(CommandException.class, () -> select());
        assertThrows(CommandException.class, () -> select("Nope"));
        assertThrows(CommandException.class, () -> select("all", "--regex", "(["));
    }

    @Test
    void theSyntaxCompletesTypesAndNames() {
        assertTrue(complete("").containsAll(Arrays.asList("all", "Fake")));
        assertTrue(complete("Fake", "").containsAll(Arrays.asList("grass", "tile_sand", "tile_stone")));
        assertTrue(complete("Fake", "grass", "--").contains("--regex"));
    }

    @Test
    void reloadOnlyTheSelectedData() throws IOException {
        int grassVersion = grass.getVersion();
        int sandVersion = sand.getVersion();

        assertEquals(2, resources.reload(select("Fake", "--regex", "tile_.*")));
        pump();

        assertEquals(grassVersion, grass.getVersion());
        assertEquals(sandVersion + 1, sand.getVersion());
        assertEquals(sandVersion + 1, stone.getVersion());
    }

    @Test
    void reloadingNothingIsNotAnError() {
        assertEquals(0, resources.reload(select("Fake", "nothing")));
    }

    @Test
    void describeListsTheSelectedData() {
        JSONObject result = resources.describe(select("Fake", "grass"));

        assertEquals(1, result.getInt("count"));
        JSONObject item = result.getJSONArray("data").getJSONObject(0);
        assertEquals("grass", item.getString("name"));
        assertEquals("loaded", item.getString("state"));
        assertFalse(item.getJSONArray("references").isEmpty());
    }
}
