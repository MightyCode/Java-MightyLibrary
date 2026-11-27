package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.Workers;
import io.github.mightycode.mightylib.registry.EngineRegistry;
import io.github.mightycode.mightylib.registry.IPlugin;
import io.github.mightycode.mightylib.utils.ELogLevel;
import io.github.mightycode.mightylib.utils.LogEntry;
import io.github.mightycode.mightylib.utils.Logger;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

// A test of the kit fails on an error of the Logger channel it did not announce, like LogAssert of Unity
class ProblemsKitTest {
    private static final String CATEGORY = "kittest";

    private static EngineConfig config() {
        return HeadlessTestEngineTest.config(Paths.get("target", "no-game-folder"));
    }

    private static TestEngine started() {
        return MightyTest.headless(config()).start();
    }

    @Test
    void unexpectedErrorFailsTheTest() {
        try (TestEngine engine = started()) {
            Logger.Error(CATEGORY, "Uniform color not found");

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertTrue(failure.getMessage().contains("1 unexpected error(s)"), failure.getMessage());
            assertTrue(failure.getMessage().contains("ERROR [kittest] Uniform color not found"), failure.getMessage());
            // Where it was sent from, for an error without exception
            assertNotNull(failure.getCause());
            assertTrue(failure.getCause().getStackTrace().length > 0);
            assertTrue(stackContains(failure.getCause(), "unexpectedErrorFailsTheTest"));

            // Checked: the next test starts without it
            assertNull(engine.endTest(false));
        }
    }

    @Test
    void exceptionIsTheCauseOfTheFailure() {
        try (TestEngine engine = started()) {
            IllegalStateException boom = new IllegalStateException("boom");
            Logger.LogException("Hook failed", boom);
            Logger.Error(CATEGORY, "second");

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertSame(boom, failure.getCause());
            assertTrue(failure.getMessage().contains("2 unexpected error(s)"));
        }
    }

    @Test
    void announcedErrorPasses() {
        try (TestEngine engine = started()) {
            engine.expectProblem(ELogLevel.ERROR, "not fo+und");
            Logger.Error(CATEGORY, "Uniform color not found");
            Logger.Error(CATEGORY, "Uniform size not found");

            assertNull(engine.endTest(false));
        }
    }

    @Test
    void announceMatchesTheException() {
        try (TestEngine engine = started()) {
            engine.expectProblem(ELogLevel.ERROR, "IllegalStateException: boom");
            Logger.LogException("Hook failed", new IllegalStateException("boom"));

            assertNull(engine.endTest(false));
        }
    }

    @Test
    void announceOfAnotherLevelDoesNotCover() {
        try (TestEngine engine = started()) {
            engine.expectProblem(ELogLevel.WARNING, "not found");
            Logger.Error(CATEGORY, "Uniform color not found");

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertTrue(failure.getMessage().contains("unexpected error"));
            assertTrue(failure.getMessage().contains("never came:\n  WARNING matching \"not found\""),
                    failure.getMessage());
        }
    }

    @Test
    void announceNeverMetFails() {
        try (TestEngine engine = started()) {
            engine.expectProblem(ELogLevel.ERROR, "never sent");

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertTrue(failure.getMessage().contains("Announced problem(s) that never came:\n  ERROR matching \"never sent\""),
                    failure.getMessage());
            assertNull(failure.getCause());
        }
    }

    // A test that failed already: its unmet announces are not added to its failure
    @Test
    void failedTestOnlyReportsItsErrors() {
        try (TestEngine engine = started()) {
            engine.expectProblem(ELogLevel.ERROR, "never sent");
            assertNull(engine.endTest(true));

            Logger.Error(CATEGORY, "real");
            assertNotNull(engine.endTest(true));
        }
    }

    @Test
    void warningPassesAndIsKept() {
        try (TestEngine engine = started()) {
            Logger.Warn(CATEGORY, "Unknown property");

            List<LogEntry> problems = engine.problems();
            assertTrue(problems.stream().anyMatch(entry -> entry.level() == ELogLevel.WARNING
                    && entry.message().equals("Unknown property")));
            assertNull(engine.endTest(false));
            assertTrue(engine.problems().isEmpty());
        }
    }

    // The warnings are listed in the message of a failure
    @Test
    void warningsAreListedInAFailure() {
        try (TestEngine engine = started()) {
            Logger.Warn(CATEGORY, "Unknown property");
            Logger.Error(CATEGORY, "bug");

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertTrue(failure.getMessage().contains("Warnings (1):\n  WARNING [kittest] Unknown property"),
                    failure.getMessage());
        }
    }

    @Test
    void errorOfAWorkerCountsInTheCurrentTest() throws Exception {
        try (TestEngine engine = started()) {
            Workers.run(() -> Logger.Error(CATEGORY, "from a worker")).get(10, TimeUnit.SECONDS);

            AssertionError failure = engine.endTest(false);
            assertNotNull(failure);
            assertTrue(failure.getMessage().contains("from a worker"));
            assertFalse(failure.getMessage().contains("(thread " + Thread.currentThread().getName() + ")"),
                    failure.getMessage());
        }
    }

    // A plugin that sends an error while the engine starts
    private static final class ErrorPlugin implements IPlugin {
        @Override
        public String namespace() {
            return "kiterror";
        }

        @Override
        public void register(EngineRegistry registry) {
            Logger.Error(CATEGORY, "broken plugin");
        }
    }

    @Test
    void errorWhileStartingFailsTheStart() {
        TestEngine engine = MightyTest.headless(config().addPlugin(new ErrorPlugin()));
        IllegalStateException failure = assertThrows(IllegalStateException.class, engine::start);
        assertTrue(failure.getMessage().contains("The engine started with errors"), failure.getMessage());
        assertTrue(failure.getMessage().contains("broken plugin"), failure.getMessage());
        assertTrue(engine.isClosed());

        // Closed: the next engine starts
        started().close();
    }

    @Test
    void announcedErrorWhileStartingIsAllowed() {
        TestEngine engine = MightyTest.headless(config().addPlugin(new ErrorPlugin()))
                .expectProblem(ELogLevel.ERROR, "broken plugin");
        try (TestEngine running = engine.start()) {
            assertNull(running.endTest(false));
        }
    }

    @Test
    void loneEngineChecksAtClose() {
        TestEngine engine = started();
        Logger.Error(CATEGORY, "left for close");

        AssertionError failure = assertThrows(AssertionError.class, engine::close);
        assertTrue(failure.getMessage().contains("left for close"));
        assertTrue(engine.isClosed());
        // Everything was freed before the failure
        started().close();
    }

    @Test
    void problemsAfterCloseAreNotReceived() {
        TestEngine engine = started();
        engine.close();
        Logger.Error(CATEGORY, "after close");
        assertTrue(engine.problems().isEmpty());
    }

    @Test
    void leaksAndProblemsGiveOneFailure() {
        AssertionError leak = new AssertionError("Leak: GL objects");
        AssertionError problems = new AssertionError("1 unexpected error(s)", new IllegalStateException("cause"));

        AssertionError both = MightySceneExtension.Combined(leak, problems);
        assertEquals("Leak: GL objects\n1 unexpected error(s)", both.getMessage());
        assertSame(problems.getCause(), both.getCause());
        assertSame(leak, MightySceneExtension.Combined(leak, null));
        assertSame(problems, MightySceneExtension.Combined(null, problems));
        assertNull(MightySceneExtension.Combined(null, null));
    }

    // Two settings that differ by strictResources: two engines
    static final class Config implements Supplier<EngineConfig> {
        @Override
        public EngineConfig get() {
            return config();
        }
    }

    @MightySceneTest(config = Config.class)
    private static final class StrictGame {
    }

    @MightySceneTest(config = Config.class, strictResources = false)
    private static final class LenientGame {
    }

    @Test
    void lenientEngineIsNotTheStrictOne() {
        MightySceneTest strict = StrictGame.class.getAnnotation(MightySceneTest.class);
        MightySceneTest lenient = LenientGame.class.getAnnotation(MightySceneTest.class);
        assertTrue(strict.strictResources());
        assertFalse(lenient.strictResources());

        MightySceneExtension.Kept kept = new MightySceneExtension.Kept();
        kept.remember(strict);
        assertTrue(kept.sameSettings(strict));
        assertFalse(kept.sameSettings(lenient));
        kept.remember(lenient);
        assertTrue(kept.sameSettings(lenient));
    }

    private static boolean stackContains(Throwable error, String method) {
        for (StackTraceElement element : error.getStackTrace()) {
            if (element.getMethodName().equals(method))
                return true;
        }
        return false;
    }
}
