package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

// Unit of the width of a stroke or a line
public enum EStrokeUnit {
    // World units (the default): the width follows the zoom of the camera
    WORLD(0f),
    // Pixels of the screen: the same width at every zoom
    SCREEN_PIXELS(1f);

    private final float shaderValue;

    EStrokeUnit(float shaderValue) {
        this.shaderValue = shaderValue;
    }

    // Sent to the shaders
    public float shaderValue() {
        return shaderValue;
    }
}
