package MightyLibrary.mightylib.graphics.shape._3D;

import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.util.math.Color4f;

public class ModelRenderer extends Renderer {
    public ModelRenderer(String shaderName, String modelPath, String texture) {
        super(shaderName, true, false);
        shape = OBJLoader.loadObjTexturedModel(modelPath);
        setTexture(texture);
    }

    public ModelRenderer(String shaderName, String modelPath, Color4f color) {
        super(shaderName, true, false);
        shape = OBJLoader.loadObjColoredModel(modelPath);
        setColor(color);
    }
}
