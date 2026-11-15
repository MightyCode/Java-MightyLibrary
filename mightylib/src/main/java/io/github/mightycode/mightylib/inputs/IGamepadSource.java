package io.github.mightycode.mightylib.inputs;

/**
 * Where the real gamepads are read : GLFW in the engine, a fake one in the tests.
 */
interface IGamepadSource {
    interface IConnectionCallback {
        void changed(int joystickId, boolean connected);
    }

    // Called from the main thread (GLFW calls it while polling the events), null to remove it
    void setConnectionCallback(IConnectionCallback callback);

    boolean isPresent(int joystickId);
    boolean isGamepad(int joystickId);
    String getName(int joystickId);
    String getGuid(int joystickId);

    // Read the mapped state, false if it can't be read (disconnected, no mapping)
    boolean read(int joystickId, boolean[] buttons, float[] axes);

    // Add SDL_GameControllerDB mappings, false if they are invalid
    boolean addMappings(String mappings);

    void free();
}
