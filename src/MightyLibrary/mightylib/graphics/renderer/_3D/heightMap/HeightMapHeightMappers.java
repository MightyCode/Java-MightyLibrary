package MightyLibrary.mightylib.graphics.renderer._3D.heightMap;

public abstract class HeightMapHeightMappers {
    private HeightMapHeightMappers(){}

    public static HeightMapRenderer.IHeightMapper GetHeightMapperDefault() {
       return (height, min, max) -> height;
    }

    public static HeightMapRenderer.IHeightMapper GetHeightNormalized() {
        return (height, min, max) -> (height - min) / ((max - min) == 0 ? 1 : max - min);
    }
}
