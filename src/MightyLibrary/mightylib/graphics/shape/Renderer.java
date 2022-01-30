package MightyLibrary.mightylib.graphics.shape;

import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.util.math.EShapeType;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.ColorList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class Renderer{
    public static final int NOTHING = 0;
    public static final int COLOR = 1;
    public static final int TEXTURE = 2;

    public Vector3f modelV;
    protected Matrix4f model;
    protected boolean display;
    protected FloatBuffer translateF;
    protected Shape shape;
    protected ShaderManager shadManager;

    protected int displayMode;

    // Textured
    protected Texture texture;

    // Colored
    public Color4f color;

    private EShapeType type;

    public Renderer(String shaderName, boolean useEbo, boolean in2D){
        shadManager = ShaderManager.getInstance();

        shape = new Shape(shaderName, useEbo, in2D);
        model = new Matrix4f().identity();
        translateF = BufferUtils.createFloatBuffer(16);
        display = true;

        // Display mode
        texture = null;
        displayMode = NOTHING;
        color = ColorList.BLACK;
    }


    public void display(){
        if(display){
            updateShader();
            draw();
        }
    }


    public void updateShader(){
        // Apply model matrix
        if (!shape.getIn2D()){
            shadManager.getShader(shape.getShaderId()).glUniform("model", translateF);
        }

        if (displayMode == COLOR) {
            shadManager.getShader(
                    shape.getShaderId()).glUniform("color", color.getR(), color.getG(), color.getB(), color.getA()
            );
        } else if (displayMode == TEXTURE){
            texture.bind(0);
        }
    }


    public void draw(){
        shape.display();
    }


    public void setPosition(Vector3f position){
        this.modelV = position;
        this.model.identity();
        this.model.translate(this.modelV);
        this.model.get(translateF);
    }


    public void setDisplayState(boolean state) {
        display = state;
    }


    public void invertDisplayState() {
        display = !display;
    }


    public void setTexture(String name){

        setTexture(Resources.getInstance().getResource(Texture.class, name));

    }

    public void setTexture(Texture texture){
        displayMode = TEXTURE;
        this.texture = texture;
        shape.enableVbo(1);
    }


    public void setColor(Color4f color){
        displayMode = COLOR;
        this.color = color.copy();
        shape.disableVbo(1);
    }


    public Shape getShape(){
        return shape;
    }


    public void setShape(Shape shape){
        this.shape = shape;
    }


    public void unload(){
        shape.unload();
        translateF.clear();
    }
}
