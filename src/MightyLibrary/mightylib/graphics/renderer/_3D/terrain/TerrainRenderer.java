package MightyLibrary.mightylib.graphics.renderer._3D.terrain;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.graphics.renderer._3D.Mesh;
import MightyLibrary.mightylib.graphics.renderer._3D.MeshEntry;
import MightyLibrary.mightylib.utils.math.color.Color4f;

public class TerrainRenderer extends Renderer {
    private final boolean willUpdateFrequently;
    private int shapePosition;
    private int colorPosition;
    private int normalPosition;

    private Mesh<? extends MeshEntry> mesh;

    private float[] positions;
    private float[] normals;
    private int[] colors;

    public TerrainRenderer(Mesh<? extends MeshEntry> mesh, boolean willUpdateFrequently) {
        super("multiColoredLightning3D", true);
        this.mesh = mesh;
        this.willUpdateFrequently = willUpdateFrequently;

        if (willUpdateFrequently) {
            positions = new float[0];
            colors = new int[0];
            normals = new float[0];
        }
    }

    public void load() {
        // Example
        int vertexCount = mesh.entries.size();

        int[] indices = new int[vertexCount];

        for (int i = 0; i < indices.length; ++i) {
            indices[i] = i;
        }

        shape.setEbo(indices);

        positions = new float[vertexCount * 3];
        colors = new int[vertexCount];
        normals = new float[vertexCount * 3];

        for (int i = 0; i < mesh.entries.size(); ++i) {
            MeshEntry current = mesh.entries.get(i);

            positions[i * 3] = current.positions.x;
            positions[i * 3 + 1] = current.positions.y;
            positions[i * 3 + 2] = current.positions.z;

            //System.out.println(tempColor);
            colors[i] = new Color4f(current.normals.x, current.normals.y, current.normals.z, 1).toIntRGBA();

            normals[i * 3] = current.normals.x;
            normals[i * 3 + 1] = current.normals.y;
            normals[i * 3 + 2] = current.normals.z;
        }

        int store = willUpdateFrequently ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE;

        shapePosition = shape.addVboFloat(positions, 3, store);
        colorPosition = shape.addVboInt(colors, 1, store);
        normalPosition = shape.addVboFloat(normals, 3, store);

        if (!willUpdateFrequently) {
            positions = null;
            colors = null;
            normals = null;
        }
    }
}
