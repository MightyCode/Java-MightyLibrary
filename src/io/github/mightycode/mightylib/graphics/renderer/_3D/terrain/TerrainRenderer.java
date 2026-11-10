package io.github.mightycode.mightylib.graphics.renderer._3D.terrain;

import io.github.mightycode.mightylib.graphics.renderer.Renderer;
import io.github.mightycode.mightylib.graphics.renderer.Shape;
import io.github.mightycode.mightylib.resources.models.ModelMesh;
import io.github.mightycode.mightylib.utils.math.color.Color4f;

// Draws a generated mesh (TerrainGeneration), colored by its normals
public class TerrainRenderer extends Renderer {
    private final boolean willUpdateFrequently;
    private int shapePosition;
    private int colorPosition;
    private int normalPosition;

    private final ModelMesh mesh;

    public TerrainRenderer(ModelMesh mesh, boolean willUpdateFrequently) {
        super("multiColoredLighting3D", true);
        this.mesh = mesh;
        this.willUpdateFrequently = willUpdateFrequently;
    }

    public void load() {
        float[] normals = mesh.getNormals();

        int[] colors = new int[mesh.getNumberOfVertices()];
        for (int i = 0; i < colors.length; ++i)
            colors[i] = new Color4f(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2], 1).toIntRGBA();

        shape.setEbo(mesh.getIndices());

        int store = willUpdateFrequently ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE;

        shapePosition = shape.addVboFloat(mesh.getPositions(), 3, store);
        colorPosition = shape.addVboInt(colors, 1, store);
        normalPosition = shape.addVboFloat(normals, 3, store);
    }
}
