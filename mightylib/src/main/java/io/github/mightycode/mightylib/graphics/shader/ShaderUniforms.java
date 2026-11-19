package io.github.mightycode.mightylib.graphics.shader;

// The uniforms given by the renderers to every shader that declares them (Renderer): their names in the GLSL files
public final class ShaderUniforms {
    private ShaderUniforms() {}

    public static final String PROJECTION = "projection";
    public static final String VIEW = "view";
    public static final String MODEL = "model";
    public static final String COLOR = "color";
    // 1 for a linear draw, 0 for a gamma one (utils/colorSpace.glsl)
    public static final String LINEAR_COLORS = "mlLinearColors";
    // Settings of the final pass of a view (PictureSettings), sent when its shader has them
    public static final String GAMMA = "mlGamma";
    public static final String BRIGHTNESS = "mlBrightness";
    public static final String CONTRAST = "mlContrast";
}
