package io.github.mightycode.mightylib.console;

import io.github.mightycode.mightylib.utils.math.color.Color4f;

/** Colors of the console, gathered here until there is a theme. */
final class ConsoleColors {
    static final Color4f OUT = new Color4f(0.55f, 0.55f, 0.55f, 1f);
    static final Color4f ERR = new Color4f(0.85f, 0.5f, 0.35f, 1f);
    static final Color4f COMMAND = new Color4f(1f, 0.85f, 0.35f, 1f);
    static final Color4f RESULT = new Color4f(1f, 1f, 1f, 1f);
    static final Color4f ERROR = new Color4f(1f, 0.35f, 0.35f, 1f);
    static final Color4f MESSAGE = new Color4f(0.55f, 0.85f, 1f, 1f);
    static final Color4f INPUT = new Color4f(1f, 1f, 1f, 1f);
    // Indicator of the lines below the view, and its background
    static final Color4f INDICATOR = new Color4f(1f, 1f, 1f, 1f);
    static final Color4f INDICATOR_BACKGROUND = new Color4f(0.2f, 0.35f, 0.6f, 0.9f);
    // Behind the selected text
    static final Color4f SELECTION = new Color4f(0.25f, 0.45f, 0.85f, 0.6f);
    // Width shown selected after a line whose end is selected
    static final float SELECTION_LINE_END = 4;

    private ConsoleColors() {}

    static Color4f of(ConsoleLog.EType type) {
        switch (type) {
            case ERR:
                return ERR;
            case ERROR:
                return ERROR;
            case COMMAND:
                return COMMAND;
            case RESULT:
                return RESULT;
            case MESSAGE:
                return MESSAGE;
            default:
                return OUT;
        }
    }
}
