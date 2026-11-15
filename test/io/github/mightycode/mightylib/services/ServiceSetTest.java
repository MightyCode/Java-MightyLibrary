package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Creation of the services (order, fallback, failures) and their phases, without engine
class ServiceSetTest {
    private interface IFake extends IEngineService {}

    private interface IOther extends IEngineService {}

    private final List<String> calls = new ArrayList<>();

    private final class Fake implements IFake {
        private final String name;
        private final int layer;

        private Fake(String name, int layer) {
            this.name = name;
            this.layer = layer;
            calls.add("create " + name);
        }

        @Override public void update() { calls.add("update " + name); }
        @Override public void lateUpdate() { calls.add("late " + name); }
        @Override public void displayOverlay() { calls.add("overlay " + name); }
        @Override public int overlayLayer() { return layer; }
        @Override public void unload() { calls.add("unload " + name); }
        @Override public void terminate() { calls.add("terminate " + name); }

        // A method of the service itself, not a phase (the console closes its overlay)
        public void close() { calls.add("close " + name); }
    }

    private static ServiceKey<IFake> key(String path) {
        return ServiceKey.of(Identifier.of("game", path), IFake.class);
    }

    private ServiceDeclaration<IFake> declare(String path, int layer) {
        return new ServiceDeclaration<>(key(path), engine -> new Fake(path, layer), "game", null);
    }

    private ServiceDeclaration<IFake> failing(String path) {
        return new ServiceDeclaration<>(key(path), engine -> {
            throw new IllegalStateException("no device");
        }, "game", null);
    }

    private static List<ServiceDeclaration<?>> sorted(ServiceDeclaration<?>... declarations) {
        return ServiceOrder.Sort(List.of(declarations));
    }

    @Test
    void servicesAreCreatedAndUpdatedInOrderAndReleasedInReverse() {
        ServiceSet set = new ServiceSet();
        assertTrue(set.create(null, sorted(declare("a", 0), declare("b", 0))));
        set.update();
        set.lateUpdate();
        set.unload();
        set.terminate();

        // close is not a phase: the engine never calls it
        assertEquals(List.of("create a", "create b", "update a", "update b", "late a", "late b",
                "unload b", "unload a", "terminate b", "terminate a"), calls);
        assertTrue(set.running().isEmpty());
    }

    @Test
    void overlaysAreDrawnByLayerThenOrderOfCreation() {
        ServiceSet set = new ServiceSet();
        set.create(null, sorted(declare("console", 1000), declare("hud", 0), declare("debug", 5),
                declare("counter", 0)));
        calls.clear();
        set.displayOverlays();
        assertEquals(List.of("overlay hud", "overlay counter", "overlay debug", "overlay console"), calls);
    }

    @Test
    void aFailingFactoryUsesTheFallback() {
        ServiceDeclaration<IFake> sound = failing("sound");
        sound.fallback(engine -> new Fake("silent", 0));
        ServiceSet set = new ServiceSet();

        assertTrue(set.create(null, sorted(sound)));
        assertFalse(set.hasFailed());
        assertEquals(List.of("create silent"), calls);
        assertTrue(set.running().get(0).isFallback());
        assertSame(set.running().get(0).service(), set.get(key("sound")));
    }

    // The native library of OpenAL can't be loaded: the silent sound, not a crash of the engine
    @Test
    void aFactoryFailingToLinkUsesTheFallback() {
        ServiceDeclaration<IFake> sound = new ServiceDeclaration<>(key("sound"), engine -> {
            throw new UnsatisfiedLinkError("no openal");
        }, "game", null);
        sound.fallback(engine -> new Fake("silent", 0));
        ServiceSet set = new ServiceSet();

        assertTrue(set.create(null, sorted(sound)));
        assertTrue(set.running().get(0).isFallback());
    }

    @Test
    void aFactoryReturningNullIsAFailure() {
        ServiceDeclaration<IFake> sound = new ServiceDeclaration<>(key("sound"), engine -> null, "game", null);
        sound.fallback(engine -> new Fake("silent", 0));
        ServiceSet set = new ServiceSet();
        assertTrue(set.create(null, sorted(sound)));
        assertTrue(set.running().get(0).isFallback());

        ServiceSet withoutFallback = new ServiceSet();
        assertFalse(withoutFallback.create(null, sorted(new ServiceDeclaration<>(key("x"), engine -> null, "game",
                null))));
        assertFalse(withoutFallback.has(key("x")));
    }

    @Test
    void aFailureWithoutFallbackSkipsItsDependentsOnly() {
        ServiceDeclaration<IFake> counter = declare("counter", 0).requires(key("sound"));
        ServiceDeclaration<IFake> hud = declare("hud", 0).requires(key("counter"));
        ServiceDeclaration<IFake> ordered = declare("ordered", 0).after(key("sound"));
        ServiceSet set = new ServiceSet();

        assertFalse(set.create(null, sorted(failing("sound"), counter, hud, ordered, declare("free", 0))));
        assertTrue(set.hasFailed());
        assertEquals(List.of("create ordered", "create free"), calls);
        assertFalse(set.has(key("sound")));
        assertFalse(set.has(key("counter")));
        assertFalse(set.has(key("hud")));

        // What was created is released at the stop
        calls.clear();
        set.unload();
        set.terminate();
        assertEquals(List.of("unload free", "unload ordered", "terminate free", "terminate ordered"), calls);
    }

    @Test
    void aFailingFallbackIsAFailure() {
        ServiceDeclaration<IFake> sound = failing("sound");
        sound.fallback(engine -> {
            throw new IllegalStateException("no fallback either");
        });
        ServiceSet set = new ServiceSet();
        assertFalse(set.create(null, sorted(sound)));
        assertTrue(set.running().isEmpty());
    }

    @Test
    void aReleaseThatFailsDoesNotStopTheOthers() {
        ServiceDeclaration<IFake> broken = new ServiceDeclaration<>(key("broken"), engine -> new IFake() {
            @Override
            public void unload() {
                throw new IllegalStateException("broken unload");
            }
        }, "game", null);
        ServiceSet set = new ServiceSet();
        set.create(null, sorted(declare("a", 0), broken));
        set.unload();
        assertEquals(List.of("create a", "unload a"), calls);
    }

    @Test
    void getChecksTheKeyAndItsContract() {
        ServiceSet set = new ServiceSet();
        set.create(null, sorted(declare("a", 0)));

        assertInstanceOf(Fake.class, set.get(key("a")));
        assertThrows(IllegalArgumentException.class, () -> set.get(key("missing")));
        assertThrows(IllegalArgumentException.class,
                () -> set.get(ServiceKey.of(Identifier.of("game", "a"), IOther.class)));
    }
}
