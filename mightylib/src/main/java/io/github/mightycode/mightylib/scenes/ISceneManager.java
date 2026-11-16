package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.main.Context;

/**
 * The scenes of a context (Context.getSceneManager): the current scene, its changes and its loading screen. One per
 * context, created by the factory of the registry (registry.services().sceneManager) and driven by its Context. The
 * scenes see it through SceneManagerInterface.
 */
public interface ISceneManager {
    // The first scene, loaded at once
    void init(Scene firstScene, String[] firstArguments);

    // One tick: the pending change, then the current scene
    void update();

    void display();

    // End of the tick
    void postUpdate();

    boolean isExitRequested();

    int getExitStatus();

    Scene getCurrentScene();

    // A scene change is asked (by a command or a scene) and will be done at the beginning of the next tick
    boolean isSceneChangePending();

    boolean isCurrentSceneLoading();

    // The change is done at the beginning of the next tick
    void requestSceneChange(Scene scene, String[] args);

    // The context stops at the beginning of the next tick (the program, for the main context)
    void requestExit(int status);

    // Its windows and its engine
    Context getContext();

    // The current scene is unloaded; the resources are freed by the engine at its end
    void unload();

    // The context is closed while the engine goes on: what its scene loaded must not stay loaded
    void releaseSceneResources();
}
