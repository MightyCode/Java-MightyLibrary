package io.github.mightycode.mightylib.graphics.lighting.materials;

import io.github.mightycode.mightylib.graphics.lighting.IShaderUniforms;
import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;
import org.joml.Vector3f;

public class BasicPhongMaterial implements IShaderUniforms {
    public final Vector3f Ambient;
    public final Vector3f Diffuse;
    public final Vector3f Specular;

    public BasicPhongMaterial(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        this.Ambient = ambient;
        this.Diffuse = diffuse;
        this.Specular = specular;
    }

    /**
     * Assuming that the shader contains struct Material with attributes
     */
    @Override
    public void addTo(IUniformTarget target, String structureName){
        target.addShaderValue(structureName + ".ambient", Vector3f.class, Ambient)
                .addShaderValue(structureName + ".diffuse", Vector3f.class, Diffuse)
                .addShaderValue(structureName + ".specular", Vector3f.class, Specular);
    }

    @Override
    public void update(IUniformTarget target, String structureName){
        target.updateShaderValue(structureName + ".ambient", Ambient)
                .updateShaderValue(structureName + ".diffuse", Diffuse)
                .updateShaderValue(structureName + ".specular", Specular);

    }

    // A copy with its own vectors: changing the copy leaves this material as it is
    public BasicPhongMaterial copy(){
        return copyBasicPhongMaterial();
    }

    public final BasicPhongMaterial copyBasicPhongMaterial(){
        return new BasicPhongMaterial(new Vector3f(Ambient), new Vector3f(Diffuse), new Vector3f(Specular));
    }
}
