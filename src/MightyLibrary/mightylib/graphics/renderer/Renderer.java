package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.ColorList;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Renderer {
    public static final int NOTHING = 0;
    public static final int COLOR = 1;
    public static final int TEXTURE = 2;

    protected final Vector3f position;
    protected final Vector3f scale;
    protected final Vector3f rotation;
    protected float angle;
    protected boolean shouldSendProjection, shouldSendView, shouldSendModel, shouldSendColor;

    protected Matrix4f model;
    protected ShaderValue modelShaderValue;

    protected boolean display;

    protected Shape shape;

    protected int displayMode;

    // Textured
    protected IRenderTextureBindable texture;

    // Colored
    public Color4f color;
    protected ShaderValue colorShaderValue;
    protected Camera referenceCamera;

    public Renderer(String shaderName, boolean useEbo){
        shape = new Shape(shaderName, useEbo);
        model = new Matrix4f().identity();

        display = true;

        // Display mode
        texture = null;
        displayMode = NOTHING;
        color = ColorList.Black();

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();
        shouldSendProjection = shape.getShader().getLink(ShaderManager.GENERIC_PROJECTION_FIELD_NAME) != -1;
        shouldSendView = shape.getShader().getLink(ShaderManager.GENERIC_VIEW_FIELD_NAME) != -1;
        shouldSendModel = shape.getShader().getLink(ShaderManager.GENERIC_MODEL_FIELD_NAME) != -1;

        shouldSendColor = shape.getShader().getLink(ShaderManager.GENERIC_COLOR_FIELD_NAME) != -1;

        if (shouldSendModel)
            modelShaderValue = new ShaderValue(ShaderManager.GENERIC_MODEL_FIELD_NAME, Matrix4f.class, model);

        if (shouldSendColor)
            colorShaderValue = new ShaderValue(ShaderManager.GENERIC_COLOR_FIELD_NAME, Color4f.class, color);

        referenceCamera = null;

        applyModel();
    }


    public void display(){
        if(display){
            updateShader();
            draw();
        }
    }

    public void updateShader(){
        if (referenceCamera == null) {
            ShaderManager.getInstance().sendCameraToShader(shape.getShader(), shouldSendProjection, shouldSendView);
        } else {
            if (shouldSendProjection)
                getShape().getShader().sendValueToShader(referenceCamera.getProjection());

            if (shouldSendView)
                getShape().getShader().sendValueToShader(referenceCamera.getView());
        }

        // Apply model matrix
        if (shouldSendModel){
            modelShaderValue.setObject(model);
            sentToShader(modelShaderValue);
        }

        if (displayMode == COLOR && shouldSendColor) {
            colorShaderValue.setObject(color);
            sentToShader(colorShaderValue);
        } else if (displayMode == TEXTURE){
            texture.bindRenderTexture(0);
        }
    }


    protected void sentToShader(ShaderValue value){
        shape.getShader().sendValueToShader(value);
    }

    public void draw(){
        shape.display();
    }


    public void setPosition(Vector3f position){
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = position.z;

        applyModel();
    }

    public void setX(float x){
        this.position.x = x;

        applyModel();
    }

    public void setY(float y){
        this.position.y = y;

        applyModel();
    }

    public void setZ(float z){
        this.position.z = z;

        applyModel();
    }

    public void setScale(Vector3f scale){
        this.scale.x = scale.x;
        this.scale.y = scale.y;
        this.scale.z = scale.z;

        applyModel();
    }

    public void setRotation(float angle, Vector3f rotation){
        this.rotation.x = rotation.x;
        this.rotation.y = rotation.y;
        this.rotation.z = rotation.z;

        this.angle = angle;

        applyModel();
    }


    public void setRotation(Renderer other){
        this.rotation.x = other.rotation.x;
        this.rotation.y = other.rotation.y;
        this.rotation.z = other.rotation.z;

        this.angle = other.angle;

        applyModel();
    }

    public void applyModel(){
        this.model.identity();

        this.model.translate(this.position);
        this.model.rotate(angle, this.rotation);
        this.model.scale(this.scale);
    }

    public void hide(boolean state) {
        display = state;
    }


    public void invertDisplayState() {
        display = !display;
    }


    public void switchToTextureMode(String name) {
        switchToTextureMode(Resources.getInstance().getResource(Texture.class, name));
    }

    public void switchToTextureMode(IRenderTextureBindable texture){
        displayMode = TEXTURE;
        shape.enableVbo(1);

        this.texture = texture;
    }


    public void switchToColorMode(Color4f color){
        displayMode = COLOR;
        this.color = color.copy();
    }


    public Shape getShape(){
        return shape;
    }

    public void setShape(Shape shape){
        this.shape = shape;
    }

    public Vector3f position(){
        return position;
    }

    public Vector3f scale() { return scale; }

    public Vector3f rotationCoef() { return rotation; }

    public float getRotationAngle() { return angle; }

    public void setReferenceCamera(Camera camera){
        referenceCamera = camera;
    }

    public void unload(){
        shape.unload();
    }
}
