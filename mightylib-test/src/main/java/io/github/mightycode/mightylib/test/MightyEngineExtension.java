package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * The extension of @MightyEngineTest: a started headless engine per test, closed after it. At its close, an error of
 * the Logger channel (during the test or its shutdown) no announce of the test covers fails the test.
 */
public final class MightyEngineExtension implements ParameterResolver, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MightyEngineExtension.class);

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == TestEngine.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        MightyEngineTest settings = AnnotationSupport.findAnnotation(context.getRequiredTestClass(),
                MightyEngineTest.class).orElseThrow(() ->
                new ParameterResolutionException("A TestEngine parameter needs @MightyEngineTest on the class"));

        // Stored in the context of the test: JUnit closes it after the test (CloseableResource)
        return context.getStore(NAMESPACE).getOrComputeIfAbsent(TestEngine.class, key -> {
            TestEngine engine = MightyTest.headless(newConfig(settings.config()));
            for (String source : settings.sources())
                engine.addSource(Paths.get(source));
            return new ClosedAfterTest(engine.start());
        }, ClosedAfterTest.class).engine;
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ClosedAfterTest started = context.getStore(NAMESPACE).get(TestEngine.class, ClosedAfterTest.class);
        // The engine is checked at close (CloseableResource of the test): the problems of its shutdown are checked
        // with the announces of the test
        if (started != null && !started.engine.isClosed() && context.getExecutionException().isPresent())
            started.engine.markTestFailed();
    }

    static EngineConfig newConfig(Class<? extends Supplier<EngineConfig>> type) {
        try {
            Constructor<? extends Supplier<EngineConfig>> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            EngineConfig config = constructor.newInstance().get();
            if (config == null)
                throw new ParameterResolutionException(type.getName() + " gave no configuration");
            return config;
        } catch (ReflectiveOperationException e) {
            throw new ParameterResolutionException("The configuration " + type.getName()
                    + " can't be created (constructor without parameter)", e);
        }
    }

    static final class ClosedAfterTest implements ExtensionContext.Store.CloseableResource {
        final TestEngine engine;

        ClosedAfterTest(TestEngine engine) {
            this.engine = engine;
        }

        @Override
        public void close() {
            engine.close();
        }
    }
}
