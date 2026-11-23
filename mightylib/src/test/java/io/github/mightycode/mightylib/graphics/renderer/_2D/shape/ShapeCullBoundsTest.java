package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The boxes of the vector shapes for the view culling: the quads and triangles their vertex shaders place
class ShapeCullBoundsTest {
    private static final float EPSILON = 1e-4f;

    // A pixel of worldPerPixel world units, the camera turned by angle (the size of a pixel stays the same)
    private static ViewCullBounds bounds(Matrix4f model, float worldPerPixel, float angle) {
        float cos = (float) Math.cos(angle), sin = (float) Math.sin(angle);
        return new ViewCullBounds().clear(model, ShapeCullBoundsTest.class)
                .pixelToWorld(cos * worldPerPixel, sin * worldPerPixel, -sin * worldPerPixel, cos * worldPerPixel);
    }

    private static ShapeStroke stroke(float width, EStrokeAlignment alignment, EStrokeUnit unit) {
        ShapeStroke stroke = new ShapeStroke();
        stroke.setWidth(width);
        stroke.setAlignment(alignment);
        stroke.setUnit(unit);
        return stroke;
    }

    // The size of a pixel of the shaders: rotations of the camera don't change it, a non uniform one is its largest
    @Test
    void theSizeOfAPixelIsTheOneOfTheShaders() {
        assertEquals(0.5f, bounds(null, 0.5f, 0f).worldPerPixel(), EPSILON);
        assertEquals(0.5f, bounds(null, 0.5f, 0.7f).worldPerPixel(), EPSILON);
        assertEquals(3f, new ViewCullBounds().clear(null, getClass()).pixelToWorld(1, 0, 0, 3).worldPerPixel(),
                EPSILON);
        assertTrue(Float.isNaN(new ViewCullBounds().clear(null, getClass()).worldPerPixel()));

        // In the space of the model: a scale of 4 makes a pixel 4 times smaller, a turned model changes nothing
        Matrix4f model = new Matrix4f().translate(100, 50, 0).rotateZ(0.6f).scale(4, 4, 1);
        assertEquals(0.125f, bounds(model, 0.5f, 0.3f).localPerPixel(), EPSILON);
        // Non uniform: the largest
        model.identity().scale(2, 0.5f, 1);
        assertEquals(1f, bounds(model, 0.5f, 0f).localPerPixel(), EPSILON);
        // Flat: unknown
        model.identity().scale(0, 1, 1);
        assertTrue(Float.isNaN(bounds(model, 0.5f, 0f).localPerPixel()));
    }

    // shapeVertex: local = +-(halfSize + margin), unit position = center + local / size
    private static float[] shaderQuad(float sizeX, float sizeY, float referenceX, float referenceY, float margin) {
        float centerX = 0.5f - referenceX, centerY = 0.5f - referenceY;
        float halfX = (Math.abs(sizeX) * 0.5f + margin) / Math.abs(sizeX);
        float halfY = (Math.abs(sizeY) * 0.5f + margin) / Math.abs(sizeY);
        return new float[]{centerX - halfX, centerY - halfY, centerX + halfX, centerY + halfY};
    }

    private static void assertBox(float[] expected, ViewCullBounds bounds, String name) {
        assertTrue(bounds.hasBox(), name);
        assertTrue(bounds.isLocal(), name);
        assertEquals(expected[0], bounds.minX(), EPSILON, name);
        assertEquals(expected[1], bounds.minY(), EPSILON, name);
        assertEquals(expected[2], bounds.maxX(), EPSILON, name);
        assertEquals(expected[3], bounds.maxY(), EPSILON, name);
    }

    // Each alignment of the stroke, in world units and in pixels at two zooms, with and without anti-aliasing
    @Test
    void aVectorShapeIsItsQuad() {
        float[][] sizes = {{120, 80}, {30, 200}, {-60, 40}};
        float[][] references = {{0, 0}, {0.5f, 0.5f}, {1, 0.25f}};
        for (EStrokeAlignment alignment : EStrokeAlignment.values()) {
            for (EStrokeUnit unit : EStrokeUnit.values()) {
                for (float worldPerPixel : new float[]{0.25f, 4f}) {
                    for (boolean antialiasing : new boolean[]{true, false}) {
                        for (int i = 0; i < sizes.length; ++i) {
                            ShapeStroke stroke = stroke(6, alignment, unit);
                            float width = unit == EStrokeUnit.SCREEN_PIXELS ? 6 * worldPerPixel : 6;
                            float margin = width * alignment.outsideFraction()
                                    + (antialiasing ? worldPerPixel : 0) + 3;
                            ViewCullBounds bounds = bounds(null, worldPerPixel, 0.4f);
                            assertTrue(ShapeCullBounds.VectorShape(bounds, sizes[i][0], sizes[i][1],
                                    references[i][0], references[i][1], stroke, antialiasing, 3));
                            String name = alignment + " " + unit + " " + worldPerPixel + " " + antialiasing + " " + i;
                            assertBox(shaderQuad(sizes[i][0], sizes[i][1], references[i][0], references[i][1],
                                    margin), bounds, name);
                        }
                    }
                }
            }
        }
    }

    @Test
    void withoutTheSizeOfAPixelThereIsNoBox() {
        ViewCullBounds bounds = new ViewCullBounds().clear(null, getClass());
        assertFalse(ShapeCullBounds.VectorShape(bounds, 10, 10, 0, 0, new ShapeStroke(), true, 0));
        assertFalse(ShapeCullBounds.Points(bounds, new float[]{0, 0, 1, 1}, 4, new ShapeStroke(), true, 0));
        assertFalse(ShapeCullBounds.Stroke(bounds, new float[]{0, 0, 1, 1}, 1, 1, 1, 1, new ShapeStroke(), true));
        assertFalse(bounds.hasBox());
    }

    // shapePointVertex: center + corner * (halfSize + margin), the pixel measured in the space of the model
    @Test
    void pointsAreTheBoxOfTheirQuads() {
        Matrix4f model = new Matrix4f().translate(10, 20, 0).rotateZ(0.5f).scale(2, 2, 1);
        for (EStrokeUnit unit : EStrokeUnit.values()) {
            ShapeStroke stroke = stroke(4, EStrokeAlignment.OUTER, unit);
            ViewCullBounds bounds = bounds(model, 1f, 0.2f);
            assertTrue(ShapeCullBounds.Points(bounds, new float[]{-5, 3, 40, 12}, 10, stroke, true, 0));
            // A pixel is half a unit of the model
            float margin = 5 + (unit == EStrokeUnit.SCREEN_PIXELS ? 4 * 0.5f : 4) + 0.5f;
            assertBox(new float[]{-5 - margin, 3 - margin, 40 + margin, 12 + margin}, bounds, unit.toString());
        }
    }

    private static List<Vector2f> points(float... coordinates) {
        List<Vector2f> points = new ArrayList<>();
        for (int i = 0; i < coordinates.length; i += 2)
            points.add(new Vector2f(coordinates[i], coordinates[i + 1]));
        return points;
    }

    // Every vertex strokeVertex places is in the box (divided by the size), which is not much larger than them
    private static void assertStrokeInside(StrokeGeometry geometry, float sizeX, float sizeY, ShapeStroke stroke,
                                           float worldPerPixel, boolean antialiasing, String name) {
        float[] bases = geometry.bases();
        int count = geometry.vertexCount();
        float[] basesBox = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < count; ++i) {
            basesBox[0] = Math.min(basesBox[0], bases[2 * i]);
            basesBox[1] = Math.min(basesBox[1], bases[2 * i + 1]);
            basesBox[2] = Math.max(basesBox[2], bases[2 * i]);
            basesBox[3] = Math.max(basesBox[3], bases[2 * i + 1]);
        }
        float[] longest = new float[2];
        ShapeCullBounds.LongestExtrusions(geometry.packedExtrusions(), count, longest);

        ViewCullBounds bounds = bounds(null, worldPerPixel, 0f);
        assertTrue(ShapeCullBounds.Stroke(bounds, basesBox, longest[0], longest[1], sizeX, sizeY, stroke,
                antialiasing), name);

        float halfWidth = 0.5f * stroke.worldWidth(worldPerPixel);
        float fringe = antialiasing ? worldPerPixel : 0f;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < count; ++i) {
            Vector2f position = geometry.position(i, halfWidth, fringe);
            float x = position.x / sizeX, y = position.y / sizeY;
            assertTrue(x >= bounds.minX() - EPSILON && x <= bounds.maxX() + EPSILON
                    && y >= bounds.minY() - EPSILON && y <= bounds.maxY() + EPSILON, name + ": vertex " + i);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // At most the longest extrusion beyond the vertices on each side
        float slackX = (longest[0] * (halfWidth + fringe) + longest[1] * fringe) / Math.abs(sizeX) + EPSILON;
        float slackY = (longest[0] * (halfWidth + fringe) + longest[1] * fringe) / Math.abs(sizeY) + EPSILON;
        assertTrue(minX - bounds.minX() <= slackX && bounds.maxX() - maxX <= slackX, name);
        assertTrue(minY - bounds.minY() <= slackY && bounds.maxY() - maxY <= slackY, name);
    }

    // Joins (miter limited or not), caps, closed lines, in world units and pixels, mirrored sizes
    @Test
    void theTrianglesOfAStrokeAreInItsBox() {
        List<Vector2f> zigzag = points(0, 0, 40, 5, 0, 10, 40, 15, 45, 60);
        List<Vector2f> square = points(0, 0, 30, 0, 30, 30, 0, 30);
        for (EStrokeJoin join : EStrokeJoin.values()) {
            for (EStrokeCap cap : EStrokeCap.values()) {
                for (float miterLimit : new float[]{1f, 1.5f, Stroker.DEFAULT_MITER_LIMIT, 20f}) {
                    for (EStrokeUnit unit : EStrokeUnit.values()) {
                        ShapeStroke stroke = stroke(5, EStrokeAlignment.CENTERED, unit);
                        String name = join + " " + cap + " " + miterLimit + " " + unit;
                        assertStrokeInside(Stroker.stroke(zigzag, false, join, cap, miterLimit), 1, 1, stroke,
                                0.5f, true, name + " open");
                        assertStrokeInside(Stroker.stroke(square, true, join, cap, miterLimit), 2, -3, stroke,
                                2f, true, name + " closed");
                        assertStrokeInside(Stroker.stroke(zigzag, false, join, cap, miterLimit), -0.5f, 4, stroke,
                                0.25f, false, name + " sharp");
                    }
                }
            }
        }
    }
}
