package MightyLibrary.mightylib.graphics.renderer._3D.heightMap;

public abstract class HeightMapColorFunctions {
    private HeightMapColorFunctions(){}

    public static HeightMapRenderer.IColorMapper HeightToColorDefault() {
        // Simple grayscale
        return (height, colorToFill) -> colorToFill.set(height, height, height, 1);
    }

    public static HeightMapRenderer.IColorMapper HeightToColorSimple() {
        return (height, colorToFill) -> colorToFill.set(height, 0f, 1f - height, 1);
    }

    public static HeightMapRenderer.IColorMapper HeightToColorMountain(float waterThreshold, float greenThreshold, float snowThreshold) {
        return (height, colorToFill) -> {
            height = Math.max(0f, Math.min(1f, height)); // Clamp between 0 and 1

            if (height <= waterThreshold) {
                colorToFill.x = 0;
                colorToFill.y = 0.3f;
                colorToFill.z = 1;
            } else if (height <= greenThreshold) {
                float t = height / greenThreshold; // Normalize to 0–1

                colorToFill.x = 0.36f + 0.24f * t;
                colorToFill.y = 0.25f + 0.15f * t;
                colorToFill.z = 0.20f + 0.1f * t;
            } else if (height <= snowThreshold) {
                float t = (height - greenThreshold) / (1.0f - greenThreshold);

                colorToFill.x = 0.3f - 0.2f * t;
                colorToFill.y = 0.6f + 0.3f * t;
                colorToFill.z = 0.3f - 0.1f * t;
            } else {
                colorToFill.x = 1;
                colorToFill.y = 1;
                colorToFill.z = 1;
            }
        };
    }
}
