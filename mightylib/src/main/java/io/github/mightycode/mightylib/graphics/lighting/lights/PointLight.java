package io.github.mightycode.mightylib.graphics.lighting.lights;

import io.github.mightycode.mightylib.graphics.lighting.materials.BasicPhongMaterial;
import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;
import org.joml.Vector3f;

public final class PointLight extends BasicPhongMaterial {
    private Vector3f position;
    private boolean shouldUseDistance;
    private LightDecrease lightDecrease = null;
    private int currentNumber;

    public PointLight(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        super(ambient, diffuse, specular);

        shouldUseDistance = false;
    }

    public PointLight(BasicPhongMaterial material){
        this(material.Ambient, material.Diffuse, material.Specular);
    }

    public PointLight(BasicPhongMaterial material, LightDecrease lightDecrease){
        this(material.Ambient, material.Diffuse, material.Specular, lightDecrease);
    }

    public PointLight(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular, LightDecrease lightDecrease){
        super(ambient, diffuse, specular);

        this.lightDecrease = lightDecrease;

        shouldUseDistance = lightDecrease != null;
    }

    public PointLight setPosition(Vector3f position){
        this.position = position;

        return this;
    }

    public void setLightCurrentNumber(int number){
        this.currentNumber = number;
    }

    public PointLight enableDistance(){
        this.shouldUseDistance = this.lightDecrease != null;

        return this;
    }

    public PointLight disableDistance(){
        this.shouldUseDistance = false;

        return this;
    }

    @Override
    public void addTo(IUniformTarget target, String structureName) {
        String name = structureName + "[" + currentNumber + "]";

        super.addTo(target, name);

        target.addShaderValue(name + ".useDistance", Integer.class, shouldUseDistance ? 1 : 0);
        target.addShaderValue(name + ".position", Vector3f.class, position);

        if (shouldUseDistance){
            lightDecrease.addTo(target, name);
        }
    }

    @Override
    public void update(IUniformTarget target, String structureName) {
        String name = structureName + "[" + currentNumber + "]";

        super.update(target, name);

        target.updateShaderValue(name + ".useDistance", shouldUseDistance? 1 : 0);
        target.updateShaderValue(name + ".position", position);

        if (shouldUseDistance){
            lightDecrease.update(target, name);
        }
    }

    // A point light with the same settings and its own vectors (LightDecrease is immutable: shared)
    @Override
    public PointLight copy(){
        PointLight copy = new PointLight(copyBasicPhongMaterial(), lightDecrease);
        copy.position = position == null ? null : new Vector3f(position);
        copy.shouldUseDistance = shouldUseDistance;
        copy.currentNumber = currentNumber;

        return copy;
    }
}
