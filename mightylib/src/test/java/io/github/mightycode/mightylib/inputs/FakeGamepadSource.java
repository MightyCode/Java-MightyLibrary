package io.github.mightycode.mightylib.inputs;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

// Gamepads plugged by the test, read like GLFW would
class FakeGamepadSource implements IGamepadSource {
    final Map<Integer, boolean[]> buttons = new HashMap<>();
    final Map<Integer, float[]> axes = new HashMap<>();
    final Map<Integer, Boolean> mapped = new HashMap<>();
    IConnectionCallback callback;

    void plug(int id, boolean isMapped) {
        buttons.put(id, new boolean[Gamepad.BUTTONS]);
        float[] values = new float[Gamepad.AXES];
        values[GLFW_GAMEPAD_AXIS_LEFT_TRIGGER] = -1;
        values[GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER] = -1;
        axes.put(id, values);
        mapped.put(id, isMapped);
        if (callback != null)
            callback.changed(id, true);
    }

    void unplug(int id) {
        buttons.remove(id);
        axes.remove(id);
        mapped.remove(id);
        if (callback != null)
            callback.changed(id, false);
    }

    @Override public void setConnectionCallback(IConnectionCallback callback) { this.callback = callback; }
    @Override public boolean isPresent(int id) { return buttons.containsKey(id); }
    @Override public boolean isGamepad(int id) { return mapped.getOrDefault(id, false); }
    @Override public String getName(int id) { return "Fake " + id; }
    @Override public String getGuid(int id) { return "guid" + id; }
    final java.util.List<String> addedMappings = new java.util.ArrayList<>();

    @Override public boolean addMappings(String mappings) { addedMappings.add(mappings); return !mappings.isEmpty(); }
    @Override public void free() { callback = null; }

    @Override
    public boolean read(int id, boolean[] outButtons, float[] outAxes) {
        if (!buttons.containsKey(id) || !mapped.get(id))
            return false;
        System.arraycopy(buttons.get(id), 0, outButtons, 0, Gamepad.BUTTONS);
        System.arraycopy(axes.get(id), 0, outAxes, 0, Gamepad.AXES);
        return true;
    }
}
