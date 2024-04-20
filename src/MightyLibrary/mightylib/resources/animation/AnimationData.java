package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.SingleSourceDataType;
import MightyLibrary.mightylib.resources.texture.Texture;

public class AnimationData extends SingleSourceDataType {
    private FrameData[] framesData;
    private String textureName;

    public AnimationData(String name, String path){
        super(name, path);

        textureName = "error";
    }


    public AnimationData setTexture(String texture){
        textureName = texture;

        checkLoaded();
        return this;
    }


    public AnimationData setFramesData(FrameData[] framesData){
        this.framesData = framesData;

        checkLoaded();
        return this;
    }

    private void checkLoaded(){
        correctlyLoaded = framesData != null && Resources.getInstance().isExistingResource(Texture.class, textureName);
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
    public void unload(){
        textureName = "error";
        framesData = null;

        correctlyLoaded = false;
    }
}
