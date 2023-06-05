package MightyLibrary.project.main;

import MightyLibrary.mightylib.main.IProjectLoading;
import MightyLibrary.mightylib.main.StartLibraryArguments;
import MightyLibrary.mightylib.main.MainLoop;
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
        IProjectLoading projectLoading = new ProjectLoading();
        StartLibraryArguments startArguments = new StartLibraryArguments(
                projectLoading,
                new Vector2i(1280, 720),
                new Vector2i(1280, 720));

        startArguments.admin = true;

        MainLoop.run(startArguments);
    }
}
