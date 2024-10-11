package MightyLibrary.mightylib.graphics.debug;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

/**
 * Display a bunch of rectangles that correspond to roads.
 */
public class GraphRenderer {
    private static final int EBO_SHIFT = 6;
    private static final int VBO_SHIFT = 8;

    // Custom renderer
    private final Renderer renderer;

    // List of rectangles' vertices
    float[] vbo;
    private final int vboPositionIndex;

    // Number of lines of the graph


    private int edgeNumber;

    private float edgeSize;

    public GraphRenderer(Camera2D referenceCamera){
        // Setup the renderer
        renderer = new Renderer("colorShape2D", true);
        renderer.setColorMode(ColorList.Blue());
        renderer.getShape().setEboStorage(Shape.DYNAMIC_STORE);
        renderer.getShape().setEbo(new int[0]);
        renderer.setReferenceCamera(referenceCamera);

        vboPositionIndex = renderer.getShape().addVboFloat(new float[0], 2, Shape.DYNAMIC_STORE);

        edgeNumber = 0;
        edgeSize = 2;
    }

    public void setColor(Color4f color){
        renderer.setColorMode(color);
    }

    /**
     * Precompute the size of storage using the number of road.
     * Only the position in scene will change in time, precompute the fixed order of vertex to draw.
     * @param nodes Ordered list of connected nodes
     */
    public void init(SortedMap<Vector2f, Long> nodes, HashMap<Vector2f, ArrayList<Vector2f>> connections){
        edgeNumber = 0;
        for (Map.Entry<Vector2f, Long> entry: nodes.entrySet()){
            for (Vector2f neighbour : connections.get(entry.getKey())){
                if (nodes.get(neighbour) < entry.getValue())
                    ++edgeNumber;
            }
        }

        int [] ebo = new int[EBO_SHIFT  * edgeNumber];
        int [] eboValues = new int[]{ 0, 1, 2, 0, 2, 3 };

        for (int i = 0; i < edgeNumber; ++i) {
            for (int j = 0; j < EBO_SHIFT; ++j)
                ebo[EBO_SHIFT * i + j] = eboValues[j] + 4 * i;
        }

        // Give the renderer the order of vertex to draw
        renderer.getShape().setEbo(ebo);

        // Create the array containing the future vertex positions.
        vbo = new float[VBO_SHIFT * edgeNumber];

        //System.out.println("Number of road : " + roadNumber);
    }

    public void setEdgeSize(float size){
        this.edgeSize = size;
    }

    /**
     * Update the renderer with connected nodes and their positions
     *
     * @param nodes List of nodes inside the boundaries
     * @param boundaries geographic boundaries
     * @param displayBoundaries display size destination
     * @param zoomLevel current camera zoom level
     */
    public void updateRoads(SortedMap<Vector2f, Long> nodes,
                            HashMap<Vector2f, ArrayList<Vector2f>> connections,
                            Vector4f boundaries, Vector4f displayBoundaries, float zoomLevel){
        float roadSize = this.edgeSize / zoomLevel;

        int i = 0;
        Vector4f temp1 = new Vector4f(), temp2 = new Vector4f();

        Vector2f position1, position2, result = new Vector2f();
        for (Map.Entry<Vector2f, Long> entry: nodes.entrySet()){
            for (Vector2f neighbour : connections.get(entry.getKey())){
                if (nodes.get(neighbour) >= entry.getValue())
                    continue;

                position1 = entry.getKey();
                position2 = neighbour;

                result = position1.sub(position2, result).perpendicular().normalize();

                // The rectangle will be rotated

                temp1.x = position1.x - result.x * roadSize / 2;
                temp1.y = position1.y - result.y * roadSize / 2;

                temp1.z = position1.x + result.x * roadSize / 2;
                temp1.w = position1.y + result.y * roadSize / 2;

                temp2.x = position2.x + result.x * roadSize / 2;
                temp2.y = position2.y + result.y * roadSize / 2;

                temp2.z = position2.x - result.x * roadSize / 2;
                temp2.w = position2.y - result.y * roadSize / 2;

                vbo[i * VBO_SHIFT] = temp1.x; vbo[i * VBO_SHIFT + 1] = temp1.y;
                vbo[i * VBO_SHIFT + 2] = temp1.z; vbo[i * VBO_SHIFT + 3] = temp1.w;
                vbo[i * VBO_SHIFT + 4] = temp2.x; vbo[i * VBO_SHIFT + 5] = temp2.y;
                vbo[i * VBO_SHIFT + 6] = temp2.z; vbo[i * VBO_SHIFT + 7] = temp2.w;
                ++i;
            }
        }

        renderer.getShape().updateVbo(vbo, vboPositionIndex);
    }

    public void display() {
        renderer.display();
    }

    public void unload() {
        renderer.unload();
    }
}
