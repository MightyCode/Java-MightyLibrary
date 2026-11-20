package io.github.mightycode.mightylib.graphics.lighting.lights;

import io.github.mightycode.mightylib.graphics.lighting.materials.BasicPhongMaterial;
import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;
import org.joml.Vector3f;

public final class DirectionalLight extends BasicPhongMaterial {
    private Vector3f direction;

    public DirectionalLight(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        super(ambient, diffuse, specular);
    }

    public DirectionalLight(final BasicPhongMaterial mat){
        super(mat.Ambient, mat.Diffuse, mat.Specular);
    }

    public DirectionalLight setDirection(Vector3f direction){
        this.direction = direction;

        return this;
    }

    public Vector3f getDirection(){
        return direction;
    }

    @Override
    public DirectionalLight copy(){
        return new DirectionalLight(copyBasicPhongMaterial())
                .setDirection(direction == null ? null : new Vector3f(direction));
    }

    @Override
    public void addTo(IUniformTarget target, String structureName) {
        super.addTo(target, structureName);

        target.addShaderValue(structureName + ".direction", Vector3f.class, direction);
    }

    @Override
    public void update(IUniformTarget target, String structureName) {
        super.update(target, structureName);

        target.updateShaderValue(structureName + ".direction", direction);
    }
}
