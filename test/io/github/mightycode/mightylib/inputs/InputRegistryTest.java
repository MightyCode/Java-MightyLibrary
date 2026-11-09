package io.github.mightycode.mightylib.inputs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputRegistryTest {
    // The registry is shared by the whole JVM : identifiers unique to this class
    private static final InputAction JUMP = InputRegistry.action("test_registry:jump");
    private static final InputAction MOD_JUMP = InputRegistry.action("test_registry_mod:jump");
    private static final InputAction CROUCH = InputRegistry.action("test_registry:crouch");
    private static final InputAxis LOOK = InputRegistry.axis("test_registry:look");

    @Test
    void duplicateIdentifiersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> InputRegistry.action("test_registry:jump"));
        assertThrows(IllegalArgumentException.class, () -> InputRegistry.axis("test_registry:jump"));
        assertThrows(IllegalArgumentException.class, () -> InputRegistry.action("test_registry:look"));
    }

    @Test
    void findByFullIdentifierOrUniquePath() {
        assertSame(JUMP, InputRegistry.findAction("test_registry:jump"));
        assertSame(MOD_JUMP, InputRegistry.findAction("TEST_REGISTRY_MOD:JUMP"));
        assertSame(CROUCH, InputRegistry.findAction("CROUCH"));
        assertSame(LOOK, InputRegistry.findAxis("look"));
    }

    @Test
    void ambiguousOrUnknownNamesFindNothing() {
        assertNull(InputRegistry.findAction("jump"));
        assertEquals(2, InputRegistry.matchActions("jump").size());
        assertNull(InputRegistry.findAction("test_registry:unknown"));
        assertNull(InputRegistry.findAction("not an:identifier"));
        // Actions and axes are searched apart
        assertNull(InputRegistry.findAction("look"));
        assertNull(InputRegistry.findAxis("crouch"));
    }

    @Test
    void engineActionsAreRegistered() {
        assertSame(EngineActions.TOGGLE_CONSOLE, InputRegistry.findAction("mightylib:toggle_console"));
        assertSame(EngineActions.DETACH_CONSOLE, InputRegistry.findAction("mightylib:detach_console"));
        assertEquals("mightylib:reload_texture", EngineActions.RELOAD_TEXTURE.toString());
    }
}
