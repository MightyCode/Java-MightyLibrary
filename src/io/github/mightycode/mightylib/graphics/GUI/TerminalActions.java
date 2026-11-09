package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.inputs.InputAction;

// Actions read by a Terminal ; null : not used
public class TerminalActions {
    public InputAction Paste;
    public InputAction DeleteRight;
    public InputAction DeleteLeft;
    public InputAction GoWordAtRight;
    public InputAction GoWordAtLeft;

    public InputAction MoveCursorRight;
    public InputAction MoveCursorLeft;

    public InputAction SetPreviousCommand;
    public InputAction SetNextCommand;
    public InputAction EnterCommand;
}
