package io.github.mightycode.mightylib.algorithms;

import io.github.mightycode.mightylib.resources.models.ModelMesh;
import com.raylabz.opensimplex.OpenSimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector3f;

// Inspired by Sebastien Lague (https://www.youtube.com/@SebastianLague) : https://www.youtube.com/watch?v=yyJ-hdISgnw
public class TerrainGeneration {
    private final OpenSimplexNoise noiseGen;
    public int LayerCount = 5;
    public float Lacunarity = 2f;
    public float Persistence = 0.5f;
    public int RidgeLayerStart = 3;

    public float Jiggle = 0.7f;

    public TerrainGeneration () {
        noiseGen = new OpenSimplexNoise(42);
    }

    private float calculateElevation(Vector2f pos) {
        float frequency = 0.05f;
        float amplitude = 1;
        float elevation = 0;

        for (int i = 0; i < LayerCount; ++i) {
            float noise = (float)noiseGen.getNoise2D(pos.x * frequency, pos.y * frequency).getValue();

            if (i >= RidgeLayerStart) {
                noise = 0.5f - Math.abs(noise);
            }

            elevation += noise * amplitude;
            amplitude *= Persistence;
            frequency *= Lacunarity;
        }

        return elevation * 10;
    }

    private Vector3f[][] generatePointMap(int resolution, float worldSize, Vector2f gridCenter) {
        Vector3f[][] pointMap = new Vector3f[resolution][resolution];

        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {

                // localGridPos_sNorm = (x, y) / (resolution - 1) - (0.5, 0.5)
                Vector2f localGridPos_sNorm = new Vector2f(
                        (float) x / (resolution - 1) - 0.5f,
                        (float) y / (resolution - 1) - 0.5f
                );

                // gridWorldPos = gridCentre + worldSize * localGridPos_sNorm
                Vector2f gridWorldPos = new Vector2f(gridCenter)
                        .add(new Vector2f(localGridPos_sNorm).mul(worldSize));

                // gridWorldPos += CalculateJiggle(gridWorldPos)
                gridWorldPos.add(calculateJiggle(gridWorldPos));

                // elevation = max(0, CalculateElevation(gridWorldPos) + 0.8)
                float elevation = Math.max(0f, calculateElevation(gridWorldPos) + 0.8f);

                // pointMap[x, y] = (gridWorldPos.x, elevation, gridWorldPos.y)
                pointMap[x][y] = new Vector3f(gridWorldPos.x, elevation, gridWorldPos.y);
            }
        }

        return pointMap;
    }

    private Vector2f calculateJiggle(Vector2f pos) {
        float ox = (float)noiseGen.getNoise2D(pos.x, pos.y).getValue() * Jiggle;
        float oy = (float)noiseGen.getNoise2D(pos.x - 10000, pos.y - 10000).getValue() * Jiggle;

        return new Vector2f(ox, oy);
    }

    // Flat shaded terrain: each triangle has its own 3 vertices, sharing its surface normal
    public ModelMesh generateTerrain(int resolution, float worldSize, Vector2f gridCentre) {
        Vector3f[][] pointMap = generatePointMap(resolution, worldSize, gridCentre);

        int vertexCount = (resolution - 1) * (resolution - 1) * 2 * 3;
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] textureCoordinates = new float[vertexCount * 2];
        int[] indices = new int[vertexCount];

        int vertex = 0;
        for (int y = 0; y < resolution - 1; y++) {
            for (int x = 0; x < resolution - 1; x++) {
                // A, C, B
                vertex = addTriangle(pointMap[x][y], pointMap[x][y + 1], pointMap[x + 1][y],
                        vertex, positions, normals, textureCoordinates);
                // B, C, D
                vertex = addTriangle(pointMap[x + 1][y], pointMap[x][y + 1], pointMap[x + 1][y + 1],
                        vertex, positions, normals, textureCoordinates);
            }
        }

        for (int i = 0; i < indices.length; ++i)
            indices[i] = i;

        return new ModelMesh("terrain", positions, normals, textureCoordinates, indices, null, null, 0);
    }

    // Writes the 3 vertices from the index vertex, returns the index of the next one
    private static int addTriangle(Vector3f a, Vector3f b, Vector3f c, int vertex,
                                   float[] positions, float[] normals, float[] textureCoordinates) {
        // textureCoordinate = (a.y + b.y + c.y, 0) / 3
        float textureCoordinate = (a.y + b.y + c.y) / 3f;

        // surfaceNormal = normalize(cross(b - a, c - b))
        Vector3f edge1 = new Vector3f(b).sub(a);
        Vector3f edge2 = new Vector3f(c).sub(b);
        Vector3f surfaceNormal = edge1.cross(edge2).normalize();

        for (Vector3f point : new Vector3f[]{a, b, c}) {
            positions[vertex * 3] = point.x;
            positions[vertex * 3 + 1] = point.y;
            positions[vertex * 3 + 2] = point.z;
            normals[vertex * 3] = surfaceNormal.x;
            normals[vertex * 3 + 1] = surfaceNormal.y;
            normals[vertex * 3 + 2] = surfaceNormal.z;
            textureCoordinates[vertex * 2] = textureCoordinate;
            textureCoordinates[vertex * 2 + 1] = 0f;
            ++vertex;
        }

        return vertex;
    }
}
