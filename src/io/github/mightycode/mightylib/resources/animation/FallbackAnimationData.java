package io.github.mightycode.mightylib.resources.animation;

import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.FallbackTextureData;
import io.github.mightycode.mightylib.resources.texture.TextureData;

/**
 * Fallback of the animations: a single frame showing the whole fallback texture, created by the code.
 * Played in place of an animation that failed, is still loading or has an unknown name.
 */
public class FallbackAnimationData extends AnimationData {
    public FallbackAnimationData() {
        super(Resources.FALLBACK_NAME, null);
        // The fallback texture: a dependency, so this animation is loaded after it
        addDependency(Resources.getInstance().getFallback(TextureData.class));
    }

    @Override
    protected boolean internLoad() {
        FrameData frame = new FrameData();
        // x, y, width, height, hot point x, hot point y, frame time
        frame.init("0,0," + FallbackTextureData.SIZE + "," + FallbackTextureData.SIZE + ",0,0,1");

        setTexture(Resources.FALLBACK_NAME);
        setFramesData(new FrameData[]{ frame });

        return super.internLoad();
    }
}
