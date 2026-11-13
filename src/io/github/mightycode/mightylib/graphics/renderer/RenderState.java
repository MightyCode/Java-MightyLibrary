package io.github.mightycode.mightylib.graphics.renderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * The OpenGL state a renderer draws with: depth test, depth write and face culling, applied before each draw.
 * The depth mask is left writable between draws (glClear of the depth buffer depends on it).
 */
public class RenderState {
    private boolean depthTest;
    private boolean depthWrite;
    private boolean faceCulling;

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

    // Without depth test nothing is written to the depth buffer: the mask matters only with the test
    private boolean masksDepth() {
        return depthTest && !depthWrite;
    }

    public void apply() {
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

    // After the draw: the depth mask back to writable
    public void restore() {
        if (masksDepth())
            glDepthMask(true);
    }
}
