package MightyLibrary.mightylib.graphics.renderer._2D;

public interface IRenderTextureBindable {
    void bindRenderTexture(int position);

    int getWidth();
    int getHeight();

    int getRenderTextureId();

    void unload();
}
