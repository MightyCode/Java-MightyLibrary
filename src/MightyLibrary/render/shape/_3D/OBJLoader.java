package MightyLibrary.render.shape._3D;

import MightyLibrary.render.shape.Shape;
import MightyLibrary.util.valueDebug.TableDebug;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public abstract class OBJLoader {

    public static Shape loadObjModel(String path){
        // TODO Rewrite with file methods
        // TODO Rewrite with own way
        FileReader fr = null;
        try {
            fr = new FileReader(new File("resources/3dmodels/" + path + ".obj"));
        } catch (FileNotFoundException e){
            System.err.println("Couldn't find the file !");
            e.printStackTrace();
            return new Shape("colorShape2D", false, false);
        }

        BufferedReader bReader = new BufferedReader(fr);
        String line;
        List<Vector3f> vertices = new ArrayList<Vector3f>();
        List<Vector2f> textures = new ArrayList<Vector2f>();
        List<Vector3f> normals = new ArrayList<Vector3f>();
        List<Integer> indices = new ArrayList<Integer>();

        float[] verticesArray = null;
        float[] texturesArray = null;
        float[] normalsArray = null;
        int[] indicesArray = null;
        try{
            // TODO Refactor the loop to not have a break
            while(true){
                line = bReader.readLine();
                String[] currentLine = line.split(" ");
                // Vertex
                if (line.startsWith("v ")){
                    Vector3f vertex = new Vector3f(Float.parseFloat(currentLine[1]),
                            Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                    vertices.add(vertex);

                // Texture position associated to each vertex
                } else if (line.startsWith("vt")){
                    Vector2f texture = new Vector2f(Float.parseFloat(currentLine[1]),
                            Float.parseFloat(currentLine[2]));
                    textures.add(texture);

                // Normal associated to each vertex
                } else if (line.startsWith("vn")){
                    Vector3f normal = new Vector3f(Float.parseFloat(currentLine[1]),
                            Float.parseFloat(currentLine[2]), Float.parseFloat(currentLine[3]));
                    normals.add(normal);

                // Indices - each face indicates which vertex is connected with which
                } else if (line.startsWith("f")){
                    texturesArray = new float[vertices.size() * 2];
                    normalsArray = new float[vertices.size() * 3];
                    break;
                }
            }

            // TODO Refactor to not have the continu
            while(line != null){
                if(!line.startsWith("f ")){
                    line = bReader.readLine();
                    continue;
                }
                String[] currentLine = line.split(" ");
                String[] vertex1 = currentLine[1].split("/");
                String[] vertex2 = currentLine[2].split("/");
                String[] vertex3 = currentLine[3].split("/");

                processVertex(vertex1, indices, textures, normals, texturesArray, normalsArray);
                processVertex(vertex2, indices, textures, normals, texturesArray, normalsArray);
                processVertex(vertex3, indices, textures, normals, texturesArray, normalsArray);

                line = bReader.readLine();
            }
            bReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        verticesArray = new float[vertices.size() * 3];
        indicesArray = new int[indices.size()];

        int vertexPointer = 0;
        for (Vector3f vertex: vertices){
            verticesArray[vertexPointer++] = vertex.x;
            verticesArray[vertexPointer++] = vertex.y;
            verticesArray[vertexPointer++] = vertex.z;
        }

        for (int i = 0; i < indices.size(); i++){
            indicesArray[i] = indices.get(i);
        }

        Shape shape = new Shape("texture3D", true, false);
        shape.setEbo(indicesArray);
        shape.setReading(new int[]{3, 2});

        float[] vertex = new float[verticesArray.length + texturesArray.length];
        for (int i = 0; i < vertex.length / 5; i++){
            vertex[i * 5] = verticesArray[i * 3];
            vertex[i * 5  + 1] = verticesArray[i * 3 + 1];
            vertex[i * 5 + 2] = verticesArray[i * 3 + 2];
            vertex[i * 5 + 3] = texturesArray[i * 2];
            vertex[i * 5 + 4] = texturesArray[i * 2 + 1];

        }

        shape.setVbo(vertex);
        return shape;
    }

    private static void processVertex(String[] vertexData, List<Integer> indices, List<Vector2f> textures,
                                      List<Vector3f> normals, float[] texturesArray, float[] normalsArray){

        int currentVertexPointer = Integer.parseInt(vertexData[0]) - 1;
        indices.add(currentVertexPointer);

        Vector2f currentTex = textures.get(Integer.parseInt(vertexData[1]) -1);
        texturesArray[currentVertexPointer * 2] = currentTex.x;
        texturesArray[currentVertexPointer * 2 + 1] = 1 - currentTex.y;

        Vector3f currentNorm = normals.get(Integer.parseInt(vertexData[2])-1);
        normalsArray[currentVertexPointer * 3] = currentNorm.x;
        normalsArray[currentVertexPointer * 3 + 1] = currentNorm.y;
        normalsArray[currentVertexPointer * 3 + 2] = currentNorm.z;
    }
}
