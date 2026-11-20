package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;

/** The shareable objects alive: read by the command gl materials and by the leak check. */
public final class SharedObjects {
    public static final LiveObjects<Mesh> MESHES = new LiveObjects<>();
    public static final LiveObjects<Material> MATERIALS = new LiveObjects<>();

    private SharedObjects() {}
}
