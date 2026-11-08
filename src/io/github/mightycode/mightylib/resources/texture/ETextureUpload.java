package io.github.mightycode.mightylib.resources.texture;

/**
 * How the big textures are sent to the graphic card, by strips of rows (see TextureUploader).
 * The result depends on the driver and the card: measure it with the command "textureupload bench".
 */
public enum ETextureUpload {
    // glTexSubImage2D reads the pixels from the memory of the program
    DIRECT,
    // The pixels go through a pixel buffer object (PBO) that the driver can transfer by itself
    PBO;

    public static final String PROPERTY = "mightylib.texture.upload";

    // From a command or a property, null if unknown
    public static ETextureUpload parse(String name) {
        if (name == null)
            return null;

        for (ETextureUpload value : values()) {
            if (value.name().equalsIgnoreCase(name.trim()))
                return value;
        }

        return null;
    }
}
