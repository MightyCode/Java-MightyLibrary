package MightyLibrary.mightylib.graphics.debug;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.*;

/**
 * Display a bunch of rectangles that correspond to node position.
 */
public class NodesRenderer {
    private static final int EBO_SHIFT = 6;
    private static final int VBO_SHIFT = 8;

    // Custom renderer
    private final Renderer renderer;

    // List of rectangles' vertices
    private float[] vbo;
    private final int vboPositionIndex;
    private float nodeSize;

    public NodesRenderer(Camera2D referenceCamera){
        // Setup the renderer
        renderer = new Renderer("colorShape2D", true);
        renderer.setColorMode(ColorList.Red());
        renderer.getShape().setEboStorage(Shape.DYNAMIC_STORE);
        renderer.getShape().setEbo(new int[0]);
        renderer.setReferenceCamera(referenceCamera);

        vboPositionIndex = renderer.getShape().addVboFloat(new float[0], 2, Shape.DYNAMIC_STORE);

        nodeSize = 2;
    }

    public void setColor(Color4f color){
        renderer.setColorMode(color);
    }

    public void setNodeSize(float nodeSize){
        this.nodeSize = nodeSize;
    }

    public float getNodeSize() {
        return nodeSize;
    }

    /**
     * Precompute the size of storage using the number of nodes.
     * Only the position in scene will change in time, precompute the fixed order of vertex to draw.
     * @param nodes Collection of parametrized nodes
     */
    public void init(Collection<Vector2f> nodes) {
        int [] ebo = new int[EBO_SHIFT  * nodes.size()];
        int [] eboValues = new int[]{ 0, 1, 2, 0, 2, 3 };

        for (int i = 0; i < nodes.size(); ++i) {
            for (int j = 0; j < EBO_SHIFT; ++j)
                ebo[EBO_SHIFT * i + j] = eboValues[j] + 4 * i;
        }

        // Give the renderer the order of vertex to draw
        renderer.getShape().setEbo(ebo);

        // Create the array containing the future vertex positions.
        vbo = new float[VBO_SHIFT * nodes.size()];

        System.out.println("Number of nodes : " + nodes.size());
    }

    /**
     * Update the renderer with nodes and their positions
     *
     * @param nodes List of nodes inside the boundaries
     * @param boundaries geographic boundaries
     * @param displayBoundaries display size destination
     * @param zoomLevel current camera zoom level
     */
    public void updateNodes(Collection<Vector2f> nodes, Vector4f boundaries, Vector4f displayBoundaries, float zoomLevel) {
        // Zoom level is used to have the same node size.
        float nodeSize = this.nodeSize / zoomLevel;

        int i = 0;
        Vector4f temp = new Vector4f();
        Vector2f position;

        for (Vector2f node : nodes){
            position = node;

            // Compute four positions of the current node representation (a rectangle)
            temp.x = position.x - nodeSize / 2;
            temp.z = position.x + nodeSize / 2;
            temp.y = position.y - nodeSize / 2;
            temp.w = position.y + nodeSize / 2;

            vbo[i * VBO_SHIFT + 0] = temp.x; vbo[i * VBO_SHIFT + 1] = temp.w;
            vbo[i * VBO_SHIFT + 2] = temp.x; vbo[i * VBO_SHIFT + 3] = temp.y;
            vbo[i * VBO_SHIFT + 4] = temp.z; vbo[i * VBO_SHIFT + 5] = temp.y;
            vbo[i * VBO_SHIFT + 6] = temp.z; vbo[i * VBO_SHIFT + 7] = temp.w;
            ++i;
        }

        // Update the renderer with computed vertex positions
        renderer.getShape().updateVbo(vbo, vboPositionIndex);
    }

    public void display() {
        renderer.display();
    }

    public void unload() {
        renderer.unload();
    }
}
