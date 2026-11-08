package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.keyboardlanguage.KeyboardLanguage;
import io.github.mightycode.mightylib.main.WindowInfo;

import org.lwjgl.glfw.GLFWCharCallback;

import java.util.ArrayList;
import java.util.Arrays;

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

    public final WindowInfo windowInfo;
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
    private final GLFWCharCallback charCallback;

    // Simulated keys and text (commands, scripts, later replays), applied at the next postUpdate like real inputs
    private final InputSimulation keySimulation;
    private final StringBuilder pendingSimulatedText;

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
        keySimulation = new InputSimulation();
        pendingSimulatedText = new StringBuilder();

        glfwSetCharCallback(info.getWindowId(), charCallback = new GLFWCharCallback() {
            @Override
            public void invoke(long window, int codepoint) {
                incomingText.appendCodePoint(codepoint);
            }
        });
    }

    public boolean getKeyState(int keyID){
        if(keyID >= BEGIN_KEYS && keyID < KEYS) return state[keyID];
        return false;
    }

    // Text typed during this tick, empty if none : the way to read text (text fields, terminal)
    public String getTypedText() {
        return typedText;
    }

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
        return (state[keyID] && !oldState[keyID]);
    }

    /**
     * Test if the key has just been released.
     *
     * @param keyID Key's ID.
     * @return boolean
     */
    public boolean keyReleased(int keyID){
        return (!state[keyID] && oldState[keyID]);
    }

    public void postUpdate(){
        boolean mainKeyChanged = false;
        numberKeyPressed = 0;

        // Text typed since the last postUpdate, then the simulated one
        incomingText.append(pendingSimulatedText);
        pendingSimulatedText.setLength(0);
        typedText = incomingText.toString();
        incomingText.setLength(0);

        keySimulation.update();
        for(int key = BEGIN_KEYS; key < state.length; ++key){
            oldState[key] = state[key];
            state[key] = testState(key) || keySimulation.getState(key);

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
                if (!getKeyState(wasMain.get(index))) {
                    wasMain.remove(index);
                }
            }

            wasMain.add(mainKey);
        }

        if (keyPressed(GLFW_KEY_CAPS_LOCK))
            capsLockActivated = !capsLockActivated;
    }

    public boolean isCapsLock() { return capsLockActivated; }

    public int getMainKeyPressed() { return mainKey; }
    public char getCorrespondingCharMainKeyPressed() { return language.translateKeyTo(mainKey); }
    public int getNumberKeyPressed(){ return numberKeyPressed; }
}