package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.map.Tileset;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class AnimationDataLoader extends ResourceLoader {

    public AnimationDataLoader(){
        super(AnimationData.class);
    }

    public void load(Map<String, DataType> data){
        load(data, "resources/animations");
    }


    private void load(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new AnimationData(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                load(data, path + "/" + childPath);
            }
        }
    }
}
