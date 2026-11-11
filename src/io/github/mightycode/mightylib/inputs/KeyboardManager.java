package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.main.WindowInfo;

import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * This class is the keyboard manager.
 *
 * @author MightyCode
 * @version 1.0
 */
public class KeyboardManager {

    private static final int KEYS = 348;
    private static final int BEGIN_KEYS = 32;

    private static final int MIN_LANGUAGE_KEY = GLFW_KEY_A;
    private static final int MAX_LANGUAGE_KEY = GLFW_KEY_Z;

    private final WindowInfo windowInfo;
    private final boolean[] state = new boolean[KEYS];
    private final boolean[] oldState = new boolean[KEYS];

    private boolean capsLockActivated;

    private int numberKeyPressed;

    private int mainKey;

    private final ArrayList<Integer> wasMain;

    private KeyboardLanguage language;

    // Text typed during the tick, given by the system (keyboard layout, dead keys and IME already applied)
    private final StringBuilder incomingText;
    private String typedText;

    // Keys pressed or repeated during the tick, in order (text editing)
    private final ArrayList<KeyEvent> incomingKeyEvents;
    private List<KeyEvent> keyEvents;

    // Simulated keys and text (commands, scripts, later replays), applied at the next postUpdate like real inputs
    private final InputSimulation keySimulation;
    private final StringBuilder pendingSimulatedText;
    private final boolean[] oldSimulated = new boolean[KEYS];

    /**
     * Keyboard manager class.
     * Instance the class.
     */
    public KeyboardManager(WindowInfo info, KeyboardLanguage defaultLanguage){
        this.windowInfo = info;
        Arrays.fill(state, false);
        Arrays.fill(oldState, false);

        wasMain = new ArrayList<>();

        language = defaultLanguage;
        mainKey = -1;
        numberKeyPressed = 0;
        capsLockActivated = false;

        incomingText = new StringBuilder();
        typedText = "";
        incomingKeyEvents = new ArrayList<>();
        keyEvents = Collections.emptyList();
        keySimulation = new InputSimulation();
        pendingSimulatedText = new StringBuilder();

        // Freed with the window (Window.destroyWindow)
        glfwSetCharCallback(info.getWindowId(), new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                incomingText.appendCodePoint(codepoint);
            }
        });

        glfwSetKeyCallback(info.getWindowId(), new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (action != GLFW_RELEASE && key != GLFW_KEY_UNKNOWN)
                    // The text that came before it in this tick: the editors put them back in order
                    incomingKeyEvents.add(new KeyEvent(key, scancode, mods, action == GLFW_REPEAT,
                            KeyEvent.LetterOf(glfwGetKeyName(key, scancode)), incomingText.length()));
            }
        });
    }

    public boolean getKeyState(int keyID){
        if (hidden())
            return false;

        return getRawKeyState(keyID);
    }

    // Text typed during this tick, empty if none : the way to read text (text fields, terminal)
    public String getTypedText() {
        return hidden() ? "" : typedText;
    }

    // Keys pressed or repeated during this tick, in order: for text editing (cursor, deletions, shortcuts)
    public List<KeyEvent> getKeyEvents() {
        return hidden() ? Collections.emptyList() : keyEvents;
    }

    // ---- Focus: the owner of the keyboard (the console, a text field) reads it, the game sees nothing ----

    // Set by InputManager when its focus is held
    private boolean captured;
    // Engine actions are read while the keyboard is captured (InputManager)
    private boolean rawRead;
    // The focus changed during this tick: what was typed or pressed in it is for nobody (until postUpdate)
    private boolean edgesDropped;

    void setCaptured(boolean captured) { this.captured = captured; }

    void dropEdges() {
        edgesDropped = true;
        typedText = "";
        keyEvents = Collections.emptyList();
    }

    public boolean isCaptured() { return captured; }

    void setRawRead(boolean rawRead) { this.rawRead = rawRead; }

    private boolean hidden() { return captured && !rawRead; }

    // What the game would see without capture: for the owner of the capture only
    public boolean getRawKeyState(int keyID) {
        if(keyID >= BEGIN_KEYS && keyID < KEYS) return state[keyID];
        return false;
    }

    public String getRawTypedText() { return typedText; }

    public List<KeyEvent> getRawKeyEvents() { return keyEvents; }

    // Press a key (GLFW_KEY_...) during a number of ticks (or InputSimulation.UNTIL_RELEASE) from the next tick
    public void simulateKey(int keyId, int ticks) {
        checkKey(keyId);
        keySimulation.press(keyId, ticks);
    }

    public void simulateKeyRelease(int keyId) {
        checkKey(keyId);
        keySimulation.release(keyId);
    }

    // Text seen by getTypedText() at the next tick
    public void simulateText(String text) {
        pendingSimulatedText.append(text);
    }

    private void checkKey(int keyId) {
        if (keyId < BEGIN_KEYS || keyId >= KEYS)
            throw new IllegalArgumentException("Key must be between " + BEGIN_KEYS + " and " + (KEYS - 1));
    }

    private boolean testState(int keyID){
        return glfwGetKey(windowInfo.getWindowId(), keyID) == 1;
    }

    public int getKeyByLanguage(int key){
        return language.translateKeyTo(key);
    }

    public void setKeyboardLanguage(KeyboardLanguage language){
        this.language = language;
        this.language.setManager(this);
    }

    /**
     * Test if the key has just been pressed.
     *
     * @param keyID Key's ID.
     * @return boolean
     */
    public boolean keyPressed(int keyID){
        if (hidden() || edgesDropped)
            return false;

        return (state[keyID] && !oldState[keyID]);
    }

    /**
     * Test if the key has just been released.
     *
     * @param keyID Key's ID.
     * @return boolean
     */
    public boolean keyReleased(int keyID){
        if (hidden() || edgesDropped)
            return false;

        return (!state[keyID] && oldState[keyID]);
    }

    // A blocked keyboard (the window waits for a modal window) sees no key and no text
    private boolean blocked;

    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public boolean isBlocked() { return blocked; }

    public void postUpdate(){
        boolean mainKeyChanged = false;
        edgesDropped = false;
        numberKeyPressed = 0;

        // Text typed since the last postUpdate, then the simulated one
        incomingText.append(pendingSimulatedText);
        pendingSimulatedText.setLength(0);
        typedText = blocked ? "" : incomingText.toString();
        incomingText.setLength(0);

        keySimulation.update();
        ArrayList<KeyEvent> events = new ArrayList<>(incomingKeyEvents);
        incomingKeyEvents.clear();
        for(int key = BEGIN_KEYS; key < state.length; ++key){
            oldState[key] = state[key];
            boolean simulated = keySimulation.getState(key);
            state[key] = !blocked && (testState(key) || simulated);

            // A simulated key is typed like a real one
            if (simulated && !oldSimulated[key])
                events.add(new KeyEvent(key, 0, simulatedMods(), false, KeyEvent.LetterOf(glfwGetKeyName(key, 0))));
            oldSimulated[key] = simulated;

            if (state[key]){
                numberKeyPressed += 1;

                if (key >= MIN_LANGUAGE_KEY && key <= MAX_LANGUAGE_KEY){
                    if (!mainKeyChanged && mainKey != key && !wasMain.contains(key)){
                        mainKey = key;
                        mainKeyChanged = true;
                    }
                } else {
                    if (!mainKeyChanged && !wasMain.contains(key)) {
                        mainKey = key;
                    }
                }
            }
        }

        if (numberKeyPressed == 0) {
            mainKey = -1;
            wasMain.clear();
        } else {
            for (int index = wasMain.size() - 1; index >= 0; --index) {
                if (!getRawKeyState(wasMain.get(index))) {
                    wasMain.remove(index);
                }
            }

            wasMain.add(mainKey);
        }

        keyEvents = blocked ? Collections.emptyList() : Collections.unmodifiableList(events);

        if (state[GLFW_KEY_CAPS_LOCK] && !oldState[GLFW_KEY_CAPS_LOCK])
            capsLockActivated = !capsLockActivated;
    }

    // Modifiers of a simulated key: the simulated or real modifier keys held
    private int simulatedMods() {
        int mods = 0;
        if (keySimulation.getState(GLFW_KEY_LEFT_CONTROL) || keySimulation.getState(GLFW_KEY_RIGHT_CONTROL)
                || testState(GLFW_KEY_LEFT_CONTROL) || testState(GLFW_KEY_RIGHT_CONTROL))
            mods |= GLFW_MOD_CONTROL;
        if (keySimulation.getState(GLFW_KEY_LEFT_SHIFT) || keySimulation.getState(GLFW_KEY_RIGHT_SHIFT)
                || testState(GLFW_KEY_LEFT_SHIFT) || testState(GLFW_KEY_RIGHT_SHIFT))
            mods |= GLFW_MOD_SHIFT;
        return mods;
    }

    public boolean isCapsLock() { return capsLockActivated; }

    public int getMainKeyPressed() { return hidden() ? -1 : mainKey; }
    public char getCorrespondingCharMainKeyPressed() { return hidden() ? (char) -1 : language.translateKeyTo(mainKey); }
    public int getNumberKeyPressed(){ return hidden() ? 0 : numberKeyPressed; }
}