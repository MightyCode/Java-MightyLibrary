package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.utils.GameTime;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// @MightyEngineTest: a started headless engine per test, closed after it
@MightyEngineTest(config = EngineExtensionTest.Config.class)
class EngineExtensionTest {
    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            return HeadlessTestEngineTest.config(Paths.get("target", "no-game-folder"));
        }
    }

    private static TestEngine previous;

    @AfterAll
    static void theLastEngineIsClosed() {
        assertThrows(IllegalStateException.class, previous::step);
    }

    private void check(TestEngine engine) {
        assertNotSame(previous, engine);
        if (previous != null)
            assertThrows(IllegalStateException.class, previous::step);
        previous = engine;

        assertTrue(engine.step(2).execute("kitgame:time").isSuccess());
        assertEquals(1f / 50f, GameTime.DeltaTime(), 1e-6);
    }

    @Test
    void anEngineIsGiven(TestEngine engine) {
        check(engine);
    }

    @Test
    void eachTestHasItsOwnEngine(TestEngine engine) {
        check(engine);
    }
}
