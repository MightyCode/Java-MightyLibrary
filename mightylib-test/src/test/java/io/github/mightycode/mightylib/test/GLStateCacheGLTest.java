package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.GLStateCache;
import io.github.mightycode.mightylib.graphics.renderer.EBlendMode;
import io.github.mightycode.mightylib.graphics.renderer.RenderState;
import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL30C;

import static org.junit.jupiter.api.Assertions.*;

// The state the cache of the context knows is the one OpenGL has, after a series of changes
@MightySceneTest(config = SceneRunnerTest.Config.class)
class GLStateCacheGLTest {
    private static boolean enabled(int capability) {
        return GL11C.glIsEnabled(capability);
    }

    private static void assertSameAsGL(GLStateCache state) {
        assertEquals(state.isEnabled(GLStateCache.BLEND), enabled(GL11C.GL_BLEND));
        assertEquals(state.isEnabled(GLStateCache.DEPTH_TEST), enabled(GL11C.GL_DEPTH_TEST));
        assertEquals(state.isEnabled(GLStateCache.CULL_FACE), enabled(GL11C.GL_CULL_FACE));
        assertEquals(state.isEnabled(GLStateCache.FRAMEBUFFER_SRGB), enabled(GL30C.GL_FRAMEBUFFER_SRGB));
        assertEquals(state.isEnabled(GLStateCache.SCISSOR_TEST), enabled(GL11C.GL_SCISSOR_TEST));
        assertEquals(state.isDepthMaskWritable(), GL11C.glGetBoolean(GL11C.GL_DEPTH_WRITEMASK));
        if (state.isEnabled(GLStateCache.BLEND)) {
            assertEquals(state.blendSource(), GL11C.glGetInteger(GL30C.GL_BLEND_SRC_RGB));
            assertEquals(state.blendDestination(), GL11C.glGetInteger(GL30C.GL_BLEND_DST_RGB));
        }
    }

    @Test
    void theCacheIsTheStateOfOpenGL(SceneRunner runner) {
        runner.frames(1);
        GLStateCache state = GLContext.currentState();
        assertSameAsGL(state);

        RenderState renderState = new RenderState(true, false, true);
        for (EBlendMode mode : EBlendMode.values()) {
            renderState.setBlendMode(mode);
            renderState.apply(mode.ordinal() % 2 == 0);
            assertSameAsGL(state);
        }

        state.scissor(1, 2, 30, 40);
        int[] box = new int[4];
        GL11C.glGetIntegerv(GL11C.GL_SCISSOR_BOX, box);
        assertArrayEquals(new int[]{1, 2, 30, 40}, box);
        state.disableScissor();

        GLContext.ClearColorAndDepth();
        assertSameAsGL(state);

        // Bound texture of a unit, the active unit left where the cache says
        state.bindTexture(3, GL11C.GL_TEXTURE_2D, 0);
        assertEquals(GL13C.GL_TEXTURE0 + 3, GL11C.glGetInteger(GL13C.GL_ACTIVE_TEXTURE));

        // Changed behind the cache, then invalidated: the next change is sent
        GL11C.glDisable(GL11C.GL_DEPTH_TEST);
        RenderState.invalidateContextState();
        new RenderState(true, true, false).apply(false);
        assertTrue(enabled(GL11C.GL_DEPTH_TEST));
        assertSameAsGL(state);

        // Back to the default of the engine
        new RenderState(true, true, false).apply(false);
    }
}
