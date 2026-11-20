package io.github.mightycode.mightylib.inputs;

/*
 * Whether the real devices (keyboard, mouse, gamepads) reach the engine. Ignored by the test kit: its tests only see
 * the simulated inputs, whatever the person at the machine does meanwhile. Process-wide, like the hidden windows.
 */
final class RealDevices {
    private static volatile boolean ignored;

    private RealDevices() {}

    static boolean isIgnored() {
        return ignored;
    }

    static void setIgnored(boolean ignore) {
        ignored = ignore;
    }
}
