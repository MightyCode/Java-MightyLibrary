package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.main.utils.IDisplayable;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.utils.math.UUID;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Renderer extends UUID implements IDisplayable {
    protected GLResources glResources = Views.engine().glResources();
    protected Resources resources = Views.engine().resources();

    protected Camera referenceCamera;

    protected Shape shape;

    protected final Vector3f position;
    protected final Vector3f scale;
    protected final Vector3f rotation;
    protected float angle;

    protected Matrix4f model;

    private boolean visible;

    // Textures channels
    protected IRenderTextureBindable mainTexture;
    protected IRenderTextureBindable[] textures;

    /*
     * Every value this renderer sends, sent again at each draw (filtered by the program, which skips unchanged ones):
     * the program can change at any time (fallback while loading, hot reload) and has none of them at first.
     */
    protected HashMap<String, ShaderValue> shaderValues;
    // Keys of the values sent only when the current program has their uniform, never reported missing (generic ones)
    protected HashSet<String> optionalValues;

    public Renderer(String shaderName, boolean useEbo) {
        shape = new Shape(shaderName, useEbo);
        model = new Matrix4f().identity();

        shaderValues = new HashMap<>();
        optionalValues = new HashSet<>();

        visible = true;

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();

        mainTexture = null;

        referenceCamera = null;

        // Generic matrices: sent at each draw when the current program uses them
        if (shape.is2DPurpose()) {
            putOptionalValue(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, Views.current().camera2D().getProjection());
            putOptionalValue(ShaderManager.GENERIC_VIEW_FIELD_NAME, Views.current().camera2D().getView());
        } else {
            putOptionalValue(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, Views.current().camera3D().getProjection());
            putOptionalValue(ShaderManager.GENERIC_VIEW_FIELD_NAME, Views.current().camera3D().getView());
        }

        applyModel();

        putOptionalValue(ShaderManager.GENERIC_MODEL_FIELD_NAME,
                new ShaderValue(ShaderManager.GENERIC_MODEL_FIELD_NAME, Matrix4f.class, model));
    }

    // A value sent only when the current program has its uniform (not reported missing otherwise)
    protected void putOptionalValue(String key, ShaderValue value) {
        shaderValues.put(key, value);
        optionalValues.add(key);
    }

    // True when the current program of the shape has this uniform
    protected boolean hasUniform(String name) {
        return shape.getShader().getLink(name) != -1;
    }

    // Send the values of this renderer to the current program (the unchanged ones are skipped by it)
    protected void sendShaderValues() {
        for (Map.Entry<String, ShaderValue> entry : shaderValues.entrySet()) {
            ShaderValue value = entry.getValue();
            if (optionalValues.contains(entry.getKey()) && !hasUniform(value.getName()))
                continue;

            sentToShader(value);
        }
    }

    public void display(){
        if(visible) {
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

        sendShaderValues();
    }

    public Renderer addShaderValue(String name, Class<?> type, Object object){
        shaderValues.put(name, new ShaderValue(name, type, object));
        optionalValues.remove(name);

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
            if (optionalValues.contains(value.getName()))
                other.optionalValues.add(value.getName());
            else
                other.optionalValues.remove(value.getName());
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

    public Renderer setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public boolean isVisible() {
        return visible;
    }

    public Renderer toggleVisible() {
        visible = !visible;
        return this;
    }

    public void setMainTextureChannel(String name) {
        setMainTextureChannel(glResources.addElementOrReturnIfPresent(Texture.class, new Texture(name), name));
    }

    public void setMainTextureChannel(String name, String nameOfUniform) {
        addShaderValue(nameOfUniform, Integer.class, 0);

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
                glResources.addElementOrReturnIfPresent(Texture.class, new Texture(resources, name), name),
                nameOfUniform, channelNumber);
    }

    public void addTextureChannel(IRenderTextureBindable texture, String nameOfUniform, int channelNumber) {
        addShaderValue(nameOfUniform, Integer.class, channelNumber);

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
        putOptionalValue(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, camera.getProjection());
        putOptionalValue(ShaderManager.GENERIC_VIEW_FIELD_NAME, camera.getView());
    }

    public void unload() {
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
