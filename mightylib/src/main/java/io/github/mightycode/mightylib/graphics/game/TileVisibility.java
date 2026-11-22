package io.github.mightycode.mightylib.graphics.game;

import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Which tiles of a map the view shows, without OpenGL: the box of the view in the space of the map, the range of
 * columns or rows it covers, and the tiles of a row of the compact arrays in a range of columns.
 */
final class TileVisibility {
    private TileVisibility() {}

    /**
     * The box of the viewport (-1..1 in clip space) in the space of the map (minX, minY, maxX, maxY), clip =
     * projection * view * model of the map. False when that is not an invertible 2D affine transform (a perspective,
     * a flat scale): the caller shows the whole map.
     */
    static boolean LocalBox(Matrix4fc clip, Vector4f box) {
        if (clip.m03() != 0f || clip.m13() != 0f || clip.m33() != 1f)
            return false;

        // ndc = A * local + t, A = [a c; b d] (columns: local x, local y)
        float a = clip.m00(), b = clip.m01(), c = clip.m10(), d = clip.m11();
        float tx = clip.m30(), ty = clip.m31();
        float determinant = a * d - b * c;
        if (determinant == 0f || !Float.isFinite(determinant))
            return false;

        box.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
        for (int corner = 0; corner < 4; ++corner) {
            float nx = (corner & 1) == 0 ? -1f : 1f;
            float ny = (corner & 2) == 0 ? -1f : 1f;
            float px = nx - tx, py = ny - ty;
            // local = A^-1 * (ndc - t)
            float x = (d * px - c * py) / determinant;
            float y = (-b * px + a * py) / determinant;
            box.x = Math.min(box.x, x);
            box.y = Math.min(box.y, y);
            box.z = Math.max(box.z, x);
            box.w = Math.max(box.w, y);
        }
        return true;
    }

    /**
     * The cells (columns or rows) of [0, count) that overlap a segment [min, max]: cell i spans [i * size + start,
     * i * size + end]; a cell that only touches it (no pixel in common) is not. Written as first, last at out[at],
     * out[at + 1]; last < first when none.
     */
    static void Range(float min, float max, float size, float start, float end, int count, int[] out, int at) {
        float low = Math.min(start, end), high = Math.max(start, end);
        // i * size + low < max and i * size + high > min
        double first = Math.floor((min - high) / (double) size) + 1;
        double last = Math.ceil((max - low) / (double) size) - 1;
        out[at] = (int) Math.max(0, Math.min(count, first));
        out[at + 1] = (int) Math.max(-1, Math.min(count - 1, last));
    }

    /** The first tile of [from, to) whose column is at least column (to when none): columns ascending in a row. */
    static int FirstInRow(int[] columns, int from, int to, int column) {
        int low = from, high = to;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (columns[middle] < column)
                low = middle + 1;
            else
                high = middle;
        }
        return low;
    }
}
