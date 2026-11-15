package io.github.mightycode.mightylib.inputs.text;

/** The clipboard of the system: read on Ctrl+V only, written on Ctrl+C and Ctrl+X. */
public interface IClipboard {
    String get();

    void set(String text);
}
