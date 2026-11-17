package io.github.mightycode.mightylib.resources;

import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;


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
        StringBuilder source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(sources.open(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
        } catch (NoSuchFileException e) {
            throw new ResourceException("file not found: " + path);
        } catch (IOException e) {
            throw new ResourceException("can't read the file " + path, e);
        } catch (IllegalArgumentException e) {
            // Absolute or with "..": written in a description
            throw new ResourceException(e.getMessage());
        }

        return source.toString();
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

    // A file of the disk. Throws a ResourceException when the file is missing or unreadable
    public static String readFileAsString(String filePath) {
        StringBuilder source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
        } catch (FileNotFoundException e) {
            throw new ResourceException("file not found: " + filePath);
        } catch (IOException e) {
            throw new ResourceException("can't read the file " + filePath, e);
        }

        return source.toString();
    }

    // Throws a ResourceException when the file is missing, unreadable or not a JSON object
    public static JSONObject readJSONObject(String filePath) {
        String content = readFileAsString(filePath);
        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            throw new ResourceException("invalid JSON in " + filePath, e);
        }
    }
}
