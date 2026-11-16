package io.github.mightycode.mightylib.inputs;

import io.github.mightycode.mightylib.inputs.inputType.ActionInput;
import io.github.mightycode.mightylib.inputs.inputType.AxisInput;
import io.github.mightycode.mightylib.inputs.inputType.EInputType;
import io.github.mightycode.mightylib.inputs.inputType.IAction;
import io.github.mightycode.mightylib.inputs.inputType.InputSimple;
import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// The actions and axes of an engine, declared by its plugins, read by its InputManagers
class InputRegistryTest {
    private static final InputAction JUMP = InputAction.of("game:jump");
    private static final InputAction MOD_JUMP = InputAction.of("mod:jump");
    private static final InputAction CROUCH = InputAction.of("game:crouch");
    private static final InputAxis LOOK = InputAxis.of("game:look");

    private static InputRegistry registry() {
        return TestInputs.Registry(JUMP, MOD_JUMP, CROUCH, LOOK);
    }

    // A key is its identifier: two keys of the same identifier are the same action
    @Test
    void keysAreTheirIdentifier() {
        assertEquals(JUMP, InputAction.of("game:jump"));
        assertEquals(JUMP.hashCode(), InputAction.of(Identifier.of("game", "jump")).hashCode());
        assertNotEquals(JUMP, MOD_JUMP);
        Object axis = InputAxis.of("game:jump");
        assertNotEquals(JUMP, axis);
        assertTrue(registry().isDeclared(InputAction.of("game:jump")));
    }

    @Test
    void duplicateIdentifiersAreRefused() {
        InputRegistry registry = new InputRegistry().declareAction(JUMP, null).declareAxis(LOOK, null);
        assertThrows(IllegalArgumentException.class, () -> registry.declareAction(InputAction.of("game:jump"), null));
        assertThrows(IllegalArgumentException.class, () -> registry.declareAxis(InputAxis.of("game:jump"), null));
        assertThrows(IllegalArgumentException.class, () -> registry.declareAction(InputAction.of("game:look"), null));
    }

    @Test
    void frozenRegistryRefusesDeclarations() {
        InputRegistry registry = registry();
        assertThrows(IllegalStateException.class, () -> registry.declareAction(InputAction.of("game:late"), null));
    }

    @Test
    void findByFullIdentifierOrUniquePath() {
        InputRegistry registry = registry();
        assertEquals(JUMP, registry.findAction("game:jump"));
        assertEquals(MOD_JUMP, registry.findAction("MOD:JUMP"));
        assertEquals(CROUCH, registry.findAction("CROUCH"));
        assertEquals(LOOK, registry.findAxis("look"));
        assertEquals(List.of(CROUCH, JUMP, MOD_JUMP), registry.getActions());
    }

    @Test
    void ambiguousOrUnknownNamesFindNothing() {
        InputRegistry registry = registry();
        assertNull(registry.findAction("jump"));
        assertEquals(List.of(JUMP, MOD_JUMP), registry.matchActions("jump"));
        assertNull(registry.findAction("game:unknown"));
        assertNull(registry.findAction("not an:identifier"));
        // Actions and axes are searched apart
        assertNull(registry.findAction("look"));
        assertNull(registry.findAxis("crouch"));
    }

    // A key used without being declared: a clear error, not a silent "never pressed"
    @Test
    void undeclaredKeyIsAnError() {
        InputManager manager = new InputManager(null, null, null, registry());
        InputAction unknown = InputAction.of("game:unknown");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> manager.getState(unknown));
        assertTrue(error.getMessage().contains("game:unknown"), error.getMessage());
        assertThrows(IllegalStateException.class, () -> manager.simulatePress(unknown, 1));
        assertThrows(IllegalStateException.class, () -> manager.getAxis(InputAxis.of("game:unknown")));
        assertThrows(IllegalStateException.class, () -> manager.init(new ActionInput[]{
                new ActionInput(unknown, new InputSimple(0, EInputType.Keyboard))}));
        // Declared but not bound in this window: never pressed
        assertFalse(manager.getState(JUMP));
    }

    // The defaults of the engine are bound in every window, those of the plugins in the windows of the game
    @Test
    void defaultBindingsByWindow() {
        InputAction console = InputAction.of(Identifier.of(Identifier.ENGINE_NAMESPACE, "console"));
        InputRegistry registry = new InputRegistry()
                .declareAction(console, new InputSimple(1, EInputType.Keyboard))
                .declareAction(JUMP, new InputSimple(2, EInputType.Keyboard))
                .declareAction(CROUCH, null)
                .declareAxis(LOOK, manager -> 0)
                .freeze();

        assertEquals(List.of(console, JUMP),
                registry.defaultActions(true).stream().map(ActionInput::action).collect(Collectors.toList()));
        assertEquals(List.of(console),
                registry.defaultActions(false).stream().map(ActionInput::action).collect(Collectors.toList()));
        assertEquals(List.of(LOOK),
                registry.defaultAxes(true).stream().map(AxisInput::axis).collect(Collectors.toList()));
        assertTrue(registry.defaultAxes(false).isEmpty());

        InputManager gameWindow = new InputManager(null, null, null, registry);
        gameWindow.bindDefaults(true);
        assertNotNull(gameWindow.findAction("jump"));
        InputManager consoleWindow = new InputManager(null, null, null, registry);
        consoleWindow.bindDefaults(false);
        assertNull(consoleWindow.findAction("jump"));
        assertNotNull(consoleWindow.findAction("mightylib:console"));
    }

    // The simulation is indexed by the registry: two managers of the same registry agree
    @Test
    void simulationUsesTheIndexOfTheRegistry() {
        InputManager manager = new InputManager(null, null, null, registry());
        manager.init(new ActionInput[]{
                new ActionInput(JUMP, new IAction() {
                    @Override
                    public boolean getState(IInputManager inputManager) { return false; }

                    @Override
                    public boolean actionPressed(IInputManager inputManager) { return false; }

                    @Override
                    public boolean actionReleased(IInputManager inputManager) { return false; }
                })});
        manager.simulatePress(JUMP, 2);
        manager.updateActionSimulation();
        assertTrue(manager.getState(InputAction.of("game:jump")));
        assertFalse(manager.isSimulated(CROUCH));
    }
}
