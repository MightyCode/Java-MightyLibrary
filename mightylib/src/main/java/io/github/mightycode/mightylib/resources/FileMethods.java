package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.List;


public final class FileMethods {
    private FileMethods() {}

    public static boolean copy(File source, File dest) {
        try (InputStream sourceFile = new FileInputStream(source);
             OutputStream destinationFile = new FileOutputStream(dest)) {
            // Lecture par segment de 0.5Mo
            byte[] buffer = new byte[512 * 1024];
            int nbLecture;
            while ((nbLecture = sourceFile.read(buffer)) != -1){
                destinationFile.write(buffer, 0, nbLecture);
            }
        } catch (IOException e){
            Logger.LogException("Can't copy " + source + " to " + dest, e);
            return false;
        }

        return true; // Result OK
    }


    public static boolean copy(String source, String dest) {
        return copy(new File(source), new File(dest));
    }


    /**
     * The text (UTF-8) of the file at this logical path, served by the highest source that has it; lines ended by
     * '\n'. Throws a ResourceException when no source has it or it is unreadable.
     */
    public static String readString(ResourceSources sources, String path) {
        try {
            return readLines(sources.open(path));
        } catch (NoSuchFileException e) {
            throw new ResourceException("file not found: " + path);
        } catch (IOException e) {
            throw new ResourceException("can't read the file " + path, e);
        } catch (IllegalArgumentException e) {
            // Absolute or with "..": written in a description
            throw new ResourceException(e.getMessage());
        }
    }

    // UTF-8, lines ended by '\n'; closes the stream
    public static String readLines(InputStream stream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                content.append(line).append('\n');
        }

        return content.toString();
    }

    // Throws a ResourceException when no source has the file, or it is unreadable or not a JSON object
    public static JSONObject readJSONObject(ResourceSources sources, String path) {
        String content = readString(sources, path);
        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            throw new ResourceException("invalid JSON in " + path, e);
        }
    }

    /**
     * The JSON object of the file at this logical path in every source that has it, from the lowest to the highest:
     * the layers of a list merged by entry (textures.json, fonts.json, shaders.json, batches). Throws a
     * ResourceException when no source has it, or a layer is unreadable or not a JSON object (naming its source).
     */
    public static List<JSONObject> readJSONObjectLayers(ResourceSources sources, String path) {
        List<JSONObject> layers = new ArrayList<>();
        for (IResourceSource source : sources.layers(path)) {
            if (source.size(path) < 0)
                continue;

            String where = path + " (" + source.name() + ")";
            try {
                layers.add(new JSONObject(readLines(source.open(path))));
            } catch (JSONException e) {
                throw new ResourceException("invalid JSON in " + where, e);
            } catch (IOException e) {
                throw new ResourceException("can't read the file " + where, e);
            }
        }

        if (layers.isEmpty())
            throw new ResourceException("file not found: " + path);

        return layers;
    }

    /**
     * The layers of a list merged by entry, from the lowest to the highest: the object under listKey gets the entries
     * of every layer, a higher layer replacing an entry of the same name as a whole; the other keys are the highest
     * layer's that has them.
     */
    public static JSONObject mergeEntries(List<JSONObject> layers, String listKey) {
        JSONObject merged = new JSONObject();
        JSONObject entries = new JSONObject();
        for (JSONObject layer : layers) {
            for (String key : layer.keySet()) {
                if (!key.equals(listKey))
                    merged.put(key, layer.get(key));
            }

            JSONObject list = layer.optJSONObject(listKey);
            if (list != null) {
                for (String name : list.keySet())
                    entries.put(name, list.get(name));
            }
        }

        return merged.put(listKey, entries);
    }
}
