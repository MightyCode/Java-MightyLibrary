package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.FileMethods;
import MightyLibrary.mightylib.resources.ResourceLoader;
import org.joml.Vector2i;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class TilesetLoader  extends ResourceLoader {

    public TilesetLoader(){
        super(Tileset.class);
    }

    @Override
    public void create(Map<String, DataType> data){
        create(data, "resources/tileset");
    }

    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new Tileset(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
    }


    @Override
    public boolean load(DataType dataType) {
        if (dataType.getType() != EDataType.TileSet)
            return false;

        Tileset tileset = (Tileset) dataType;

        String data = FileMethods.readFileAsString(tileset.getPath());
        String[] parts = data.split("\n");
        tileset.setTexture(parts[0]);

        String[] sizePart = parts[1].trim().split(" ");

        tileset.setTileSize(
                new Vector2i(
                        Integer.parseInt(sizePart[0]),
                        Integer.parseInt(sizePart[1])
                )
        );

        return false;
    }

}
