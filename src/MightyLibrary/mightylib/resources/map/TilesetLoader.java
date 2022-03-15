package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class TilesetLoader {
    public static void creates(Map<String, DataType> data){
        creates(data, "resources/tileset");
    }


    private static void creates(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new Tileset(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                creates(data, path + "/" + childPath);
            }
        }
    }
}
