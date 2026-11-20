package io.github.mightycode.mightylib.graphics.renderer;

import static org.lwjgl.opengl.GL11.*;

/**
 * How a draw mixes its colors with those already drawn (glBlendFunc, the same factors for the alpha).
 * Straight colors (not multiplied by their alpha) unless the name says otherwise.
 */
public enum EBlendMode {
    /** Written as is, the alpha ignored. */
    NONE(false, GL_ONE, GL_ZERO),
    /** Transparency: color * alpha + background * (1 - alpha). The default. */
    ALPHA(true, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA),
    /** Colors already multiplied by their alpha: color + background * (1 - alpha). */
    PREMULTIPLIED(true, GL_ONE, GL_ONE_MINUS_SRC_ALPHA),
    /** Light: color * alpha added to the background. */
    ADDITIVE(true, GL_SRC_ALPHA, GL_ONE),
    /** Shadow, tint: color * background. The alpha is ignored (a transparent black pixel darkens too). */
    MULTIPLY(true, GL_DST_COLOR, GL_ZERO);

    private final boolean enabled;
    private final int source;
    private final int destination;

    EBlendMode(boolean enabled, int source, int destination) {
        this.enabled = enabled;
        this.source = source;
        this.destination = destination;
    }

    /** False: GL_BLEND disabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Source factor of glBlendFunc. */
    public int source() {
        return source;
    }

    /** Destination factor of glBlendFunc. */
    public int destination() {
        return destination;
    }
}
