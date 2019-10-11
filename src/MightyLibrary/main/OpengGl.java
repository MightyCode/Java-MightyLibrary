package MightyLibrary.main;

/**
 * Main class of the game.
 *
 * @author MightyCode
 * @version of the current game developed: 0.3.4
 */
public class OpengGl {

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
