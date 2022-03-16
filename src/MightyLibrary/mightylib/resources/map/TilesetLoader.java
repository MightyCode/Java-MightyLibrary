package MightyLibrary.mightylib.resources.map;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class TilesetLoader  extends ResourceLoader {

    public TilesetLoader(){
        super(Tileset.class);
    }

    @Override
    public void load(Map<String, DataType> data){
        load(data, "resources/tileset");
    }


    private void load(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new Tileset(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                load(data, path + "/" + childPath);
            }
        }
    }
}
