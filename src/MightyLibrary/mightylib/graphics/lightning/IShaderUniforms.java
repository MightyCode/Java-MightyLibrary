package MightyLibrary.mightylib.graphics.lightning;

import MightyLibrary.mightylib.graphics.renderer.Renderer;

public interface IShaderUniforms {
    void addToRenderer(Renderer renderer, String structureName);

    void updateRenderer(Renderer renderer, String structureName);
}
