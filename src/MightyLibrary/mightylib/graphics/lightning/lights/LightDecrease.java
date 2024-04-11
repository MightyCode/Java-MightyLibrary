package MightyLibrary.mightylib.graphics.lightning.lights;

import MightyLibrary.mightylib.graphics.lightning.IShaderUniforms;
import MightyLibrary.mightylib.graphics.renderer.Renderer;

public class LightDecrease implements IShaderUniforms {
    private final float constant;
    private final float linear;
    private final float quadratic;

    public LightDecrease(float constant, float linear, float quadratic) {
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }

    @Override
    public void addToRenderer(Renderer renderer, String structureName){
        renderer.addShaderValue(structureName + ".constant", Float.class, constant)
                .addShaderValue(structureName + ".linear", Float.class, linear)
                .addShaderValue(structureName + ".quadratic", Float.class, quadratic);
    }

    @Override
    public void updateRenderer(Renderer renderer, String structureName){
        renderer.updateShaderValue(structureName + ".constant", constant)
                .updateShaderValue(structureName + ".linear", linear)
                .updateShaderValue(structureName + ".quadratic", quadratic);

    }

    @Override
    public LightDecrease clone(){
        return new LightDecrease(constant, linear, quadratic);
    }
}


