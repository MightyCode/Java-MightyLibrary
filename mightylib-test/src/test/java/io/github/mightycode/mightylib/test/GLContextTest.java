package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.utils.KitGraphicsAccess;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL32C;

import static org.junit.jupiter.api.Assertions.*;

// The hidden window of the kit has an OpenGL 3.3 core context (Mesa under Xvfb on the CI); without it, skipped
@MightySceneTest(config = SceneRunnerTest.Config.class)
class GLContextTest {
    @Test
    void theContextIsOpenGL33Core(SceneRunner runner) {
        runner.frames(1);
        String version = GL11C.glGetString(GL11C.GL_VERSION);
        System.out.println("--GL of the tests: " + version + " | " + GL11C.glGetString(GL11C.GL_RENDERER));

        int major = GL11C.glGetInteger(GL32C.GL_MAJOR_VERSION);
        int minor = GL11C.glGetInteger(GL32C.GL_MINOR_VERSION);
        assertTrue(major > 3 || (major == 3 && minor >= 3), version);
        assertNotEquals(0, GL11C.glGetInteger(GL32C.GL_CONTEXT_PROFILE_MASK) & GL32C.GL_CONTEXT_CORE_PROFILE_BIT,
                version);
    }

    // The images of the tests (SceneRunner.capture) need it: the pixels of a hidden window may not belong to it
    @Test
    void theDriverReadsTheImageOfAHiddenWindowBack(SceneRunner runner) {
        runner.frames(1);
        boolean reads = KitGraphicsAccess.readsBackBuffer(runner.scene().context().getWindow());
        System.out.println("--Image of a hidden window read back: " + (reads ? "yes" : "NO, no image test here"));
        assertTrue(reads, "This driver does not read the image of a hidden window back: SceneRunner.capture fails");
    }
}
