package MightyLibrary.mightylib.graphics.renderer.utils;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Texture;

public class TextureMaterial {
    private final Texture texture;
    private final float shininess;

    public TextureMaterial(String texture, float shininess){
        this.texture = Resources.getInstance().getResource(Texture.class, texture);
        this.shininess = shininess;
    }

    /**
     * Assuming that the shader contains struct Material with attributes
     */
    public void addToRenderer(Renderer renderer, String structureName){
        renderer.addShaderValue(structureName + ".specular", Integer.class, 1)
                .addShaderValue(structureName + ".shininess", Float.class,shininess);
    }

    public void updateRenderer(Renderer renderer, String structureName){
        renderer.updateShaderValue(structureName + ".specular", 1)
                .updateShaderValue(structureName + ".shininess", shininess);
    }

    public void apply(){
        texture.bindRenderTexture(1);
    }

    @Override
    public TextureMaterial clone(){
        return new TextureMaterial(texture.getDataName(), shininess);
    }
}
