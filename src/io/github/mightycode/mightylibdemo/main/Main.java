package io.github.mightycode.mightylibdemo.main;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import io.github.mightycode.mightylib.main.*;
import io.github.mightycode.mightylib.main.monitors.MonitorSelectors;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
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

    // Test of the commands of a released game: RELEASE mode (no debug server), player access USER, cheats allowed
    private static final String RELEASE_PROPERTY = "demo.release";
    // Maximum access of the build (user, cheat, dev): DEV in debug mode by default
    private static final String MAX_COMMAND_ACCESS_PROPERTY = "demo.commands.max";

    public static EngineConfig createConfig() {
        EngineConfig config = createBaseConfig();

        if (Boolean.getBoolean(RELEASE_PROPERTY)) {
            config.setMode(EEngineMode.RELEASE)
                    .setCommandAccess(ECommandLevel.USER)
                    .setMaxCommandAccess(ECommandLevel.CHEAT);
        }

        String max = System.getProperty(MAX_COMMAND_ACCESS_PROPERTY);
        if (max != null)
            config.setMaxCommandAccess(ECommandLevel.Parse(max));

        return config;
    }

    private static EngineConfig createBaseConfig() {
        return new EngineConfig()
                .setStartScene(MenuScene::new)
                .setLoadingScene(LoadingSceneImplementation::new)
                // Screen of the window, tools/scenario.py overrides it to open the tests on another screen
                .setMonitorSelector(MonitorSelectors.Primary())
                .setDisplay(new DisplaySettings().setResizable(true))
                .setProjectName("Java-MightyLibrary")
                .setFps(70)
                .setTps(70)
                .setKeyboardLanguage(AZERTYKeyboardLanguage.getInstance())
                .setGainTreePath("sounds/sounds.gaintree")
                .setMode(EEngineMode.DEBUG)
                // The player turns the cheats on with "cheats on" (always on in DEV)
                .setCheatCommand(true)
                // Set to true to locate an OpenGL error, it slows down rendering
                .setOpenGLErrorCheck(false)
                .setProjectLoading(new ProjectLoading())
                //.setResourcesLoadingMethod(new Resources.AllResourcesMethod())
                .setResourcesLoadingMethod(new Resources.BatchResourcesMethod("init"));
    }
}
