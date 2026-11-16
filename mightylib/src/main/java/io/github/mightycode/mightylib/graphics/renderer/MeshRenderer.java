package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.utils.math.color.Color4f;

/**
 * A renderer which draws one Mesh, with its textures. The renderer owns its mesh: unload releases it.
 * A subclass fills the mesh (getMesh) in its constructor and can redefine the hooks of Renderer;
 * the mesh is drawn by draw() (bind, then Mesh.draw).
 */
public class MeshRenderer extends Renderer {
    // Texture units 1 to MAX_TEXTURE_CHANNELS of addTextureChannel (unit 0 is the main texture)
    public static final int MAX_TEXTURE_CHANNELS = 30;

    private final Mesh mesh;

    // Textures channels
    private IRenderTextureBindable mainTexture;
    private IRenderTextureBindable[] textures;

    // A renderer with a new empty mesh, to fill through getMesh()
    public MeshRenderer(String shaderName, boolean useEbo) {
        this(new Mesh(useEbo), shaderName);
    }

    // A renderer drawing the given mesh, which it owns from now on
    public MeshRenderer(Mesh mesh, String shaderName) {
        super(shaderName);

        this.mesh = mesh;
        mainTexture = null;
    }

    // Hook: binds the textures, then sends the values (a subclass calls super)
    @Override
    public void updateShader(){
        if (mainTexture != null)
            mainTexture.bindRenderTexture(0);

        if (textures != null) {
            for (int i = 0; i < textures.length; ++i) {
                if (textures[i] != null)
                    textures[i].bindRenderTexture(i + 1);
            }
        }

        super.updateShader();
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

    public final void setMainTextureChannel(String name, String nameOfUniform) {
        addShaderValue(nameOfUniform, Integer.class, 0);

        setMainTextureChannel(name);
    }

    public final void setMainTextureChannel(IRenderTextureBindable texture) {
        if (mainTexture instanceof GLElement) {
            if (texture instanceof GLElement) {
                if (mainTexture == texture) {
                    glResources().releaseResource((GLElement) texture);
                    return;
                }
            }

            glResources().releaseResource((GLElement) mainTexture);
        }

        mesh.enableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);
        mainTexture = texture;
    }

    /**
     * The texture of this name bound on the unit channelNumber (1 to MAX_TEXTURE_CHANNELS), sent to the sampler
     * nameOfUniform; the texture already on this unit is released.
     * @throws IllegalArgumentException if channelNumber is out of range
     */
    public final void addTextureChannel(String name, String nameOfUniform, int channelNumber) {
        checkTextureChannel(channelNumber);
        addTextureChannel(acquireTexture(name), nameOfUniform, channelNumber);
    }

    // Same with a texture of the caller (released by this renderer when it is a GL element)
    public final void addTextureChannel(IRenderTextureBindable texture, String nameOfUniform, int channelNumber) {
        checkTextureChannel(channelNumber);
        addShaderValue(nameOfUniform, Integer.class, channelNumber);

        mesh.enableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        if (textures == null)
            textures = new IRenderTextureBindable[MAX_TEXTURE_CHANNELS];
        else if (textures[channelNumber - 1] instanceof GLElement)
            glResources().releaseResource((GLElement) textures[channelNumber - 1]);

        textures[channelNumber - 1] = texture;
    }

    static void checkTextureChannel(int channelNumber) {
        if (channelNumber <= 0 || channelNumber > MAX_TEXTURE_CHANNELS)
            throw new IllegalArgumentException("Texture channel " + channelNumber + " out of 1.." + MAX_TEXTURE_CHANNELS
                    + " (unit 0 is the main texture: setMainTextureChannel)");
    }

    // The color, and the texture coordinates cut when disableTexture
    public final ShaderValue setColorMode(Color4f color, boolean disableTexture) {
        ShaderValue value = super.setColorMode(color);

        if (disableTexture)
            mesh.disableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        return value;
    }

    // Hook (call super): the color, the texture coordinates cut
    @Override
    public ShaderValue setColorMode(Color4f color) {
        return setColorMode(color, true);
    }

    // Hook (call super)
    @Override
    public void setNoColorNoTextureMode() {
        mesh.disableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        super.setNoColorNoTextureMode();
    }

    public final Mesh getMesh(){
        return mesh;
    }

    // The texture bound on unit 0, null when none
    protected final IRenderTextureBindable getMainTexture() {
        return mainTexture;
    }

    // Hook: a subclass releases what it created, then calls super (the mesh, the textures, the program)
    @Override
    public void unload() {
        mesh.unload();

        if (mainTexture instanceof GLElement)
            glResources().releaseResource((GLElement) mainTexture);

        if (textures != null) {
            for (IRenderTextureBindable texture : textures) {
                if (texture instanceof GLElement)
                    glResources().releaseResource((GLElement) texture);
            }
        }

        super.unload();
    }
}
