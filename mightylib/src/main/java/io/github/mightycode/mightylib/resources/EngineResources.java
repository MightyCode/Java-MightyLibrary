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

    // ---- Files ----

    // Gamepad mappings (SDL_GameControllerDB) at the root of the sources, read by ContextManager: the one of the
    // highest source that has it is used
    public static final String GAMEPAD_MAPPINGS = "gamecontrollerdb.txt";

    // ---- Shaders (shaders/<version>/shaders.json) ----

    public static final String COLOR_SHAPE_2D = "colorShape2D";
    public static final String TEXTURE_2D = "texture2D";
    // Vector shapes (VectorShape2DRenderer), one per shape
    public static final String ROUNDED_RECTANGLE_2D = "roundedRectangle2D";
    public static final String CIRCLE_2D = "circle2D";
    public static final String ELLIPSE_2D = "ellipse2D";
    public static final String TRIANGLE_2D = "triangle2D";
    public static final String ARC_2D = "arc2D";
    public static final String REGULAR_POLYGON_2D = "regularPolygon2D";
    public static final String POINTS_2D = "points2D";
    public static final String GRID_2D = "grid2D";
    // Geometric shapes (polylines, arrows, polygons): triangles of Stroker extruded by the vertex shader
    public static final String STROKE_2D = "stroke2D";
    // Batch shaders (tags batch2D, batch2DInstanced): texture x vertex color; texture2D and colorShape2D link to them
    public static final String SPRITE_2D = "sprite2D";
    public static final String SPRITE_2D_INSTANCED = "sprite2DInstanced";
    // Batch programs of the vector shapes (tag batch2D, BatchFormats.SHAPE and POINT): each vector shape links to its own
    public static final String ROUNDED_RECTANGLE_2D_BATCH = "roundedRectangle2DBatch";
    public static final String CIRCLE_2D_BATCH = "circle2DBatch";
    public static final String ELLIPSE_2D_BATCH = "ellipse2DBatch";
    public static final String TRIANGLE_2D_BATCH = "triangle2DBatch";
    public static final String ARC_2D_BATCH = "arc2DBatch";
    public static final String REGULAR_POLYGON_2D_BATCH = "regularPolygon2DBatch";
    public static final String GRID_2D_BATCH = "grid2DBatch";
    public static final String POINTS_2D_BATCH = "points2DBatch";
    // Batch program of the geometric shapes (BatchFormats.STROKE): stroke2D links to it
    public static final String STROKE_2D_BATCH = "stroke2DBatch";
    public static final String MULTI_COLORED_SHAPE_2D = "multiColoredShape2D";
    public static final String COLORED_TEXT = "coloredText";
    public static final String POST_PROCESSING = "postProcessing";
    public static final String COLOR_SHAPE_3D = "colorShape3D";
    public static final String TEXTURE_3D = "texture3D";
    public static final String SKYBOX = "skybox";
    public static final String MULTI_COLORED_LIGHTING_3D = "multiColoredLighting3D";
    public static final String TEXTURE_MULTIPLE_LIGHTS_3D = "textureMultipleLights3D";

    private static final List<String> SHADERS = List.of(
            COLOR_SHAPE_2D, TEXTURE_2D,
            ROUNDED_RECTANGLE_2D, CIRCLE_2D, ELLIPSE_2D, TRIANGLE_2D, ARC_2D, REGULAR_POLYGON_2D,
            POINTS_2D, GRID_2D, STROKE_2D, SPRITE_2D, SPRITE_2D_INSTANCED,
            ROUNDED_RECTANGLE_2D_BATCH, CIRCLE_2D_BATCH, ELLIPSE_2D_BATCH, TRIANGLE_2D_BATCH, ARC_2D_BATCH,
            REGULAR_POLYGON_2D_BATCH, GRID_2D_BATCH, POINTS_2D_BATCH, STROKE_2D_BATCH,
            MULTI_COLORED_SHAPE_2D, COLORED_TEXT, POST_PROCESSING,
            COLOR_SHAPE_3D, TEXTURE_3D, SKYBOX, MULTI_COLORED_LIGHTING_3D, TEXTURE_MULTIPLE_LIGHTS_3D);

    // The shaders of the engine, all loaded by its batch
    public static List<String> shaders() {
        return SHADERS;
    }

    // ---- Textures ----

    // White 1x1 sRGB texture (ColorTextureData, no file), declared by the texture loader and loaded by the batch of
    // the engine: the texture of a colored draw in a 2D batch
    public static final String WHITE_TEXTURE = "color/ffffffff";

    // ---- Fonts (textures/fonts/fonts.json, atlas in textures/textures.json) ----

    // Atlases derived from Liberation Sans 2.1.4 (SIL OFL 1.1; a change of format is a modified version, so the
    // reserved name "Liberation" is not used): the font of the interface, EngineConfig.setDefaultFont
    public static final String DEFAULT_FONT = "engineSans";
    // Derived from Liberation Mono 2.1.4: the font of the console, EngineConfig.setConsoleFont
    public static final String CONSOLE_FONT = "engineMono";

    // Declared by the engine, but only the two regular ones are loaded by its batch (4 MB of texture each)
    public static final String DEFAULT_FONT_BOLD = "engineSansBold";
    public static final String DEFAULT_FONT_ITALIC = "engineSansItalic";
    public static final String DEFAULT_FONT_BOLD_ITALIC = "engineSansBoldItalic";
    public static final String CONSOLE_FONT_BOLD = "engineMonoBold";
    public static final String CONSOLE_FONT_ITALIC = "engineMonoItalic";
    public static final String CONSOLE_FONT_BOLD_ITALIC = "engineMonoBoldItalic";

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
