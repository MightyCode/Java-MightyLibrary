package MightyLibrary.mightylib.graphics.renderer._3D;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.resources.models.Model;
import MightyLibrary.mightylib.resources.models.ModelTriangle;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimpleModelRenderer extends Renderer {
    public static final String DEFAULT_SHADER = "texture3D";

    private String modelName;

    public SimpleModelRenderer(String shaderName) {
        super(shaderName, true);
        shape.setEboStorage(Shape.STATIC_STORE);

        shape.addVboFloat(new float[]{}, 3, Shape.STATIC_STORE);
        shape.addVboFloat(new float[]{}, 2, Shape.STATIC_STORE);
        shape.addVboFloat(new float[]{}, 3, Shape.STATIC_STORE);

        shape.disableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);
        shape.disableVbo(Shape.COMMON_NORMAL_POSITION_CHANNEL);
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public SimpleModelRenderer setUseTexture() {
        shape.enableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);
        return this;
    }

    public SimpleModelRenderer setUseNormal() {
        shape.enableVbo(Shape.COMMON_NORMAL_POSITION_CHANNEL);
        return this;
    }

    public void init(int remainingMilliseconds) {
        if (modelName == null) {
            throw new IllegalStateException("Model name is not set");
        }

        Model model = resources.getResource(Model.class, modelName);

        // init with texture

        float[] verticesArray;
        float[] texturesArray = null;
        float[] normalsArray = null;
        int[] indicesArray;

        List<Integer> indices = new ArrayList<>();

        boolean useTexture = shape.isEnableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);
        boolean useNormal = shape.isEnableVbo(Shape.COMMON_NORMAL_POSITION_CHANNEL);

        System.out.println("Loading a gl model with " + model.getNumberOfVertices() + " vertices and " + model.getNumberOfFaces() + " faces " + "using model " + modelName);

        if (useTexture) {
            texturesArray = new float[model.getNumberOfVertices() * 2];
        }

        if (useNormal) {
            normalsArray = new float[model.getNumberOfVertices() * 3];
        }

        for (int i = 0; i < model.getNumberOfFaces(); ++i) {
            ModelTriangle face = model.getFace(i);
            for (int j = 0; j < 3; ++j){
                HashMap<String, Integer> rowOfValues = face.getRowValueOf(j);

                int currentVertexPosition = rowOfValues.get(Model.VERTEX_POSITION);
                indices.add(currentVertexPosition);

                if (useTexture) {
                    Vector2f textureCoordinate = model.getTexture(rowOfValues.get(Model.TEXTURE_COORDINATE));
                    texturesArray[currentVertexPosition * 2] = textureCoordinate.x;
                    texturesArray[currentVertexPosition * 2 + 1] = textureCoordinate.y;
                }

                if (useNormal) {
                    Vector3f normal = model.getNormal(rowOfValues.get(Model.NORMAL_VECTOR));
                    normalsArray[currentVertexPosition * 3] = normal.x;
                    normalsArray[currentVertexPosition * 3 + 1] = normal.y;
                    normalsArray[currentVertexPosition * 3 + 2] = normal.z;
                }
            }
        }

        verticesArray = new float[model.getNumberOfVertices() * 3];
        indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (int i = 0; i < model.getNumberOfVertices(); ++i) {
            Vector3f vertex = model.getVertex(i);
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        for (int i = 0; i < indices.size(); ++i) {
            indicesArray[i] = indices.get(i);
        }

        // init with color
        // Todo : create an algorithm to compute the ebo and reduce the number of sended vertices to the GPU
        shape.setEbo(indicesArray);

        shape.updateVbo(verticesArray, Shape.COMMON_VERTEX_POSITION_CHANNEL);

        if (useTexture) {
            shape.updateVbo(texturesArray, Shape.COMMON_TEXTURE_POSITION_CHANNEL);
        }

        if (useNormal) {
            shape.updateVbo(normalsArray, Shape.COMMON_NORMAL_POSITION_CHANNEL);
        }
    }
}
