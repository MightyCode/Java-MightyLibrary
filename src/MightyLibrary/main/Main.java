package MightyLibrary.main;

/**
 * Main class of the game.
 *
 * @author MightyCode
 * @version of the library : 0.1.2
 */
public class Main {

    public static boolean admin;

    /**
     * Window.
     * This global variable contains all of the main game structure.
     */
    public static Window window;

    /**
     * Run the game.
     */
    public static void main(String[] args) {
        admin = true;

        window = new Window();
        window.run();
    }
}
