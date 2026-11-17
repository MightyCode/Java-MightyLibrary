package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.data.JSONLoader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_CONTEXT;
import static org.junit.jupiter.api.Assertions.*;

// The batch of the engine (EngineResources.ENGINE_BATCH): loaded by Resources.load before the first batch of the game
class ResourcesEngineBatchTest extends ResourcesTestBase {
    private static final String GAME_BATCH = "game";

    private void useMethod(Resources.LoadingMethod method) {
        resources = new Resources(method, WORKERS, LoaderTestAccess.sources(folder),
                Arrays.asList(fakeLoader, new JSONLoader(), new BatchLoader()));
    }

    // Runs Resources.load, returns what it wrote on System.out (index 0) and System.err (index 1)
    private String[] loadCapturing() {
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            resources.init();
            resources.load();
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
        return new String[]{out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8)};
    }

    @Test
    void loadedBeforeTheFirstBatchAndKeptAfterIt() throws Exception {
        FakeData engine = fake(MAIN_CONTEXT, "engineData");
        FakeData game = fake(THREAD_CONTEXT, "gameData");
        batch(EngineResources.ENGINE_BATCH, "engineData");
        batch(GAME_BATCH, "gameData");
        useMethod(new Resources.BatchResourcesMethod(GAME_BATCH));

        String[] output = loadCapturing();

        assertEquals("", output[1]);
        int engineLoad = output[0].indexOf("--Load batch: " + EngineResources.ENGINE_BATCH);
        int gameLoad = output[0].indexOf("--Load batch: " + GAME_BATCH);
        assertTrue(engineLoad >= 0 && gameLoad > engineLoad, output[0]);
        assertTrue(engine.isLoaded());
        assertTrue(game.isLoaded());
        assertTrue(engine.references.contains(EngineResources.ENGINE_BATCH));

        resources.unloadBatch(GAME_BATCH);
        pump();
        assertTrue(engine.isLoaded());
        assertFalse(game.isLoaded());
        assertConsistent();
    }

    // Everything is loaded already: the batch of the engine still holds its data
    @Test
    void referencedWithTheMethodThatLoadsEverything() throws Exception {
        FakeData engine = fake(MAIN_CONTEXT, "engineData");
        FakeData other = fake(THREAD_CONTEXT, "other");
        batch(EngineResources.ENGINE_BATCH, "engineData");
        useMethod(new Resources.AllResourcesMethod());

        String[] output = loadCapturing();

        assertEquals("", output[1]);
        assertTrue(output[0].contains("--Load batch: " + EngineResources.ENGINE_BATCH), output[0]);
        assertTrue(engine.isLoaded());
        assertTrue(other.isLoaded());
        assertTrue(engine.references.contains(EngineResources.ENGINE_BATCH));
        assertFalse(other.references.contains(EngineResources.ENGINE_BATCH));
        assertConsistent();
    }

    // Source of the engine missing or incomplete: one clear line, the game loads anyway
    @Test
    void missingBatchIsReported() throws Exception {
        FakeData game = fake(THREAD_CONTEXT, "gameData");
        batch(GAME_BATCH, "gameData");
        useMethod(new Resources.BatchResourcesMethod(GAME_BATCH));

        String[] output = loadCapturing();

        assertTrue(output[1].contains("the batch of the engine " + EngineResources.ENGINE_BATCH + " is in no source"),
                output[1]);
        assertEquals(1, output[1].trim().split("\n").length, output[1]);
        assertTrue(game.isLoaded());
        assertConsistent();
    }

    @Test
    void missingBatchThrowsInStrictMode() throws Exception {
        fake(THREAD_CONTEXT, "gameData");
        batch(GAME_BATCH, "gameData");
        useMethod(new Resources.BatchResourcesMethod(GAME_BATCH));
        resources.setStrict(true);
        resources.init();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, resources::load);
        assertTrue(thrown.getMessage().contains(EngineResources.ENGINE_BATCH), thrown.getMessage());
    }
}
