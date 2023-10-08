package MightyLibrary.mightylib.graphics.renderer.utils;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class BasicMaterial {
    public Vector3f Ambient;
    public Vector3f Diffuse;
    public Vector3f Specular;

    public BasicMaterial(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        this.Ambient = ambient;
        this.Diffuse = diffuse;
        this.Specular = specular;
    }

    /**
     * Assuming that the shader contains struct Material with attributes
     */
    public void addToRenderer(Renderer renderer, String structureName){
        renderer.addShaderValue(structureName + ".ambient", Vector3f.class, Ambient)
                .addShaderValue(structureName + ".diffuse", Vector3f.class, Diffuse)
                .addShaderValue(structureName + ".specular", Vector3f.class, Specular);
    }

    public void updateRenderer(Renderer renderer, String structureName){
        renderer.updateShaderValue(structureName + ".ambient", Ambient)
                .updateShaderValue(structureName + ".diffuse", Diffuse)
                .updateShaderValue(structureName + ".specular", Specular);

    }

    @Override
    public BasicMaterial clone(){
        return new BasicMaterial(Ambient, Diffuse, Specular);
    }
}
