package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.renderer.Shape;

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
