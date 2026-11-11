package io.github.mightycode.mightylib.algorithms;

import io.github.mightycode.mightylib.resources.models.ModelMesh;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainGenerationTest {
    private static final int RESOLUTION = 6;
    private static final float WORLD_SIZE = 20f;

    private static ModelMesh generate(TerrainGeneration generation, Vector2f center) {
        return generation.generateTerrain(RESOLUTION, WORLD_SIZE, center);
    }

    @Test
    void settersChainAndGettersReturnTheValues() {
        TerrainGeneration generation = new TerrainGeneration()
                .setLayerCount(7).setLacunarity(3f).setPersistence(0.25f).setRidgeLayerStart(2).setJiggle(0.1f);

        assertEquals(7, generation.getLayerCount());
        assertEquals(3f, generation.getLacunarity());
        assertEquals(0.25f, generation.getPersistence());
        assertEquals(2, generation.getRidgeLayerStart());
        assertEquals(0.1f, generation.getJiggle());
    }

    @Test
    void theMeshHasTwoFlatShadedTrianglesPerCell() {
        ModelMesh mesh = generate(new TerrainGeneration(), new Vector2f());

        int vertices = (RESOLUTION - 1) * (RESOLUTION - 1) * 2 * 3;
        assertEquals(vertices, mesh.getNumberOfVertices());
        assertEquals(vertices * 3, mesh.getNormals().length);
        assertEquals(vertices * 2, mesh.getTextureCoordinates().length);
        assertEquals(vertices, mesh.getIndices().length);
        assertEquals("terrain", mesh.getName());
        for (int i = 0; i < vertices; ++i)
            assertEquals(i, mesh.getIndices()[i]);
    }

    @Test
    void theSameSettingsGiveTheSameTerrain() {
        ModelMesh first = generate(new TerrainGeneration(), new Vector2f(3, 4));
        ModelMesh second = generate(new TerrainGeneration(), new Vector2f(3, 4));

        assertArrayEquals(first.getPositions(), second.getPositions());
    }

    @Test
    void theHeightIsNeverNegativeAndTheTerrainStaysInsideItsArea() {
        Vector2f center = new Vector2f(100, -50);
        float jiggle = 0.7f;
        float[] positions = generate(new TerrainGeneration().setJiggle(jiggle), center).getPositions();

        for (int i = 0; i < positions.length; i += 3) {
            assertTrue(positions[i + 1] >= 0f, "elevation");
            assertTrue(Math.abs(positions[i] - center.x) <= WORLD_SIZE / 2 + jiggle + 1e-3, "x");
            assertTrue(Math.abs(positions[i + 2] - center.y) <= WORLD_SIZE / 2 + jiggle + 1e-3, "z");
        }
    }

    @Test
    void withoutJiggleThePointsAreOnARegularGrid() {
        float[] positions = generate(new TerrainGeneration().setJiggle(0f), new Vector2f()).getPositions();

        // first triangle of the first cell: A (0,0), C (0,1), B (1,0), in cells of world size / (resolution - 1)
        float step = WORLD_SIZE / (RESOLUTION - 1);
        assertEquals(-WORLD_SIZE / 2, positions[0], 1e-4);
        assertEquals(-WORLD_SIZE / 2, positions[2], 1e-4);
        assertEquals(-WORLD_SIZE / 2, positions[3], 1e-4, "C keeps the x of A");
        assertEquals(-WORLD_SIZE / 2 + step, positions[5], 1e-4, "C is one cell further along z");
        assertEquals(-WORLD_SIZE / 2 + step, positions[6], 1e-4, "B is one cell further along x");
        assertEquals(-WORLD_SIZE / 2, positions[8], 1e-4, "B keeps the z of A");
    }

    @Test
    void eachTriangleSharesOneUnitNormalAndOneTextureCoordinate() {
        ModelMesh mesh = generate(new TerrainGeneration(), new Vector2f());
        float[] normals = mesh.getNormals();
        float[] uv = mesh.getTextureCoordinates();
        float[] positions = mesh.getPositions();

        for (int triangle = 0; triangle < mesh.getNumberOfVertices() / 3; ++triangle) {
            int first = triangle * 3;
            Vector3f normal = new Vector3f(normals[first * 3], normals[first * 3 + 1], normals[first * 3 + 2]);
            assertEquals(1f, normal.length(), 1e-3, "unit normal");

            float meanHeight = 0;
            for (int v = 0; v < 3; ++v) {
                assertEquals(normal.x, normals[(first + v) * 3], 1e-6);
                assertEquals(normal.y, normals[(first + v) * 3 + 1], 1e-6);
                assertEquals(normal.z, normals[(first + v) * 3 + 2], 1e-6);
                assertEquals(uv[first * 2], uv[(first + v) * 2], 1e-6);
                assertEquals(0f, uv[(first + v) * 2 + 1]);
                meanHeight += positions[(first + v) * 3 + 1] / 3f;
            }

            assertEquals(meanHeight, uv[first * 2], 1e-3, "the texture coordinate is the mean height");
        }
    }

    @Test
    void theNormalsPointUpward() {
        float[] normals = generate(new TerrainGeneration(), new Vector2f()).getNormals();
        for (int i = 1; i < normals.length; i += 3)
            assertTrue(normals[i] > 0f, "the triangles are wound so that they face up");
    }

    @Test
    void moreLayersChangeTheRelief() {
        float[] flat = generate(new TerrainGeneration().setLayerCount(1), new Vector2f()).getPositions();
        float[] rough = generate(new TerrainGeneration().setLayerCount(6), new Vector2f()).getPositions();
        assertFalse(java.util.Arrays.equals(flat, rough));
    }
}
