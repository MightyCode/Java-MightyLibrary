package MightyLibrary.project.main;

import MightyLibrary.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import MightyLibrary.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import MightyLibrary.mightylib.main.procedures.IProjectLoading;
import MightyLibrary.mightylib.main.procedures.IStartLibraryProcedure;
import MightyLibrary.mightylib.main.MainLoop;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.project.scenes.MenuScene;
import org.joml.Vector2i;

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
        MainLoop.run(new LibraryLoading());
    }

    public static class LibraryLoading implements IStartLibraryProcedure {

        @Override
        public Scene returnStartScene() {
            return new MenuScene();
        }

        @Override
        public Vector2i returnSceneSize() {
            return new Vector2i(1280, 720);
        }

        @Override
        public Vector2i returnVirtualSceneSize() {
            return new Vector2i(1280, 720);
        }

        @Override
        public String returnProjectName() {
            return "Java-MightyLibrary";
        }

        @Override
        public int returnFPS() {
            return 240;
        }

        @Override
        public int returnTPS() {
            return 240;
        }

        @Override
        public String returnIconName() {
            return null;
        }

        @Override
        public boolean returnFullscreenState() {
            return false;
        }

        @Override
        public KeyboardLanguage returnDefaultKeyboardLanguage() {
            return AZERTYKeyboardLanguage.getInstance();
        }

        @Override
        public String returnGainTreePath() {
            return "sounds/sounds.gaintree";
        }

        @Override
        public boolean returnAdminState() {
            return true;
        }

        @Override
        public IProjectLoading returnIProjectLoading() {
            return  new ProjectLoading();
        }

        @Override
        public Resources.LoadingMethod returnResourcesLoadingMethod() {
            //return new Resources.AllResourcesMethod();
            return new Resources.BatchResourcesMethod("init");
        }

        // Not important
        @Override
        // Return -1 as the newest version
        public int returnShaderVersion() {
            return -1;
        }
    }
}
