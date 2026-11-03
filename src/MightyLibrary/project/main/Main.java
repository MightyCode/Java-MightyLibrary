package MightyLibrary.project.main;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import MightyLibrary.mightylib.main.EngineConfig;
import MightyLibrary.mightylib.main.MainLoop;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.project.scenes.MenuScene;

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
        GLResources.GLResourceCreation creation = new GLResources.GLResourceCreation();
        creation.MILLISECONDS_ALLOWED_TO_PROCESS_PER_FRAME = 10;

        return new EngineConfig()
                .setStartScene(MenuScene::new)
                .setWindowSize(1280, 720)
                .setVirtualSize(1280, 720)
                .setProjectName("Java-MightyLibrary")
                .setFps(70)
                .setTps(70)
                .setKeyboardLanguage(AZERTYKeyboardLanguage.getInstance())
                .setGainTreePath("sounds/sounds.gaintree")
                .setAdmin(true)
                // Set to true to locate an OpenGL error, it slows down rendering
                .setOpenGLErrorCheck(false)
                .setProjectLoading(new ProjectLoading())
                //.setResourcesLoadingMethod(new Resources.AllResourcesMethod())
                .setResourcesLoadingMethod(new Resources.BatchResourcesMethod("init"))
                .setGLResourceCreation(creation);
    }
}
