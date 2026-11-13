package io.github.mightycode.mightylib.resources.models;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 3D model read by Assimp (ModelLoader): a list of meshes, each with its material.
 * The textures of the materials are resources owned by the model, loaded and unloaded with it.
 */
public class Model extends SingleSourceDataType {
    private List<ModelMesh> meshes;

    // Textures registered by this model, released when it is unloaded
    private List<DataType> ownedTextures;

    public Model(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        meshes = new ArrayList<>();
        ownedTextures = new ArrayList<>();
    }

    // The copy registers the textures again with the same owner name: the previous ones are released with it
    @Override
    protected DataType createReloadCopy() {
        return getClass() == Model.class ? new Model(dataName, path) : null;
    }

    @Override
    protected void swapContent(DataType copy) {
        Model other = (Model) copy;

        List<ModelMesh> otherMeshes = other.meshes;
        other.meshes = meshes;
        meshes = otherMeshes;

        List<DataType> otherTextures = other.ownedTextures;
        other.ownedTextures = ownedTextures;
        ownedTextures = otherTextures;
    }

    // The Resources the owned textures are added to and released from (the one managing this model)
    Resources ownedResources() {
        return resources();
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

    /**
     * The vertex and index arrays once read (the textures are counted as dependencies); before, the size of the
     * file. Nothing on the card: the renderers send the meshes themselves (no GL element shared by the batches).
     */
    @Override
    public long estimatedCpuBytes() {
        if (meshes.isEmpty())
            return super.estimatedCpuBytes();

        long total = 0;
        for (ModelMesh mesh : meshes)
            total += mesh.byteSize();

        return total;
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
            resources().releaseOwnedResource(texture, ownerName());
        }

        ownedTextures.clear();
    }
}
