package MightyLibrary.mightylib.main.procedures;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scenes.Scene;
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

    // Check OpenGL errors after each call (glGetError): helps to locate an error, but slows down rendering
    default boolean returnOpenGLErrorCheckState() {
        return false;
    }

    IProjectLoading returnIProjectLoading();

    // Return -1 as the newest version
    int returnShaderVersion();

    Resources.LoadingMethod returnResourcesLoadingMethod();

    int returnMaxNumberOfThreadsToUse();

    GLResources.GLResourceCreation returnGLResourceCreation();
}
