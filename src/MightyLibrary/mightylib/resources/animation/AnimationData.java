package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;

public class AnimationData extends DataType {

    private FrameData[] framesData;
    private String textureName;


    public AnimationData(String name, String path){
        super(EDataType.AnimationData, name, path);
        textureName = "";
    }


    public AnimationData setTexture(String texture){
        textureName = texture;

        return this;
    }


    public AnimationData setFramesData(FrameData[] framesData){
        this.framesData = framesData;

        return this;
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
