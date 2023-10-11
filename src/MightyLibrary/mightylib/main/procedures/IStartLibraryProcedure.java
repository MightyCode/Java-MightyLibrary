package MightyLibrary.mightylib.main.procedures;

import MightyLibrary.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import MightyLibrary.mightylib.scene.Scene;
import org.joml.Vector2i;

public interface IStartLibraryProcedure {
   Scene returnStartScene();

   Vector2i returnSceneSize();
   Vector2i returnVirtualSceneSize();

    String returnProjectName();
    int returnFPS();
    int returnTPS();
    String returnIconName();
    boolean returnFullscreenState();
    KeyboardLanguage returnDefaultKeyboardLanguage();

    String returnGainTreePath();

    boolean returnAdminState();

    IProjectLoading returnIProjectLoading();
}
