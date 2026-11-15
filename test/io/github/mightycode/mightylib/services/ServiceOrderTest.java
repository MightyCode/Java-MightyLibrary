package io.github.mightycode.mightylib.services;

import io.github.mightycode.mightylib.registry.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// The order of creation of the services: dependencies, stable order, errors
class ServiceOrderTest {
    private interface IFake extends IEngineService {}

    private static ServiceKey<IFake> key(String id) {
        return ServiceKey.of(Identifier.parse(id), IFake.class);
    }

    private static ServiceDeclaration<IFake> declare(String id) {
        ServiceKey<IFake> key = key(id);
        return new ServiceDeclaration<>(key, engine -> new IFake() {}, key.id().namespace(), null);
    }

    private static List<String> order(List<ServiceDeclaration<?>> sorted) {
        List<String> keys = new ArrayList<>();
        for (ServiceDeclaration<?> declaration : sorted)
            keys.add(declaration.key().toString());
        return keys;
    }

    @Test
    void withoutDependenciesTheRegistrationOrderIsKept() {
        List<ServiceDeclaration<?>> sorted = ServiceOrder.Sort(List.of(declare("mightylib:sound"),
                declare("mightylib:console"), declare("game:a")));
        assertEquals(List.of("mightylib:sound", "mightylib:console", "game:a"), order(sorted));
    }

    @Test
    void aServiceIsCreatedAfterWhatItRequiresAndTheOthersKeepTheirOrder() {
        ServiceDeclaration<IFake> first = declare("game:first").requires(key("game:last"));
        List<ServiceDeclaration<?>> sorted = ServiceOrder.Sort(List.of(declare("mightylib:sound"), first,
                declare("game:middle"), declare("game:last")));
        // first waits for last; middle, ready earlier, keeps its place
        assertEquals(List.of("mightylib:sound", "game:middle", "game:last", "game:first"), order(sorted));
    }

    @Test
    void theEngineComesFirstAmongTheReadyServices() {
        ServiceDeclaration<IFake> game = declare("game:a").requires(key("mightylib:b"));
        List<ServiceDeclaration<?>> sorted = ServiceOrder.Sort(List.of(declare("mightylib:a"),
                declare("mightylib:b"), game, declare("game:c")));
        assertEquals(List.of("mightylib:a", "mightylib:b", "game:a", "game:c"), order(sorted));
    }

    @Test
    void afterOrdersOnlyWhenTheServiceIsRegistered() {
        ServiceDeclaration<IFake> counter = declare("game:counter").after(key("mightylib:console"),
                key("game:late"));
        List<ServiceDeclaration<?>> sorted = ServiceOrder.Sort(List.of(counter, declare("game:late")));
        assertEquals(List.of("game:late", "game:counter"), order(sorted));

        // The console is not registered: no error, no order
        List<ServiceDeclaration<?>> alone = ServiceOrder.Sort(List.of(declare("game:counter")
                .after(key("mightylib:console"))));
        assertEquals(List.of("game:counter"), order(alone));
    }

    @Test
    void aMissingRequiredServiceNamesTheServiceItsPluginAndTheKey() {
        ServiceDeclaration<IFake> counter = declare("game:counter").requires(key("mightylib:sound"));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ServiceOrder.Sort(List.of(counter)));
        assertTrue(error.getMessage().contains("game:counter"));
        assertTrue(error.getMessage().contains("plugin game"));
        assertTrue(error.getMessage().contains("mightylib:sound"));
    }

    @Test
    void aCycleIsAnErrorListingIt() {
        ServiceDeclaration<IFake> a = declare("game:a").requires(key("game:b"));
        ServiceDeclaration<IFake> b = declare("game:b").after(key("game:c"));
        ServiceDeclaration<IFake> c = declare("game:c").requires(key("game:a"));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ServiceOrder.Sort(List.of(declare("game:free"), a, b, c)));
        String message = error.getMessage();
        assertTrue(message.startsWith("Cycle between the services: "), message);
        assertTrue(message.contains("game:a") && message.contains("game:b") && message.contains("game:c"), message);
        assertFalse(message.contains("game:free"), message);
    }

    @Test
    void aServiceThatNeedsItselfIsACycle() {
        ServiceDeclaration<IFake> self = declare("game:self").after(key("game:self"));
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> ServiceOrder.Sort(List.of(self)));
        assertTrue(error.getMessage().contains("game:self (plugin game) needs game:self"), error.getMessage());
    }

    @Test
    void theSortFreezesTheDeclarations() {
        ServiceDeclaration<IFake> declaration = declare("game:a");
        ServiceOrder.Sort(List.of(declaration));
        assertThrows(IllegalStateException.class, () -> declaration.requires(key("game:b")));
        assertThrows(IllegalStateException.class, () -> declaration.after(key("game:b")));
        assertThrows(IllegalStateException.class, () -> declaration.fallback(engine -> new IFake() {}));
    }

    @Test
    void keysAreEqualByIdentifier() {
        assertEquals(key("game:a"), ServiceKey.of(Identifier.parse("game:a"), IEngineService.class));
        assertNotEquals(key("game:a"), key("game:b"));
    }
}
