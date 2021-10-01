package MightyLibrary.mightylib.graphics.shape;

public abstract class PresetsShape {
    public Shape getCubeVerticesWithoutEbo(float size){
        float[] vertex = new float[]{
                -size, -size, -size,
                size, -size, -size,
                size,  size, -size,
                size,  size, -size,
                -size,  size, -size,
                -size, -size, -size,

                -size, -size,  size,
                size, -size,  size,
                size,  size,  size,
                size,  size,  size,
                -size,  size,  size,
                -size, -size,  size,

                -size,  size,  size,
                -size,  size, -size,
                -size, -size, -size,
                -size, -size, -size,
                -size, -size,  size,
                -size,  size,  size,

                size,  size,  size,
                size,  size, -size,
                size, -size, -size,
                size, -size, -size,
                size, -size,  size,
                size,  size,  size,

                -size, -size, -size,
                size, -size, -size,
                size, -size,  size,
                size, -size,  size,
                -size, -size,  size,
                -size, -size, -size,

                -size,  size, -size,
                size,  size, -size,
                size,  size,  size,
                size,  size,  size,
                -size,  size,  size,
                -size,  size, -size,
        };
        return null;
    }
}
