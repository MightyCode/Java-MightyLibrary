package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.DataType;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public abstract class AnimationDataLoader {
    public static void creates(Map<String, DataType> data){
        creates(data, "resources/animations");
    }


    private static void creates(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new AnimationData(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                creates(data, path + "/" + childPath);
            }
        }
    }
}
