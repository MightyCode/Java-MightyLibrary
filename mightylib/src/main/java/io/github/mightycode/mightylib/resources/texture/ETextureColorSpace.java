package io.github.mightycode.mightylib.resources.texture;

import java.util.Locale;

/**
 * How the values of a texture are encoded. A color picture is sRGB (decoded to linear by the card when it is read);
 * a data map (specular, normals, font coverage...) is linear: its values are used as they are.
 * Field "colorSpace" of textures.json ("srgb" by default, or "linear").
 */
public enum ETextureColorSpace {
    SRGB,
    LINEAR;

    // Name written in textures.json
    public String jsonName() {
        return name().toLowerCase(Locale.ROOT);
    }

    // From textures.json, null if unknown
    public static ETextureColorSpace parse(String name) {
        if (name == null)
            return null;

        for (ETextureColorSpace value : values()) {
            if (value.name().equalsIgnoreCase(name.trim()))
                return value;
        }

        return null;
    }
}
