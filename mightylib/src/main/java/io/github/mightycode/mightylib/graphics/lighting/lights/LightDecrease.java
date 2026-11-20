package io.github.mightycode.mightylib.graphics.lighting.lights;

import io.github.mightycode.mightylib.graphics.lighting.IShaderUniforms;
import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;

public final class LightDecrease implements IShaderUniforms {
    private final float constant;
    private final float linear;
    private final float quadratic;

    public LightDecrease(float constant, float linear, float quadratic) {
        this.constant = constant;
        this.linear = linear;
        this.quadratic = quadratic;
    }

    @Override
    public void addTo(IUniformTarget target, String structureName){
        target.addShaderValue(structureName + ".constant", Float.class, constant)
                .addShaderValue(structureName + ".linear", Float.class, linear)
                .addShaderValue(structureName + ".quadratic", Float.class, quadratic);
    }

    @Override
    public void update(IUniformTarget target, String structureName){
        target.updateShaderValue(structureName + ".constant", constant)
                .updateShaderValue(structureName + ".linear", linear)
                .updateShaderValue(structureName + ".quadratic", quadratic);

    }

    public LightDecrease copy(){
        return new LightDecrease(constant, linear, quadratic);
    }
}


