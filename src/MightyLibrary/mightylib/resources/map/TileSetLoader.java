package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import org.joml.Vector2i;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class TileSetLoader extends ResourceLoader {

    @Override
    public Class<?> getType() {
        return TileSet.class;
    }

    @Override
    public String getResourceNameType() {
        return "TileSet";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, "resources/tileset");
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new TileSet(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending.equals(".tileset"))
            return getFileName(path);

        return null;
    }

    @Override
    public void load(DataType dataType) {
        if (!(dataType instanceof TileSet))
            return;

        TileSet tileset = (TileSet) dataType;

        String data = FileMethods.readFileAsString(tileset.getPath());
        String[] parts = data.split("\n");

        int index = 0;

        String texture = parts[index++];

        String[] sizePart = parts[index++].trim().split(" ");

        tileset.setTileSize(texture,
                new Vector2i(
                        Integer.parseInt(sizePart[0]),
                        Integer.parseInt(sizePart[1])
                )
        );

        sizePart = parts[index++].trim().split(" ");

        tileset.setTileParameters(
                Boolean.parseBoolean(sizePart[0]),
                Boolean.parseBoolean(sizePart[1])
        );
    }

    @Override
    public void createAndLoad(Map<String, DataType> data, String resourceName, String resourcePath) {
        // Todo
    }
}
