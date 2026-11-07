package io.github.mightycode.mightylib.inputs;

import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.glfw.GLFWJoystickCallback;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Gamepads read through GLFW (XInput / DirectInput on Windows, mappings of SDL_GameControllerDB included in GLFW).
 */
class GlfwGamepadSource implements IGamepadSource {
    private final GLFWGamepadState state = GLFWGamepadState.malloc();
    private GLFWJoystickCallback callback;

    @Override
    public void setConnectionCallback(IConnectionCallback connectionCallback) {
        if (callback != null) {
            callback.free();
            callback = null;
        }

        if (connectionCallback == null) {
            glfwSetJoystickCallback(null);
            return;
        }

        callback = GLFWJoystickCallback.create((jid, event) -> connectionCallback.changed(jid, event == GLFW_CONNECTED));
        glfwSetJoystickCallback(callback);
    }

    @Override
    public boolean isPresent(int joystickId) {
        return glfwJoystickPresent(joystickId);
    }

    @Override
    public boolean isGamepad(int joystickId) {
        return glfwJoystickIsGamepad(joystickId);
    }

    @Override
    public String getName(int joystickId) {
        String name = glfwJoystickIsGamepad(joystickId) ? glfwGetGamepadName(joystickId) : null;
        return name != null ? name : glfwGetJoystickName(joystickId);
    }

    @Override
    public String getGuid(int joystickId) {
        return glfwGetJoystickGUID(joystickId);
    }

    @Override
    public boolean read(int joystickId, boolean[] buttons, float[] axes) {
        if (!glfwGetGamepadState(joystickId, state))
            return false;

        ByteBuffer stateButtons = state.buttons();
        for (int button = 0; button < buttons.length; ++button)
            buttons[button] = stateButtons.get(button) == GLFW_PRESS;

        FloatBuffer stateAxes = state.axes();
        for (int axis = 0; axis < axes.length; ++axis)
            axes[axis] = stateAxes.get(axis);

        return true;
    }

    @Override
    public boolean addMappings(String mappings) {
        ByteBuffer text = MemoryUtil.memASCII(mappings);
        try {
            return glfwUpdateGamepadMappings(text);
        } finally {
            MemoryUtil.memFree(text);
        }
    }

    @Override
    public void free() {
        setConnectionCallback(null);
        state.free();
    }
}
