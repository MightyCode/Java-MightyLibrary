package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;

import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_SRGB;

/**
 * The OpenGL state a renderer draws with: depth test, depth write, face culling and color space, applied before each
 * draw. The depth mask is left writable and the sRGB encoding disabled between draws (glClear depends on them).
 */
public final class RenderState {
    private boolean depthTest;
    private boolean depthWrite;
    private boolean faceCulling;
    // Level of the renderer: INHERIT takes the one of the view, then of the engine
    private ERenderColorSpace colorSpace = ERenderColorSpace.INHERIT;
    // The mode of the last apply, undone by restore
    private boolean linearApplied;

    public RenderState(boolean depthTest, boolean depthWrite, boolean faceCulling) {
        this.depthTest = depthTest;
        this.depthWrite = depthWrite;
        this.faceCulling = faceCulling;
    }

    public boolean isDepthTest() {
        return depthTest;
    }

    public void setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
    }

    public boolean isDepthWrite() {
        return depthWrite;
    }

    public void setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
    }

    public boolean isFaceCulling() {
        return faceCulling;
    }

    public void setFaceCulling(boolean faceCulling) {
        this.faceCulling = faceCulling;
    }

    public ERenderColorSpace getColorSpace() {
        return colorSpace;
    }

    public void setColorSpace(ERenderColorSpace colorSpace) {
        this.colorSpace = Objects.requireNonNull(colorSpace, "colorSpace");
    }

    // Without depth test nothing is written to the depth buffer: the mask matters only with the test
    private boolean masksDepth() {
        return depthTest && !depthWrite;
    }

    // A gamma draw
    public void apply() {
        apply(false);
    }

    // linear: the resolved mode of the draw (ERenderColorSpace.Resolve), the card encodes what is written in sRGB
    public void apply(boolean linear) {
        linearApplied = linear;
        if (linear)
            glEnable(GL_FRAMEBUFFER_SRGB);

        if (depthTest)
            glEnable(GL_DEPTH_TEST);
        else
            glDisable(GL_DEPTH_TEST);

        if (masksDepth())
            glDepthMask(false);

        if (faceCulling)
            glEnable(GL_CULL_FACE);
        else
            glDisable(GL_CULL_FACE);
    }

    // After the draw: the depth mask back to writable, the sRGB encoding back to disabled
    public void restore() {
        if (masksDepth())
            glDepthMask(true);

        if (linearApplied) {
            glDisable(GL_FRAMEBUFFER_SRGB);
            linearApplied = false;
        }
    }
}
