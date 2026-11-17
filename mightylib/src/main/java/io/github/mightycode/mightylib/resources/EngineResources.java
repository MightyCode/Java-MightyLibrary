package io.github.mightycode.mightylib.resources;

import java.util.List;

/**
 * Names of the resources the engine provides (classpath assets/mightylib/) and uses. A game replaces one by
 * declaring the same name (shaders.json, fonts.json, textures.json) or the same path in a higher source.
 */
public final class EngineResources {
    private EngineResources() {}

    // ---- Batch ----

    // Loaded by Resources.load before the first batch of the game, never unloaded
    public static final String ENGINE_BATCH = "mightylib";

    // ---- Shaders (shaders/<version>/shaders.json) ----

    public static final String COLOR_SHAPE_2D = "colorShape2D";
    public static final String COLOR_ELLIPSE_2D = "colorEllipse2D";
    public static final String TEXTURE_2D = "texture2D";
    public static final String MULTI_COLORED_SHAPE_2D = "multiColoredShape2D";
    public static final String COLORED_TEXT = "coloredText";
    public static final String POST_PROCESSING = "postProcessing";
    public static final String COLOR_SHAPE_3D = "colorShape3D";
    public static final String TEXTURE_3D = "texture3D";
    public static final String SKYBOX = "skybox";
    public static final String MULTI_COLORED_LIGHTING_3D = "multiColoredLighting3D";
    public static final String TEXTURE_MULTIPLE_LIGHTS_3D = "textureMultipleLights3D";

    private static final List<String> SHADERS = List.of(
            COLOR_SHAPE_2D, COLOR_ELLIPSE_2D, TEXTURE_2D, MULTI_COLORED_SHAPE_2D, COLORED_TEXT, POST_PROCESSING,
            COLOR_SHAPE_3D, TEXTURE_3D, SKYBOX, MULTI_COLORED_LIGHTING_3D, TEXTURE_MULTIPLE_LIGHTS_3D);

    // The shaders of the engine, all loaded by its batch
    public static List<String> shaders() {
        return SHADERS;
    }
}
