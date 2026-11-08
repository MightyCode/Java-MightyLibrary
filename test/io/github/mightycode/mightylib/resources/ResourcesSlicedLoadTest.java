package io.github.mightycode.mightylib.resources;

import org.junit.jupiter.api.Test;

import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.MAIN_CONTEXT;
import static io.github.mightycode.mightylib.resources.DataType.TYPE_SET_UP.THREAD_PRELOAD_MAIN_LOAD;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Data read by a worker then loaded on the main thread in several steps (shaders): each step is a step of the
 * scheduler, the data is loaded only after the last one.
 */
class ResourcesSlicedLoadTest extends ResourcesTestBase {
    // The read is on a worker, every load step on the main thread (FakeData reports any other thread as an error)
    @Test
    void loadsInSeveralMainThreadSteps() throws Exception {
        FakeData shader = fake(THREAD_PRELOAD_MAIN_LOAD, "shader");
        shader.loadSteps = 4;
        batch("level", "shader");
        start();

        resources.loadBatch("level");
        pump();

        assertTrue(shader.isLoaded());
        assertEquals(4, shader.loadStepCalls.get());
        assertEquals(1, shader.loads.get(), "counted loaded once, at the last step");
        assertNotEquals(0, shader.content());
        assertConsistent();
    }

    // A step of a sliced load lets the other main thread steps go between two of its steps
    @Test
    void otherStepsGoBetweenTheSteps() throws Exception {
        FakeData shader = fake(THREAD_PRELOAD_MAIN_LOAD, "shader");
        shader.loadSteps = 3;
        FakeData other = fake(MAIN_CONTEXT, "other");
        batch("level", "shader", "other");
        start();

        resources.loadBatch("level");
        pump();

        assertTrue(shader.isLoaded() && other.isLoaded());
        assertConsistent();
    }

    // Scene left between two steps: nothing stays loaded, what the steps made is released
    @Test
    void leavingBetweenTheStepsReleasesTheData() throws Exception {
        FakeData shader = fake(THREAD_PRELOAD_MAIN_LOAD, "shader");
        shader.loadSteps = 50;
        shader.duringLoadStep = call -> {
            if (call == 2)
                resources.unloadBatch("level");
        };
        batch("level", "shader");
        start();

        resources.loadBatch("level");
        pump();
        shader.duringLoadStep = null;

        assertFalse(shader.isLoaded());
        assertEquals(0, shader.loads.get());
        assertEquals(2, shader.loadStepCalls.get(), "the remaining steps are not run");
        assertConsistent();

        // Wanted again: loaded from the start
        resources.loadBatch("level");
        shader.loadSteps = 2;
        pump();
        assertTrue(shader.isLoaded());
        assertConsistent();
    }

    // Hot reload: the copy is loaded in steps apart, the data stays usable until it is complete
    @Test
    void reloadsInSteps() throws Exception {
        FakeData shader = fake(THREAD_PRELOAD_MAIN_LOAD, "shader");
        shader.loadSteps = 3;
        batch("level", "shader");
        start();

        resources.loadBatch("level");
        pump();
        int version = shader.getVersion();
        int firstContent = shader.content();

        resources.reload(shader);
        pump();

        assertTrue(shader.isLoaded());
        assertTrue(shader.getVersion() > version);
        assertNotEquals(firstContent, shader.content());
        assertEquals(6, shader.loadStepCalls.get());
        assertConsistent();
    }
}
