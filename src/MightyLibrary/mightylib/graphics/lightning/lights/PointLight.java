package MightyLibrary.mightylib.graphics.lightning.lights;

import MightyLibrary.mightylib.graphics.lightning.materials.BasicMaterial;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class PointLight extends BasicMaterial {
    private Vector3f position;
    private boolean shouldUseDistance;
    private LightDecrease lightDecrease = null;
    private int currentNumber;

    public PointLight(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        super(ambient, diffuse, specular);

        shouldUseDistance = false;
    }

    public PointLight(BasicMaterial material){
        this(material.Ambient, material.Diffuse, material.Specular);
    }

    public PointLight(BasicMaterial material, LightDecrease lightDecrease){
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
    public void addToRenderer(Renderer renderer, String structureName) {
        String name = structureName + "[" + currentNumber + "]";

        super.addToRenderer(renderer, name);

        renderer.addShaderValue(name + ".useDistance", Integer.class, shouldUseDistance ? 1 : 0);
        renderer.addShaderValue(name + ".position", Vector3f.class, position);

        if (shouldUseDistance){
            lightDecrease.addToRenderer(renderer, name);
        }
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName) {
        String name = structureName + "[" + currentNumber + "]";

        super.updateRenderer(renderer, name);

        renderer.updateShaderValue(name + ".useDistance", shouldUseDistance? 1 : 0);
        renderer.updateShaderValue(name + ".position", position);

        if (shouldUseDistance){
            lightDecrease.updateRenderer(renderer, name);
        }
    }

    @Override
    public PointLight clone(){
        if (this.shouldUseDistance)
            return new PointLight(Ambient, Diffuse, Specular, lightDecrease);

        return new PointLight(Ambient, Diffuse, Specular);
    }
}
