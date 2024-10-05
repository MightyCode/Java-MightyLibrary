package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.mightylib.utils.math.geometry.Polygon;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class PolygonRenderer extends Shape2DRenderer {
    private final Polygon polygon;

    public PolygonRenderer (String shaderName, Polygon polygon){
        super(shaderName, true);
        this.polygon = polygon;
        texturePosition = RectangularFace.BasicTexturePosition();
    }

    @Override
    public boolean load(int remainingMilliseconds) {
        super.load(remainingMilliseconds);
        setReferenceDirection(EDirection.None);

        return true;
    }

    // position of point comparing to the bounding box
    @Override
    protected float[] calculateTexturePosition(){
        float[] position = new float[polygon.size() * 2];
        Vector4f boundingBox = polygon.getBoundingBox();

        Vector2f size = new Vector2f(boundingBox.z - boundingBox.x, boundingBox.w - boundingBox.y);
        Vector2f temp;

        for (int i = 0; i < polygon.size(); ++i){
            temp = polygon.get(i);
            position[i * 2] = (temp.x - boundingBox.x) / size.x;
            position[i * 2 + 1] = (temp.y - boundingBox.y) / size.y;
        }

        return position;
    }

    public int numberVertex(){
        return polygon.size();
    }

    public void setReferenceVertex(int index){
        Vector4f boundingBox = polygon.getBoundingBox();
        Vector2f size = new Vector2f(boundingBox.z - boundingBox.x, boundingBox.w - boundingBox.y);

        Vector2f temp = polygon.get(index);
        Vector2f position = new Vector2f(
                (temp.x - boundingBox.x) / size.x,
                (temp.y - boundingBox.y) / size.y);

        setReferencePoint(position);
    }

    @Override
    protected int[] getIndices() {
        if (polygon.isConvex()) {
            int[] indices = new int[(polygon.size() - 2) * 3];

            for (int i = 0; i < (polygon.size() - 2); ++i) {
                indices[i * 3] = 0;
                indices[i * 3 + 1] = i + 1;
                indices[i * 3 + 2] = i + 2;
            }

            return indices;
        }

        return polygon.earClipIndices();
    }

    @Override
    protected float[] calculatePosition() {
        float[] position = calculateTexturePosition();

        Vector2f reference = getReferencePoint();

        if (reference.x != 0 || reference.y != 0) {
            for (int i = 0; i < position.length; i += 2) {
                position[i] -= reference.x;
                position[i + 1] -= reference.y;
            }
        }

        return position;
    }
}
