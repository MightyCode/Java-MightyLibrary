package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.*;
import io.github.mightycode.mightylib.resources.models.Model;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class AnimationDataLoader extends ResourceLoader {
    private static final int TEXTURE_POS = 0;
    private static final int SIZE_FRAME_POS = 1;
    private static final int FRAME_ENUM_START_POS = 2;

    @Override
    public Class<? extends DataType> getType() {
        return AnimationData.class;
    }

    @Override
    public String getResourceNameType() {
        return "AnimationData";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new AnimationData(name, currentPath));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (Objects.equals(ending, ".anim"))
            return getFileName(path);

        return null;
    }

    // Needs the TextureData loader registered first (its fallback is the texture of this one)
    @Override
    public DataType createFallback() {
        return new FallbackAnimationData();
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof AnimationData))
            return;

        AnimationData animationData = (AnimationData)dataType;

        String path = animationData.path();
        String data = FileMethods.readFileAsString(path);
        String[] parts = data.split("\n");

        if (parts.length <= SIZE_FRAME_POS)
            throw TextFormat.error(path, parts.length, "expected the texture name then the number of frames");

        int numberFrames = TextFormat.integer(new String[]{parts[SIZE_FRAME_POS].trim()}, 0,
                path, SIZE_FRAME_POS + 1, "number of frames");
        if (numberFrames < 0)
            throw TextFormat.error(path, SIZE_FRAME_POS + 1, "number of frames can't be negative");
        if (parts.length < FRAME_ENUM_START_POS + numberFrames)
            throw TextFormat.error(path, parts.length, "expected " + numberFrames + " frames, found "
                    + Math.max(0, parts.length - FRAME_ENUM_START_POS));

        FrameData[] framesData = new FrameData[numberFrames];

        for (int i = 0; i < numberFrames; ++i){
            framesData[i] = new FrameData();
            framesData[i].init(parts[FRAME_ENUM_START_POS + i].trim(), path, FRAME_ENUM_START_POS + i + 1);
        }

        animationData.setTexture(parts[TEXTURE_POS].trim()).setFramesData(framesData);
    }
}
