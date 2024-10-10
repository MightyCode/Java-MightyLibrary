package MightyLibrary.mightylib.resources.texture;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;

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
        exploreResourcesFile(data, "resources");
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
            atlas.addTexture(part);
        }
    }
}
