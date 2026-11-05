package MightyLibrary.mightylib.resources.models;

/**
 * Geometry and material of one mesh of a model, already in the model space (node transformations applied).
 * Texture names are names of TextureData registered in Resources.
 */
public class ModelMesh {
    private final String name;

    private final float[] positions;
    private final float[] normals;
    private final float[] textureCoordinates;
    private final int[] indices;

    private final String diffuseTexture;
    private final String specularTexture;
    private final float shininess;

    public ModelMesh(String name, float[] positions, float[] normals, float[] textureCoordinates, int[] indices,
                     String diffuseTexture, String specularTexture, float shininess) {
        this.name = name;
        this.positions = positions;
        this.normals = normals;
        this.textureCoordinates = textureCoordinates;
        this.indices = indices;
        this.diffuseTexture = diffuseTexture;
        this.specularTexture = specularTexture;
        this.shininess = shininess;
    }

    public String getName() {
        return name;
    }

    public float[] getPositions() {
        return positions;
    }

    public float[] getNormals() {
        return normals;
    }

    public float[] getTextureCoordinates() {
        return textureCoordinates;
    }

    public int[] getIndices() {
        return indices;
    }

    public int getNumberOfVertices() {
        return positions.length / 3;
    }

    public String getDiffuseTexture() {
        return diffuseTexture;
    }

    public String getSpecularTexture() {
        return specularTexture;
    }

    public float getShininess() {
        return shininess;
    }
}
