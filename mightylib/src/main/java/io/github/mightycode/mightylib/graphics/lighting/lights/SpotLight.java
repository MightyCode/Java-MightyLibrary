package io.github.mightycode.mightylib.graphics.lighting.lights;

import io.github.mightycode.mightylib.graphics.lighting.materials.BasicPhongMaterial;
import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import org.joml.Vector3f;

public final class SpotLight extends BasicPhongMaterial {
    private Vector3f position, direction;
    private final LightDecrease lightDecrease;

    private float cutOff;
    private float outerCutOff;

    private boolean shouldUseDistance;
    private boolean enabled;

    public SpotLight(Vector3f ambient, Vector3f diffuse, Vector3f specular, LightDecrease lightDecrease) {
        super(ambient, diffuse, specular);

        this.lightDecrease = lightDecrease;
        this.cutOff = (float)Math.cos(MightyMath.toRads(12.5f));
        this.outerCutOff = (float)Math.cos(MightyMath.toRads(15f));

        this.enabled = false;
        shouldUseDistance = true;
    }

    public SpotLight(BasicPhongMaterial mat, LightDecrease lightDecrease) {
        this(mat.Ambient, mat.Diffuse, mat.Specular, lightDecrease);
    }

    public SpotLight enable(){
        this.enabled = true;

        return this;
    }

    public SpotLight disable(){
        this.enabled = false;

        return this;
    }

    public SpotLight enableDistance(){
        this.shouldUseDistance = this.lightDecrease != null;

        return this;
    }

    public SpotLight disableDistance(){
        this.shouldUseDistance = false;

        return this;
    }

    public SpotLight setDirection(Vector3f direction){
        this.direction = direction;

        return this;
    }

    public SpotLight setPosition(Vector3f position){
        this.position = position;

        return this;
    }

    public SpotLight setCutOff(float value){
        this.cutOff = value;

        return this;
    }

    public SpotLight setOuterCutOff(float value){
        this.outerCutOff = value;

        return this;
    }

    @Override
    public void addTo(IUniformTarget target, String structureName) {
        super.addTo(target, structureName);

        target.addShaderValue(structureName + ".enabled", Integer.class, enabled ? 1 : 0);

        target.addShaderValue(structureName + ".cutOff", Float.class, cutOff);
        target.addShaderValue(structureName + ".outerCutOff", Float.class, outerCutOff);
        target.addShaderValue(structureName + ".position", Vector3f.class, position);
        target.addShaderValue(structureName + ".direction", Vector3f.class, direction);

        target.addShaderValue(structureName + ".useDistance", Integer.class, shouldUseDistance ? 1 : 0);
        lightDecrease.addTo(target, structureName);
    }

    @Override
    public void update(IUniformTarget target, String structureName) {
        super.update(target, structureName);

        target.updateShaderValue(structureName + ".enabled", enabled ? 1 : 0);

        if (enabled) {
            target.updateShaderValue(structureName + ".position", position);
            target.updateShaderValue(structureName + ".direction", direction);

            target.updateShaderValue(structureName + ".cutOff", cutOff);
            target.updateShaderValue(structureName + ".outerCutOff", outerCutOff);

            target.updateShaderValue(structureName + ".useDistance", shouldUseDistance ? 1 : 0);
            if (shouldUseDistance) {
                lightDecrease.update(target, structureName);
            }
        }
    }

    // A spotlight with the same settings and its own vectors (LightDecrease is immutable: shared)
    @Override
    public SpotLight copy(){
        SpotLight copy = new SpotLight(copyBasicPhongMaterial(), lightDecrease);
        copy.position = position == null ? null : new Vector3f(position);
        copy.direction = direction == null ? null : new Vector3f(direction);
        copy.cutOff = cutOff;
        copy.outerCutOff = outerCutOff;
        copy.shouldUseDistance = shouldUseDistance;
        copy.enabled = enabled;

        return copy;
    }
}
