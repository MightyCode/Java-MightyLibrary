package MightyLibrary.project.main;

/**
 * Main class of the project.
 *
 * @author MightyCode
 * @version of the library : 0.1.3
 */
public class Main {

    public static boolean admin;

    /**
     * Run the game.
     */
    public static void main(String[] args) {
        admin = true;

        MainLoop mainLoop = new MainLoop();
        mainLoop.run();
    }
}
