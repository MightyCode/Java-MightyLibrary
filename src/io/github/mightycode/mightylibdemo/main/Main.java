package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import io.github.mightycode.mightylib.main.EEngineMode;
import io.github.mightycode.mightylib.main.EngineConfig;
import io.github.mightycode.mightylib.main.MainLoop;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylibdemo.scenes.MenuScene;
import io.github.mightycode.mightylibdemo.scenes.loadingScenes.LoadingSceneImplementation;

/**
 * Main class of the project.
 *
 * @author MightyCode
 * @version of the library : 0.1.3
 */
public class Main {

    /**
     * Run the game.
     */
    public static void main(String[] args) {
        System.exit(MainLoop.run(createConfig()));
    }

    public static EngineConfig createConfig() {
        return new EngineConfig()
                .setStartScene(MenuScene::new)
                .setLoadingScene(LoadingSceneImplementation::new)
                .setWindowSize(1280, 720)
                // Screen of the window, tools/scenario.py overrides it to open the tests on another screen
                .setMonitorSelector(MonitorSelectors.Primary())
                .setVirtualSize(1280, 720)
                .setProjectName("Java-MightyLibrary")
                .setFps(70)
                .setTps(70)
                .setKeyboardLanguage(AZERTYKeyboardLanguage.getInstance())
                .setGainTreePath("sounds/sounds.gaintree")
                .setMode(EEngineMode.DEBUG)
                // Set to true to locate an OpenGL error, it slows down rendering
                .setOpenGLErrorCheck(false)
                .setProjectLoading(new ProjectLoading())
                //.setResourcesLoadingMethod(new Resources.AllResourcesMethod())
                .setResourcesLoadingMethod(new Resources.BatchResourcesMethod("init"));
    }
}
