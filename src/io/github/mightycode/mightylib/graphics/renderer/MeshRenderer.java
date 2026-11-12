package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.utils.math.color.Color4f;

/**
 * A renderer which draws one Mesh, with its textures. The renderer owns its mesh: unload releases it.
 */
public class MeshRenderer extends Renderer {
    protected final Mesh mesh;

    // Textures channels
    protected IRenderTextureBindable mainTexture;
    protected IRenderTextureBindable[] textures;

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

    // Bind the textures, then send the values
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

    @Override
    public void draw(){
        mesh.bind();
        mesh.draw();
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

        mesh.enableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);
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

        mesh.enableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        if (textures == null)
            textures = new IRenderTextureBindable[30];
        else {
            if (textures[channelNumber - 1] instanceof GLElement)
                glResources.releaseResource((GLElement) textures[channelNumber - 1]);
        }

        textures[channelNumber - 1] = texture;
    }

    // The color, and the texture coordinates cut when disableTexture
    public ShaderValue setColorMode(Color4f color, boolean disableTexture) {
        ShaderValue value = super.setColorMode(color);

        if (disableTexture)
            mesh.disableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        return value;
    }

    @Override
    public ShaderValue setColorMode(Color4f color) {
        return setColorMode(color, true);
    }

    @Override
    public void setNoColorNoTextureMode() {
        mesh.disableVbo(Mesh.COMMON_TEXTURE_POSITION_CHANNEL);

        super.setNoColorNoTextureMode();
    }

    public Mesh getMesh(){
        return mesh;
    }

    @Override
    public void unload() {
        mesh.unload();

        if (mainTexture instanceof GLElement)
            glResources.releaseResource((GLElement) mainTexture);

        if (textures != null) {
            for (IRenderTextureBindable texture : textures) {
                if (texture instanceof GLElement)
                    glResources.releaseResource((GLElement) texture);
            }
        }

        super.unload();
    }
}
