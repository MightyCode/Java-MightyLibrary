package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.main.Engine;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.registry.Identifier;
import io.github.mightycode.mightylib.scenes.ISceneManager;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.services.IEngineService;
import io.github.mightycode.mightylib.services.ServiceKey;

/**
 * F7 (ActionId.TOGGLE_COLOR_SPACE) switches the color space of the current scene of the main window, in every scene
 * of the demo: linear becomes gamma and the other way round (the view takes the mode the draws do not have now).
 */
public final class ColorSpaceToggleService implements IEngineService {
    public static final ServiceKey<ColorSpaceToggleService> KEY =
            ServiceKey.of(Identifier.of(DemoPlugin.NAMESPACE, "color_space_toggle"), ColorSpaceToggleService.class);

    private final Engine engine;

    ColorSpaceToggleService(Engine engine) {
        this.engine = engine;
    }

    @Override
    public void update() {
        WindowContext window = engine.contexts().getMainWindow();
        if (window == null || !window.getInputManager().inputPressed(ActionId.TOGGLE_COLOR_SPACE))
            return;

        ISceneManager manager = window.getContext().getSceneManager();
        Scene scene = manager == null ? null : manager.getCurrentScene();
        if (scene == null || !scene.isLaunched())
            return;

        ERenderColorSpace now = engine.colors().resolve(null, scene.colorSpace());
        scene.setColorSpace(now.isLinear() ? ERenderColorSpace.GAMMA : ERenderColorSpace.LINEAR);
        System.out.println("--Color space of " + scene.getClass().getSimpleName() + " : "
                + scene.colorSpace().lowerName() + " (was " + now.lowerName() + ")");
    }
}
