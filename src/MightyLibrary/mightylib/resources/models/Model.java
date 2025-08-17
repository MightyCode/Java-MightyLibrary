package MightyLibrary.mightylib.resources.models;

import MightyLibrary.mightylib.resources.SingleSourceDataType;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Model extends SingleSourceDataType {
    public static final String VERTEX_POSITION = "v";
    public static final String TEXTURE_COORDINATE = "vt";
    public static final String NORMAL_VECTOR = "vn";

    private final List<Vector3f> vertices;
    private final List<Vector2f> textures;
    private final List<Vector3f> normals;
    private final List<ModelTriangle> faces;

    public Model(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        vertices = new ArrayList<>();
        textures = new ArrayList<>();
        normals = new ArrayList<>();
        faces = new ArrayList<>();
    }

    public void addVertexPosition(Vector3f vertex) {
        vertices.add(vertex);
    }

    public int getNumberOfVertices() {
        return vertices.size();
    }

    public Vector3f getVertex(int index) {
        return vertices.get(index);
    }

    public void addTextureCoordinate(Vector2f texture) {
        textures.add(texture);
    }

    public int getNumberOfTextures() {
        return textures.size();
    }

    public Vector2f getTexture(int index) {
        return textures.get(index);
    }

    public void addNormalVector(Vector3f normal) {
        normals.add(normal);
    }

    public int getNumberOfNormals() {
        return normals.size();
    }

    public Vector3f getNormal(int index) {
        return normals.get(index);
    }

    public void addFace(ModelTriangle face) {
        faces.add(face);
    }

    public int getNumberOfFaces() {
        return faces.size();
    }

    public ModelTriangle getFace(int index) {
        return faces.get(index);
    }

    @Override
    public boolean internLoad() {
        for (ModelTriangle face : faces) {
            if (!face.isValid()) {
                return false;
            }
        }

        return !vertices.isEmpty();
    }

    @Override
    public void internUnload() {
        vertices.clear();
        textures.clear();
        normals.clear();
        faces.clear();
    }
}
