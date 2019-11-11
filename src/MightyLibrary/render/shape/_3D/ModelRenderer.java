package MightyLibrary.render.shape._3D;

import MightyLibrary.render.shape.Renderer;
import MightyLibrary.util.math.Color4f;

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
