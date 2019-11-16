package MightyLibrary.mightylib.render.shape;

import MightyLibrary.mightylib.main.ManagerContainer;
import MightyLibrary.mightylib.render.shader.ShaderManager;
import MightyLibrary.mightylib.render.texture.TextureManager;
import MightyLibrary.mightylib.util.EShapeType;
import MightyLibrary.mightylib.util.Id;
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
    protected TextureManager textureManager;

    protected int displaydMode;

    // Textured
    protected Id textureId;

    // Colored
    public Color4f color;

    private EShapeType type;

    public Renderer(String shaderName, boolean useEbo, boolean in2D){
        shadManager = ManagerContainer.getInstance().shadManager;
        textureManager = ManagerContainer.getInstance().textureManager;

        shape = new Shape(shaderName, useEbo, in2D);
        model = new Matrix4f().identity();
        translateF = BufferUtils.createFloatBuffer(16);
        display = true;

        // Display mode
        textureId = new Id(0);
        displaydMode = NOTHING;
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
        if (!shape.getIn2D()) shadManager.getShader(shape.getShaderId()).glUniform("model", translateF);

        if (displaydMode == COLOR)          shadManager.getShader(shape.getShaderId()).glUniform("color", color.getR(), color.getG(), color.getB(), color.getA());
        else if (displaydMode == TEXTURE)   textureManager.bind(textureId, 0);
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


    public void setDisplayState(boolean state) {display = state;}
    public void invertDisplayState() {display = !display;}


    public void setTexture(String texture){
        displaydMode = TEXTURE;
        textureId = textureManager.getIdShaderFromString(texture);
        shape.enableVbo(1);
    }

    public void setTexture(Id texture){
        displaydMode = TEXTURE;
        textureId = texture;
        shape.enableVbo(1);
    }

    public void setColor(Color4f color){
        displaydMode = COLOR;
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
