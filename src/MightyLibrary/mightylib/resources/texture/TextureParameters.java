package MightyLibrary.mightylib.resources.texture;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

public abstract class TextureParameters {
    public static final int REALISTIC_PARAMETERS = 11848848;
    public static void realisticParameters(IGLBindable bindableObject){
        if (bindableObject.getTextureType() == GL_TEXTURE_RECTANGLE){
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glGenerateMipmap(bindableObject.getTextureType());
        }
    }

    public static final int PIXEL_ART_PARAMETERS = 28775178;
    public static void pixelArtParameters(IGLBindable bindableObject){
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_T, GL_REPEAT);

        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    public static void applyParameters(IGLBindable bindableObject){
        switch (bindableObject.getQualityType()){
            case REALISTIC_PARAMETERS:
                realisticParameters(bindableObject);
                break;
            case PIXEL_ART_PARAMETERS:
                pixelArtParameters(bindableObject);
        }
    }
}
