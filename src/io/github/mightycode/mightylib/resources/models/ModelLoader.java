package io.github.mightycode.mightylib.resources.models;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.ColorTextureData;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.assimp.Assimp.*;

/**
 * Read 3D models with Assimp (CPU only, in the resource workers).
 * A model is named after its file name, its textures "modelName/relative/path" (relative to the model folder).
 */
public class ModelLoader extends ResourceLoader {
    private static final String[] EXTENSIONS = { ".obj", ".fbx", ".gltf", ".glb", ".dae", ".3ds", ".ply", ".stl" };

    private static final int IMPORT_FLAGS = aiProcess_Triangulate | aiProcess_FlipUVs
            | aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices;

    private static final float DEFAULT_SHININESS = 32f;

    // Used when a material has no specular map: no specular reflection
    private static final int NO_SPECULAR_COLOR = 0xFF000000;

    @Override
    public Class<? extends DataType> getType() {
        return Model.class;
    }

    @Override
    public String getResourceNameType() {
        return "Model";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new Model(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);
        if (ending == null)
            return null;

        ending = ending.toLowerCase();
        for (String extension : EXTENSIONS) {
            if (extension.equals(ending))
                return getFileName(path);
        }

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof Model))
            return;

        Model model = (Model) dataType;

        AIScene scene = aiImportFile(model.path(), IMPORT_FLAGS);
        if (scene == null || scene.mRootNode() == null) {
            System.err.println("--Can't import the model " + model.getDataName() + " (" + model.path() + ") : " + aiGetErrorString());
            if (scene != null)
                aiReleaseImport(scene);
            return;
        }

        try {
            String folder = new File(model.path()).getParent();
            String[][] materialsTextures = readMaterials(model, scene, folder);
            float[] materialsShininess = readShininess(scene);

            List<ModelMesh> meshes = new ArrayList<>();
            readNode(scene, scene.mRootNode(), new Matrix4f(), materialsTextures, materialsShininess, meshes);
            model.setMeshes(meshes);
        } finally {
            aiReleaseImport(scene);
        }
    }

    // For each material: { diffuse texture name, specular texture name }
    private String[][] readMaterials(Model model, AIScene scene, String folder) {
        int count = scene.mNumMaterials();
        String[][] textures = new String[count][2];
        PointerBuffer materials = scene.mMaterials();

        for (int i = 0; i < count; ++i) {
            AIMaterial material = AIMaterial.create(materials.get(i));

            String diffuse = readTexture(model, material, aiTextureType_DIFFUSE, folder);
            if (diffuse == null)
                diffuse = registerColor(model, readColor(material, AI_MATKEY_COLOR_DIFFUSE));

            String specular = readTexture(model, material, aiTextureType_SPECULAR, folder);
            if (specular == null)
                specular = registerColor(model, NO_SPECULAR_COLOR);

            textures[i][0] = diffuse;
            textures[i][1] = specular;
        }

        return textures;
    }

    private float[] readShininess(AIScene scene) {
        int count = scene.mNumMaterials();
        float[] shininess = new float[count];
        PointerBuffer materials = scene.mMaterials();

        float[] value = new float[1];
        int[] max = new int[]{ 1 };
        for (int i = 0; i < count; ++i) {
            AIMaterial material = AIMaterial.create(materials.get(i));
            max[0] = 1;
            if (aiGetMaterialFloatArray(material, AI_MATKEY_SHININESS, aiTextureType_NONE, 0, value, max) == aiReturn_SUCCESS
                    && value[0] >= 1f)
                shininess[i] = value[0];
            else
                shininess[i] = DEFAULT_SHININESS;
        }

        return shininess;
    }

    private static int readColor(AIMaterial material, String key) {
        try (AIColor4D color = AIColor4D.calloc()) {
            if (aiGetMaterialColor(material, key, aiTextureType_NONE, 0, color) == aiReturn_SUCCESS)
                return ColorTextureData.ToArgb(color.r(), color.g(), color.b(), 1f);
        }

        return 0xFFFFFFFF;
    }

    // Return the name of the registered texture, or null if the material has no texture of this type
    private String readTexture(Model model, AIMaterial material, int type, String folder) {
        if (aiGetMaterialTextureCount(material, type) == 0)
            return null;

        String file;
        try (AIString path = AIString.calloc()) {
            if (aiGetMaterialTexture(material, type, 0, path, (IntBuffer) null, null, null, null, null, null) != aiReturn_SUCCESS)
                return null;

            file = path.dataString().replace('\\', '/');
        }

        // Embedded textures ("*0") are not supported yet
        if (file.isEmpty() || file.startsWith("*")) {
            System.err.println("--Model " + model.getDataName() + " : embedded texture " + file + " ignored");
            return null;
        }

        String fullPath = new File(folder, file).getPath().replace('\\', '/');
        if (!new File(fullPath).isFile()) {
            System.err.println("--Model " + model.getDataName() + " : texture not found " + fullPath);
            return null;
        }

        TextureData texture = Resources.getInstance().addOwnedResource(TextureData.class,
                new TextureData(model.getDataName() + "/" + file, fullPath), model.ownerName());
        model.addOwnedTexture(texture);

        return texture.getDataName();
    }

    private String registerColor(Model model, int argb) {
        TextureData texture = Resources.getInstance().addOwnedResource(TextureData.class, new ColorTextureData(argb), model.ownerName());
        model.addOwnedTexture(texture);

        return texture.getDataName();
    }

    private void readNode(AIScene scene, AINode node, Matrix4f parentTransform,
                          String[][] materialsTextures, float[] materialsShininess, List<ModelMesh> meshes) {
        Matrix4f transform = new Matrix4f(parentTransform).mul(toMatrix(node.mTransformation()));

        IntBuffer meshIndices = node.mMeshes();
        PointerBuffer sceneMeshes = scene.mMeshes();
        for (int i = 0; i < node.mNumMeshes(); ++i) {
            AIMesh mesh = AIMesh.create(sceneMeshes.get(meshIndices.get(i)));
            ModelMesh modelMesh = readMesh(mesh, transform, materialsTextures, materialsShininess);
            if (modelMesh != null)
                meshes.add(modelMesh);
        }

        PointerBuffer children = node.mChildren();
        for (int i = 0; i < node.mNumChildren(); ++i)
            readNode(scene, AINode.create(children.get(i)), transform, materialsTextures, materialsShininess, meshes);
    }

    private ModelMesh readMesh(AIMesh mesh, Matrix4f transform, String[][] materialsTextures, float[] materialsShininess) {
        int vertexCount = mesh.mNumVertices();
        if (vertexCount == 0)
            return null;

        Matrix3f normalTransform = transform.normal(new Matrix3f());

        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] textureCoordinates = new float[vertexCount * 2];

        AIVector3D.Buffer vertices = mesh.mVertices();
        AIVector3D.Buffer meshNormals = mesh.mNormals();
        AIVector3D.Buffer coordinates = mesh.mTextureCoords(0);

        Vector3f vector = new Vector3f();
        for (int i = 0; i < vertexCount; ++i) {
            AIVector3D vertex = vertices.get(i);
            transform.transformPosition(vector.set(vertex.x(), vertex.y(), vertex.z()));
            positions[i * 3] = vector.x;
            positions[i * 3 + 1] = vector.y;
            positions[i * 3 + 2] = vector.z;

            if (meshNormals != null) {
                AIVector3D normal = meshNormals.get(i);
                normalTransform.transform(vector.set(normal.x(), normal.y(), normal.z())).normalize();
                normals[i * 3] = vector.x;
                normals[i * 3 + 1] = vector.y;
                normals[i * 3 + 2] = vector.z;
            }

            if (coordinates != null) {
                AIVector3D coordinate = coordinates.get(i);
                textureCoordinates[i * 2] = coordinate.x();
                textureCoordinates[i * 2 + 1] = coordinate.y();
            }
        }

        // Points and lines left by the triangulation are ignored
        AIFace.Buffer faces = mesh.mFaces();
        int[] indices = new int[mesh.mNumFaces() * 3];
        int indexCount = 0;
        for (int i = 0; i < mesh.mNumFaces(); ++i) {
            AIFace face = faces.get(i);
            if (face.mNumIndices() != 3)
                continue;

            IntBuffer faceIndices = face.mIndices();
            indices[indexCount++] = faceIndices.get(0);
            indices[indexCount++] = faceIndices.get(1);
            indices[indexCount++] = faceIndices.get(2);
        }

        if (indexCount == 0)
            return null;

        if (indexCount != indices.length) {
            int[] trimmed = new int[indexCount];
            System.arraycopy(indices, 0, trimmed, 0, indexCount);
            indices = trimmed;
        }

        int materialIndex = mesh.mMaterialIndex();
        String diffuse = null, specular = null;
        float shininess = DEFAULT_SHININESS;
        if (materialIndex >= 0 && materialIndex < materialsTextures.length) {
            diffuse = materialsTextures[materialIndex][0];
            specular = materialsTextures[materialIndex][1];
            shininess = materialsShininess[materialIndex];
        }

        return new ModelMesh(mesh.mName().dataString(), positions, normals, textureCoordinates, indices,
                diffuse, specular, shininess);
    }

    // Assimp matrices are row-major, JOML constructor takes the columns
    private static Matrix4f toMatrix(AIMatrix4x4 m) {
        return new Matrix4f(
                m.a1(), m.b1(), m.c1(), m.d1(),
                m.a2(), m.b2(), m.c2(), m.d2(),
                m.a3(), m.b3(), m.c3(), m.d3(),
                m.a4(), m.b4(), m.c4(), m.d4());
    }
}
