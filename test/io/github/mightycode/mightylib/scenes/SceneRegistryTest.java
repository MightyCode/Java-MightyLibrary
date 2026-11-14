package io.github.mightycode.mightylib.scenes;

import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// The scenes by name: identifier or path alone, ambiguity, completion (no scene is created here: no GL)
class SceneRegistryTest {
    private abstract static class Level extends Scene {}
    private abstract static class Menu extends Scene {}

    // Never called by these tests
    private static <T extends Scene> Supplier<T> never() {
        return () -> {
            throw new AssertionError("The scene should not be created");
        };
    }

    private static SceneRegistry registry() {
        return new SceneRegistry()
                .declare(Identifier.parse("game:level"), Level.class, never(), "The level")
                .declare(Identifier.parse("game:menu"), Menu.class, never(), "The menu")
                .declare(Identifier.parse("mod:level"), Level.class, never(), "The level of the mod")
                .declare(Identifier.parse("game:world.cave"), Level.class, never(), "A cave")
                .freeze();
    }

    private static List<String> ids(List<SceneRegistry.Entry> entries) {
        return entries.stream().map(SceneRegistry.Entry::toString).collect(Collectors.toList());
    }

    @Test
    void nameIsTheIdentifierOrThePathAloneIgnoringTheCase() {
        SceneRegistry registry = registry();
        assertEquals("game:menu", registry.find("Menu").toString());
        assertEquals("game:menu", registry.find("GAME:MENU").toString());
        assertEquals("game:world.cave", registry.find("World.Cave").toString());
        assertEquals("The menu", registry.find("menu").description());
        assertSame(Menu.class, registry.find("menu").type());
    }

    @Test
    void ambiguousOrUnknownNames() {
        SceneRegistry registry = registry();
        assertNull(registry.find("level"));
        assertEquals(List.of("game:level", "mod:level"), ids(registry.match("level")));
        assertEquals(List.of("mod:level"), ids(registry.match("mod:level")));
        assertTrue(registry.match("unknown").isEmpty());
        assertTrue(registry.match("not an:identifier").isEmpty());
        assertTrue(registry.match(null).isEmpty());
    }

    // The path alone when it is unique, the identifier when another namespace has the same path
    @Test
    void completionGivesThePathsOrTheIdentifiersWhenAmbiguous() {
        assertEquals(List.of("game:level", "menu", "mod:level", "world.cave"), registry().completions());
    }

    // A scene created by the code is named by the first identifier of its class, or not at all
    @Test
    void entryOfAClass() {
        SceneRegistry registry = registry();
        assertEquals("game:level", registry.entryOf(Level.class).toString());
        assertNull(registry.entryOf(Scene.class));
    }

    @Test
    void registrationIsChecked() {
        SceneRegistry registry = new SceneRegistry().declare(Identifier.parse("game:level"), Level.class, never(), "A");
        assertThrows(IllegalArgumentException.class,
                () -> registry.declare(Identifier.parse("game:level"), Menu.class, never(), "B"));
        assertThrows(IllegalArgumentException.class,
                () -> registry.declare(Identifier.parse("game:other"), Menu.class, never(), ""));
        assertThrows(NullPointerException.class,
                () -> registry.declare(Identifier.parse("game:other"), Menu.class, null, "B"));

        registry.freeze();
        assertThrows(IllegalStateException.class,
                () -> registry.declare(Identifier.parse("game:late"), Menu.class, never(), "B"));
        assertThrows(UnsupportedOperationException.class, () -> registry.entries().clear());
    }

    @Test
    void factoryGivingNullIsAnError() {
        SceneRegistry registry = new SceneRegistry().declare(Identifier.parse("game:level"), Level.class, () -> null, "A");
        assertThrows(NullPointerException.class, () -> registry.find("level").create());
    }
}
