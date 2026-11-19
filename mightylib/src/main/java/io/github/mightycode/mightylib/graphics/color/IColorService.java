package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.services.IEngineService;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * The colors of the engine (service mightylib:colors, Engine.colors): the color space of the draws, the shader of
 * the final pass of the views (framebuffer of the view to the window) and its settings (gamma, brightness, contrast),
 * by default. A view overrides them (IView.colorSpace, finalPassShader, picture), a renderer its color space
 * (Renderer.setColorSpace). ColorService by default, replaceable by registry.services().replace. Without GL.
 * A change takes effect at the next draw, nothing is reloaded.
 */
@API(status = EXPERIMENTAL)
public interface IColorService extends IEngineService {
    // Configuration (ConfigData) of the settings of the picture: "gamma", "brightness", "contrast"
    String DISPLAY_CONFIG = "mightylib/display";
    String GAMMA_KEY = "gamma";
    String BRIGHTNESS_KEY = "brightness";
    String CONTRAST_KEY = "contrast";

    // LINEAR or GAMMA, never INHERIT; read at each draw
    ERenderColorSpace colorSpace();

    /**
     * Changes the default of the draws. Main thread.
     *
     * @throws IllegalArgumentException if the mode is INHERIT or null
     */
    void setColorSpace(ERenderColorSpace colorSpace);

    // Name of the ShaderData of the final pass; it reads the sRGB framebuffer and writes encoded values (see
    // shaders/framebuffers/postProcessingF.glsl, uniforms ShaderUniforms.GAMMA, BRIGHTNESS, CONTRAST)
    String finalPassShader();

    // Main thread; null or empty: IllegalArgumentException
    void setFinalPassShader(String shaderName);

    // The settings of the final pass, never null
    PictureSettings picture();

    /**
     * The player's settings (command picture): ColorService keeps them in the player's layer of the configuration
     * DISPLAY_CONFIG. Main thread.
     *
     * @throws IllegalArgumentException if the settings are null
     */
    void setPicture(PictureSettings picture);

    // Back to the settings of the game (the player's are forgotten); Main thread
    default void resetPicture() {
        setPicture(PictureSettings.DEFAULT);
    }

    // The mode of a draw from the levels below the engine (null or INHERIT: the level above)
    default ERenderColorSpace resolve(ERenderColorSpace renderer, ERenderColorSpace view) {
        return ERenderColorSpace.Resolve(renderer, view, colorSpace());
    }

    // The final pass of a view: its own (not null) or the default one
    default String resolveFinalPassShader(String view) {
        return view != null ? view : finalPassShader();
    }

    default PictureSettings resolvePicture(PictureSettings view) {
        return view != null ? view : picture();
    }
}
