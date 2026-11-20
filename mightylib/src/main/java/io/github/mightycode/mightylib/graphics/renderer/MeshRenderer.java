package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.utils.math.color.Color4f;

/**
 * A renderer which draws one Mesh, with the textures of its material. The renderer retains its mesh and releases it
 * in unload: a
 * mesh given to several renderers is shared, unloaded by the release of the last one (Mesh.retain).
 * A subclass fills the mesh (getMesh) in its constructor and can redefine the hooks of Renderer;
 * the mesh is drawn by draw() (bind, then Mesh.draw).
 * <p>
 * Texture mode: on with a texture (setMainTextureChannel, addTextureChannel), off with setColorMode(color) and
 * setNoColorNoTextureMode (isTextureEnabled, read by the renderers whose shader has both modes). The mesh is never
 * changed by the mode: a shader of color only does not read the texture coordinates.
 */
public class MeshRenderer extends Renderer {
    // Texture units 1 to MAX_TEXTURE_CHANNELS of addTextureChannel (unit 0 is the main texture)
    public static final int MAX_TEXTURE_CHANNELS = Material.MAX_TEXTURE_CHANNELS;

    private final Mesh mesh;

    // Texture mode (see the class)
    private boolean textureEnabled;

    // A renderer with a new empty mesh, to fill through getMesh()
    public MeshRenderer(String shaderName, boolean useEbo) {
        this(new Mesh(useEbo), shaderName);
    }

    // A renderer drawing the given mesh, retained until this renderer is unloaded (shared with its other users)
    public MeshRenderer(Mesh mesh, String shaderName) {
        this(mesh, new Material(shaderName));
    }

    // A renderer drawing the given mesh with the given material, both retained until this renderer is unloaded
    public MeshRenderer(Mesh mesh, Material material) {
        super(material);

        this.mesh = mesh;
        mesh.retain();
    }

    // Hook: binds the mesh and draws it (Mesh.draw is the hook of the draw call itself)
    @Override
    public void draw(){
        mesh.bind();
        mesh.draw();
    }

    public final void setMainTextureChannel(String name) {
        setMainTextureChannel(acquireTexture(name));
    }

    // The sampler nameOfUniform reads unit 0 (a value of the material)
    public final void setMainTextureChannel(String name, String nameOfUniform) {
        material().addShaderValue(nameOfUniform, Integer.class, 0);

        setMainTextureChannel(name);
    }

    /**
     * The texture on unit 0, in the material (shared with its other renderers when it is shared); a GL element is
     * referenced by the material until replaced or freed. Turns the texture mode on.
     */
    public final void setMainTextureChannel(IRenderTextureBindable texture) {
        material().setMainTexture(texture);
        textureEnabled = true;
    }

    /**
     * The texture of this name bound on the unit channelNumber (1 to MAX_TEXTURE_CHANNELS), sent to the sampler
     * nameOfUniform (both in the material); the texture already on this unit is released.
     * @throws IllegalArgumentException if channelNumber is out of range
     */
    public final void addTextureChannel(String name, String nameOfUniform, int channelNumber) {
        checkTextureChannel(channelNumber);
        addTextureChannel(acquireTexture(name), nameOfUniform, channelNumber);
    }

    // Same with a texture of the caller (released by the material when it is a GL element)
    public final void addTextureChannel(IRenderTextureBindable texture, String nameOfUniform, int channelNumber) {
        checkTextureChannel(channelNumber);
        material().addShaderValue(nameOfUniform, Integer.class, channelNumber);
        material().setTextureChannel(texture, channelNumber);

        textureEnabled = true;
    }

    static void checkTextureChannel(int channelNumber) {
        Material.checkTextureChannel(channelNumber);
    }

    // The color, and the texture mode off when disableTexture
    public final ShaderValue setColorMode(Color4f color, boolean disableTexture) {
        ShaderValue value = super.setColorMode(color);

        if (disableTexture)
            textureEnabled = false;

        return value;
    }

    // Hook (call super): the color, the texture mode off
    @Override
    public ShaderValue setColorMode(Color4f color) {
        return setColorMode(color, true);
    }

    // Hook (call super)
    @Override
    public void setNoColorNoTextureMode() {
        textureEnabled = false;

        super.setNoColorNoTextureMode();
    }

    // A texture is set and the texture mode was not turned off since (see the class)
    public final boolean isTextureEnabled() {
        return textureEnabled;
    }

    public final Mesh getMesh(){
        return mesh;
    }

    // The texture bound on unit 0 (of the material), null when none
    protected final IRenderTextureBindable getMainTexture() {
        return material().getMainTexture();
    }

    // Hook: a subclass releases what it created, then calls super (the mesh, the material)
    @Override
    public void unload() {
        mesh.release();

        super.unload();
    }
}
