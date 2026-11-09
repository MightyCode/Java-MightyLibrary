package io.github.mightycode.mightylib.registry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentifierTest {

    @Test
    void parseSplitsNamespaceAndPath() {
        Identifier identifier = Identifier.parse("demo:gui/move_right-2.d");
        assertEquals("demo", identifier.namespace());
        assertEquals("gui/move_right-2.d", identifier.path());
        assertEquals("demo:gui/move_right-2.d", identifier.toString());
    }

    @Test
    void sameTextGivesEqualIdentifiers() {
        assertEquals(Identifier.of("demo", "jump"), Identifier.parse("demo:jump"));
        assertEquals(Identifier.of("demo", "jump").hashCode(), Identifier.parse("demo:jump").hashCode());
        assertNotEquals(Identifier.parse("demo:jump"), Identifier.parse("mod:jump"));
    }

    @Test
    void invalidIdentifiersAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("jump"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse(":jump"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("demo:"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("Demo:jump"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("demo:Jump"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("demo:a:b"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse("de/mo:jump"));
        assertThrows(IllegalArgumentException.class, () -> Identifier.parse(null));
    }

    @Test
    void sortedByNamespaceThenPath() {
        assertTrue(Identifier.parse("a:z").compareTo(Identifier.parse("b:a")) < 0);
        assertTrue(Identifier.parse("a:a").compareTo(Identifier.parse("a:b")) < 0);
    }
}
