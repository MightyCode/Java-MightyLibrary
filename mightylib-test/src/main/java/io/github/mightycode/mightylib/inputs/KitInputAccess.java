package io.github.mightycode.mightylib.inputs;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.INTERNAL;

/** Bridge of the test kit to the package-private switch of the real devices. Not for the games. */
@API(status = INTERNAL)
public final class KitInputAccess {
    private KitInputAccess() {}

    // True: the keyboard, the mouse and the gamepads of the machine are ignored, only the simulated inputs count
    public static void setRealDevicesIgnored(boolean ignored) {
        RealDevices.setIgnored(ignored);
    }
}
