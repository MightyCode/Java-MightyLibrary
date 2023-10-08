package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.util.math.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;

public class Renderer {
    protected Camera referenceCamera;

    protected Shape shape;

    protected final Vector3f position;
    protected final Vector3f scale;
    protected final Vector3f rotation;
    protected float angle;

    protected Matrix4f model;

    protected boolean display;
    // Textures channels
    protected IRenderTextureBindable[] textures;

    // Colored
    protected HashMap<String, ShaderValue> shaderValues;

    public Renderer(String shaderName, boolean useEbo){
        shape = new Shape(shaderName, useEbo);
        model = new Matrix4f().identity();

        shaderValues = new HashMap<>();

        display = true;

        // Display mode
        textures = new IRenderTextureBindable[31];

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();

        referenceCamera = null;

        if (shape.getShader().getLink(ShaderManager.GENERIC_PROJECTION_FIELD_NAME) != -1){
            if (shape.getIn2D())
                shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera2D().getProjection());
            else
                shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera3D().getView());
        }

        if (shape.getShader().getLink(ShaderManager.GENERIC_VIEW_FIELD_NAME) != -1){
            if (shape.getIn2D())
                shaderValues.put(ShaderManager.GENERIC_VIEW_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera2D().getView());
            else
                shaderValues.put(ShaderManager.GENERIC_VIEW_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera3D().getProjection());
        }


        applyModel();

        if (shape.getShader().getLink(ShaderManager.GENERIC_MODEL_FIELD_NAME) != -1) {
            shaderValues.put(ShaderManager.GENERIC_MODEL_FIELD_NAME,
                    new ShaderValue(ShaderManager.GENERIC_MODEL_FIELD_NAME, Matrix4f.class, model));
        }
    }


    public void display(){
        if(display) {
            updateShader();
            draw();
        }
    }

    public void updateShader(){
        for (int i = 0; i < textures.length; ++i){
            if (textures[i] != null)
                textures[i].bindRenderTexture(i);
        }

        for (ShaderValue value : shaderValues.values()){
            sentToShader(value);
        }
    }

    public Renderer addShaderValue(String name, Class<?> type, Object object){
        shaderValues.put(name, new ShaderValue(name, type, object));

        return this;
    }

    public Renderer updateShaderValue(String name, Object object){
        shaderValues.get(name).setObject(object);

        return this;
    }

    public Renderer copyShaderValuesTo(Renderer other, boolean eraseExisting){
        for (ShaderValue value : shaderValues.values()) {
            if (other.shaderValues.containsKey(value.getName()) && !eraseExisting)
                continue;

            other.shaderValues.put(value.getName(), value);
        }

        return other;
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

    public void setMainTextureChannel(String name) {
        setMainTextureChannel(Resources.getInstance().getResource(Texture.class, name));
    }

    public void setMainTextureChannel(String name, String nameOfUniform) {
        shape.getShader().sendValueToShader(
                new ShaderValue(nameOfUniform, Integer.class, 0)
        );

        setMainTextureChannel(Resources.getInstance().getResource(Texture.class, name));
    }

    public void setMainTextureChannel(IRenderTextureBindable texture){
        shape.enableVbo(1);

        textures[0] = texture;
    }

    public void addTextureChannel(String name, String nameOfUniform, int channelNumber) {
        addTextureChannel(
                Resources.getInstance().getResource(Texture.class, name),
                nameOfUniform, channelNumber);
    }

    public void addTextureChannel(IRenderTextureBindable texture, String nameOfUniform, int channelNumber) {
        shape.getShader().sendValueToShader(
                new ShaderValue(nameOfUniform, Integer.class, channelNumber)
        );

        shape.enableVbo(1);

        textures[channelNumber] = texture;
    }


    public ShaderValue setColorMode(Color4f color) {
        ShaderValue value = new ShaderValue(ShaderManager.GENERIC_COLOR_FIELD_NAME, Color4f.class, color);
        shaderValues.put(ShaderManager.GENERIC_COLOR_FIELD_NAME, value);

        return value;
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
        if (camera == null)
            return;

        referenceCamera = camera;
        shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, camera.getProjection());
        shaderValues.put(ShaderManager.GENERIC_VIEW_FIELD_NAME, camera.getView());
    }

    public void unload(){
        shape.unload();
    }
}
