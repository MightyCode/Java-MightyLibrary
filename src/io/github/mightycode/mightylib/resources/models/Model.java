package io.github.mightycode.mightylib.resources.models;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3D model read by Assimp (ModelLoader): a list of meshes, each with its material.
 * The textures of the materials are resources owned by the model, loaded and unloaded with it.
 */
public class Model extends SingleSourceDataType {
    private final List<ModelMesh> meshes;

    // Textures registered by this model, released when it is unloaded
    private final List<DataType> ownedTextures;

    public Model(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        meshes = new ArrayList<>();
        ownedTextures = new ArrayList<>();
    }

    // Name used as reference on the owned textures
    public String ownerName() {
        return "model:" + dataName;
    }

    void setMeshes(List<ModelMesh> meshes) {
        this.meshes.clear();
        this.meshes.addAll(meshes);
    }

    void addOwnedTexture(DataType texture) {
        if (ownedTextures.contains(texture))
            return;

        ownedTextures.add(texture);
        addDependency(texture);
    }

    public List<ModelMesh> getMeshes() {
        return Collections.unmodifiableList(meshes);
    }

    public int getNumberOfMeshes() {
        return meshes.size();
    }

    public int getNumberOfVertices() {
        int count = 0;
        for (ModelMesh mesh : meshes)
            count += mesh.getNumberOfVertices();

        return count;
    }

    @Override
    public boolean internLoad() {
        if (meshes.isEmpty()) {
            // Not loaded: internUnload won't be called, release the textures now
            releaseTextures();
            return false;
        }

        return true;
    }

    @Override
    public void internUnload() {
        meshes.clear();
        releaseTextures();
    }

    @Override
    protected void internCancelPreload() {
        internUnload();
    }

    private void releaseTextures() {
        for (DataType texture : ownedTextures) {
            removeDependency(texture);
            resources.releaseOwnedResource(texture, ownerName());
        }

        ownedTextures.clear();
    }
}
