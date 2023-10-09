package MightyLibrary.mightylib.graphics.renderer.utils;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import org.joml.Vector3f;

public class LightPoint extends BasicMaterial {
    private boolean shouldUseDistance;
    private float attenuationConstant, attenuationLinear, attenuationQuadratic;

    public LightPoint(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular){
        super(ambient, diffuse, specular);

        shouldUseDistance = false;
    }

    public LightPoint(BasicMaterial material){
        this(material.Ambient, material.Diffuse, material.Specular);
    }

    public LightPoint(BasicMaterial material,
                      float attenuationConstant, float attenuationLinear, float attenuationQuadratic){
        this(material.Ambient, material.Diffuse, material.Specular,
                attenuationConstant, attenuationLinear, attenuationQuadratic);
    }

    public LightPoint(final Vector3f ambient, final Vector3f diffuse, final Vector3f specular,
                      float attenuationConstant, float attenuationLinear, float attenuationQuadratic){
        super(ambient, diffuse, specular);

        shouldUseDistance = true;
        this.attenuationQuadratic = attenuationQuadratic;
        this.attenuationConstant = attenuationConstant;
        this.attenuationLinear = attenuationLinear;
    }

    @Override
    public LightPoint clone(){
        if (this.shouldUseDistance)
            return new LightPoint(Ambient, Diffuse, Specular, attenuationConstant, attenuationLinear, attenuationQuadratic);

        return new LightPoint(Ambient, Diffuse, Specular);
    }

    @Override
    public void addToRenderer(Renderer renderer, String structureName) {
        super.addToRenderer(renderer, structureName);

        renderer.addShaderValue(structureName + ".useDistance", Integer.class, shouldUseDistance ? 1 : 0);

        if (shouldUseDistance){
            renderer.addShaderValue(structureName + ".constant", Float.class, attenuationConstant);
            renderer.addShaderValue(structureName + ".linear", Float.class, attenuationLinear);
            renderer.addShaderValue(structureName + ".quadratic", Float.class, attenuationQuadratic);
        }
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName) {
        super.updateRenderer(renderer, structureName);

        renderer.updateShaderValue(structureName + ".useDistance", shouldUseDistance? 1 : 0);

        if (shouldUseDistance){
            renderer.updateShaderValue(structureName + ".constant", attenuationConstant);
            renderer.updateShaderValue(structureName + ".linear", attenuationLinear);
            renderer.updateShaderValue(structureName + ".quadratic", attenuationQuadratic);
        }
    }
}
