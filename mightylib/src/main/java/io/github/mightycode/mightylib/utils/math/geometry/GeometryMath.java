package io.github.mightycode.mightylib.utils.math.geometry;

import org.joml.Vector2f;

import java.util.List;

public final class GeometryMath {
    public static float CrossProductLength(float Ax, float Ay, float Bx,
                                           float By, float Cx, float Cy) {
        // Get the vectors' coordinates.
        float BAx = Ax - Bx;
        float BAy = Ay - By;
        float BCx = Cx - Bx;
        float BCy = Cy - By;

        // Calculate the Z coordinate of the cross product.
        return (BAx * BCy - BAy * BCx);
    }

    public static float CrossProductLength(Vector2f A, Vector2f B, Vector2f C) {
        return CrossProductLength(A.x, A.y, B.x, B.y, C.x, C.y);
    }

    public static boolean IsConvex(Vector2f A, Vector2f B, Vector2f C) {
        return CrossProductLength(A, B, C) < 0;
    }

    public static boolean IsConvex(float Ax, float Ay, float Bx, float By, float Cx, float Cy) {
        return CrossProductLength(Ax, Ay, Bx, By, Cx, Cy) < 0;
    }

    // A vertex is an ear when its corner is convex (the polygon is counter-clockwise) and no other vertex is in the
    // triangle made with its two neighbors (the edges count as inside)
    public static boolean isEar(int index, List<Vector2f> polygon) {
        int previous = index > 0 ? index - 1 : polygon.size() - 1;
        int next = (index + 1) % polygon.size();
        Vector2f vertex = polygon.get(index);
        Vector2f prevVertex = polygon.get(previous);
        Vector2f nextVertex = polygon.get(next);

        if (!IsConvex(prevVertex, vertex, nextVertex))
            return false;

        for (int other = 0; other < polygon.size(); ++other) {
            // The corner itself, not another vertex at the same place
            if (other == previous || other == index || other == next)
                continue;

            if (isInsideTriangle(polygon.get(other), prevVertex, vertex, nextVertex))
                return false;
        }

        return true;
    }

    private static float edge(Vector2f a, Vector2f b, Vector2f p) {
        return (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x);
    }

    // Inside or on an edge
    private static boolean isInsideTriangle(Vector2f p, Vector2f a, Vector2f b, Vector2f c) {
        float d1 = edge(a, b, p), d2 = edge(b, c, p), d3 = edge(c, a, p);
        boolean hasNegative = d1 < 0 || d2 < 0 || d3 < 0;
        boolean hasPositive = d1 > 0 || d2 > 0 || d3 > 0;

        return !(hasNegative && hasPositive);
    }
}
