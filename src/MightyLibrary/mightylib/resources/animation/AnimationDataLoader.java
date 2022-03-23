package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.*;
import MightyLibrary.mightylib.sounds.SoundData;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class AnimationDataLoader extends ResourceLoader {

    private static final int TEXTURE_POS = 0;
    private static final int SIZE_FRAME_POS = 1;
    private static final int FRAME_ENUM_START_POS = 2;

    @Override
    public Class<?> getType() {
        return AnimationData.class;
    }

    @Override
    public String getResourceNameType() {
        return "AnimationData";
    }

    @Override
    public void create(Map<String, DataType> data){
        create(data, "resources/animations");
    }


    private void create(Map<String, DataType> data, String path){
        File file = new File(path);

        if (file.isFile()){
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            data.put(name, new AnimationData(name, path));
        } else if (file.isDirectory()) {
            for (String childPath : Objects.requireNonNull(file.list())){
                create(data, path + "/" + childPath);
            }
        }
    }


    @Override
    public boolean load(DataType dataType) {
        if (!(dataType instanceof AnimationData))
            return false;

        AnimationData animationData = (AnimationData)dataType;

        String data = FileMethods.readFileAsString(animationData.getPath());
        String[] parts = data.split("\n");

        animationData.setTexture(parts[TEXTURE_POS].trim());

        int numberFrames = Integer.parseInt(parts[SIZE_FRAME_POS].trim());
        FrameData[] framesData = new FrameData[numberFrames];

        for (int i = 0; i < numberFrames; ++i){
            framesData[i] = new FrameData();
            framesData[i].init(parts[FRAME_ENUM_START_POS + i].trim());
        }

        animationData.setFramesData(framesData);

        return true;

    }
}
