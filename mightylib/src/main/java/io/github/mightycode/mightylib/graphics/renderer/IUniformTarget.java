package io.github.mightycode.mightylib.graphics.renderer;

/**
 * Something that holds values of uniforms by name, sent at each draw: a Material (shared by its renderers) or a
 * Renderer (its own values, which win over those of its material). A group of values (a Phong material, a light)
 * writes itself into either (IShaderUniforms).
 */
public interface IUniformTarget {
    /** Adds the value (replaces the one of this name); the object is read at each draw. */
    IUniformTarget addShaderValue(String name, Class<?> type, Object object);

    /**
     * Changes the object of the value of this name.
     * @throws IllegalArgumentException when there is no value of this name
     */
    IUniformTarget updateShaderValue(String name, Object object);

    IUniformTarget removeShaderValue(String name);

    boolean hasShaderValue(String name);
}
