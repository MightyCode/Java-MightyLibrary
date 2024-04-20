package MightyLibrary.mightylib.utils.math.geometry;

import org.joml.Vector2f;

import java.util.List;

public class GeometryMath {
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

    // Check if the three points are in a clockwise order
    public static boolean isEar(int index, List<Vector2f> polygon) {
        Vector2f vertex = polygon.get(index);
        Vector2f prevVertex = index > 0 ? polygon.get(index - 1) : polygon.get(polygon.size() - 1);
        Vector2f nextVertex = polygon.get((index + 1) % polygon.size());

        return polygon.stream()
                .filter(v -> v != prevVertex && v != vertex && v != nextVertex)
                .allMatch(v -> GeometryMath.IsConvex(prevVertex, vertex, nextVertex));
    }
}
