package io.github.mightycode.mightylib.graphics.renderer._3D;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer.Shape;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.resources.models.Model;
import io.github.mightycode.mightylib.resources.models.ModelMesh;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Display a Model (ModelLoader): one Shape per mesh, sent to the graphic card one mesh per frame
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

    private final String shaderName;
    private final String modelName;
    // The model asked for; the one displayed is its usable version (the fallback cube while it is not loaded)
    private final Model requestedModel;
    private Model builtFrom;
    private int builtVersion;
    private List<ModelMesh> meshes;

    private Shape[] shapes;
    private Texture[] diffuseTextures;
    private Texture[] specularTextures;
    private int sentMeshes;
    // VBOs of the base shape (first mesh), filled again when the model changes
    private int[] baseShapeVbos;
    private String diffuseOverride;

    private boolean bindSpecular;
    private boolean faceCulling;

    private final ShaderValue shininessValue;
    private final boolean sendShininess;

    // Sends the next mesh at each GLResources step
    private final MeshUpload upload;

    public ModelRenderer(String modelName) {
        this(DEFAULT_SHADER, modelName);
    }

    public ModelRenderer(String shaderName, String modelName) {
        super(shaderName, true);
        this.shaderName = shaderName;
        this.modelName = modelName;

        // Unknown, failed or not loaded yet: the fallback cube, replaced once the model is loaded (see rebuildIfChanged)
        requestedModel = resources.getResourceOrFallback(Model.class, modelName);
        Model model = resources.usable(Model.class, requestedModel);
        if (model == null || !model.isLoaded())
            throw new IllegalStateException("Model not loaded : " + modelName + " (load its batch before creating the renderer)");

        build(model);

        setTextureUniforms(DEFAULT_DIFFUSE_UNIFORM, DEFAULT_SPECULAR_UNIFORM);

        sendShininess = shape.getShader().getLink(SHININESS_UNIFORM) != -1;
        shininessValue = new ShaderValue(SHININESS_UNIFORM, Float.class, 32f);

        faceCulling = true;

        upload = glResources.addElementOrReturnIfPresent(MeshUpload.class, new MeshUpload(this),
                "ModelRenderer/" + modelName + "#" + id());
    }

    // Meshes and textures of this model, sent by MeshUpload in the next frames
    private void build(Model model) {
        builtFrom = model;
        builtVersion = model.getVersion();
        // A copy: a reloaded model gives its previous list to the copy that unloads it
        meshes = new ArrayList<>(model.getMeshes());

        Texture[] previousDiffuse = diffuseTextures, previousSpecular = specularTextures;
        shapes = new Shape[meshes.size()];
        diffuseTextures = new Texture[meshes.size()];
        specularTextures = new Texture[meshes.size()];
        sentMeshes = 0;

        for (int i = 0; i < meshes.size(); ++i) {
            diffuseTextures[i] = takeTexture(diffuseOverride != null ? diffuseOverride : meshes.get(i).getDiffuseTexture());
            specularTextures[i] = takeTexture(meshes.get(i).getSpecularTexture());
        }

        // Released after taking the new ones: a texture used by both is not unloaded
        releaseTextures(previousDiffuse);
        releaseTextures(previousSpecular);
    }

    private void releaseTextures(Texture[] textures) {
        if (textures == null)
            return;

        for (Texture texture : textures)
            glResources.releaseResource(texture);
    }

    // The model was loaded (the fallback cube was displayed) or reloaded: its meshes are sent again
    private void rebuildIfChanged() {
        Model model = resources.usable(Model.class, requestedModel);
        if (model == builtFrom && model.getVersion() == builtVersion)
            return;

        // shapes[0] is the base shape, filled again
        for (int i = 1; i < sentMeshes; ++i)
            shapes[i].unload();

        build(model);
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
        sendSampler(diffuseUniform, DIFFUSE_UNIT);
        bindSpecular = specularUniform != null && sendSampler(specularUniform, SPECULAR_UNIT);

        return this;
    }

    private boolean sendSampler(String uniform, int unit) {
        if (uniform == null || shape.getShader().getLink(uniform) == -1)
            return false;

        shape.getShader().sendValueToShader(new ShaderValue(uniform, Integer.class, unit));
        return true;
    }

    // Replace the diffuse map of every mesh (model file without material)
    public ModelRenderer setDiffuseOverride(String textureName) {
        diffuseOverride = textureName;

        // Each mesh holds its own reference, taken before releasing the old one to avoid reloading a shared texture
        for (int i = 0; i < diffuseTextures.length; ++i) {
            Texture previous = diffuseTextures[i];
            diffuseTextures[i] = takeTexture(textureName);
            glResources.releaseResource(previous);
        }

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
        return meshes.size();
    }

    public int getNumberOfSentMeshes() {
        return sentMeshes;
    }

    // Send the next mesh, true when all of them are sent
    private boolean sendNextMesh() {
        if (sentMeshes >= meshes.size())
            return true;

        ModelMesh mesh = meshes.get(sentMeshes);

        // The base shape of the renderer holds the first mesh: its VBOs are filled again when the model changes
        Shape meshShape;
        if (sentMeshes == 0 && baseShapeVbos != null) {
            meshShape = shape;
            shape.updateVbo(mesh.getPositions(), baseShapeVbos[0]);
            shape.updateVbo(mesh.getTextureCoordinates(), baseShapeVbos[1]);
            shape.updateVbo(mesh.getNormals(), baseShapeVbos[2]);
        } else {
            meshShape = sentMeshes == 0 ? shape : new Shape(shaderName, true);
            meshShape.setEboStorage(Shape.STATIC_STORE);
            int[] vbos = {
                    meshShape.addVboFloat(mesh.getPositions(), 3, Shape.STATIC_STORE),
                    meshShape.addVboFloat(mesh.getTextureCoordinates(), 2, Shape.STATIC_STORE),
                    meshShape.addVboFloat(mesh.getNormals(), 3, Shape.STATIC_STORE)
            };
            if (sentMeshes == 0)
                baseShapeVbos = vbos;
        }
        meshShape.setEbo(mesh.getIndices());
        Logger.CheckOpenGLError("Send mesh " + mesh.getName() + " of model " + modelName);

        shapes[sentMeshes] = meshShape;
        ++sentMeshes;

        return sentMeshes >= meshes.size();
    }

    @Override
    public void display() {
        rebuildIfChanged();

        if (!isVisible() || sentMeshes == 0)
            return;

        for (ShaderValue value : shaderValues.values())
            sentToShader(value);

        boolean sendMeshShininess = sendShininess && !shaderValues.containsKey(SHININESS_UNIFORM);

        if (faceCulling)
            glEnable(GL_CULL_FACE);

        for (int i = 0; i < sentMeshes; ++i) {
            diffuseTextures[i].bindRenderTexture(DIFFUSE_UNIT);
            if (bindSpecular)
                specularTextures[i].bindRenderTexture(SPECULAR_UNIT);

            if (sendMeshShininess) {
                shininessValue.setObject(meshes.get(i).getShininess());
                sentToShader(shininessValue);
            }

            shapes[i].display();
        }

        if (faceCulling)
            glDisable(GL_CULL_FACE);

        if (Logger.DO_CHECK)
            Logger.CheckOpenGLError("Display model " + modelName);
    }

    @Override
    public void unload() {
        glResources.releaseResource(upload);

        // shapes[0] is the base shape, unloaded by Renderer
        for (int i = 1; i < sentMeshes; ++i)
            shapes[i].unload();

        releaseTextures(diffuseTextures);
        releaseTextures(specularTextures);

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
            // The shapes belong to the renderer
        }
    }
}
