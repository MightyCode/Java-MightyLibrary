package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.utils.IDisplayable;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderUniforms;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import io.github.mightycode.mightylib.scenes.camera.Camera;
import io.github.mightycode.mightylib.utils.math.UUID;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;


/**
 * Base of every renderer: draws something with a Material (program, common values, textures, OpenGL state). Holds
 * what is its own: the transform (model matrix), the reference camera, the visibility, and its own values of uniforms
 * (overrides: the generic matrices, the color, and every value added through this renderer), which win over those of
 * its material. It has no geometry: MeshRenderer draws one Mesh, ModelRenderer a model.
 * <p>
 * Created with a shader name, a renderer has a material of its own; setMaterial (or a constructor that takes a
 * Material) shares one between renderers. The setters of the state (setDepthTest, setBlendMode...) write into the
 * material: shared, they change all its renderers.
 *
 * <p>Hooks of a subclass: draw (the geometry), updateShader (bind and send before draw), applyRenderState, display
 * (a condition, then super), unload (release, then super), and the setters whose change it must follow
 * (setPosition, setRotationZ, position, setColorMode, setNoColorNoTextureMode, setFaceCulling: call super).
 * display() calls them in this order: applyRenderState, program in use, updateShader, draw.
 * The state of the renderer is reached through methods only.
 */
public abstract class Renderer extends UUID implements IDisplayable, IUniformTarget {
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

    // Retained until unload (shared with its other renderers)
    private Material material;
    private boolean materialReleased;

    /*
     * Every value of this renderer, sent again at each draw after those of the material (filtered by the program,
     * which skips unchanged ones): the program can change at any time (fallback while loading, hot reload) and has
     * none of them at first.
     */
    private final UniformValues overrides = new UniformValues();
    // 1 when the draw is linear: sent with the other values
    private final ShaderValue linearColors;
    // Size of the viewport of the draw, in pixels: sent with the other values
    private final Vector2f viewportSize = new Vector2f();

    /*
     * A material of its own, of the program of the shader (the fallback one while it loads or when it failed); the
     * tag of the shader chooses the default camera (2D or 3D) and the default depth state (2D: neither test nor write).
     */
    protected Renderer(String shaderName) {
        this(new Material(shaderName));
    }

    /*
     * Draws with the material, retained until unload (shared with the other renderers that have it); its tag chooses
     * the default camera (2D or 3D).
     */
    protected Renderer(Material material) {
        this.material = material;
        material.retain();
        boolean in2D = material.isDimension2D();

        model = new Matrix4f().identity();

        visible = true;

        position = new Vector3f();
        scale = new Vector3f(1f);
        rotation = new Vector3f();

        // Generic matrices: sent at each draw when the current program uses them
        if (in2D) {
            putOptionalValue(ShaderUniforms.PROJECTION, Views.current().camera2D().getProjection());
            putOptionalValue(ShaderUniforms.VIEW, Views.current().camera2D().getView());
        } else {
            putOptionalValue(ShaderUniforms.PROJECTION, Views.current().camera3D().getProjection());
            putOptionalValue(ShaderUniforms.VIEW, Views.current().camera3D().getView());
        }

        applyModel();

        putOptionalValue(ShaderUniforms.MODEL,
                new ShaderValue(ShaderUniforms.MODEL, Matrix4f.class, model));
        // Color space of the draw, resolved at each draw: a shader without the uniform draws in gamma
        linearColors = new ShaderValue(ShaderUniforms.LINEAR_COLORS, Integer.class, 0);
        putOptionalValue(ShaderUniforms.LINEAR_COLORS, linearColors);
        putOptionalValue(ShaderUniforms.VIEWPORT_SIZE,
                new ShaderValue(ShaderUniforms.VIEWPORT_SIZE, Vector2f.class, viewportSize));
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

    // A value of this renderer sent only when the current program has its uniform (not reported missing otherwise)
    protected final void putOptionalValue(String key, ShaderValue value) {
        if (!key.equals(value.getName()))
            throw new IllegalArgumentException("Optional value " + value.getName() + " put under the name " + key);
        overrides.put(value, true);
    }

    // ---- Material ----

    /** The material this renderer draws with (shared with the other renderers that have it). */
    public final Material material() {
        return material;
    }

    /**
     * Draws with this material from now on: retained, then the previous one released (freed when it was its last
     * renderer). The values of this renderer are kept, and still win over those of the material.
     */
    public final Renderer setMaterial(Material newMaterial) {
        if (newMaterial == material)
            return this;

        newMaterial.retain();
        Material previous = material;
        material = newMaterial;
        if (!materialReleased)
            previous.release();
        materialReleased = false;
        return this;
    }

    // The program of the material if linked, otherwise the fallback one (failed or still loading), always linked
    public final Shader getShader() {
        return material.getShader();
    }

    // True when the current program of this renderer has this uniform
    protected final boolean hasUniform(String name) {
        return getShader().location(name) != -1;
    }

    // Send the values to the current program: those of the material, then those of this renderer (which win)
    protected final void sendShaderValues() {
        material.sendValues(overrides);
        overrides.send(material.sink(), null);
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
        }
    }

    // Hook: sets the OpenGL state of the draw (that of the material) before the program is used; call super
    protected void applyRenderState() {
        boolean linear = resolveColorSpace().isLinear();
        material.renderState().apply(linear);
        linearColors.setObject(linear ? 1 : 0);

        GLContext context = GLContext.currentOrNull();
        if (context != null)
            viewportSize.set(context.viewportWidth(), context.viewportHeight());
    }

    /**
     * The mode of the next draw: the one of the material, otherwise of the current view, otherwise of the engine.
     */
    public final ERenderColorSpace resolveColorSpace() {
        IView view = Views.current();
        return view.engine().colors().resolve(material.getColorSpace(), view.colorSpace());
    }

    // INHERIT (the default) takes the mode of the view; LINEAR or GAMMA fixes the mode (of the material: shared)
    public final Renderer setColorSpace(ERenderColorSpace colorSpace) {
        material.setColorSpace(colorSpace);
        return this;
    }

    public final ERenderColorSpace getColorSpace() {
        return material.getColorSpace();
    }

    // Depth test of the material (shared with its other renderers)
    public final Renderer setDepthTest(boolean depthTest) {
        material.setDepthTest(depthTest);
        return this;
    }

    public final boolean isDepthTest() {
        return material.isDepthTest();
    }

    // Depth write of the material (shared with its other renderers)
    public final Renderer setDepthWrite(boolean depthWrite) {
        material.setDepthWrite(depthWrite);
        return this;
    }

    public final boolean isDepthWrite() {
        return material.isDepthWrite();
    }

    // Hook (call super): a subclass may redefine it to return its own type. Culling of the material (shared)
    public Renderer setFaceCulling(boolean faceCulling) {
        material.setFaceCulling(faceCulling);
        return this;
    }

    public final boolean isFaceCulling() {
        return material.isFaceCulling();
    }

    // How the draws mix with what is already drawn (ALPHA by default), in the material (shared)
    public final Renderer setBlendMode(EBlendMode blendMode) {
        material.setBlendMode(blendMode);
        return this;
    }

    public final EBlendMode getBlendMode() {
        return material.getBlendMode();
    }

    /*
     * Hook called with the program in use, before draw: binds the textures of the material and sends the values.
     * A subclass binds its own textures or sends values computed at each draw here, and calls super
     * (or sendShaderValues).
     */
    public void updateShader(){
        material.bindTextures();
        sendShaderValues();
    }

    // Hook: draws the geometry, with the program in use and the values sent (one or several draw calls)
    public abstract void draw();

    // A value of this renderer (wins over the one of its material); material().addShaderValue for a common one
    @Override
    public final Renderer addShaderValue(String name, Class<?> type, Object object){
        overrides.put(new ShaderValue(name, type, object), false);
        return this;
    }

    // A value of this renderer (not of its material)
    @Override
    public final boolean hasShaderValue(String name) {
        return overrides.has(name);
    }

    // The value of this renderer under this name, null when none (like hasShaderValue; material().getShaderValue
    // for the one of its material)
    public final ShaderValue getShaderValue(String name) {
        return overrides.get(name);
    }

    // Removes a value of this renderer (the one of its material, if any, is sent again)
    @Override
    public final Renderer removeShaderValue(String name) {
        overrides.remove(name);
        return this;
    }

    // Change the object of a value added with addShaderValue on this renderer
    @Override
    public final Renderer updateShaderValue(String name, Object object) {
        ShaderValue value = overrides.get(name);
        if (value == null)
            throw new IllegalArgumentException("No shader value " + name + " in " + getClass().getSimpleName()
                    + ": add it first with addShaderValue(name, type, object)"
                    + (material.hasShaderValue(name) ? " (its material has one: material().updateShaderValue)" : ""));

        value.setObject(object);

        return this;
    }

    // The values of this renderer (not of its material) given to the other one: the same objects, shared
    public final Renderer copyShaderValuesTo(Renderer other, boolean eraseExisting){
        for (ShaderValue value : overrides.values()) {
            if (other.overrides.has(value.getName()) && !eraseExisting)
                continue;

            other.overrides.put(value, overrides.isOptional(value.getName()));
        }

        return other;
    }

    // Send one value to the current program, found by its name (skipped by it when unchanged)
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

    // Through setPosition: a subclass that follows the position sees these changes too
    public final void setX(float x){
        setPosition(new Vector3f(x, position.y, position.z));
    }

    public final void setY(float y){
        setPosition(new Vector3f(position.x, y, position.z));
    }

    public final void setZ(float z){
        setPosition(new Vector3f(position.x, position.y, z));
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
        ShaderValue value = new ShaderValue(ShaderUniforms.COLOR, Color4f.class, color);
        overrides.put(value, false);

        return value;
    }

    // Hook (call super): neither color nor texture
    public void setNoColorNoTextureMode() {
        removeShaderValue(ShaderUniforms.COLOR);
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
        putOptionalValue(ShaderUniforms.PROJECTION, camera.getProjection());
        putOptionalValue(ShaderUniforms.VIEW, camera.getView());
    }

    // Hook called when the renderer is no longer used: a subclass releases what it created, then calls super
    @Override
    public void unload() {
        // Given back once: unload can be called twice
        if (!materialReleased) {
            material.release();
            materialReleased = true;
        }
    }
}
