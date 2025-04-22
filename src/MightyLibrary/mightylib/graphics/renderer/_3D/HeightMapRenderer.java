package MightyLibrary.mightylib.graphics.renderer._3D;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.TextureData;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class HeightMapRenderer extends Renderer {

    public enum ColorMode {
        DEFAULT,
        SIMPLE_GRADIENT,
        GREENISH_MOUNTAIN,
        // Add more if needed
    }

    @FunctionalInterface
    public interface IHeightMapper {
        float map(float x);
    }

    private ColorMode colorMode = ColorMode.GREENISH_MOUNTAIN; // Default

    private float minHeight = Float.MAX_VALUE;
    private float maxHeight = Float.MIN_VALUE;
    private float heightRange = 0f;

    private IHeightMapper heightMapper = x -> x;

    private final boolean willUpdateFrequently;
    private final int shapePosition;
    private final int colorPosition;
    private final int normalPosition;

    private float[] rawHeights;

    private float[] positions;
    private float[] colors;
    private float[] normals;

    private final Vector4f tempColor;

    private final TextureData heightMapTexture;
    public HeightMapRenderer(String heightMapName, boolean willUpdateFrequently) {
        super("multiColoredLightning3D", true);
        this.heightMapTexture = Resources.getInstance().getResource(TextureData.class, heightMapName);
        heightMapTexture.computeCache();

        this.willUpdateFrequently = willUpdateFrequently;

        int width = heightMapTexture.getWidth();
        int height = heightMapTexture.getHeight();

        rawHeights = new float[width * height];

        int vertexCount = width * height;

        int[] indices = new int[(width - 1) * (height - 1) * 6];

        int index = 0;
        for (int z = 0; z < height - 1; z++) {
            for (int x = 0; x < width - 1; x++) {
                int topLeft = z * width + x;
                int topRight = topLeft + 1;
                int bottomLeft = (z + 1) * width + x;
                int bottomRight = bottomLeft + 1;

                indices[index++] = topLeft;
                indices[index++] = bottomLeft;
                indices[index++] = topRight;
                indices[index++] = topRight;
                indices[index++] = bottomLeft;
                indices[index++] = bottomRight;
            }
        }

        shape.setEbo(indices);

        if (willUpdateFrequently) {
            positions = new float[vertexCount * 3];
            colors = new float[vertexCount * 4];
            normals = new float[vertexCount * 3];
        }

        int store = willUpdateFrequently ? Shape.DYNAMIC_STORE : Shape.STATIC_STORE;

        shapePosition = shape.addVboFloat(positions, 3, store);
        colorPosition = shape.addVboFloat(colors, 4, store);
        normalPosition  = shape.addVboFloat(normals, 3, store);

        tempColor = new Color4f(1);
    }

    public HeightMapRenderer setColorMode(ColorMode mode) {
        this.colorMode = mode;

        return this;
    }

    public HeightMapRenderer setHeightMapper(IHeightMapper mapper) {
        this.heightMapper = mapper;
        return this;
    }

    public HeightMapRenderer setHeightMapperDefault() {
        this.heightMapper = x -> x;
        return this;
    }

    public HeightMapRenderer setHeightNormalized() {
        this.heightMapper = x -> (x - minHeight) / (heightRange == 0 ? 1 : heightRange);
        return this;
    }

    public void load() {
        // Example

        int width = heightMapTexture.getWidth();
        int height = heightMapTexture.getHeight();

        if (willUpdateFrequently) {
            rawHeights = new float[width * height];
        } else {
            int vertexCount = width * height;

            positions = new float[vertexCount * 3];
            colors = new float[vertexCount * 4];
            normals = new float[vertexCount * 3];
        }

        // First loop: compute raw heights and find min/max
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                float raw = getRawHeightAt(x, z);
                rawHeights[z * width + x] = raw;

                if (raw < minHeight)
                    minHeight = raw;

                if (raw > maxHeight)
                    maxHeight = raw;
            }
        }

        heightRange = maxHeight - minHeight;

        Vector3f tangentX = new Vector3f();
        Vector3f tangentZ = new Vector3f();
        Vector3f normal = new Vector3f();

        int vertexIndex = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                float rawHeight = rawHeights[z * width + x];
                float y = heightMapper.map(rawHeight);

                positions[vertexIndex * 3]     = x / (float)(width - 1);
                positions[vertexIndex * 3 + 1] = y;
                positions[vertexIndex * 3 + 2] = z / (float)(height - 1);

                heightToColor(y);
                //System.out.println(tempColor);
                colors[vertexIndex * 4] = tempColor.x;
                colors[vertexIndex * 4 + 1] = tempColor.y;
                colors[vertexIndex * 4 + 2] = tempColor.z;
                colors[vertexIndex * 4 + 3] = 1;

                float heightL = getMappedHeightAt(rawHeights, x - 1, z, width, height);
                float heightR = getMappedHeightAt(rawHeights, x + 1, z, width, height);
                float heightD = getMappedHeightAt(rawHeights, x, z - 1, width, height);
                float heightU = getMappedHeightAt(rawHeights, x, z + 1, width, height);

                tangentX.set(1, heightR - heightL, 0);
                tangentZ.set(0, heightU - heightD, 1);
                tangentZ.cross(tangentX, normal).normalize();

                normals[vertexIndex * 3]     = normal.x;
                normals[vertexIndex * 3 + 1] = normal.y;
                normals[vertexIndex * 3 + 2] = normal.z;

                vertexIndex++;
            }
        }

        shape.updateSubVbo(positions, shapePosition, 0);
        shape.updateSubVbo(colors, colorPosition, 0);
        shape.updateSubVbo(normals, normalPosition, 0);

        if (!willUpdateFrequently) {
            positions = null;
            colors = null;
            normals = null;
            rawHeights = null;
        }
    }

    private float getRawHeightAt(int x, int z) {
        int pixel = heightMapTexture.getPixelCached(x, z);
        int r = (pixel >> 16) & 0xFF;

        return r / 255.0f;
    }

    private float getMappedHeightAt(float[] rawHeights, int x, int z, int width, int height) {
        x = Math.max(0, Math.min(width - 1, x));
        z = Math.max(0, Math.min(height - 1, z));
        return heightMapper.map(rawHeights[z * width + x]);
    }

    public void heightToColor(float height) {
        switch (colorMode) {
            case SIMPLE_GRADIENT :
                heightToColorSimple(height);
            case GREENISH_MOUNTAIN :
                heightToColorMountain(height, 0.7f, 0.8f, 0.9f);
            default:
               heightToColorDefault(height);
        }
    }

    private void heightToColorDefault(float value) {
        // Simple grayscale
        tempColor.set(value, value, value, 1);
    }

    private void heightToColorSimple(float value) {
        tempColor.set(value, 0f,  1f - value, 1);
    }

    public void heightToColorMountain(float height, float waterThreshold, float greenThreshold, float snowThreshold) {
        height = Math.max(0f, Math.min(1f, height)); // Clamp between 0 and 1

        if (height <= waterThreshold) {
            tempColor.x = 0;
            tempColor.y = 0.3f;
            tempColor.z = 1;
        } else if (height <= greenThreshold) {
            float t = height / greenThreshold; // Normalize to 0–1

            tempColor.x = 0.36f + 0.24f * t;
            tempColor.y = 0.25f + 0.15f * t;
            tempColor.z = 0.20f + 0.1f * t;
        } else if (height <= snowThreshold) {
            float t = (height - greenThreshold) / (1.0f - greenThreshold);

            tempColor.x = 0.3f - 0.2f * t;
            tempColor.y = 0.6f + 0.3f * t;
            tempColor.z = 0.3f - 0.1f * t;
        } else {
            tempColor.x = 1;
            tempColor.y = 1;
            tempColor.z = 1;
        }
    }
    public float getMinHeight() {
        return minHeight;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public float getHeightRange() {
        return heightRange;
    }
}
