package MightyLibrary.mightylib.resources.texture;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class BasicBindableObject implements IGLBindable{
    private int qualityType;
    private int textureType;

    public BasicBindableObject(){
        qualityType = TextureParameters.REALISTIC_PARAMETERS;
        textureType = GL_TEXTURE_2D;
    }

    public BasicBindableObject setQualityTexture(int qualityTexture){
        this.qualityType = qualityTexture;

        return this;
    }

    public BasicBindableObject setTextureType(int textureType){
        this.textureType = textureType;

        return this;
    }

    @Override
    public int getQualityType() {
        return qualityType;
    }

    @Override
    public int getTextureType() {
        return textureType;
    }
}
