package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.resources.source.ResourceSources;

// Gives tests access to the package-private parts of ShaderService
public final class ShaderServiceTestAccess {
    private ShaderServiceTestAccess() {}

    // A service reading shaders.json in the given sources, without reload
    public static ShaderService create(ResourceSources sources) {
        return new ShaderService(sources, () -> {});
    }
}
