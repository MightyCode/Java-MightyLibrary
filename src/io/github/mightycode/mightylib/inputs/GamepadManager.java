package io.github.mightycode.mightylib.inputs;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

import static org.lwjgl.glfw.GLFW.GLFW_JOYSTICK_LAST;

/**
 * All the gamepads, shared by every context (gamepads don't belong to a window).
 *
 * Slots : one per GLFW joystick id (0 to GLFW_JOYSTICK_LAST), then the virtual gamepads (commands, tests,
 * several players with a single real gamepad).
 * Players : a connected mapped gamepad takes the lowest free player (auto assignment, can be disabled for a
 * "press a button to join" screen, then assignPlayer). A gamepad that reconnects takes back its player if it
 * is still free. A disconnected gamepad keeps its last player in getPlayer() but frees the player slot.
 *
 * Connections are applied in postUpdate, with the state of the next tick : a tick sees a stable set of gamepads.
 * Main thread only.
 */
public class GamepadManager {
    // Any connected gamepad, assigned or not
    public static final int ANY_PLAYER = -1;

    // Optional SDL_GameControllerDB file of the project, read at startup
    public static final String MAPPINGS_FILE = "resources/gamecontrollerdb.txt";
    public static final int REAL_SLOTS = GLFW_JOYSTICK_LAST + 1;
    public static final int VIRTUAL_SLOTS = 8;

    private static class ConnectionEvent {
        private final int id;
        private final boolean connected;

        private ConnectionEvent(int id, boolean connected) {
            this.id = id;
            this.connected = connected;
        }
    }

    private final IGamepadSource source;
    private final Gamepad[] gamepads;
    // Gamepad of each player, null if free
    private final List<Gamepad> players;
    private final Queue<ConnectionEvent> pendingEvents;

    private final List<Gamepad> connected;
    private final List<Gamepad> connectedView;

    private boolean autoAssign;
    private boolean initialized;

    private final boolean[] rawButtons = new boolean[Gamepad.BUTTONS];
    private final float[] rawAxes = new float[Gamepad.AXES];

    public GamepadManager() {
        this(new GlfwGamepadSource());
    }

    GamepadManager(IGamepadSource source) {
        this.source = source;

        gamepads = new Gamepad[REAL_SLOTS + VIRTUAL_SLOTS];
        for (int id = 0; id < gamepads.length; ++id)
            gamepads[id] = new Gamepad(id, id >= REAL_SLOTS);

        players = new ArrayList<>();
        pendingEvents = new ArrayDeque<>();
        connected = new ArrayList<>();
        connectedView = Collections.unmodifiableList(connected);
        autoAssign = true;
    }

    // Listen to the connections and find the gamepads already connected (seen from the next tick)
    public void init() {
        if (initialized)
            return;

        initialized = true;
        source.setConnectionCallback((id, isConnected) -> {
            if (id >= 0 && id < REAL_SLOTS)
                pendingEvents.add(new ConnectionEvent(id, isConnected));
        });

        for (int id = 0; id < REAL_SLOTS; ++id) {
            if (source.isPresent(id))
                pendingEvents.add(new ConnectionEvent(id, true));
        }
    }

    // Compute the gamepads of the next tick
    public void postUpdate() {
        for (Gamepad gamepad : gamepads)
            gamepad.clearConnectionFlags();

        ConnectionEvent event;
        while ((event = pendingEvents.poll()) != null) {
            if (event.connected)
                onConnected(gamepads[event.id]);
            else
                onDisconnected(gamepads[event.id]);
        }

        for (Gamepad gamepad : gamepads) {
            if (!gamepad.isConnected()) {
                // Last update after a disconnection : everything is released
                if (gamepad.disconnectedThisTick())
                    gamepad.update(null, null);
                continue;
            }

            if (gamepad.isVirtual() || !gamepad.isMapped()) {
                gamepad.update(null, null);
            } else if (source.read(gamepad.getId(), rawButtons, rawAxes)) {
                gamepad.update(rawButtons, rawAxes);
            } else {
                // Lost without callback (yet) : seen as disconnected
                onDisconnected(gamepad);
                gamepad.update(null, null);
            }
        }
    }

    private void onConnected(Gamepad gamepad) {
        if (gamepad.isConnected())
            return;

        if (gamepad.isVirtual())
            gamepad.connect("Virtual gamepad " + (gamepad.getId() - REAL_SLOTS), "virtual", true);
        else
            gamepad.connect(source.getName(gamepad.getId()), source.getGuid(gamepad.getId()), source.isGamepad(gamepad.getId()));

        connected.add(gamepad);

        if (gamepad.isMapped() && autoAssign) {
            int lastPlayer = gamepad.getPlayer();
            if (lastPlayer != Gamepad.NO_PLAYER && getPlayerGamepad(lastPlayer) == null)
                assignPlayer(gamepad, lastPlayer);
            else
                assignPlayer(gamepad, firstFreePlayer());
        } else {
            gamepad.player = Gamepad.NO_PLAYER;
        }

        if (gamepad.isMapped()) {
            System.out.println("--Gamepad connected : " + gamepad);
        } else {
            System.out.println("--Joystick " + gamepad.getId() + " connected without gamepad mapping : " + gamepad.getName()
                    + ", GUID " + gamepad.getGuid() + " (add an SDL_GameControllerDB line with GamepadManager.addMappings)");
        }
    }

    private void onDisconnected(Gamepad gamepad) {
        if (!gamepad.isConnected())
            return;

        gamepad.disconnect();
        connected.remove(gamepad);

        int player = gamepad.getPlayer();
        if (player != Gamepad.NO_PLAYER && player < players.size() && players.get(player) == gamepad)
            players.set(player, null);

        System.out.println("--Gamepad disconnected : " + gamepad);
    }

    private int firstFreePlayer() {
        for (int player = 0; player < players.size(); ++player) {
            if (players.get(player) == null)
                return player;
        }

        return players.size();
    }

    /**
     * Give a player to a connected mapped gamepad. A gamepad already on this player loses it (it takes the
     * former player of the given gamepad if any, else none).
     */
    public void assignPlayer(Gamepad gamepad, int player) {
        if (player < 0)
            throw new IllegalArgumentException("Player must be positive : " + player);
        if (!gamepad.isConnected() || !gamepad.isMapped())
            throw new IllegalArgumentException("Only a connected gamepad with a mapping can be assigned : " + gamepad);

        while (players.size() <= player)
            players.add(null);

        int formerPlayer = players.contains(gamepad) ? gamepad.getPlayer() : Gamepad.NO_PLAYER;
        Gamepad current = players.get(player);
        if (current == gamepad)
            return;

        if (formerPlayer != Gamepad.NO_PLAYER)
            players.set(formerPlayer, current);

        if (current != null)
            current.player = formerPlayer;

        players.set(player, gamepad);
        gamepad.player = player;
    }

    public void unassignPlayer(Gamepad gamepad) {
        int player = gamepad.getPlayer();
        if (player != Gamepad.NO_PLAYER && player < players.size() && players.get(player) == gamepad)
            players.set(player, null);

        gamepad.player = Gamepad.NO_PLAYER;
    }

    // Gamepad of a player, null if none is connected for it
    public Gamepad getPlayerGamepad(int player) {
        if (player < 0 || player >= players.size())
            return null;

        return players.get(player);
    }

    // Number of player slots (the highest assigned player + 1), some can be free
    public int getPlayerSlots() {
        return players.size();
    }

    public boolean isAutoAssign() { return autoAssign; }

    // When disabled, new gamepads get no player : the game assigns them (join screen)
    public void setAutoAssign(boolean autoAssign) {
        this.autoAssign = autoAssign;
    }

    // Slot by id, connected or not, null if the id is out of range
    public Gamepad getGamepad(int id) {
        if (id < 0 || id >= gamepads.length)
            return null;

        return gamepads[id];
    }

    // Connected gamepads (with or without mapping) in their connection order, not to be modified
    public List<Gamepad> getConnectedGamepads() {
        return connectedView;
    }

    // Connect a virtual gamepad from the next tick, null if all the virtual slots are used
    public Gamepad connectVirtual() {
        for (int id = REAL_SLOTS; id < gamepads.length; ++id) {
            Gamepad gamepad = gamepads[id];
            if (!gamepad.isConnected() && !isPending(id)) {
                pendingEvents.add(new ConnectionEvent(id, true));
                return gamepad;
            }
        }

        return null;
    }

    public void disconnectVirtual(Gamepad gamepad) {
        if (!gamepad.isVirtual())
            throw new IllegalArgumentException("Only a virtual gamepad can be disconnected : " + gamepad);

        pendingEvents.add(new ConnectionEvent(gamepad.getId(), false));
    }

    private boolean isPending(int id) {
        for (ConnectionEvent event : pendingEvents) {
            if (event.id == id)
                return true;
        }

        return false;
    }

    // Add SDL_GameControllerDB mappings (lines of gamecontrollerdb.txt), for gamepads unknown to GLFW
    public boolean addMappings(String mappings) {
        boolean added = source.addMappings(mappings);
        if (!added)
            System.err.println("--Invalid gamepad mappings");

        return added;
    }

    /**
     * Add the mappings of a gamecontrollerdb.txt of the project. False when the file doesn't exist (nothing to do,
     * not an error), can't be read or holds no valid mapping.
     */
    public boolean loadMappingsFile(Path file) {
        if (!Files.isRegularFile(file))
            return false;

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("--Gamepad mappings file " + file + " can't be read : " + e.getMessage());
            return false;
        }

        boolean added = addMappings(content);
        if (added)
            System.out.println("--Gamepad mappings loaded from " + file);

        return added;
    }

    /// Queries by player, used by the actions. ANY_PLAYER : any connected gamepad.

    public boolean getButton(int button, int player) {
        if (player != ANY_PLAYER) {
            Gamepad gamepad = getPlayerGamepad(player);
            return gamepad != null && gamepad.getButton(button);
        }

        for (Gamepad gamepad : connected) {
            if (gamepad.getButton(button))
                return true;
        }

        return false;
    }

    public boolean buttonPressed(int button, int player) {
        if (player != ANY_PLAYER) {
            Gamepad gamepad = getPlayerGamepad(player);
            return gamepad != null && gamepad.buttonPressed(button);
        }

        // Pressed only if no other gamepad already held it
        boolean pressed = false;
        for (Gamepad gamepad : connected) {
            if (gamepad.buttonPressed(button))
                pressed = true;
            else if (gamepad.getButton(button))
                return false;
        }

        return pressed;
    }

    public boolean buttonReleased(int button, int player) {
        if (player != ANY_PLAYER) {
            Gamepad gamepad = getPlayerGamepad(player);
            return gamepad != null && gamepad.buttonReleased(button);
        }

        // Released by the last gamepad that held it (a disconnected one releases everything)
        boolean released = false;
        for (Gamepad gamepad : gamepads) {
            if (!isUpdated(gamepad))
                continue;
            if (gamepad.getButton(button))
                return false;
            if (gamepad.buttonReleased(button))
                released = true;
        }

        return released;
    }

    // Value of the axis, for ANY_PLAYER the one farthest from 0
    public float getAxis(int axis, int player) {
        return axisValue(axis, player, false);
    }

    // Value of the axis at the previous tick, for ANY_PLAYER the one farthest from 0
    public float getOldAxis(int axis, int player) {
        return axisValue(axis, player, true);
    }

    private float axisValue(int axis, int player, boolean old) {
        if (player != ANY_PLAYER) {
            Gamepad gamepad = getPlayerGamepad(player);
            if (gamepad == null)
                return 0;
            return old ? gamepad.getOldAxis(axis) : gamepad.getAxis(axis);
        }

        float result = 0;
        for (Gamepad gamepad : gamepads) {
            if (!isUpdated(gamepad))
                continue;
            float value = old ? gamepad.getOldAxis(axis) : gamepad.getAxis(axis);
            if (Math.abs(value) > Math.abs(result))
                result = value;
        }

        return result;
    }

    // State computed for the current tick : connected, or released by its disconnection
    private static boolean isUpdated(Gamepad gamepad) {
        return gamepad.isConnected() || gamepad.disconnectedThisTick();
    }

    public void unload() {
        if (initialized)
            source.free();

        initialized = false;
    }
}
