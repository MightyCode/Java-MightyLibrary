package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The hooks of the scenes in their order: a change unloads and releases the previous scene before the next one is
 * set; a scene left while it loads is only released; its loading scene is launched first, shown until the scene is
 * ready, then unloaded; an exit asked by a scene stops its updates and gives its status.
 */
@MightySceneTest(config = SceneRunnerTest.Config.class)
class SceneLifecycleGLTest {
    // Writes each hook it goes through (the first update and the first display only)
    static final class RecordingScene extends Scene {
        private final String name;
        private final List<String> events;
        private final RecordingLoading loading;
        private boolean updated;
        int updates;
        int exitAt = -1;

        RecordingScene(String name, List<String> events, RecordingLoading loading) {
            this.name = name;
            this.events = events;
            this.loading = loading;
        }

        @Override
        protected TemplateSceneLoading createLoadingScene(String[] args) {
            return loading;
        }

        @Override
        public void init(String[] args) {
            events.add(name + ".init" + Arrays.toString(args));
        }

        @Override
        public void launch(String[] args) {
            events.add(name + ".launch");
        }

        @Override
        public void update() {
            super.update();
            ++updates;
            if (!updated)
                events.add(name + ".update");
            updated = true;
            if (updates == exitAt)
                sceneManager().exit(3);
        }

        @Override
        public void unload() {
            events.add(name + ".unload");
            super.unload();
        }

        @Override
        public void release() {
            events.add(name + ".release");
        }
    }

    // A loading scene that waits for its gate
    static final class RecordingLoading extends TemplateSceneLoading {
        private final List<String> events;
        boolean open;
        private boolean updated, displayed;

        RecordingLoading(List<String> events) {
            this.events = events;
            addElementToLoad(() -> open);
        }

        @Override
        public void launch(String[] args) {
            events.add("loading.launch");
        }

        @Override
        public void update() {
            super.update();
            if (!updated)
                events.add("loading.update");
            updated = true;
        }

        @Override
        public void display() {
            if (!displayed)
                events.add("loading.display");
            displayed = true;
        }

        @Override
        public void unload() {
            events.add("loading.unload");
            super.unload();
        }
    }

    @Test
    void aChangeUnloadsAndReleasesThePreviousSceneBeforeTheNextIsSet(SceneRunner runner) {
        List<String> events = new ArrayList<>();
        runner.start(() -> new RecordingScene("a", events, null), "x", "y").untilLoaded();
        runner.frames(2);
        assertTrue(runner.scene().isLaunched());
        runner.start(() -> new RecordingScene("b", events, null)).untilLoaded();
        runner.frames(1);

        assertEquals(Arrays.asList("a.init[x, y]", "a.launch", "a.update", "a.unload", "a.release", "b.init[]",
                "b.launch", "b.update"), events);
    }

    @Test
    void theLoadingSceneIsShownUntilTheSceneIsReady(SceneRunner runner) {
        List<String> events = new ArrayList<>();
        RecordingLoading loading = new RecordingLoading(events);
        RecordingScene scene = new RecordingScene("a", events, loading);
        runner.start(() -> scene).frames(5);

        assertEquals(Arrays.asList("loading.launch", "a.init[]", "loading.update", "loading.display"), events);
        assertFalse(scene.isLaunched(), "waits for the element of its loading scene");

        loading.open = true;
        runner.untilLoaded();
        runner.frames(2);
        assertEquals(Arrays.asList("loading.launch", "a.init[]", "loading.update", "loading.display", "a.launch",
                "loading.unload", "a.update"), events);
    }

    @Test
    void aSceneLeftWhileItLoadsIsOnlyReleased(SceneRunner runner) {
        List<String> events = new ArrayList<>();
        RecordingLoading loading = new RecordingLoading(events);
        runner.start(() -> new RecordingScene("a", events, loading)).frames(3);
        runner.start(() -> new RecordingScene("b", events, null)).untilLoaded();

        assertEquals(Arrays.asList("loading.launch", "a.init[]", "loading.update", "loading.display",
                "loading.unload", "a.release", "b.init[]", "b.launch"), events);
    }

    // The exit is asked at the third update: the scene is no longer updated, the manager gives the status
    @Test
    void anExitStopsTheUpdatesAndGivesItsStatus(SceneRunner runner) {
        List<String> events = new ArrayList<>();
        RecordingScene scene = new RecordingScene("a", events, null);
        scene.exitAt = 3;
        runner.start(() -> scene).untilLoaded();
        ISceneManager manager = runner.engine().engine().contexts().requireMainWindow().getContext()
                .getSceneManager();
        assertFalse(manager.isExitRequested());

        runner.frames(6);
        assertTrue(manager.isExitRequested());
        assertEquals(3, manager.getExitStatus());
        assertEquals(3, scene.updates);

        // A scene set after it starts again
        runner.toEmptyScene().untilLoaded();
        assertFalse(manager.isExitRequested());
    }
}
