package io.github.mightycode.mightylib.graphics.lighting.materials;

import io.github.mightycode.mightylib.graphics.lighting.IShaderUniforms;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class BasicMaterial implements IShaderUniforms {
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

    // A copy with its own vectors: changing the copy leaves this material as it is
    @Override
    public BasicMaterial clone(){
        return cloneBasicMaterial();
    }

    public final BasicMaterial cloneBasicMaterial(){
        return new BasicMaterial(new Vector3f(Ambient), new Vector3f(Diffuse), new Vector3f(Specular));
    }
}
