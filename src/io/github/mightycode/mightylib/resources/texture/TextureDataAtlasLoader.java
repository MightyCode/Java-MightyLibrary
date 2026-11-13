package io.github.mightycode.mightylib.resources.texture;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceLoader;

import java.util.Map;

public class TextureDataAtlasLoader extends ResourceLoader {

    @Override
    public Class<? extends DataType> getType() {
        return TextureDataAtlas.class;
    }

    @Override
    public String getResourceNameType() {
        return "TextureDataAtlas";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, resources().folder());
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new TextureDataAtlas(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".atlas"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof TextureDataAtlas))
            return;

        TextureDataAtlas atlas = (TextureDataAtlas)dataType;
        String data = FileMethods.readFileAsString(atlas.path());

        String[] parts = data.split("\n");

        for (String part : parts) {
            // Blank lines (trailing newline, CRLF leftovers) are not texture names
            if (!part.trim().isEmpty())
                atlas.addTexture(part.trim());
        }
    }
}
