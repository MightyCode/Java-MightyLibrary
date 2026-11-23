package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.ViewCullBounds;

/*
 * The boxes of the vector shapes for the view culling, without GL: the quads their vertex shaders place
 * (utils/shape2DVertex.glsl, utils/stroke2DVertex.glsl), in the space of their model, so that the model matrix turns
 * them like the shader does. The size of a pixel is the one of the shaders (ViewCullBounds.worldPerPixel): without it,
 * no box.
 */
final class ShapeCullBounds {
    // The shaders divide by the size, at least this
    private static final float SMALLEST_SIZE = 1e-6f;

    private ShapeCullBounds() {}

    /*
     * A shape of VectorShape2DRenderer (shapeVertex): the unit square around its reference point, widened by the quad
     * margin (outer part of the stroke, a pixel of anti-aliasing, the extra margin) divided by the size on each axis
     */
    static boolean VectorShape(ViewCullBounds out, float sizeX, float sizeY, float referenceX, float referenceY,
                               ShapeStroke stroke, boolean antialiasing, float extraMargin) {
        float worldPerPixel = out.worldPerPixel();
        if (!IsPixelSize(worldPerPixel))
            return false;

        float margin = stroke.quadMargin(worldPerPixel, antialiasing, extraMargin);
        float centerX = 0.5f - referenceX, centerY = 0.5f - referenceY;
        float halfX = 0.5f + margin / Math.max(Math.abs(sizeX), SMALLEST_SIZE);
        float halfY = 0.5f + margin / Math.max(Math.abs(sizeY), SMALLEST_SIZE);
        out.local(centerX - halfX, centerY - halfY, centerX + halfX, centerY + halfY);
        return true;
    }

    /*
     * The points of PointsRenderer (shapePointVertex): the box of their centers (minX, minY, maxX, maxY in centers),
     * widened by half the size of a point and the quad margin, in the units of the model (the pixel through it)
     */
    static boolean Points(ViewCullBounds out, float[] centers, float pointSize, ShapeStroke stroke,
                          boolean antialiasing, float extraMargin) {
        float localPerPixel = out.localPerPixel();
        if (!IsPixelSize(localPerPixel))
            return false;

        float margin = 0.5f * Math.abs(pointSize) + stroke.quadMargin(localPerPixel, antialiasing, extraMargin);
        out.local(centers[0] - margin, centers[1] - margin, centers[2] + margin, centers[3] + margin);
        return true;
    }

    /*
     * The triangles of GeometricShape2DRenderer (strokeVertex): the box of their bases (minX, minY, maxX, maxY in
     * bases, frame of the shape), widened by the longest extrusion times the half width and the fringe, and the
     * longest fringe extrusion times the fringe, then divided by the size (the model scales it back)
     */
    static boolean Stroke(ViewCullBounds out, float[] bases, float longestExtrusion, float longestFringe,
                          float sizeX, float sizeY, ShapeStroke stroke, boolean antialiasing) {
        float worldPerPixel = out.worldPerPixel();
        if (!IsPixelSize(worldPerPixel))
            return false;

        float fringe = antialiasing ? worldPerPixel : 0f;
        float halfWidth = 0.5f * stroke.worldWidth(worldPerPixel);
        float margin = longestExtrusion * (halfWidth + fringe) + longestFringe * fringe;
        float safeX = Math.copySign(Math.max(Math.abs(sizeX), SMALLEST_SIZE), sizeX);
        float safeY = Math.copySign(Math.max(Math.abs(sizeY), SMALLEST_SIZE), sizeY);
        out.local((bases[0] - margin) / safeX, (bases[1] - margin) / safeY, (bases[2] + margin) / safeX,
                (bases[3] + margin) / safeY);
        return true;
    }

    /*
     * The longest extrusion and fringe extrusion of the packed extrusions (4 floats per vertex: extrusion, then
     * fringe extrusion), written at longest[0] and longest[1]
     */
    static void LongestExtrusions(float[] packed, int vertexCount, float[] longest) {
        float extrusion = 0f, fringe = 0f;
        for (int i = 0; i < vertexCount; ++i) {
            extrusion = Math.max(extrusion, (float) Math.hypot(packed[4 * i], packed[4 * i + 1]));
            fringe = Math.max(fringe, (float) Math.hypot(packed[4 * i + 2], packed[4 * i + 3]));
        }

        longest[0] = extrusion;
        longest[1] = fringe;
    }

    private static boolean IsPixelSize(float worldPerPixel) {
        return worldPerPixel > 0f && Float.isFinite(worldPerPixel);
    }
}
