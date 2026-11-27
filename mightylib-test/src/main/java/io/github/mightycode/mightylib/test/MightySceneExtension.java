package io.github.mightycode.mightylib.test;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.opentest4j.TestAbortedException;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * The extension of @MightySceneTest: one engine with a hidden window per configuration, kept in the root store
 * (closed at the end of the run, or when another engine starts), leak check around each test.
 */
public final class MightySceneExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MightySceneExtension.class);

    /** True (the CI): a test with GL fails without GL instead of being skipped, so none is skipped unseen. */
    public static final String REQUIRE_GL_PROPERTY = "mightylib.test.requireGL";

    // The engine kept between the tests, and what it was started from
    static final class Kept implements ExtensionContext.Store.CloseableResource {
        TestEngine engine;
        Class<?> config;
        String[] sources;
        boolean strictResources;
        // GL unavailable here: every scene test is skipped with this reason
        String unavailable;

        // The engine was started from these settings (the problems and leaks of a test do not count)
        boolean sameSettings(MightySceneTest settings) {
            return config == settings.config() && Arrays.equals(sources, settings.sources())
                    && strictResources == settings.strictResources();
        }

        void remember(MightySceneTest settings) {
            config = settings.config();
            sources = settings.sources().clone();
            strictResources = settings.strictResources();
        }

        @Override
        public void close() {
            if (engine != null)
                engine.close();
            engine = null;
        }
    }

    private static MightySceneTest settings(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getRequiredTestClass(), MightySceneTest.class)
                .orElseThrow(() -> new ParameterResolutionException("A SceneRunner needs @MightySceneTest on the class"));
    }

    private static Kept kept(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent(Kept.class, key -> new Kept(), Kept.class);
    }

    private static SceneRunner runner(ExtensionContext context) {
        MightySceneTest settings = settings(context);
        Kept kept = kept(context);
        if (kept.unavailable != null)
            throw unavailable(kept.unavailable, null);

        boolean same = kept.engine != null && !kept.engine.isClosed() && kept.sameSettings(settings);
        if (!same) {
            kept.close();
            TestEngine engine = MightyTest.withWindow(MightyEngineExtension.newConfig(settings.config()));
            for (String source : settings.sources())
                engine.addSource(Paths.get(source));
            if (!settings.strictResources())
                engine.allowResourceFailures();
            try {
                engine.start();
            } catch (GLUnavailableException e) {
                kept.unavailable = e.getMessage();
                throw unavailable(kept.unavailable, e);
            }
            engine.markShared();
            kept.engine = engine;
            kept.remember(settings);
        }

        return kept.engine.scenes();
    }

    private static RuntimeException unavailable(String reason, Throwable cause) {
        if (Boolean.getBoolean(REQUIRE_GL_PROPERTY))
            throw new AssertionError("Test with GL without GL (" + REQUIRE_GL_PROPERTY + "=true): " + reason, cause);
        return new TestAbortedException("Test with GL skipped: " + reason, cause);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        SceneRunner runner = runner(context);
        runner.toEmptyScene();
        if (settings(context).checkLeaks())
            context.getStore(NAMESPACE).put(Map.class, runner.leakSnapshot());
        runner.engine().beginTest();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterEach(ExtensionContext context) {
        Kept kept = kept(context);
        if (kept.engine == null || kept.engine.isClosed())
            return;

        SceneRunner runner = kept.engine.scenes();
        boolean testFailed = context.getExecutionException().isPresent();
        AssertionError leak = null;
        try {
            runner.toEmptyScene();
            Map<String, Integer> before = context.getStore(NAMESPACE).remove(Map.class, Map.class);
            if (before != null && !testFailed)
                runner.assertNoLeak(before);
        } catch (AssertionError e) {
            leak = e;
        } catch (RuntimeException e) {
            // The engine may be in a bad state: the next test starts a new one
            AssertionError problems = kept.engine.endTest(true);
            kept.close();
            if (problems != null)
                e.addSuppressed(problems);
            throw e;
        }

        // After the return to the empty scene: the unload of the scene of the test is part of it
        AssertionError problems = kept.engine.endTest(testFailed);
        // A test that threw (not an assertion) may leave a half-loaded scene whose objects are freed later, in the
        // next test: the next test starts a new engine
        Throwable thrown = context.getExecutionException().orElse(null);
        if (thrown != null && !(thrown instanceof AssertionError))
            kept.close();
        AssertionError failure = Combined(leak, problems);
        if (failure != null)
            throw failure;
    }

    // One failure for the leaks and the problems of a test (the stack trace of the first error as cause)
    static AssertionError Combined(AssertionError leak, AssertionError problems) {
        if (leak == null)
            return problems;
        if (problems == null)
            return leak;
        AssertionError both = new AssertionError(leak.getMessage() + "\n" + problems.getMessage(), problems.getCause());
        both.addSuppressed(leak);
        return both;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        Class<?> type = parameter.getParameter().getType();
        return type == SceneRunner.class || type == TestEngine.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        SceneRunner runner = Objects.requireNonNull(kept(context).engine, "no engine").scenes();
        return parameter.getParameter().getType() == SceneRunner.class ? runner : runner.engine();
    }
}
