package io.github.mightycode.mightylib.resources;

// Helpers for the line-based formats (.tileset, .tilemap, .anim, .atlas): every error is a
// ResourceException naming the file and the line (1-based), never a raw NumberFormatException.
public abstract class TextFormat {
    public static ResourceException error(String path, int line, String message) {
        return new ResourceException("bad format in " + path + " (line " + line + "): " + message);
    }

    public static String word(String[] parts, int index, String path, int line, String what) {
        if (index >= parts.length || parts[index].isEmpty())
            throw error(path, line, "missing " + what);
        return parts[index];
    }

    public static int integer(String[] parts, int index, String path, int line, String what) {
        String word = word(parts, index, path, line, what);
        try {
            return Integer.parseInt(word.trim());
        } catch (NumberFormatException e) {
            throw error(path, line, what + " must be an integer, got '" + word + "'");
        }
    }

    public static float decimal(String[] parts, int index, String path, int line, String what) {
        String word = word(parts, index, path, line, what);
        try {
            return Float.parseFloat(word.trim());
        } catch (NumberFormatException e) {
            throw error(path, line, what + " must be a number, got '" + word + "'");
        }
    }
}
