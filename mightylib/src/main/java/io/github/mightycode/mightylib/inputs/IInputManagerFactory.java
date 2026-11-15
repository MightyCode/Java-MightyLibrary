package io.github.mightycode.mightylib.inputs;

/**
 * Creates the input manager of each window, on its creation (the keyboard, the mouse and the gamepads stay the
 * engine's). The engine gives InputManager::new; a plugin replaces it with registry.services().inputManager. The
 * manager is created without binding: the engine then calls bindDefaults.
 */
@FunctionalInterface
public interface IInputManagerFactory {
    IInputManager create(KeyboardManager keyboard, MouseManager mouse, GamepadManager gamepads, InputRegistry registry);
}
