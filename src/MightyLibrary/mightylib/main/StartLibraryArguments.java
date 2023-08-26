package MightyLibrary.mightylib.main;

import MightyLibrary.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import MightyLibrary.mightylib.inputs.keyboardlanguage.AZERTYKeyboardLanguage;
import org.joml.Vector2i;

public class StartLibraryArguments {
    public int fps = 60;
    public int tps = 60;

    public String projectName = "Java-MightyLibrary";

    public String projectIcon = null;

    public boolean fullscreen = false;

    public boolean admin = false;

    public KeyboardLanguage keyboardLanguage = AZERTYKeyboardLanguage.getInstance();

    public final IProjectLoading projectLoading;

    public final int windowWidth;
    public final int windowHeight;

    public final int windowVirtualWidth;
    public final int windowVirtualHeight;

    public StartLibraryArguments(IProjectLoading projectLoading, Vector2i windowSize, Vector2i windowVirtualSize){
        this.projectLoading = projectLoading;

        this.windowWidth = windowSize.x;
        this.windowHeight = windowSize.y;

        this.windowVirtualWidth = windowVirtualSize.x;
        this.windowVirtualHeight = windowVirtualSize.y;
    }
}
