package MightyLibrary.mightylib.resources;

import MightyLibrary.mightylib.sounds.Sound;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class SoundLoader {
    public static void creates(Map<String, DataType> data){
        creates(data, "resources/sounds");
    }


    private static void creates(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            //System.out.println("Load animation -> " + name);
            data.put(name, new Sound(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                creates(data, path + "/" + childPath);
            }
        }
    }
}
