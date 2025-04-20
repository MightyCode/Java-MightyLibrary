package MightyLibrary.mightylib.graphics.renderer._2D.shape;

import MightyLibrary.mightylib.graphics.renderer.RectangularFace;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

public class AdvancedRectangle extends RectangleRenderer {
    public static class ShaderRectangleProperty {
        public String Size = "rectangle.size";
        public String Center = "rectangle.center";
        public String Rotation = "rectangle.rotation";
        public String CornerRoundSize = "rectangle.cornerRoundSize";

        public String StrokeEnabled = "strokeEnabled";
        public String StrokeColor = "strokeColor";
        public String StrokeSize = "strokeSize";
    }

    public ShaderRectangleProperty ShaderProperty;

    public AdvancedRectangle(String shaderName, ShaderRectangleProperty property) {
        super(shaderName);

        ShaderProperty = property;
        addShaderValue(ShaderProperty.Center, Vector2f.class, new Vector2f(0));
        addShaderValue(ShaderProperty.Size, Vector2f.class, new Vector2f(10));
        addShaderValue(ShaderProperty.CornerRoundSize, Float.class, 5f);
        addShaderValue(ShaderProperty.Rotation, Float.class, 0f);

       /* addShaderValue(ShaderProperty.StrokeEnabled, Boolean.class, false);
        addShaderValue(ShaderProperty.StrokeColor, Vector4f.class, ColorList.Black());
        addShaderValue(ShaderProperty.StrokeSize, Float.class, 0.1f);*/

        texturePosition = RectangularFace.BasicTexturePosition();
    }

    public AdvancedRectangle(String shaderName) {
        this(shaderName, new ShaderRectangleProperty());
    }

    public AdvancedRectangle(){
        this("advancedRectangle2D", new ShaderRectangleProperty());
    }

    @Override
    public boolean load(int m){
        boolean state = super.load(m);
        if (!state) return false;

        setReferenceDirection(EDirection.None);

        return true;
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
    public AdvancedRectangle setPosition(Vector2f position){
        super.setPosition(position);
        updateValues();
        return this;
    }

    public AdvancedRectangle setReferenceDirection(EDirection reference) {
        super.setReferenceDirection(reference);
        updateValues();
        return this;
    }

    private void updateValues() {
        updateShaderValue(ShaderProperty.Size, new Vector2f(scale.x, scale.y));

        Vector2f pos = new Vector2f(this.position.x, this.position.y);

        switch (getReferenceDirection()){
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

        switch (getReferenceDirection()){
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
