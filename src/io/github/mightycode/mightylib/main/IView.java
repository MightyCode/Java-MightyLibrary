package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.camera.Camera3D;

/**
 * What the engine code runs for: a scene, or the engine itself before and between scenes.
 * It gives the services of the engine, the window it draws in and its cameras. The view being run is the
 * current one (Views.current): objects created without an explicit view (renderers, texts, GUI...) belong to it.
 */
public interface IView {
    Engine engine();

    // Window and inputs of the view
    Context context();

    // Throw when the view has no camera yet (the engine itself, a scene before its launch)
    Camera2D camera2D();

    Camera3D camera3D();
}
