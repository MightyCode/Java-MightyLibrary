package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.utils.math.EDirection;
import org.joml.Vector2f;

public class EllipseRenderer extends RectangleRenderer {
    public static class ShaderEllipseProperty {
        public String Center = "ellipse.center";
        public String Radius = "ellipse.radius";
        public String Rotation = "ellipse.rotation";
    }

    public ShaderEllipseProperty ShaderProperty;

    public EllipseRenderer(String shaderName, ShaderEllipseProperty property) {
        super(shaderName);

        ShaderProperty = property;

        addShaderValue(ShaderProperty.Center, Vector2f.class, new Vector2f());
        addShaderValue(ShaderProperty.Radius, Vector2f.class, new Vector2f());
        addShaderValue(ShaderProperty.Rotation, Float.class, 0f);

        setReference(EDirection.None);
    }

    public EllipseRenderer(String shaderName) {
        this(shaderName, new ShaderEllipseProperty());
    }

    public EllipseRenderer(){
        this("colorEllipse2D", new ShaderEllipseProperty());
    }

    @Override
    public void setRotationZ(float rotation){
        super.setRotationZ(rotation);
        updateShaderValue(ShaderProperty.Rotation, rotation);
    }

    @Override
    public Shape2DRenderer setSizePix(float width, float height){
        super.setSizePix(width, height);
        updateValues();

        return this;
    }

    @Override
    public Shape2DRenderer setPosition(Vector2f position){
        super.setPosition(position);
        updateValues();
        return this;
    }

    @Override
    public Shape2DRenderer setReference(EDirection reference){
        super.setReference(reference);
        updateValues();
        return this;
    }

    private void updateValues(){
        updateShaderValue(ShaderProperty.Radius, new Vector2f(scale.x / 2, scale.y / 2));

        Vector2f pos = new Vector2f(this.position.x, this.position.y);

        switch (reference){
            case Left:
            case LeftUp:
            case LeftDown:
                pos.x += scale.x / 2;
                break;
            case Right:
            case RightUp:
            case RightDown:
                pos.x -= scale.x / 2;
        }

        switch (reference){
            case Up:
            case LeftUp:
            case RightUp:
                pos.y += scale.y / 2;
                break;
            case Down:
            case LeftDown:
            case RightDown:
                pos.y -= scale.y / 2;
        }

        updateShaderValue(ShaderProperty.Center, pos);
    }
}
