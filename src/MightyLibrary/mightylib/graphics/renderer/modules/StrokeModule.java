package MightyLibrary.mightylib.graphics.renderer.modules;

import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import org.joml.Vector4f;

public class StrokeModule {
    private final Renderer renderer;
    public StrokeModule(Renderer renderer) {
        this.renderer = renderer;

        if (!renderer.hasShaderValue("strokeEnabled"))
            renderer.addShaderValue("strokeEnabled", Boolean.class, false);


        renderer.addShaderValue("strokeColor", Vector4f.class, ColorList.Black());

        renderer.addShaderValue("strokeSize", Float.class, 0.1f);
    }

    public StrokeModule setEnabled(boolean enabled) {
        renderer.updateShaderValue("strokeEnabled", enabled);

        return this;
    }

    public boolean geEnabled() {
        return renderer.getShaderValue("strokeEnabled", Boolean.class);
    }

    public StrokeModule setColor(Vector4f color) {
        renderer.updateShaderValue("strokeColor", color);

        return this;
    }

    public Vector4f getColor() {
        return renderer.getShaderValue("strokeColor", Vector4f.class);
    }

    public StrokeModule setSizePercent(float size) {
        renderer.updateShaderValue("strokeSize", size);

        return this;
    }

    public float getSizePercent() {
        return renderer.getShaderValue("strokeSizePercent", Float.class);
    }
}
