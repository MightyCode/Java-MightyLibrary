package io.github.mightycode.mightylib.graphics.lighting;

import io.github.mightycode.mightylib.graphics.renderer.IUniformTarget;

public interface IShaderUniforms {
    void addTo(IUniformTarget target, String structureName);

    void update(IUniformTarget target, String structureName);
}
