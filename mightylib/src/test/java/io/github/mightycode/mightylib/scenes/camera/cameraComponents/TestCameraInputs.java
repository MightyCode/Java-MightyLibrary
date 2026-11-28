package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.WindowInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Inputs without window for the camera components: action values, presses and releases given, a mouse placed by hand
final class TestCameraInputs {
    // A mouse without window whose cursor is given
    static final class TestMouse extends MouseManager {
        double x, y;

        @Override
        protected boolean realButton(int buttonId) {
            return false;
        }

        @Override
        protected void realCursor(double[] x, double[] y) {
            x[0] = this.x;
            y[0] = this.y;
        }
    }

    final Map<InputAction, Float> values = new HashMap<>();
    final Set<InputAction> pressed = new HashSet<>();
    final Set<InputAction> released = new HashSet<>();
    final TestMouse mouse = new TestMouse();

    // Only the reads the components make
    final IInputManager inputs = (IInputManager) Proxy.newProxyInstance(
            IInputManager.class.getClassLoader(), new Class<?>[]{IInputManager.class}, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getValue":
                        return values.getOrDefault((InputAction) args[0], 0f);
                    case "getState":
                        return values.getOrDefault((InputAction) args[0], 0f) != 0f;
                    case "inputPressed":
                        return pressed.contains((InputAction) args[0]);
                    case "inputReleased":
                        return released.contains((InputAction) args[0]);
                    default:
                        throw new UnsupportedOperationException(method.getName());
                }
            });

    void hold(InputAction action) {
        values.put(action, 1f);
    }

    // The mouse of the next tick, then the presses and releases of this tick dropped
    void tick(Runnable update) {
        mouse.postUpdate();
        update.run();
        pressed.clear();
        released.clear();
    }

    // A window that was never created, of the virtual size given: no GL
    static WindowInfo windowInfo(int width, int height) throws ReflectiveOperationException {
        Constructor<WindowInfo> constructor = WindowInfo.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        WindowInfo info = constructor.newInstance();
        info.getVirtualSizeRef().set(width, height);
        return info;
    }
}
