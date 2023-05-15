package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
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
        create(data, "resources/tileset");
    }

    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new TileSet(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
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
