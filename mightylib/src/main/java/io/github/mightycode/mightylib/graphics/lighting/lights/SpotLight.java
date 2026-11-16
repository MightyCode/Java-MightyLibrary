package io.github.mightycode.mightylib.graphics.lighting.lights;

import io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import org.joml.Vector3f;

public final class SpotLight extends BasicMaterial {
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

    public SpotLight(BasicMaterial mat, LightDecrease lightDecrease) {
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
    public void addToRenderer(Renderer renderer, String structureName) {
        super.addToRenderer(renderer, structureName);

        renderer.addShaderValue(structureName + ".enabled", Integer.class, enabled ? 1 : 0);

        renderer.addShaderValue(structureName + ".cutOff", Float.class, cutOff);
        renderer.addShaderValue(structureName + ".outerCutOff", Float.class, outerCutOff);
        renderer.addShaderValue(structureName + ".position", Vector3f.class, position);
        renderer.addShaderValue(structureName + ".direction", Vector3f.class, direction);

        renderer.addShaderValue(structureName + ".useDistance", Integer.class, shouldUseDistance ? 1 : 0);
        lightDecrease.addToRenderer(renderer, structureName);
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName) {
        super.updateRenderer(renderer, structureName);

        renderer.updateShaderValue(structureName + ".enabled", enabled ? 1 : 0);

        if (enabled) {
            renderer.updateShaderValue(structureName + ".position", position);
            renderer.updateShaderValue(structureName + ".direction", direction);

            renderer.updateShaderValue(structureName + ".cutOff", cutOff);
            renderer.updateShaderValue(structureName + ".outerCutOff", outerCutOff);

            renderer.updateShaderValue(structureName + ".useDistance", shouldUseDistance ? 1 : 0);
            if (shouldUseDistance) {
                lightDecrease.updateRenderer(renderer, structureName);
            }
        }
    }

    // A spotlight with the same settings and its own vectors (LightDecrease is immutable: shared)
    @Override
    public SpotLight copy(){
        SpotLight copy = new SpotLight(copyBasicMaterial(), lightDecrease);
        copy.position = position == null ? null : new Vector3f(position);
        copy.direction = direction == null ? null : new Vector3f(direction);
        copy.cutOff = cutOff;
        copy.outerCutOff = outerCutOff;
        copy.shouldUseDistance = shouldUseDistance;
        copy.enabled = enabled;

        return copy;
    }
}
