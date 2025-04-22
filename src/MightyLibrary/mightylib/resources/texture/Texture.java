package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.graphics.GLElement;
import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import MightyLibrary.mightylib.graphics.surface.IGLBindable;
import MightyLibrary.mightylib.graphics.surface.TextureParameters;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.utils.Logger;

import static org.lwjgl.opengl.GL13.*;

public class Texture extends GLElement implements IGLBindable, IRenderTextureBindable {
    private final TextureData data;
    private int textureId;

    private int qualityType;

    private int textureType;

    public static void CreateErrorTexture() {
        GLResources.getInstance().addElementOrReturnIfPresent(Texture.class, new Texture("error"), "error");
    }

    public Texture(String name) {
        this(Resources.getInstance().getResource(TextureData.class, name));
    }

    public Texture(TextureData data) {
        this.data = data;
        if (data != null)
            resourceDependencies.add(data);

        textureId = -1;
    }

    public void setAspectTexture(int aspectTexture){
        this.qualityType = aspectTexture;
    }

    public void bindRenderTexture() {
        bindRenderTexture(0);
    }

    @Override
    public void bindRenderTexture(int texturePos) {
        // Active the texture to right position
        glActiveTexture(GL_TEXTURE0 + texturePos);
        if (isLoaded()){
            glBindTexture(textureType, textureId);
            Logger.CheckOpenGLError("Bind texture : " + data.getDataName() + " , texture id : " + textureId + " , texture type : " + textureType);
        // If isn't correct loaded, bind error texture
        } else {
            throw new RuntimeException("Texture : " + data.getDataName() + " , isn't loaded!");
            /*glBindTexture(textureType, 1);
            Logger.CheckOpenGLError("Bind texture : error");*/
        }
    }

    @Override
    public boolean load(int remainingMilliseconds) {
        if (textureId != -1)
            unload(remainingMilliseconds);

        if (data == null) {
            return false;
        }

        qualityType = data.defaultAspectTexture;
        textureType = data.defaultTextureType;

        textureId = glGenTextures();

        try {
            glBindTexture(this.textureType, textureId);
            Logger.CheckOpenGLError("Bind texture : " + data.getDataName() + " , texture id : " + textureId + " , texture type : " + textureType + " , quality type : " + qualityType + " , path : " + data.path() + " width : " + data.getWidth() + " , height : " + data.getHeight());
            glTexImage2D(this.textureType, 0, GL_RGBA8, data.getWidth(), data.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, data.getData());
            Logger.CheckOpenGLError("Set texture data : " + data.getDataName());

            TextureParameters.applyParameters(this);
            Logger.CheckOpenGLError("Set texture data : " + data.getDataName());

            //System.out.println("Texture : " + textureId + " , loaded with path : " + path);

            return true;
        } catch (Exception e) {
            System.err.println("Fail to create texture " + data.path() + " :");
            e.printStackTrace();
            glDeleteTextures(textureId);

            return false;
        }
    }

    private void setTextParam(int param, int value) {
        bindRenderTexture();
        if (data.isLoaded())
            glTexParameteri(this.textureType, param, value);
    }

    public int getQualityType(){
        return qualityType;
    }

    public void setTextureType(int textureType){
        this.textureType = textureType;
    }

    public int getTextureType(){
        return this.textureType;
    }

    @Override
    public int getRenderTextureId() {
        return textureId;
    }

    @Override
    public int getWidth() {
        return data.getWidth();
    }

    @Override
    public int getHeight() {
        return data.getHeight();
    }

    @Override
    public void unload(int remainingMilliseconds) {
        if (!data.isLoaded())
            glDeleteTextures(textureId);
    }
}
