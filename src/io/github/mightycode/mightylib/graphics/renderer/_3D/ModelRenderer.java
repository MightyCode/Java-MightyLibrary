package io.github.mightycode.mightylib.graphics.renderer._3D;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.resources.models.Model;
import io.github.mightycode.mightylib.resources.models.ModelMesh;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Display a Model (ModelLoader): one Mesh per mesh of the model, sent to the graphic card one mesh per frame
 * within the GLResources budget, meshes are displayed as soon as they are sent.
 * Each mesh binds its diffuse map on unit 0 and its specular map on unit 1.
 */
public class ModelRenderer extends Renderer {
    public static final String DEFAULT_SHADER = "textureMultipleLights3D";
    public static final String DEFAULT_DIFFUSE_UNIFORM = "material.diffuse";
    public static final String DEFAULT_SPECULAR_UNIFORM = "material.specular";
    public static final String SHININESS_UNIFORM = "material.shininess";

    private static final int DIFFUSE_UNIT = 0;
    private static final int SPECULAR_UNIT = 1;

    private final String modelName;
    // The model asked for; the one displayed is its usable version (the fallback cube while it is not loaded)
    private final Model requestedModel;
    // The meshes displayed, and those of a newer version being sent (they replace the displayed ones once all sent)
    private MeshSet shown;
    private MeshSet pending;
    private String diffuseOverride;

    // Sampler uniforms (specular null when the shader has no specular map), in shaderValues as optional values
    private String diffuseUniform;
    private String specularUniform;
    private boolean faceCulling;

    private final ShaderValue shininessValue;

    // Sends the next mesh at each GLResources step
    private final MeshUpload upload;

    public ModelRenderer(String modelName) {
        this(DEFAULT_SHADER, modelName);
    }

    public ModelRenderer(String shaderName, String modelName) {
        super(shaderName, true);
        this.modelName = modelName;

        // Unknown, failed or not loaded yet: the fallback cube, replaced once the model is loaded (see rebuildIfChanged)
        requestedModel = resources.getResourceOrFallback(Model.class, modelName);
        Model model = resources.usable(Model.class, requestedModel);
        if (model == null || !model.isLoaded())
            throw new IllegalStateException("Model not loaded : " + modelName + " (load its batch before creating the renderer)");

        shown = new MeshSet(model);

        setTextureUniforms(DEFAULT_DIFFUSE_UNIFORM, DEFAULT_SPECULAR_UNIFORM);

        shininessValue = new ShaderValue(SHININESS_UNIFORM, Float.class, 32f);

        faceCulling = true;

        upload = glResources.addElementOrReturnIfPresent(MeshUpload.class, new MeshUpload(this),
                "ModelRenderer/" + modelName + "#" + id());
    }

    // The meshes of one version of the model, each with its own Mesh, and their textures
    private final class MeshSet {
        final Model model;
        final int version;
        // A copy: a reloaded model gives its previous list to the copy that unloads it
        final List<ModelMesh> meshes;
        final Mesh[] glMeshes;
        final Texture[] diffuseTextures;
        final Texture[] specularTextures;
        int sent;

        MeshSet(Model model) {
            this.model = model;
            version = model.getVersion();
            meshes = new ArrayList<>(model.getMeshes());
            glMeshes = new Mesh[meshes.size()];
            diffuseTextures = new Texture[meshes.size()];
            specularTextures = new Texture[meshes.size()];

            for (int i = 0; i < meshes.size(); ++i) {
                diffuseTextures[i] = takeTexture(diffuseOverride != null ? diffuseOverride : meshes.get(i).getDiffuseTexture());
                specularTextures[i] = takeTexture(meshes.get(i).getSpecularTexture());
            }
        }

        boolean isOf(Model model) {
            return this.model == model && model.getVersion() == version;
        }

        boolean complete() {
            return sent >= meshes.size();
        }

        // Send the next mesh, true when all of them are sent
        boolean sendNext() {
            if (complete())
                return true;

            ModelMesh mesh = meshes.get(sent);
            Mesh glMesh = new Mesh(true);
            glMesh.setEboStorage(Mesh.STATIC_STORE);
            glMesh.addVboFloat(mesh.getPositions(), 3, Mesh.STATIC_STORE);
            glMesh.addVboFloat(mesh.getTextureCoordinates(), 2, Mesh.STATIC_STORE);
            glMesh.addVboFloat(mesh.getNormals(), 3, Mesh.STATIC_STORE);
            glMesh.setEbo(mesh.getIndices());
            Logger.CheckOpenGLError("Send mesh " + mesh.getName() + " of model " + modelName);

            glMeshes[sent] = glMesh;
            ++sent;

            return complete();
        }

        // Each mesh holds its own reference, taken before releasing the old one to avoid reloading a shared texture
        void overrideDiffuse(String textureName) {
            for (int i = 0; i < diffuseTextures.length; ++i) {
                Texture previous = diffuseTextures[i];
                diffuseTextures[i] = takeTexture(textureName);
                glResources.releaseResource(previous);
            }
        }

        void release() {
            for (int i = 0; i < sent; ++i)
                glMeshes[i].unload();

            for (Texture texture : diffuseTextures)
                glResources.releaseResource(texture);
            for (Texture texture : specularTextures)
                glResources.releaseResource(texture);
        }
    }

    // The model was loaded (the fallback cube was displayed) or reloaded: its meshes are sent apart
    private void rebuildIfChanged() {
        Model model = resources.usable(Model.class, requestedModel);
        MeshSet latest = pending != null ? pending : shown;
        if (latest.isOf(model))
            return;

        // Created before releasing a set still being sent (changed again): their shared textures stay loaded
        MeshSet next = new MeshSet(model);
        if (pending != null)
            pending.release();

        pending = next;
        upload.sendAgain();
    }

    private Texture takeTexture(String name) {
        if (name == null)
            name = "error";

        return glResources.addElementOrReturnIfPresent(Texture.class, new Texture(name), name);
    }

    /**
     * Names of the sampler uniforms of the shader, specularUniform can be null when the shader has no specular map.
     * Uniforms absent from the shader are ignored.
     */
    public ModelRenderer setTextureUniforms(String diffuseUniform, String specularUniform) {
        this.diffuseUniform = setSampler(this.diffuseUniform, diffuseUniform, DIFFUSE_UNIT);
        this.specularUniform = setSampler(this.specularUniform, specularUniform, SPECULAR_UNIT);

        return this;
    }

    // The unit of the sampler, sent at each draw when the current program has it
    private String setSampler(String previous, String uniform, int unit) {
        if (previous != null) {
            shaderValues.remove(previous);
            optionalValues.remove(previous);
        }

        if (uniform != null)
            putOptionalValue(uniform, new ShaderValue(uniform, Integer.class, unit));

        return uniform;
    }

    // Replace the diffuse map of every mesh (model file without material), kept when the model is reloaded
    public ModelRenderer setDiffuseOverride(String textureName) {
        diffuseOverride = textureName;

        shown.overrideDiffuse(textureName);
        if (pending != null)
            pending.overrideDiffuse(textureName);

        return this;
    }

    public ModelRenderer setFaceCulling(boolean faceCulling) {
        this.faceCulling = faceCulling;
        return this;
    }

    public String getModelName() {
        return modelName;
    }

    public int getNumberOfMeshes() {
        return shown.meshes.size();
    }

    public int getNumberOfSentMeshes() {
        return shown.sent;
    }

    // Send the next mesh: the first version is displayed as it is sent, a newer one replaces it once complete
    private boolean sendNextMesh() {
        if (pending == null)
            return shown.sendNext();

        if (!pending.sendNext())
            return false;

        MeshSet previous = shown;
        shown = pending;
        pending = null;
        previous.release();

        return true;
    }

    @Override
    public void display() {
        rebuildIfChanged();

        if (!isVisible() || shown.sent == 0)
            return;

        // The program of the renderer is used once for every mesh
        mesh.applyDimension();
        getShader().use();
        sendShaderValues();

        // Decided on the current program: it can change (fallback while loading, hot reload)
        boolean bindSpecular = specularUniform != null && hasUniform(specularUniform);
        boolean sendMeshShininess = !shaderValues.containsKey(SHININESS_UNIFORM) && hasUniform(SHININESS_UNIFORM);

        if (faceCulling)
            glEnable(GL_CULL_FACE);

        for (int i = 0; i < shown.sent; ++i) {
            shown.diffuseTextures[i].bindRenderTexture(DIFFUSE_UNIT);
            if (bindSpecular)
                shown.specularTextures[i].bindRenderTexture(SPECULAR_UNIT);

            if (sendMeshShininess) {
                shininessValue.setObject(shown.meshes.get(i).getShininess());
                sentToShader(shininessValue);
            }

            shown.glMeshes[i].bind();
            shown.glMeshes[i].draw();
        }

        if (faceCulling)
            glDisable(GL_CULL_FACE);

        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Display model " + modelName);
    }

    @Override
    public void unload() {
        glResources.releaseResource(upload);

        shown.release();
        if (pending != null)
            pending.release();

        // The base mesh is empty: the meshes of the model are drawn instead
        super.unload();
    }

    // Not shared: one per renderer, its loading is the sending of the meshes
    public static class MeshUpload extends GLElement {
        private final ModelRenderer renderer;

        MeshUpload(ModelRenderer renderer) {
            this.renderer = renderer;
        }

        @Override
        public boolean load(int remainingMilliseconds) {
            return renderer.sendNextMesh();
        }

        // The renderer was built again (model loaded or reloaded): its meshes are sent again, one per step
        void sendAgain() {
            requestReload();
        }

        @Override
        protected boolean reload(int remainingMilliseconds) {
            return renderer.sendNextMesh();
        }

        @Override
        public void unload(int remainingMilliseconds) {
            // The meshes belong to the renderer
        }
    }
}
