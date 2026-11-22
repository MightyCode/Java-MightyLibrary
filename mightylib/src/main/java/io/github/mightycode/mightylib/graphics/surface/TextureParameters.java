package io.github.mightycode.mightylib.graphics.surface;

import io.github.mightycode.mightylib.utils.Logger;

import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public final class TextureParameters {
    private TextureParameters() {}

    // Linear filtering with mipmaps, clamped ("qualityType": "realistic")
    public static final int REALISTIC_PARAMETERS = 11848848;
    public static void realisticParameters(IGLBindable bindableObject){
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glGenerateMipmap(bindableObject.getTextureType());

        Logger.CheckOpenGLError("Texture parameters");
    }

    /*
     * Linear filtering without mipmaps, clamped ("qualityType": "linear"): atlases whose parts are drawn on their own,
     * like the fonts (a mipmap would blend the neighbor parts).
     */
    public static final int LINEAR_PARAMETERS = 41632789;
    public static void linearParameters(IGLBindable bindableObject){
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(bindableObject.getTextureType(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        Logger.CheckOpenGLError("Texture parameters");
    }

    // Nearest filtering, repeated ("qualityType": "pixelart")
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
            case LINEAR_PARAMETERS:
                linearParameters(bindableObject);
                break;
            case PIXEL_ART_PARAMETERS:
                pixelArtParameters(bindableObject);
                break;
            default:
                // Nothing to do for the other values
                break;
        }
    }
}
