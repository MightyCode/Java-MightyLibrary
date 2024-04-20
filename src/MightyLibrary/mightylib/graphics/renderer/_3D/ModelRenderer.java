package MightyLibrary.mightylib.graphics.renderer._3D;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.utils.math.color.Color4f;

public class ModelRenderer extends Renderer {
    public ModelRenderer(String shaderName, String modelPath, String texture) {
        super(shaderName, true);
        shape = OBJLoader.loadObjTexturedModel(modelPath);
        setMainTextureChannel(texture);
    }

    public ModelRenderer(String shaderName, String modelPath, Color4f color) {
        super(shaderName, true);
        shape = OBJLoader.loadObjColoredModel(modelPath);
        setColorMode(color);
    }
}
