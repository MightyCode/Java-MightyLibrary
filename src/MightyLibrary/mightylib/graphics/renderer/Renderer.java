package MightyLibrary.mightylib.graphics.renderer;

import MightyLibrary.mightylib.graphics.GLElement;
import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import MightyLibrary.mightylib.main.utils.IDisplayable;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.scenes.camera.Camera;
import MightyLibrary.mightylib.utils.math.UUID;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;

public class Renderer extends UUID implements IDisplayable {
    protected GLResources glResources = GLResources.getInstance();
    protected Resources resources = Resources.getInstance();

    protected Camera referenceCamera;

    protected Shape shape;

    protected final Vector3f position;
    protected final Vector3f scale;
    protected final Vector3f rotation;
    protected float angle;

    protected Matrix4f model;

    protected boolean display;

    // Textures channels
    protected IRenderTextureBindable mainTexture;
    protected IRenderTextureBindable[] textures;

    // Colored
    protected HashMap<String, ShaderValue> shaderValues;

    public Renderer(String shaderName, boolean useEbo) {
        shape = new Shape(shaderName, useEbo);
        model = new Matrix4f().identity();

        shaderValues = new HashMap<>();

        display = true;

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();

        mainTexture = null;

        referenceCamera = null;

        if (shape.getShader().getLink(ShaderManager.GENERIC_PROJECTION_FIELD_NAME) != -1){
            if (shape.is2DPurpose())
                shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera2D().getProjection());
            else
                shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera3D().getView());
        }

        if (shape.getShader().getLink(ShaderManager.GENERIC_VIEW_FIELD_NAME) != -1){
            if (shape.is2DPurpose()) {
                shaderValues.put(ShaderManager.GENERIC_VIEW_FIELD_NAME,
                        ShaderManager.getInstance().getMainCamera2D().getView());
            }
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
        if (mainTexture != null)
            mainTexture.bindRenderTexture(0);

        if (textures != null) {
            for (int i = 0; i < textures.length; ++i) {
                if (textures[i] != null)
                    textures[i].bindRenderTexture(i + 1);
            }
        }

        for (ShaderValue value : shaderValues.values()){
            sentToShader(value);
        }
    }

    public Renderer addShaderValue(String name, Class<?> type, Object object){
        shaderValues.put(name, new ShaderValue(name, type, object));

        return this;
    }

    public Renderer updateShaderValue(String name, Object object) {
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

    protected void sentToShader(ShaderValue value) {
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

    public void setRotationAngle(float angle){
        this.angle = angle;

        applyModel();
    }

    public void setRotationZ(float angle){
        this.angle = angle;

        this.rotation.x = 0;
        this.rotation.y = 0;
        this.rotation.z = 1;

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
        setMainTextureChannel(glResources.addElementOrReturnIfPresent(Texture.class, new Texture(name), name));
    }

    public void setMainTextureChannel(String name, String nameOfUniform) {
        shape.getShader().sendValueToShader(
                new ShaderValue(nameOfUniform, Integer.class, 0)
        );

        setMainTextureChannel(name);
    }

    public void setMainTextureChannel(IRenderTextureBindable texture) {
        if (mainTexture instanceof GLElement) {
            if (texture instanceof GLElement) {
                if (mainTexture == texture) {
                    glResources.releaseResource((GLElement) texture);
                    return;
                }
            }

            glResources.releaseResource((GLElement) mainTexture);
        }

        shape.enableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);
        mainTexture = texture;
    }

    public void addTextureChannel(String name, String nameOfUniform, int channelNumber) {
        if (textures == null || channelNumber <= 0)
            return;

        if (channelNumber > textures.length)
            return;

        addTextureChannel(
                GLResources.getInstance().addElementOrReturnIfPresent(Texture.class, new Texture(name), name),
                nameOfUniform, channelNumber);
    }

    public void addTextureChannel(IRenderTextureBindable texture, String nameOfUniform, int channelNumber) {
        shape.getShader().sendValueToShader(
                new ShaderValue(nameOfUniform, Integer.class, channelNumber)
        );

        shape.enableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);

        if (textures == null)
            textures = new IRenderTextureBindable[30];
        else {
            if (textures[channelNumber - 1] instanceof GLElement)
                glResources.releaseResource((GLElement) textures[channelNumber - 1]);
        }

        textures[channelNumber - 1] = texture;
    }


    public ShaderValue setColorMode(Color4f color, boolean disableTexture) {
        ShaderValue value = new ShaderValue(ShaderManager.GENERIC_COLOR_FIELD_NAME, Color4f.class, color);
        shaderValues.put(ShaderManager.GENERIC_COLOR_FIELD_NAME, value);

        if (disableTexture)
            shape.disableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);

        return value;
    }


    public ShaderValue setColorMode(Color4f color) {
        return setColorMode(color, true);
    }

    public void setNoColorNoTextureMode() {
        shape.disableVbo(Shape.COMMON_TEXTURE_POSITION_CHANNEL);

        shaderValues.remove(ShaderManager.GENERIC_COLOR_FIELD_NAME);
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

    public Vector3f rotationAxis() { return rotation; }

    public float getRotationAngle() { return angle; }

    public Matrix4f getModel(){
        return new Matrix4f(model);
    }

    public void setReferenceCamera(Camera camera){
        if (camera == null)
            return;

        referenceCamera = camera;
        shaderValues.put(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, camera.getProjection());
        shaderValues.put(ShaderManager.GENERIC_VIEW_FIELD_NAME, camera.getView());
    }

    public void unload() {
        // TODO delete that
        unload(0);
    }
    public void unload(int remainingMilliseconds) {
        shape.unload();

        if (mainTexture instanceof GLElement)
            glResources.releaseResource((GLElement) mainTexture);

        if (textures != null) {
            for (IRenderTextureBindable texture : textures) {
                if (texture instanceof GLElement)
                    glResources.releaseResource((GLElement) texture);
            }
        }
    }
}
