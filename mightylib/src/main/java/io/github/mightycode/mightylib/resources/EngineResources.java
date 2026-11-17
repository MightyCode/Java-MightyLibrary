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

    // ---- Fonts (textures/fonts/fonts.json, atlas in textures/textures.json) ----

    // Liberation Sans (SIL OFL 1.1): the font of the interface, EngineConfig.setDefaultFont
    public static final String DEFAULT_FONT = "liberationSans";
    // Liberation Mono: the font of the console, EngineConfig.setConsoleFont
    public static final String CONSOLE_FONT = "liberationMono";

    // Declared by the engine, but only the two regular ones are loaded by its batch (4 MB of texture each)
    public static final String DEFAULT_FONT_BOLD = "liberationSansBold";
    public static final String DEFAULT_FONT_ITALIC = "liberationSansItalic";
    public static final String DEFAULT_FONT_BOLD_ITALIC = "liberationSansBoldItalic";
    public static final String CONSOLE_FONT_BOLD = "liberationMonoBold";
    public static final String CONSOLE_FONT_ITALIC = "liberationMonoItalic";
    public static final String CONSOLE_FONT_BOLD_ITALIC = "liberationMonoBoldItalic";

    private static final List<String> LOADED_FONTS = List.of(DEFAULT_FONT, CONSOLE_FONT);

    private static final List<String> FONTS = List.of(
            DEFAULT_FONT, DEFAULT_FONT_BOLD, DEFAULT_FONT_ITALIC, DEFAULT_FONT_BOLD_ITALIC,
            CONSOLE_FONT, CONSOLE_FONT_BOLD, CONSOLE_FONT_ITALIC, CONSOLE_FONT_BOLD_ITALIC);

    // The fonts the engine declares (the name of the font is the name of its atlas)
    public static List<String> fonts() {
        return FONTS;
    }

    // The fonts the batch of the engine loads
    public static List<String> loadedFonts() {
        return LOADED_FONTS;
    }
}
