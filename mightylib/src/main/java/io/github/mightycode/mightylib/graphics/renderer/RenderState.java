package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.internal.GLStateCache;

import java.util.Objects;

/**
 * The OpenGL state a renderer draws with: depth test, depth write, face culling, blend and color space, applied
 * before each draw through the state cache of the current context (a state already set is not sent again). The
 * state is left as the last draw set it; a clear of a view sets what glClear depends on itself.
 */
public final class RenderState {
    private boolean depthTest;
    private boolean depthWrite;
    private boolean faceCulling;
    // Level of the renderer: INHERIT takes the one of the view, then of the engine
    private ERenderColorSpace colorSpace = ERenderColorSpace.INHERIT;
    private EBlendMode blendMode = EBlendMode.ALPHA;

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

    public EBlendMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(EBlendMode blendMode) {
        this.blendMode = Objects.requireNonNull(blendMode, "blendMode");
    }

    /**
     * The OpenGL state of the current context was changed without the engine (raw GL call, third-party library):
     * every next state change is sent again. Main thread only.
     */
    public static void invalidateContextState() {
        GLContext.currentState().invalidate();
    }

    // A gamma draw
    public void apply() {
        apply(false);
    }

    // linear: the resolved mode of the draw (ERenderColorSpace.Resolve), the card encodes what is written in sRGB
    public void apply(boolean linear) {
        GLStateCache state = GLContext.currentState();
        state.capability(GLStateCache.FRAMEBUFFER_SRGB, linear);
        state.capability(GLStateCache.DEPTH_TEST, depthTest);
        // Without depth test nothing is written to the depth buffer: the mask is left writable
        state.depthMask(!depthTest || depthWrite);
        state.capability(GLStateCache.CULL_FACE, faceCulling);
        state.blend(blendMode.isEnabled(), blendMode.source(), blendMode.destination());
    }
}
