package io.github.mightycode.mightylib.graphics.lightning;

import io.github.mightycode.mightylib.graphics.renderer.Renderer;

public interface IShaderUniforms {
    void addToRenderer(Renderer renderer, String structureName);

    void updateRenderer(Renderer renderer, String structureName);
}
