package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import org.joml.Vector4f;

/*
 * A regular polygon of the shaders (regularPolygon2DF.glsl): its vertices on the unit circle from a start angle,
 * the bounding box of them stretched onto the size of the shape, so that it fills its rectangle.
 */
final class RegularPolygonShape {
    static final int MIN_SIDES = 3;
    static final int MAX_SIDES = 64;

    private RegularPolygonShape() {}

    static int clampSides(int sides) {
        return Math.max(MIN_SIDES, Math.min(sides, MAX_SIDES));
    }

    // Bounding box of the polygon on the unit circle: min x, max x, min y, max y
    static Vector4f unitBoundingBox(int sides, float startAngle) {
        int count = clampSides(sides);
        Vector4f box = new Vector4f(Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE);
        for (int i = 0; i < count; ++i) {
            double angle = startAngle + 2 * Math.PI * i / count;
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            box.x = Math.min(box.x, x);
            box.y = Math.max(box.y, x);
            box.z = Math.min(box.z, y);
            box.w = Math.max(box.w, y);
        }

        return box;
    }

    // The parameters of the shader: sides and start angle, then the bounding box
    static void write(int sides, float startAngle, Vector4f first, Vector4f second) {
        first.set(clampSides(sides), startAngle, 0f, 0f);
        second.set(unitBoundingBox(sides, startAngle));
    }
}
