package io.github.mightycode.mightylib.graphics.color;

import java.util.Locale;

/**
 * Color space of a draw, chosen at three levels: the engine (default of the game, IColorService), the view (Scene,
 * WindowView) and the Renderer. INHERIT (the default of a view and of a renderer) takes the level above.
 * LINEAR: the colors are decoded to linear, lit and blended in linear, encoded by the card (GL_FRAMEBUFFER_SRGB).
 * GAMMA: everything is computed and blended on the sRGB values, as written (the 2D of most games).
 */
public enum ERenderColorSpace {
    INHERIT,
    LINEAR,
    GAMMA;

    public boolean isLinear() {
        return this == LINEAR;
    }

    // Name of the commands and of the launch option
    public String lowerName() {
        return name().toLowerCase(Locale.ROOT);
    }

    // From a command or a property, null if unknown
    public static ERenderColorSpace parse(String name) {
        if (name == null)
            return null;

        for (ERenderColorSpace value : values()) {
            if (value.name().equalsIgnoreCase(name.trim()))
                return value;
        }

        return null;
    }

    /**
     * The mode of a draw: the renderer when it is not INHERIT, otherwise the view when it is not INHERIT, otherwise
     * the engine. Null is INHERIT.
     *
     * @param engine LINEAR or GAMMA
     * @throws IllegalArgumentException if the engine mode is INHERIT or null
     */
    public static ERenderColorSpace Resolve(ERenderColorSpace renderer, ERenderColorSpace view,
                                            ERenderColorSpace engine) {
        CheckEngine(engine);
        if (renderer != null && renderer != INHERIT)
            return renderer;
        if (view != null && view != INHERIT)
            return view;

        return engine;
    }

    // The mode of the engine is LINEAR or GAMMA
    public static ERenderColorSpace CheckEngine(ERenderColorSpace engine) {
        if (engine == null || engine == INHERIT)
            throw new IllegalArgumentException("The color space of the engine is linear or gamma, not " + engine);

        return engine;
    }
}
