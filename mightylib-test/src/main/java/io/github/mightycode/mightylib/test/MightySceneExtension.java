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

    // The engine kept between the tests, and what it was started from
    private static final class Kept implements ExtensionContext.Store.CloseableResource {
        TestEngine engine;
        Class<?> config;
        String[] sources;
        // GL unavailable here: every scene test is skipped with this reason
        String unavailable;

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
            throw new TestAbortedException(kept.unavailable);

        boolean same = kept.engine != null && !kept.engine.isClosed() && kept.config == settings.config()
                && Arrays.equals(kept.sources, settings.sources());
        if (!same) {
            kept.close();
            TestEngine engine = MightyTest.withWindow(MightyEngineExtension.newConfig(settings.config()));
            for (String source : settings.sources())
                engine.addSource(Paths.get(source));
            try {
                engine.start();
            } catch (GLUnavailableException e) {
                kept.unavailable = "Test with GL skipped: " + e.getMessage();
                throw new TestAbortedException(kept.unavailable, e);
            }
            engine.markShared();
            kept.engine = engine;
            kept.config = settings.config();
            kept.sources = settings.sources().clone();
        }

        return kept.engine.scenes();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        SceneRunner runner = runner(context);
        runner.toEmptyScene();
        if (settings(context).checkLeaks())
            context.getStore(NAMESPACE).put(Map.class, runner.leakSnapshot());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void afterEach(ExtensionContext context) {
        Kept kept = kept(context);
        if (kept.engine == null || kept.engine.isClosed())
            return;

        SceneRunner runner = kept.engine.scenes();
        try {
            runner.toEmptyScene();
            Map<String, Integer> before = context.getStore(NAMESPACE).remove(Map.class, Map.class);
            if (before != null && context.getExecutionException().isEmpty())
                runner.assertNoLeak(before);
        } catch (RuntimeException | AssertionError e) {
            // The engine may be in a bad state: the next test starts a new one
            if (!(e instanceof AssertionError))
                kept.close();
            throw e;
        }
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
