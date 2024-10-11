package MightyLibrary.mightylib.resources.animation;

import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.SingleSourceDataType;
import MightyLibrary.mightylib.resources.texture.TextureData;

public class AnimationData extends SingleSourceDataType {
    private FrameData[] framesData;
    private String textureName;

    public AnimationData(String name, String path){
        super(TYPE_SET_UP.THREAD_CONTEXT, name, path);

        textureName = "error";
    }


    public AnimationData setTexture(String texture) {
        textureName = texture;
        if (isLoaded() && isPreLoaded())
            load();

        return this;
    }


    public AnimationData setFramesData(FrameData[] framesData) {
        this.framesData = framesData;

        if (isLoaded() && isPreLoaded())
            load();

        return this;
    }

    @Override
    protected boolean internLoad() {
        return framesData != null && Resources.getInstance().isExistingResource(TextureData.class, textureName);
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
    public void internUnload(){
        textureName = "error";
        framesData = null;
    }
}
