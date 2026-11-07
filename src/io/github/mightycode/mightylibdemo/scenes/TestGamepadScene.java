package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.Gamepad;
import io.github.mightycode.mightylib.inputs.GamepadManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;

import static org.lwjgl.glfw.GLFW.*;

/**
 * One square per player (up to 4) : left stick moves it, right stick moves it faster, right trigger makes it
 * bigger, A turns it white. The text lists the connected gamepads with their buttons and axes.
 * BACK (or ESCAPE) returns to the menu. Several players with one real gamepad : the gamepad connect command.
 */
public class TestGamepadScene extends Scene {
    private static final int PLAYERS = 4;
    private static final float SPEED = 400;
    private static final float SIZE = 60;

    private final Color4f[] colors = { ColorList.Red(), ColorList.Green(), ColorList.Blue(), ColorList.Yellow() };

    private RectangleRenderer[] squares;
    private Vector2f[] positions;
    private Text info;
    private String shownInfo;

    private final Vector2f stick = new Vector2f();

    @Override
    public void launch(String[] args) {
        setClearColor(0.15f, 0.15f, 0.2f);

        Vector2i size = mainContext.getWindow().getInfo().getVirtualSizeCopy();
        squares = new RectangleRenderer[PLAYERS];
        positions = new Vector2f[PLAYERS];
        for (int player = 0; player < PLAYERS; ++player) {
            squares[player] = new RectangleRenderer("colorShape2D");
            squares[player].setColorMode(colors[player]);
            squares[player].setSizePix(SIZE, SIZE);
            positions[player] = new Vector2f(size.x * (player + 1) / (PLAYERS + 1f), size.y * 0.7f);
        }

        info = new Text();
        info.setFont("bahnschrift")
                .setFontSize(22)
                .setReference(EDirection.LeftUp)
                .setColor(ColorList.White())
                .setPosition(new Vector2f(20, 20));
        shownInfo = null;
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
            return;
        }

        GamepadManager gamepads = mainContext.getInputManager().getGamepadManager();
        Vector2i size = mainContext.getWindow().getInfo().getVirtualSizeCopy();

        for (int player = 0; player < PLAYERS; ++player) {
            Gamepad gamepad = gamepads.getPlayerGamepad(player);
            if (gamepad == null)
                continue;

            gamepad.getLeftStick(stick).mul(SPEED * GameTime.DeltaTime());
            positions[player].add(stick);
            gamepad.getRightStick(stick).mul(SPEED * 3 * GameTime.DeltaTime());
            positions[player].add(stick);
            positions[player].x = Math.max(0, Math.min(size.x, positions[player].x));
            positions[player].y = Math.max(0, Math.min(size.y, positions[player].y));

            float scale = 1 + 2 * gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER);
            squares[player].setSizePix(SIZE * scale, SIZE * scale);
            squares[player].setPosition(new Vector2f(positions[player]).sub(SIZE * scale / 2, SIZE * scale / 2));

            if (gamepad.buttonPressed(GLFW_GAMEPAD_BUTTON_A))
                squares[player].setColorMode(ColorList.White());
            else if (gamepad.buttonReleased(GLFW_GAMEPAD_BUTTON_A))
                squares[player].setColorMode(colors[player]);
        }

        String text = describe(gamepads);
        if (!text.equals(shownInfo)) {
            info.setText(text);
            shownInfo = text;
        }
    }

    private static String describe(GamepadManager gamepads) {
        StringBuilder builder = new StringBuilder();
        if (gamepads.getConnectedGamepads().isEmpty())
            builder.append("No gamepad connected");

        for (Gamepad gamepad : gamepads.getConnectedGamepads()) {
            builder.append(gamepad.getId()).append(" ").append(gamepad.getName());
            if (!gamepad.isMapped()) {
                builder.append(" : no mapping\n");
                continue;
            }

            builder.append(gamepad.getPlayer() == Gamepad.NO_PLAYER ? " : no player" : " : player " + gamepad.getPlayer());
            builder.append("\n    ");
            for (int button = 0; button < Gamepad.BUTTONS; ++button) {
                if (gamepad.getButton(button))
                    builder.append(Gamepad.buttonName(button)).append(" ");
            }

            for (int axis = 0; axis < Gamepad.AXES; ++axis) {
                float value = gamepad.getAxis(axis);
                if (value != 0)
                    builder.append(Gamepad.axisName(axis)).append("=").append(Math.round(value * 100) / 100f).append(" ");
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        GamepadManager gamepads = mainContext.getInputManager().getGamepadManager();
        for (int player = 0; player < PLAYERS; ++player) {
            if (gamepads.getPlayerGamepad(player) != null)
                squares[player].display();
        }

        info.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (squares != null) {
            for (RectangleRenderer square : squares)
                square.unload();
            info.unload();
        }
    }
}
