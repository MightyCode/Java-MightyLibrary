package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.main.utils.IDisplayable;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderManager;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.utils.math.UUID;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Base of every renderer: draws something with a shader program. Holds the program (its fallback while it loads
 * or when it failed), the values sent to it, the transform (model matrix), the reference camera, the visibility and
 * the OpenGL state of the draw (RenderState). It has no geometry: MeshRenderer draws one Mesh, ModelRenderer a
 * model. The program, the values and the state are grouped apart from the transform (a future shareable Material).
 *
 * Hooks of a subclass: draw (the geometry), updateShader (bind and send before draw), applyRenderState, display
 * (a condition, then super), unload (release, then super), and the setters whose change it must follow
 * (setPosition, setRotationZ, position, setColorMode, setNoColorNoTextureMode, setFaceCulling: call super).
 * display() calls them in this order: applyRenderState, program in use, updateShader, draw.
 * The state of the renderer is reached through methods only.
 */
public abstract class Renderer extends UUID implements IDisplayable {
    // Belong to the engine of the view that creates the renderer
    private final GLResources glResources = Views.engine().glResources();
    private final Resources resources = Views.engine().resources();

    private Camera referenceCamera;

    private final Vector3f position;
    private final Vector3f scale;
    private final Vector3f rotation;
    private float angle;

    private final Matrix4f model;

    private boolean visible;

    // Program of the shader, held (urgent reference) until unload
    private final Shader shader;
    // Drawn with while shader is not linked, cached at the first use (see getShader)
    private Shader fallbackShader;
    private boolean shaderReleased;
    // Depth and culling of each draw, by default from the tag of the shader (2D: no depth)
    private final RenderState renderState;

    /*
     * Every value this renderer sends, sent again at each draw (filtered by the program, which skips unchanged ones):
     * the program can change at any time (fallback while loading, hot reload) and has none of them at first.
     */
    private final HashMap<String, ShaderValue> shaderValues;
    // Keys of the values sent only when the current program has their uniform, never reported missing (generic ones)
    private final HashSet<String> optionalValues;

    /*
     * Takes the program of the shader (the fallback one while it loads or when it failed); the tag of the shader
     * chooses the default camera (2D or 3D) and the default depth state (2D: neither test nor write).
     */
    protected Renderer(String shaderName) {
        // Unknown or failed: the fallback data, whose program is linked from the start
        ShaderData shaderData = resources.getResourceOrFallback(ShaderData.class, shaderName);
        shader = (Shader) glResources.acquire(shaderData, true);
        boolean in2D = shaderData.isDimension2DShader();

        renderState = new RenderState(!in2D, !in2D, false);

        model = new Matrix4f().identity();

        shaderValues = new HashMap<>();
        optionalValues = new HashSet<>();

        visible = true;

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();

        // Generic matrices: sent at each draw when the current program uses them
        if (in2D) {
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

    protected final Resources resources() {
        return resources;
    }

    protected final GLResources glResources() {
        return glResources;
    }

    /**
     * The shared texture of the TextureData of this name, referenced (urgent) until released: an unknown name gives
     * the fallback element of the textures, shared by every unknown name instead of a copy each.
     */
    protected final Texture acquireTexture(String name) {
        return (Texture) glResources.acquire(resources.getResourceOrFallback(TextureData.class, name), true);
    }

    // The camera given to setReferenceCamera, null when the default camera of the view is used
    protected final Camera getReferenceCamera() {
        return referenceCamera;
    }

    // A value sent only when the current program has its uniform (not reported missing otherwise)
    protected final void putOptionalValue(String key, ShaderValue value) {
        shaderValues.put(key, value);
        optionalValues.add(key);
    }

    // The program if linked, otherwise the fallback one (failed or still loading), always linked
    public final Shader getShader() {
        if (shader.isLoaded())
            return shader;

        // Fixed once the fallback data are loaded: no lookup at each draw
        if (fallbackShader == null)
            fallbackShader = (Shader) glResources.fallback(ShaderData.class);
        return fallbackShader;
    }

    // True when the current program of this renderer has this uniform
    protected final boolean hasUniform(String name) {
        return getShader().getLink(name) != -1;
    }

    // Send the values of this renderer to the current program (the unchanged ones are skipped by it)
    protected final void sendShaderValues() {
        for (Map.Entry<String, ShaderValue> entry : shaderValues.entrySet()) {
            ShaderValue value = entry.getValue();
            if (optionalValues.contains(entry.getKey()) && !hasUniform(value.getName()))
                continue;

            sendToShader(value);
        }
    }

    /**
     * Hook called by the scene at each frame: applies the render state, uses the program, then updateShader and
     * draw. A subclass rather redefines those; it may check a condition or prepare its geometry, then call super.
     */
    @Override
    public void display(){
        if(visible) {
            applyRenderState();
            getShader().use();
            updateShader();
            draw();
            renderState.restore();
        }
    }

    // Hook: sets the OpenGL state of the draw (depth, culling) before the program is used; call super to keep it
    protected void applyRenderState() {
        renderState.apply();
    }

    public final Renderer setDepthTest(boolean depthTest) {
        renderState.setDepthTest(depthTest);
        return this;
    }

    public final boolean isDepthTest() {
        return renderState.isDepthTest();
    }

    public final Renderer setDepthWrite(boolean depthWrite) {
        renderState.setDepthWrite(depthWrite);
        return this;
    }

    public final boolean isDepthWrite() {
        return renderState.isDepthWrite();
    }

    // Hook (call super): a subclass may redefine it to return its own type
    public Renderer setFaceCulling(boolean faceCulling) {
        renderState.setFaceCulling(faceCulling);
        return this;
    }

    public final boolean isFaceCulling() {
        return renderState.isFaceCulling();
    }

    /*
     * Hook called with the program in use, before draw: sends the values of this renderer. A subclass binds its
     * textures or sends values computed at each draw here, and calls super (or sendShaderValues).
     */
    public void updateShader(){
        sendShaderValues();
    }

    // Hook: draws the geometry, with the program in use and the values sent (one or several draw calls)
    public abstract void draw();

    public final Renderer addShaderValue(String name, Class<?> type, Object object){
        shaderValues.put(name, new ShaderValue(name, type, object));
        optionalValues.remove(name);

        return this;
    }

    public final boolean hasShaderValue(String name) {
        return shaderValues.containsKey(name);
    }

    // The value sent under this name, null when there is none
    public final ShaderValue getShaderValue(String name) {
        return shaderValues.get(name);
    }

    public final Renderer removeShaderValue(String name) {
        shaderValues.remove(name);
        optionalValues.remove(name);

        return this;
    }

    // Change the object of a value added with addShaderValue
    public final Renderer updateShaderValue(String name, Object object) {
        ShaderValue value = shaderValues.get(name);
        if (value == null)
            throw new IllegalArgumentException("No shader value " + name + " in " + getClass().getSimpleName()
                    + ": add it first with addShaderValue(name, type, object)");

        value.setObject(object);

        return this;
    }

    public final Renderer copyShaderValuesTo(Renderer other, boolean eraseExisting){
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

    // Send one value to the current program (skipped by it when unchanged)
    protected final void sendToShader(ShaderValue value) {
        getShader().sendValueToShader(value);
    }

    // Hook (call super): a subclass that follows the position redefines it
    public void setPosition(Vector3f position){
        this.position.x = position.x;
        this.position.y = position.y;
        this.position.z = position.z;

        applyModel();
    }

    public final void setX(float x){
        this.position.x = x;

        applyModel();
    }

    public final void setY(float y){
        this.position.y = y;

        applyModel();
    }

    public final void setZ(float z){
        this.position.z = z;

        applyModel();
    }

    public final void setScale(Vector3f scale){
        this.scale.x = scale.x;
        this.scale.y = scale.y;
        this.scale.z = scale.z;

        applyModel();
    }

    public final void setRotation(float angle, Vector3f rotation){
        this.rotation.x = rotation.x;
        this.rotation.y = rotation.y;
        this.rotation.z = rotation.z;

        this.angle = angle;

        applyModel();
    }


    public final void setRotation(Renderer other){
        this.rotation.x = other.rotation.x;
        this.rotation.y = other.rotation.y;
        this.rotation.z = other.rotation.z;

        this.angle = other.angle;

        applyModel();
    }

    public final void setRotationAngle(float angle){
        this.angle = angle;

        applyModel();
    }

    // Hook (call super): a subclass that follows the rotation redefines it
    public void setRotationZ(float angle){
        this.angle = angle;

        this.rotation.x = 0;
        this.rotation.y = 0;
        this.rotation.z = 1;

        applyModel();
    }

    public final void applyModel(){
        this.model.identity();

        this.model.translate(this.position);
        this.model.rotate(angle, this.rotation);
        this.model.scale(this.scale);
    }

    public final Renderer setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public final boolean isVisible() {
        return visible;
    }

    public final Renderer toggleVisible() {
        visible = !visible;
        return this;
    }

    // Hook (call super): draws in this color; a subclass also stops its textures there
    public ShaderValue setColorMode(Color4f color) {
        ShaderValue value = new ShaderValue(ShaderManager.GENERIC_COLOR_FIELD_NAME, Color4f.class, color);
        shaderValues.put(ShaderManager.GENERIC_COLOR_FIELD_NAME, value);

        return value;
    }

    // Hook (call super): neither color nor texture
    public void setNoColorNoTextureMode() {
        removeShaderValue(ShaderManager.GENERIC_COLOR_FIELD_NAME);
    }

    // Hook: the position of the drawn object, a subclass may compute it (an animation with its own reference)
    public Vector3f position(){
        return position;
    }

    public final Vector3f scale() { return scale; }

    public final Vector3f rotationAxis() { return rotation; }

    public final float getRotationAngle() { return angle; }

    public final Matrix4f getModel(){
        return new Matrix4f(model);
    }

    public final void setReferenceCamera(Camera camera){
        if (camera == null)
            return;

        referenceCamera = camera;
        putOptionalValue(ShaderManager.GENERIC_PROJECTION_FIELD_NAME, camera.getProjection());
        putOptionalValue(ShaderManager.GENERIC_VIEW_FIELD_NAME, camera.getView());
    }

    // Hook called when the renderer is no longer used: a subclass releases what it created, then calls super
    @Override
    public void unload() {
        // Given back once: unload can be called twice
        if (!shaderReleased) {
            glResources.releaseResource(shader);
            shaderReleased = true;
        }
    }
}
