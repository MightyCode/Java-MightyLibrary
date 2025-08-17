package MightyLibrary.mightylib.algorithms;

import MightyLibrary.mightylib.graphics.renderer._3D.Mesh;
import MightyLibrary.mightylib.graphics.renderer._3D.MeshEntry;
import com.raylabz.opensimplex.OpenSimplexNoise;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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

    public Mesh<MeshEntry> generateTerrain(int resolution, float worldSize, Vector2f gridCentre) {
        Vector3f[][] pointMap = generatePointMap(resolution, worldSize, gridCentre);
        List<Vector3f> points = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();

        for (int y = 0; y < resolution - 1; y++) {
            for (int x = 0; x < resolution - 1; x++) {
                // A, C, B
                addTriangle(pointMap[x][y], pointMap[x][y + 1], pointMap[x + 1][y], points, normals, texCoords);
                // B, C, D
                addTriangle(pointMap[x + 1][y], pointMap[x][y + 1], pointMap[x + 1][y + 1], points, normals, texCoords);
            }
        }

        // Convert raw arrays to Mesh<MeshEntry>
        Mesh<MeshEntry> mesh = new Mesh<>();
        for (int i = 0; i < points.size(); i++) {
            MeshEntry entry = new MeshEntry();
            entry.positions.set(points.get(i));
            entry.normals.set(normals.get(i));
            entry.texturesPositions.set(texCoords.get(i));
            mesh.entries.add(entry);
        }

        return mesh;
    }

    private static void addTriangle(Vector3f a, Vector3f b, Vector3f c,
                                    List<Vector3f> points,
                                    List<Vector3f> normals,
                                    List<Vector2f> texCoords) {

        // texCoord = (a.y + b.y + c.y, 0) / 3
        Vector2f texCoord = new Vector2f((a.y + b.y + c.y) / 3f, 0f);

        // surfaceNormal = normalize(cross(b - a, c - b))
        Vector3f edge1 = new Vector3f(b).sub(a);
        Vector3f edge2 = new Vector3f(c).sub(b);
        Vector3f surfaceNormal = edge1.cross(edge2, new Vector3f()).normalize();

        // Add 3 vertices
        points.add(a);
        points.add(b);
        points.add(c);

        // Add 3 normals (same for each vertex of triangle)
        normals.add(surfaceNormal);
        normals.add(surfaceNormal);
        normals.add(surfaceNormal);

        // Add 3 texture coordinates (same for each vertex)
        texCoords.add(texCoord);
        texCoords.add(texCoord);
        texCoords.add(texCoord);
    }
}
