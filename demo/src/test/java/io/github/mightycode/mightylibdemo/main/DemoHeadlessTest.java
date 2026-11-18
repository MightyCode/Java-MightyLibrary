package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.test.MightyEngineTest;
import io.github.mightycode.mightylib.test.TestEngine;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of a test without GL: the demo started as the game (its plugin, its resources), without window, driven tick
 * by tick. Its commands, services and first batch of resources are checked.
 */
@MightyEngineTest(config = DemoTestConfig.class)
class DemoHeadlessTest {
    @Test
    void thePluginOfTheDemoSeesTheStart(TestEngine engine) {
        CommandResult result = engine.execute("demo:lifecycle");
        assertTrue(result.isSuccess(), result.toString());
        assertEquals(1, result.getData().getInt("started"));
        assertEquals(0, result.getData().getInt("sceneChanges"));
    }

    @Test
    void theServiceOfTheDemoCountsTheTicks(TestEngine engine) {
        TickCounterService counter = engine.engine().service(TickCounterService.KEY);
        long before = counter.getTicks();
        engine.step(5);
        assertEquals(before + 5, counter.getTicks());
    }

    @Test
    void theFirstBatchIsLoadedOnTheCpu(TestEngine engine) {
        engine.loadUntilDone();
        TextureData slime = engine.resources().getResource(TextureData.class, "slime");
        assertNotNull(slime);
        assertTrue(slime.isLoaded());
        // Without GL, the GL elements are only asked for
        assertFalse(engine.glElementRequests().isEmpty());
    }
}
