package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.SingleSourceDataType;
import io.github.mightycode.mightylib.resources.texture.TextureData;

public class AnimationData extends SingleSourceDataType {
    private FrameData[] framesData;
    private String textureName;

    public AnimationData(String name, String path){
        super(TYPE_SET_UP.THREAD_CONTEXT, name, path);

        textureName = "error";
    }


    @Override
    protected DataType createReloadCopy() {
        return getClass() == AnimationData.class ? new AnimationData(dataName, path) : null;
    }

    @Override
    protected void swapContent(DataType copy) {
        AnimationData other = (AnimationData) copy;

        FrameData[] otherFrames = other.framesData;
        other.framesData = framesData;
        framesData = otherFrames;

        String otherTexture = other.textureName;
        other.textureName = textureName;
        textureName = otherTexture;
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
        return framesData != null && resources().isExistingResource(TextureData.class, textureName);
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
