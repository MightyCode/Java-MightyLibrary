package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.console.Console;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

/**
 * Writes lines of every type in the console at a regular pace, to try it while the log moves: the scroll anchor (a
 * view scrolled up stays), "console logs off|on", the selection, the indicator of the lines below, the colors.
 * Each tick of the feed writes one line of a type in turn: System.out, System.err, console message, a long line that
 * is cut at the width, a line with a tabulation. UP / DOWN (action select) speeds the feed up or slows it down,
 * ESCAPE goes back to the menu.
 * Check without a window: python tools/scenario.py "scene ConsoleFeedScene" wait-loaded "console open" sleep:3 "?console"
 */
public class ConsoleFeedScene extends Scene {
    private static final long[] INTERVALS_MILLISECONDS = {100, 250, 500, 1000, 2000};
    private static final String LONG_LINE = "A long line that has to be cut at the width of the console: the quick brown"
            + " fox jumps over the lazy dog, again and again, and the same goes for the rest of this very long sentence"
            + " that goes on and on until it is surely wider than any panel, even a very wide one.";

    private Text help;
    private int intervalIndex = 2;
    private long lastLine;
    private int counter;

    @Override
    public void launch(String[] args) {
        setClearColor(0.15f, 0.15f, 0.2f);
        Vector2f windowSize = new Vector2f(mainContext.getWindow().getInfo().getVirtualSizeRef());

        help = new Text();
        help.setFont("bahnschrift")
                .setFontSize(30)
                .setReference(EDirection.Up)
                .setAlignment(ETextAlignment.Center)
                .setPosition(new Vector2f(windowSize.x / 2, windowSize.y * 0.1f))
                .setColor(new Color4f(1f))
                .setText(" ");
        lastLine = System.nanoTime();
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (mainContext.getInputManager().inputPressed(ActionId.SELECT_UP))
            intervalIndex = Math.max(0, intervalIndex - 1);
        if (mainContext.getInputManager().inputPressed(ActionId.SELECT_DOWN))
            intervalIndex = Math.min(INTERVALS_MILLISECONDS.length - 1, intervalIndex + 1);

        help.setText("One line every " + INTERVALS_MILLISECONDS[intervalIndex] + " ms (UP: faster, DOWN: slower, ESCAPE: menu)");

        long now = System.nanoTime();
        if (now - lastLine >= INTERVALS_MILLISECONDS[intervalIndex] * 1_000_000L) {
            lastLine = now;
            writeLine();
        }
    }

    // One line of a type, the type changes at each line
    private void writeLine() {
        ++counter;
        switch (counter % 5) {
            case 0:
                System.out.println("-- Feed " + counter + ": program output (System.out)");
                break;
            case 1:
                System.err.println("-- Feed " + counter + ": program error (System.err)");
                break;
            case 2:
                Console console = engine().console();
                if (console != null)
                    console.print("Feed " + counter + ": message of the game");
                break;
            case 3:
                System.out.println("-- Feed " + counter + ": " + LONG_LINE);
                break;
            default:
                System.out.println("-- Feed " + counter + ":\tline\twith\ttabulations");
                break;
        }
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        help.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        help.unload();
        help = null;
    }
}
