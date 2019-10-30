package MightyLibrary.main;

/**
 * Main class of the game.
 *
 * @author MightyCode
 * @version of the library : 0.1.2
 */
public class Main {

    public static boolean admin;

    public static MainLoop mainLoop;

    /**
     * Run the game.
     */
    public static void main(String[] args) {
        admin = true;

        mainLoop = new MainLoop();
        mainLoop.run();
    }
}
