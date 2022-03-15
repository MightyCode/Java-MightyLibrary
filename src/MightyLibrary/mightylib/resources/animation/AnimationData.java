package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;
import MightyLibrary.mightylib.resources.FileMethods;

public class AnimationData extends DataType {
    private static final int TEXTURE_POS = 0;
    private static final int SIZE_FRAME_POS = 1;
    private static final int FRAME_ENUM_START_POS = 2;

    private FrameData[] framesData;
    private String textureName;


    public AnimationData(String name, String path){
        super(EDataType.AnimationData, name, path);
        textureName = "";
    }


    @Override
    public boolean load(){
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");
        textureName = parts[TEXTURE_POS].trim();

        int numberFrames = Integer.parseInt(parts[SIZE_FRAME_POS].trim());
        framesData = new FrameData[numberFrames];

        for (int i = 0; i < numberFrames; ++i){
            framesData[i] = new FrameData();
            framesData[i].init(parts[FRAME_ENUM_START_POS + i].trim());
        }

        return true;
    }


    public FrameData getFrame(int index){
        if (framesData == null)
            return null;
        if (0 > index || index > frameNumber())
            return null;

        return framesData[index];
    }


    public int frameNumber(){
        return framesData.length;
    }


    public String getTextureName(){ return textureName; }


    @Override
    public boolean unload(){
        return true;
    }
}
