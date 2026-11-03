package MightyLibrary.mightylib.inputs.keyboardlanguage;

import MightyLibrary.mightylib.inputs.CharInputEntry;

import static org.lwjgl.glfw.GLFW.*;

public class AZERTYKeyboardLanguage extends KeyboardLanguage {
    private static final KeyboardLanguage instance = new AZERTYKeyboardLanguage();

    public static KeyboardLanguage getInstance() { return instance; }
    private AZERTYKeyboardLanguage(){
        super();
        inputEntries.add(new CharInputEntry(GLFW_KEY_SPACE, ' '));

        inputEntries.add(new CharInputEntry(GLFW_KEY_A, 'q').addModifier(CharInputEntry.CAPSLOCK, 'Q'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_B, 'b').addModifier(CharInputEntry.CAPSLOCK, 'B'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_C, 'c').addModifier(CharInputEntry.CAPSLOCK, 'C'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_D, 'd').addModifier(CharInputEntry.CAPSLOCK, 'D'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_E, 'e').addModifier(CharInputEntry.CAPSLOCK, 'E')
                .addModifier(CharInputEntry.ALT, '€'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_F, 'f').addModifier(CharInputEntry.CAPSLOCK, 'F'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_G, 'g').addModifier(CharInputEntry.CAPSLOCK, 'G'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_H, 'h').addModifier(CharInputEntry.CAPSLOCK, 'H'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_I, 'i').addModifier(CharInputEntry.CAPSLOCK, 'I'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_J, 'j').addModifier(CharInputEntry.CAPSLOCK, 'J'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_K, 'k').addModifier(CharInputEntry.CAPSLOCK, 'K'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_L, 'l').addModifier(CharInputEntry.CAPSLOCK, 'L'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_M, ',').addModifier(CharInputEntry.CAPSLOCK, '?'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_N, 'n').addModifier(CharInputEntry.CAPSLOCK, 'N'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_O, 'o').addModifier(CharInputEntry.CAPSLOCK, 'O'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_P, 'p').addModifier(CharInputEntry.CAPSLOCK, 'P'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_Q, 'a').addModifier(CharInputEntry.CAPSLOCK, 'A'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_R, 'r').addModifier(CharInputEntry.CAPSLOCK, 'R'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_S, 's').addModifier(CharInputEntry.CAPSLOCK, 'S'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_T, 't').addModifier(CharInputEntry.CAPSLOCK, 'T'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_U, 'u').addModifier(CharInputEntry.CAPSLOCK, 'U'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_V, 'v').addModifier(CharInputEntry.CAPSLOCK, 'V'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_W, 'z').addModifier(CharInputEntry.CAPSLOCK, 'Z'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_X, 'x').addModifier(CharInputEntry.CAPSLOCK, 'X'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_Y, 'y').addModifier(CharInputEntry.CAPSLOCK, 'Y'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_Z, 'w').addModifier(CharInputEntry.CAPSLOCK, 'W'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_WORLD_1, '<')
                .addModifier(CharInputEntry.CAPSLOCK, '>'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_LEFT_BRACKET, '^')
                .addModifier(CharInputEntry.CAPSLOCK, '¨'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_RIGHT_BRACKET, '$')
                .addModifier(CharInputEntry.CAPSLOCK, '£')

                .addModifier(CharInputEntry.ALT, '£'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_GRAVE_ACCENT, '²'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_SEMICOLON, 'm')
                .addModifier(CharInputEntry.CAPSLOCK, 'M'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_APOSTROPHE, 'ù')
                .addModifier(CharInputEntry.CAPSLOCK, '%'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_BACKSLASH, '*')
                .addModifier(CharInputEntry.CAPSLOCK, 'µ'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_COMMA, ';')
                .addModifier(CharInputEntry.CAPSLOCK, '.'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_PERIOD, ':')
                .addModifier(CharInputEntry.CAPSLOCK, '/'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_SLASH, '!')
                .addModifier(CharInputEntry.CAPSLOCK, '§'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_MINUS, ')')
                .addModifier(CharInputEntry.CAPSLOCK, '°').addModifier(CharInputEntry.ALT, ']'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_EQUAL, '=')
                .addModifier(CharInputEntry.CAPSLOCK, '+').addModifier(CharInputEntry.ALT, '+'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_KP_DIVIDE, '/'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_KP_MULTIPLY, '*'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_KP_SUBTRACT, '-'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_KP_ADD, '+'));

        inputEntries.add(new CharInputEntry(GLFW_KEY_KP_DECIMAL, '.'));

        for (int i = 0; i <= 9; ++i){
            inputEntries.add(new CharInputEntry(GLFW_KEY_KP_0 + i, (char)('0' + i)));
        }

        inputEntries.add(new CharInputEntry(GLFW_KEY_1, '&').addModifier(CharInputEntry.CAPSLOCK, '1'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_2, 'é').addModifier(CharInputEntry.CAPSLOCK, '2')
                .addModifier(CharInputEntry.ALT, '~'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_3, '\"').addModifier(CharInputEntry.CAPSLOCK, '3')
                .addModifier(CharInputEntry.ALT, '#'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_4, '\'').addModifier(CharInputEntry.CAPSLOCK, '4')
                .addModifier(CharInputEntry.ALT, '{'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_5, '(').addModifier(CharInputEntry.CAPSLOCK, '5')
                .addModifier(CharInputEntry.ALT,  '['));
        inputEntries.add(new CharInputEntry(GLFW_KEY_6, '-').addModifier(CharInputEntry.CAPSLOCK, '6')
                .addModifier(CharInputEntry.ALT, '|'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_7, 'è').addModifier(CharInputEntry.CAPSLOCK, '7')
                .addModifier(CharInputEntry.ALT, '`'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_8, '_').addModifier(CharInputEntry.CAPSLOCK, '8')
                .addModifier(CharInputEntry.ALT, '\\'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_9, 'ç').addModifier(CharInputEntry.CAPSLOCK, '9')
                .addModifier(CharInputEntry.ALT, '^'));
        inputEntries.add(new CharInputEntry(GLFW_KEY_0, 'à').addModifier(CharInputEntry.CAPSLOCK, '0')
                .addModifier(CharInputEntry.ALT, '@'));
    }

    @Override
    public String keyboardConfigurationName() {
        return "azerty";
    }
}
