package MightyLibrary.mightylib.graphics.renderer._3D;

import MightyLibrary.mightylib.graphics.GLElement;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.resources.models.Model;
import MightyLibrary.mightylib.resources.models.ModelMesh;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.utils.Logger;

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
    private final List<ModelMesh> meshes;

    private final Shape[] shapes;
    private final Texture[] diffuseTextures;
    private final Texture[] specularTextures;
    private int sentMeshes;

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

        Model model = resources.getResource(Model.class, modelName);
        if (model == null)
            throw new IllegalArgumentException("Model not found : " + modelName);
        if (!model.isLoaded())
            throw new IllegalStateException("Model not loaded : " + modelName + " (load its batch before creating the renderer)");

        meshes = model.getMeshes();
        shapes = new Shape[meshes.size()];
        diffuseTextures = new Texture[meshes.size()];
        specularTextures = new Texture[meshes.size()];
        sentMeshes = 0;

        for (int i = 0; i < meshes.size(); ++i) {
            diffuseTextures[i] = takeTexture(meshes.get(i).getDiffuseTexture());
            specularTextures[i] = takeTexture(meshes.get(i).getSpecularTexture());
        }

        setTextureUniforms(DEFAULT_DIFFUSE_UNIFORM, DEFAULT_SPECULAR_UNIFORM);

        sendShininess = shape.getShader().getLink(SHININESS_UNIFORM) != -1;
        shininessValue = new ShaderValue(SHININESS_UNIFORM, Float.class, 32f);

        faceCulling = true;

        upload = glResources.addElementOrReturnIfPresent(MeshUpload.class, new MeshUpload(this),
                "ModelRenderer/" + modelName + "#" + id());
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

        // The base shape of the renderer holds the first mesh
        Shape meshShape = sentMeshes == 0 ? shape : new Shape(shaderName, true);
        meshShape.setEboStorage(Shape.STATIC_STORE);
        meshShape.addVboFloat(mesh.getPositions(), 3, Shape.STATIC_STORE);
        meshShape.addVboFloat(mesh.getTextureCoordinates(), 2, Shape.STATIC_STORE);
        meshShape.addVboFloat(mesh.getNormals(), 3, Shape.STATIC_STORE);
        meshShape.setEbo(mesh.getIndices());
        Logger.CheckOpenGLError("Send mesh " + mesh.getName() + " of model " + modelName);

        shapes[sentMeshes] = meshShape;
        ++sentMeshes;

        return sentMeshes >= meshes.size();
    }

    @Override
    public void display() {
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

        for (int i = 0; i < meshes.size(); ++i) {
            glResources.releaseResource(diffuseTextures[i]);
            glResources.releaseResource(specularTextures[i]);
        }

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

        @Override
        public void unload(int remainingMilliseconds) {
            // The shapes belong to the renderer
        }
    }
}
