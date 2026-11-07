package io.github.mightycode.mightylib.inputs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class GamepadManagerTest {

    // Gamepads plugged by the test, read like GLFW would
    private static class FakeSource implements IGamepadSource {
        final Map<Integer, boolean[]> buttons = new HashMap<>();
        final Map<Integer, float[]> axes = new HashMap<>();
        final Map<Integer, Boolean> mapped = new HashMap<>();
        IConnectionCallback callback;

        void plug(int id, boolean isMapped) {
            buttons.put(id, new boolean[Gamepad.BUTTONS]);
            float[] values = new float[Gamepad.AXES];
            values[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = -1;
            values[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = -1;
            axes.put(id, values);
            mapped.put(id, isMapped);
            if (callback != null)
                callback.changed(id, true);
        }

        void unplug(int id) {
            buttons.remove(id);
            axes.remove(id);
            mapped.remove(id);
            if (callback != null)
                callback.changed(id, false);
        }

        @Override public void setConnectionCallback(IConnectionCallback callback) { this.callback = callback; }
        @Override public boolean isPresent(int id) { return buttons.containsKey(id); }
        @Override public boolean isGamepad(int id) { return mapped.getOrDefault(id, false); }
        @Override public String getName(int id) { return "Fake " + id; }
        @Override public String getGuid(int id) { return "guid" + id; }
        @Override public boolean addMappings(String mappings) { return true; }
        @Override public void free() { callback = null; }

        @Override
        public boolean read(int id, boolean[] outButtons, float[] outAxes) {
            if (!buttons.containsKey(id) || !mapped.get(id))
                return false;
            System.arraycopy(buttons.get(id), 0, outButtons, 0, Gamepad.BUTTONS);
            System.arraycopy(axes.get(id), 0, outAxes, 0, Gamepad.AXES);
            return true;
        }
    }

    private FakeSource source;
    private GamepadManager manager;

    @BeforeEach
    void setUp() {
        source = new FakeSource();
        manager = new GamepadManager(source);
    }

    @Test
    void gamepadsAlreadyPluggedAreFoundAtInit() {
        source.plug(0, true);
        manager.init();
        assertTrue(manager.getConnectedGamepads().isEmpty(), "Applied at the next postUpdate");

        manager.postUpdate();
        Gamepad gamepad = manager.getGamepad(0);
        assertTrue(gamepad.isConnected());
        assertTrue(gamepad.connectedThisTick());
        assertEquals("Fake 0", gamepad.getName());
        assertEquals(0, gamepad.getPlayer());
        assertSame(gamepad, manager.getPlayerGamepad(0));

        manager.postUpdate();
        assertFalse(gamepad.connectedThisTick(), "The flag lasts one tick");
    }

    @Test
    void buttonsArePressedHeldAndReleased() {
        manager.init();
        source.plug(0, true);
        manager.postUpdate();

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        manager.postUpdate();
        Gamepad gamepad = manager.getGamepad(0);
        assertTrue(gamepad.getButton(GLFW_GAMEPAD_BUTTON_A));
        assertTrue(gamepad.buttonPressed(GLFW_GAMEPAD_BUTTON_A));
        assertTrue(manager.buttonPressed(GLFW_GAMEPAD_BUTTON_A, GamepadManager.ANY_PLAYER));
        assertTrue(manager.buttonPressed(GLFW_GAMEPAD_BUTTON_A, 0));
        assertFalse(manager.buttonPressed(GLFW_GAMEPAD_BUTTON_A, 1));

        manager.postUpdate();
        assertTrue(gamepad.getButton(GLFW_GAMEPAD_BUTTON_A));
        assertFalse(gamepad.buttonPressed(GLFW_GAMEPAD_BUTTON_A));

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        manager.postUpdate();
        assertTrue(gamepad.buttonReleased(GLFW_GAMEPAD_BUTTON_A));
        assertTrue(manager.buttonReleased(GLFW_GAMEPAD_BUTTON_A, GamepadManager.ANY_PLAYER));
    }

    @Test
    void playersFollowTheConnectionOrderAndAreKeptOnReconnection() {
        manager.init();
        source.plug(3, true);
        source.plug(1, true);
        manager.postUpdate();
        assertEquals(0, manager.getGamepad(3).getPlayer());
        assertEquals(1, manager.getGamepad(1).getPlayer());

        source.unplug(3);
        manager.postUpdate();
        assertNull(manager.getPlayerGamepad(0));
        assertTrue(manager.getGamepad(3).disconnectedThisTick());
        assertEquals(0, manager.getGamepad(3).getPlayer(), "A disconnected gamepad keeps its last player");

        source.plug(3, true);
        manager.postUpdate();
        assertSame(manager.getGamepad(3), manager.getPlayerGamepad(0), "Takes back its player");
    }

    @Test
    void newGamepadTakesTheLowestFreePlayer() {
        manager.init();
        source.plug(0, true);
        source.plug(1, true);
        manager.postUpdate();

        source.unplug(0);
        manager.postUpdate();
        source.plug(2, true);
        manager.postUpdate();
        assertEquals(0, manager.getGamepad(2).getPlayer());

        // Its player is taken : the first one gets a new player
        source.plug(0, true);
        manager.postUpdate();
        assertEquals(2, manager.getGamepad(0).getPlayer());
    }

    @Test
    void assignPlayerSwapsThePlayers() {
        manager.init();
        source.plug(0, true);
        source.plug(1, true);
        manager.postUpdate();

        manager.assignPlayer(manager.getGamepad(1), 0);
        assertEquals(0, manager.getGamepad(1).getPlayer());
        assertEquals(1, manager.getGamepad(0).getPlayer());
        assertSame(manager.getGamepad(0), manager.getPlayerGamepad(1));
    }

    @Test
    void noAutoAssignLeavesGamepadsWithoutPlayer() {
        manager.setAutoAssign(false);
        manager.init();
        source.plug(0, true);
        manager.postUpdate();

        Gamepad gamepad = manager.getGamepad(0);
        assertEquals(Gamepad.NO_PLAYER, gamepad.getPlayer());
        assertNull(manager.getPlayerGamepad(0));

        manager.assignPlayer(gamepad, 2);
        assertSame(gamepad, manager.getPlayerGamepad(2));
        assertEquals(3, manager.getPlayerSlots());

        manager.unassignPlayer(gamepad);
        assertNull(manager.getPlayerGamepad(2));
    }

    @Test
    void unmappedJoystickIsConnectedWithoutPlayer() {
        manager.init();
        source.plug(0, false);
        manager.postUpdate();

        Gamepad joystick = manager.getGamepad(0);
        assertTrue(joystick.isConnected());
        assertFalse(joystick.isMapped());
        assertEquals(Gamepad.NO_PLAYER, joystick.getPlayer());
        assertThrows(IllegalArgumentException.class, () -> manager.assignPlayer(joystick, 0));
    }

    @Test
    void disconnectionReleasesTheHeldButtons() {
        manager.init();
        source.plug(0, true);
        manager.postUpdate();
        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_B] = true;
        manager.postUpdate();

        source.unplug(0);
        manager.postUpdate();
        assertFalse(manager.getButton(GLFW_GAMEPAD_BUTTON_B, GamepadManager.ANY_PLAYER));
        assertTrue(manager.buttonReleased(GLFW_GAMEPAD_BUTTON_B, GamepadManager.ANY_PLAYER));

        manager.postUpdate();
        assertFalse(manager.buttonReleased(GLFW_GAMEPAD_BUTTON_B, GamepadManager.ANY_PLAYER));
    }

    @Test
    void lostReadIsSeenAsDisconnection() {
        manager.init();
        source.plug(0, true);
        manager.postUpdate();

        // Gone without callback
        source.callback = null;
        source.unplug(0);
        manager.postUpdate();
        assertFalse(manager.getGamepad(0).isConnected());
        assertTrue(manager.getConnectedGamepads().isEmpty());
    }

    @Test
    void sticksUseARadialDeadZone() {
        manager.init();
        source.plug(0, true);
        manager.postUpdate();
        Gamepad gamepad = manager.getGamepad(0);

        float[] axes = source.axes.get(0);
        axes[GLFW_GAMEPAD_AXIS_LEFT_X] = 0.15f;
        axes[GLFW_GAMEPAD_AXIS_LEFT_Y] = -0.1f;
        manager.postUpdate();
        assertEquals(0, gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X));
        assertEquals(0, gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_Y));
        assertFalse(gamepad.isAnyInputActive());

        axes[GLFW_GAMEPAD_AXIS_LEFT_X] = 1;
        axes[GLFW_GAMEPAD_AXIS_LEFT_Y] = 0;
        manager.postUpdate();
        assertEquals(1, gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X), 1e-6);

        // Halfway between the dead zone and the border
        axes[GLFW_GAMEPAD_AXIS_LEFT_X] = 0;
        axes[GLFW_GAMEPAD_AXIS_LEFT_Y] = -0.6f;
        manager.postUpdate();
        assertEquals(-0.5f, gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_Y), 1e-6);

        // A raw diagonal beyond 1 is clamped to a length of 1
        axes[GLFW_GAMEPAD_AXIS_LEFT_X] = 1;
        axes[GLFW_GAMEPAD_AXIS_LEFT_Y] = 1;
        manager.postUpdate();
        float x = gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X);
        float y = gamepad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_Y);
        assertEquals(1, Math.sqrt(x * x + y * y), 1e-6);
    }

    @Test
    void triggersGoFromZeroToOne() {
        manager.init();
        source.plug(0, true);
        manager.postUpdate();
        Gamepad gamepad = manager.getGamepad(0);
        assertEquals(0, gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER));

        source.axes.get(0)[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = 1;
        manager.postUpdate();
        assertEquals(1, gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER), 1e-6);

        // 0.55 once normalized, 0.5 after the dead zone of 0.1
        source.axes.get(0)[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = 0.1f;
        manager.postUpdate();
        assertEquals(0.5f, gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER), 1e-6);
    }

    @Test
    void anyPlayerAxisIsTheFarthestFromZero() {
        manager.init();
        source.plug(0, true);
        source.plug(1, true);
        manager.postUpdate();

        source.axes.get(0)[GLFW_GAMEPAD_AXIS_LEFT_X] = 0.6f;
        source.axes.get(1)[GLFW_GAMEPAD_AXIS_LEFT_X] = -1f;
        manager.postUpdate();
        assertEquals(-1, manager.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X, GamepadManager.ANY_PLAYER), 1e-6);
        assertEquals(0.5f, manager.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X, 0), 1e-6);
    }

    @Test
    void anyPlayerPressIsIgnoredWhileAnotherGamepadHoldsTheButton() {
        manager.init();
        source.plug(0, true);
        source.plug(1, true);
        manager.postUpdate();

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = true;
        manager.postUpdate();
        source.buttons.get(1)[GLFW_GAMEPAD_BUTTON_A] = true;
        manager.postUpdate();
        assertFalse(manager.buttonPressed(GLFW_GAMEPAD_BUTTON_A, GamepadManager.ANY_PLAYER));
        assertTrue(manager.buttonPressed(GLFW_GAMEPAD_BUTTON_A, 1));

        source.buttons.get(0)[GLFW_GAMEPAD_BUTTON_A] = false;
        manager.postUpdate();
        assertFalse(manager.buttonReleased(GLFW_GAMEPAD_BUTTON_A, GamepadManager.ANY_PLAYER), "Still held by the second one");
    }

    @Test
    void virtualGamepadsAreSimulated() {
        manager.init();
        Gamepad first = manager.connectVirtual();
        Gamepad second = manager.connectVirtual();
        assertNotSame(first, second, "A pending slot is not given twice");
        manager.postUpdate();

        assertTrue(first.isVirtual());
        assertEquals(0, first.getPlayer());
        assertEquals(1, second.getPlayer());

        second.simulateButton(GLFW_GAMEPAD_BUTTON_X, 2);
        second.simulateAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, 1, InputSimulation.UNTIL_RELEASE);
        assertFalse(second.getButton(GLFW_GAMEPAD_BUTTON_X), "Applied at the next postUpdate");

        manager.postUpdate();
        assertTrue(second.buttonPressed(GLFW_GAMEPAD_BUTTON_X));
        assertEquals(1, second.getAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER), 1e-6);
        manager.postUpdate();
        assertTrue(second.getButton(GLFW_GAMEPAD_BUTTON_X));
        manager.postUpdate();
        assertTrue(second.buttonReleased(GLFW_GAMEPAD_BUTTON_X));
        assertEquals(1, second.getAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER), 1e-6);

        second.simulateAxisRelease(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER);
        manager.postUpdate();
        assertEquals(0, second.getAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER));

        manager.disconnectVirtual(first);
        manager.postUpdate();
        assertFalse(first.isConnected());
        assertSame(second, manager.getPlayerGamepad(1));
        assertThrows(IllegalArgumentException.class, () -> manager.disconnectVirtual(manager.getGamepad(0)));
    }

    @Test
    void simulatedValuesAreChecked() {
        Gamepad gamepad = manager.getGamepad(0);
        assertThrows(IllegalArgumentException.class, () -> gamepad.simulateAxis(GLFW_GAMEPAD_AXIS_LEFT_TRIGGER, -0.5f, 1));
        assertThrows(IllegalArgumentException.class, () -> gamepad.simulateAxis(GLFW_GAMEPAD_AXIS_LEFT_X, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> gamepad.simulateButton(Gamepad.BUTTONS, 1));
        assertThrows(IllegalArgumentException.class, () -> gamepad.setStickDeadZone(1));
    }
}
