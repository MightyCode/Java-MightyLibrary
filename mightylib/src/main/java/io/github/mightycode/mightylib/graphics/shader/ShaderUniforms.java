package io.github.mightycode.mightylib.graphics.shader;

// The uniforms given by the renderers to every shader that declares them (Renderer): their names in the GLSL files
public final class ShaderUniforms {
    private ShaderUniforms() {}

    public static final String PROJECTION = "projection";
    public static final String VIEW = "view";
    public static final String MODEL = "model";
    public static final String COLOR = "color";
}
