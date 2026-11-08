package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ViewsTest {
    private static final class FakeView implements IView {
        @Override public Engine engine() { return null; }
        @Override public WindowContext context() { return null; }
        @Override public Camera2D camera2D() { return null; }
        @Override public Camera3D camera3D() { return null; }
    }

    @BeforeEach
    void setUp() {
        MainThreadTestAccess.setCurrentAsMain();
        // A previous test of this thread must not leave a view
        assertThrows(IllegalStateException.class, Views::current);
    }

    @Test
    void thereIsNoDefaultView() {
        IllegalStateException e = assertThrows(IllegalStateException.class, Views::current);
        assertTrue(e.getMessage().contains("No current view"));
    }

    @Test
    void theViewIsCurrentInsideTheScopeOnly() {
        FakeView view = new FakeView();

        try (Views.Scope ignored = Views.enter(view)) {
            assertSame(view, Views.current());
        }

        assertThrows(IllegalStateException.class, Views::current);
    }

    @Test
    void nestedScopesRestoreTheOuterView() {
        FakeView outer = new FakeView();
        FakeView inner = new FakeView();

        try (Views.Scope ignored = Views.enter(outer)) {
            try (Views.Scope ignoredInner = Views.enter(inner)) {
                assertSame(inner, Views.current());
            }

            assertSame(outer, Views.current());
        }
    }

    @Test
    void theScopeIsRestoredWhenTheCodeThrows() {
        FakeView view = new FakeView();

        assertThrows(RuntimeException.class, () -> {
            try (Views.Scope ignored = Views.enter(view)) {
                throw new RuntimeException("boom");
            }
        });

        assertThrows(IllegalStateException.class, Views::current);
    }

    @Test
    void anotherThreadHasNoCurrentView() throws InterruptedException {
        AtomicReference<Throwable> seen = new AtomicReference<>();

        try (Views.Scope ignored = Views.enter(new FakeView())) {
            Thread worker = new Thread(() -> {
                try {
                    Views.current();
                } catch (Throwable e) {
                    seen.set(e);
                }
            });
            worker.start();
            worker.join();
        }

        assertTrue(seen.get() instanceof IllegalStateException);
    }

    @Test
    void aMainThreadTaskRunsWithTheViewThatPostedIt() {
        FakeView view = new FakeView();
        AtomicReference<IView> seenByTask = new AtomicReference<>();

        try (Views.Scope ignored = Views.enter(view)) {
            MainThread.execute(() -> seenByTask.set(Views.current()));
        }

        // Run later, outside any view: the task still gets its own
        MainThreadTestAccess.processAll();
        assertSame(view, seenByTask.get());
        assertThrows(IllegalStateException.class, Views::current);
    }

    @Test
    void aTaskPostedWithoutViewRunsWithoutView() {
        AtomicReference<Throwable> seenByTask = new AtomicReference<>();

        MainThread.execute(() -> {
            try {
                Views.current();
            } catch (Throwable e) {
                seenByTask.set(e);
            }
        });

        MainThreadTestAccess.processAll();
        assertTrue(seenByTask.get() instanceof IllegalStateException);
    }
}
