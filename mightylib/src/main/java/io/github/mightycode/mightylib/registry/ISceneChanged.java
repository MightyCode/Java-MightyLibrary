package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.main.Context;
import io.github.mightycode.mightylib.scenes.Scene;

// A new scene of a context is running (loaded, first frame complete): see LifecycleSection.onSceneChanged
@FunctionalInterface
public interface ISceneChanged {
    /**
     * @param previous the scene running before in this context (already unloaded), null for the first one
     */
    void sceneChanged(Context context, Scene previous, Scene next);
}
