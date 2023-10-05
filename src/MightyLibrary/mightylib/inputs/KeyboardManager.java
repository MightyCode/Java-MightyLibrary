package MightyLibrary.mightylib.inputs;

import MightyLibrary.mightylib.inputs.keyboardlanguage.NativeKeyboardLanguage;
import MightyLibrary.mightylib.main.WindowInfo;

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

    /**
     * Keyboard manager class.
     * Instance the class.
     */
    public KeyboardManager(WindowInfo info){
        this.windowInfo = info;
        Arrays.fill(state, false);
        Arrays.fill(oldState, false);

        wasMain = new ArrayList<>();

        language = NativeKeyboardLanguage.getInstance();
        mainKey = -1;
        numberKeyPressed = 0;
        capsLockActivated = false;
    }

    public boolean getKeyState(int keyID){
        if(keyID >= BEGIN_KEYS && keyID <= BEGIN_KEYS + KEYS) return state[keyID];
        return false;
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

    public void dispose(){
        boolean mainKeyChanged = false;
        numberKeyPressed = 0;

        for(int key = BEGIN_KEYS; key < state.length; ++key){
            oldState[key] = state[key];
            state[key] = testState(key);

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