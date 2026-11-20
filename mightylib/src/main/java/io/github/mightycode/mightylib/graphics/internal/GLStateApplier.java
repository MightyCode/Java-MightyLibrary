package io.github.mightycode.mightylib.graphics.internal;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;

// The OpenGL calls of the GLStateCache of a context (its context is current when they run)
final class GLStateApplier implements IGLStateApplier {
    private static int glCapability(int capability) {
        switch (capability) {
            case GLStateCache.BLEND: return GL_BLEND;
            case GLStateCache.DEPTH_TEST: return GL_DEPTH_TEST;
            case GLStateCache.CULL_FACE: return GL_CULL_FACE;
            case GLStateCache.FRAMEBUFFER_SRGB: return GL_FRAMEBUFFER_SRGB;
            case GLStateCache.SCISSOR_TEST: return GL_SCISSOR_TEST;
            default: throw new IllegalArgumentException("Unknown capability " + capability);
        }
    }

    @Override
    public void capability(int capability, boolean enabled) {
        if (enabled)
            glEnable(glCapability(capability));
        else
            glDisable(glCapability(capability));
    }

    @Override
    public void blendFunc(int source, int destination) {
        glBlendFunc(source, destination);
    }

    @Override
    public void depthMask(boolean writable) {
        glDepthMask(writable);
    }

    @Override
    public void scissor(int x, int y, int width, int height) {
        glScissor(x, y, width, height);
    }

    @Override
    public void useProgram(int program) {
        glUseProgram(program);
    }

    @Override
    public void activeTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
    }

    @Override
    public void bindTexture(int target, int texture) {
        glBindTexture(target, texture);
    }
}
