package io.github.mightycode.mightylib.graphics.renderer._2D.shape;

import io.github.mightycode.mightylib.graphics.renderer.RectangularFace;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

public class EllipseRenderer extends RectangleRenderer {
    public static class ShaderEllipseProperty {
        public String Center = "ellipse.center";
        public String Radius = "ellipse.radius";
        public String Rotation = "ellipse.rotation";
    }

    // Names of the uniforms, copied at the construction: the values are added and updated under the same names
    private final ShaderEllipseProperty shaderProperty;

    public EllipseRenderer(String shaderName, ShaderEllipseProperty property) {
        super(shaderName);

        shaderProperty = new ShaderEllipseProperty();
        shaderProperty.Center = property.Center;
        shaderProperty.Radius = property.Radius;
        shaderProperty.Rotation = property.Rotation;
        addShaderValue(shaderProperty.Center, Vector2f.class, new Vector2f());
        addShaderValue(shaderProperty.Radius, Vector2f.class, new Vector2f());
        addShaderValue(shaderProperty.Rotation, Float.class, 0f);

        setTexturePosition(RectangularFace.BasicTexturePosition());
        setReferenceDirection(EDirection.None);
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
        updateShaderValue(shaderProperty.Rotation, rotation);
    }

    @Override
    public Shape2DRenderer setSizePix(float width, float height){
        super.setSizePix(width, height);
        updateValues();

        return this;
    }

    @Override
    public EllipseRenderer setPosition(Vector2f position){
        super.setPosition(position);
        updateValues();
        return this;
    }

    public EllipseRenderer setReferenceDirection(EDirection reference) {
        super.setReferenceDirection(reference);
        updateValues();
        return this;
    }

    private void updateValues() {
        updateShaderValue(shaderProperty.Radius, new Vector2f(scale().x / 2, scale().y / 2));

        Vector2f pos = new Vector2f(position().x, position().y);

        switch (getReferenceDirection()){
            case Left:
            case LeftUp:
            case LeftDown:
                pos.x += scale().x / 2;
                break;
            case Right:
            case RightUp:
            case RightDown:
                pos.x -= scale().x / 2;
        }

        switch (getReferenceDirection()){
            case Up:
            case LeftUp:
            case RightUp:
                pos.y += scale().y / 2;
                break;
            case Down:
            case LeftDown:
            case RightDown:
                pos.y -= scale().y / 2;
        }

        updateShaderValue(shaderProperty.Center, pos);
    }
}
