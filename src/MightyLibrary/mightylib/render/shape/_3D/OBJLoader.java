package MightyLibrary.mightylib.render.shape._3D;

import MightyLibrary.mightylib.render.shape.Shape;
import MightyLibrary.mightylib.util.FileMethods;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public abstract class OBJLoader {

    public static Shape loadObjTexturedModel(String path) {
        String file = FileMethods.readFileAsString("resources/3dmodels/" + path + ".obj");
        int indexFrom = 0;
        int indexTo = -1;
        boolean end = false;

        String line;
        List<Vector3f> vertices = new ArrayList<Vector3f>();
        List<Vector2f> textures = new ArrayList<Vector2f>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<Integer> indices = new ArrayList<Integer>();

        float[] verticesArray;
        float[] texturesArray = null;
        float[] normalsArray = null;
        int[] indicesArray;

        while (nextLine(file, indexFrom) != -1 && !end) {
            indexFrom = indexTo + 1;
            indexTo = nextLine(file, indexFrom);
            line = file.substring(indexFrom, indexTo);
            String[] currentLine = line.split(" ");
            // Vertex
            if (line.startsWith("v ")) {
                Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]),
                        Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                vertices.add(vertex);

                // Texture position associated to each vertex
            } else if (line.startsWith("vt")) {
                Vector2f texture = new Vector2f(Float.parseFloat(currentLine[1]),
                        Float.parseFloat(currentLine[2]));
                textures.add(texture);

                // Normal associated to each vertex
            } else if (line.startsWith("vn")) {
                Vector3f normal = new Vector3f(Float.parseFloat(currentLine[1]),
                        Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                normals.add(normal);

                // Indices - each face indicates which vertex is connected with which
            } else if (line.startsWith("f")) {
                texturesArray = new float[vertices.size() * 2];
                normalsArray = new float[vertices.size() * 3];
                end = true;
            }
        }


        while (indexTo < file.length()) {
            line = file.substring(indexFrom, indexTo);
            if (line.startsWith("f ")){
                String[] currentLine = line.split(" ");
                String[] vertex1 = currentLine[1].split("/");
                String[] vertex2 = currentLine[2].split("/");
                String[] vertex3 = currentLine[3].split("/");

                processVertexWithTexture(vertex1, indices, textures, normals, texturesArray, normalsArray);
                processVertexWithTexture(vertex2, indices, textures, normals, texturesArray, normalsArray);
                processVertexWithTexture(vertex3, indices, textures, normals, texturesArray, normalsArray);

            }

            indexFrom = indexTo + 1;
            indexTo = nextLine(file, indexFrom);
        }

        verticesArray = new float[vertices.size() * 3];
        indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (Vector3f vertex : vertices) {
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        Shape shape = new Shape("texture3D", true, false);
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indicesArray);
        shape.addVbo(verticesArray, 3, Shape.STATIC_STORE);
        shape.addVbo(texturesArray, 2,  Shape.STATIC_STORE);

        return shape;
    }

    public static Shape loadObjColoredModel(String path) {
        String file = FileMethods.readFileAsString("resources/3dmodels/" + path + ".obj");
        int indexFrom = 0;
        int indexTo = 0;
        boolean end = false;

        String line;
        List<Vector3f> vertices = new ArrayList<Vector3f>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<Integer> indices = new ArrayList<Integer>();

        float[] verticesArray;
        float[] normalsArray = null;
        int[] indicesArray;

        indexTo = nextLine(file, indexFrom);
        while (indexTo != -1 && !end) {
            line = file.substring(indexFrom, indexTo);
            String[] currentLine = line.split(" ");
            // Vertex
            if (line.startsWith("v ")) {
                Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]),
                        Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                vertices.add(vertex);

                // Normal associated to each vertex
            } else if (line.startsWith("vn")) {
                Vector3f normal = new Vector3f(Float.parseFloat(currentLine[1]),
                        Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                normals.add(normal);

                // Indices - each face indicates which vertex is connected with which
            } else if (line.startsWith("f")) {
                normalsArray = new float[vertices.size() * 3];
                end = true;
            }

            indexFrom = indexTo + 1;
            indexTo = nextLine(file, indexFrom);
        }


        while (indexTo < file.length()) {
            line = file.substring(indexFrom, indexTo);
            if (line.startsWith("f ")){
                String[] currentLine = line.split(" ");
                String[] vertex1 = currentLine[1].split("/");
                String[] vertex2 = currentLine[2].split("/");
                String[] vertex3 = currentLine[3].split("/");

                processVertexWithoutTexture(vertex1, indices, normals, normalsArray);
                processVertexWithoutTexture(vertex2, indices, normals, normalsArray);
                processVertexWithoutTexture(vertex3, indices, normals, normalsArray);

            }

            indexFrom = indexTo + 1;
            indexTo = nextLine(file, indexFrom);
        }

        verticesArray = new float[vertices.size() * 3];
        indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (Vector3f vertex : vertices) {
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        for (int i = 0; i < indices.size(); i++) {
            indicesArray[i] = indices.get(i);
        }

        Shape shape = new Shape("colorShape3D", true, false);
        shape.setEboStorage(Shape.STATIC_STORE);
        shape.setEbo(indicesArray);

        shape.addVbo(verticesArray, 3, Shape.STATIC_STORE);
        return shape;
    }

    private static void processVertexWithTexture(String[] vertexData, List<Integer> indices, List<Vector2f> textures,
                                                 List<Vector3f> normals, float[] texturesArray, float[] normalsArray) {

        int currentVertexPointer = Integer.parseInt(vertexData[0]) - 1;
        indices.add(currentVertexPointer);

        Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) - 1);
        texturesArray[currentVertexPointer * 2] = currentTex.x;
        texturesArray[currentVertexPointer * 2 + 1] = 1 - currentTex.y;

        Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2]) - 1);
        normalsArray[currentVertexPointer * 3] = currentNorm.x;
        normalsArray[currentVertexPointer * 3 + 1] = currentNorm.y;
        normalsArray[currentVertexPointer * 3 + 2] = currentNorm.z;
    }

    private static void processVertexWithoutTexture(String[] vertexData, List<Integer> indices,
                                                    List<Vector3f> normals, float[] normalsArray) {

        int currentVertexPointer = Integer.parseInt(vertexData[0]) - 1;
        indices.add(currentVertexPointer);

        Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2]) - 1);
        normalsArray[currentVertexPointer * 3] = currentNorm.x;
        normalsArray[currentVertexPointer * 3 + 1] = currentNorm.y;
        normalsArray[currentVertexPointer * 3 + 2] = currentNorm.z;
    }

    private static int nextLine(String file, int currentIndex){
        if (currentIndex == file.length()-1) return -1;
        boolean found = false;
        int i = currentIndex + 1;
        while (!found && i < file.length()){
            if (file.charAt(i) == '\n')  found = true;
            else                         i++;
        }
        return i;
    }
}
