package MightyLibrary.mightylib.utils.math.geometry;

import MightyLibrary.mightylib.utils.valueDebug.TableDebug;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.image.AreaAveragingScaleFilter;
import java.util.ArrayList;
import java.util.List;

public class Polygon {
    public static class PolygonBuilder {
        private final ArrayList<Vector2f> vertices;

        public PolygonBuilder() {
            vertices = new ArrayList<>();
        }

        public PolygonBuilder addVertex(Vector2f vertex) {
            vertices.add(vertex);
            return this;
        }

        public PolygonBuilder removeVertex(int index) {
            vertices.remove(index);
            return this;
        }

        public Vector2f pop() {
            return vertices.remove(vertices.size() - 1);
        }

        public int size() {
            return vertices.size();
        }

        public Vector2f get(int index) {
            return vertices.get(index);
        }

        public Polygon build() {
            float[] verticesArray = new float[vertices.size() * 2];
            for (int i = 0; i < vertices.size(); i++) {
                verticesArray[i * 2] = vertices.get(i).x;
                verticesArray[i * 2 + 1] = vertices.get(i).y;
            }

            return new Polygon(verticesArray);
        }
    }

    protected float[] vertices;

    // x from, y from, x to, y to
    private final Vector4f boundingBox;
    public Polygon(float[] vertices) {
        this.vertices = vertices;
        boundingBox = new Vector4f();

        computeBoundingBox();
    }

    public static PolygonBuilder CreateBuilder() {
        return new PolygonBuilder();
    }

    private void computeBoundingBox(){
        for (int i = 0; i < vertices.length; i += 2){
            if (i == 0) {
                boundingBox.x = vertices[0];
                boundingBox.y = vertices[1];
                boundingBox.z = vertices[0];
                boundingBox.w = vertices[1];
            } else {

                if (vertices[i] < boundingBox.x)
                    boundingBox.x = vertices[i];
                else if (vertices[i] > boundingBox.z)
                    boundingBox.z = vertices[i];

                if (vertices[i + 1] < boundingBox.y)
                    boundingBox.y = vertices[i + 1];
                else if (vertices[i + 1] > boundingBox.w)
                    boundingBox.w = vertices[i + 1];
            }
        }
    }

    public Vector4f getBoundingBox(){
        return new Vector4f(boundingBox);
    }

    public int size() {
        return vertices.length / 2;
    }

    public Vector2f get(int index) {
        if (index > vertices.length / 2)
            return null;

        return new Vector2f(vertices[index * 2], vertices[index * 2 + 1]);
    }

    public boolean isConvex(){
        assert vertices.length % 2 == 0;

        boolean got_negative = false;
        boolean got_positive = false;
        int num_points = vertices.length / 2;
        int B, C;

        for (int A = 0; A < num_points; A++) {
            B = (A + 1) % num_points;
            C = (B + 1) % num_points;

            float cross_product = GeometryMath.CrossProductLength(
                vertices[A * 2], vertices[A * 2 + 1],
                vertices[B * 2], vertices[B * 2 + 1],
                vertices[C * 2], vertices[C * 2 + 1]);

            if (cross_product < 0) {
                got_negative = true;
            } else if (cross_product > 0) {
                got_positive = true;
            }
            if (got_negative && got_positive) return false;
        }

        return true;
    }


    public int[] earClipIndices(){
        int[] indices = new int[(vertices.length / 2 - 2) * 3];
        int fill = 0;

        ArrayList<Vector2f> polygonCopy = new ArrayList<>();
        ArrayList<Integer> polygonIndices = new ArrayList<>();

        for (int i = 0; i < vertices.length; i += 2){
            polygonCopy.add(new Vector2f(vertices[i], vertices[i + 1]));
            polygonIndices.add(i / 2);
        }

        while (polygonCopy.size() >= 3){

            List<Vector2f> earVertices = new ArrayList<>();

            for (int i = 0; i < polygonCopy.size(); ++i){
                if (GeometryMath.isEar(i, polygonCopy)){
                    earVertices.add(polygonCopy.get(i));
                }
            }

            for (Vector2f earVertex : earVertices) {
                int indexCopy = polygonCopy.indexOf(earVertex);
                Vector2f prevVertex = polygonCopy.get(indexCopy > 0 ? indexCopy - 1 : polygonCopy.size() - 1);
                int prevIndexCopy = polygonCopy.indexOf(prevVertex);

                Vector2f nextVertex = polygonCopy.get((indexCopy + 1) % polygonCopy.size());
                int nextIndex = polygonCopy.indexOf(nextVertex);

                indices[fill * 3] = polygonIndices.get(prevIndexCopy);
                indices[fill * 3 + 1] = polygonIndices.get(indexCopy);
                indices[fill * 3 + 2] = polygonIndices.get(nextIndex);

                fill += 1;

                if (fill == indices.length / 3){
                    return indices;
                }

                polygonCopy.remove(indexCopy);
                polygonIndices.remove(indexCopy);
            }

            if (earVertices.size() == 0){
                break;
            }
        }

        return indices;
    }
}
