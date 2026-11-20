package io.github.mightycode.mightylib.graphics.internal;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.renderer.Material;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;

/**
 * The shareable objects alive in one engine (its GLResources): read by the command gl materials and by the leak
 * check. Each engine has its own, so that several engines in one program (tests) do not see each other's objects.
 * Main thread only.
 */
public final class SharedObjects implements IGLSharedState {
    private final LiveObjects<Mesh> meshes = new LiveObjects<>();
    private final LiveObjects<Material> materials = new LiveObjects<>();

    private SharedObjects() {}

    /** The registry of the engine of these resources, created at the first call. */
    public static SharedObjects of(GLResources glResources) {
        return glResources.shared(SharedObjects.class, SharedObjects::new);
    }

    public LiveObjects<Mesh> meshes() {
        return meshes;
    }

    public LiveObjects<Material> materials() {
        return materials;
    }

    // The objects are freed by their users, not here
    @Override
    public void delete() {}
}
