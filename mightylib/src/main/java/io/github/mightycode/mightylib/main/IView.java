package io.github.mightycode.mightylib.main;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.graphics.debug.DebugDraw2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.DrawBatch2D;

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
    WindowContext context();

    // Throw when the view has no camera yet (the engine itself, a scene before its launch)
    Camera2D camera2D();

    Camera3D camera3D();

    // Color space of the draws of the view; INHERIT (the default): the one of the engine (Engine.colors).
    // A view that draws straight on the window (no framebuffer of view: overlays) returns GAMMA, the window is not sRGB
    default ERenderColorSpace colorSpace() {
        return ERenderColorSpace.INHERIT;
    }

    // Shader of the final pass of the view (its framebuffer to the window); null (the default): the one of the engine
    default String finalPassShader() {
        return null;
    }

    // Settings of the final pass; null (the default): those of the engine (the player's)
    default PictureSettings picture() {
        return null;
    }

    // The 2D batch of the draws of the view (Scene, WindowView); null (the default): every renderer draws alone
    default DrawBatch2D batch2D() {
        return null;
    }

    // The debug drawings of the view, drawn over it at the end of its display (Scene, WindowView); null: none
    default DebugDraw2D debugDraw() {
        return null;
    }
}
