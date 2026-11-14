package io.github.mightycode.mightylib.registry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

// The hooks of the plugins: called in their order, a failure does not stop the others (except in strict mode)
class LifecycleHooksTest {
    private static IPlugin plugin(String namespace, Consumer<EngineRegistry> register) {
        return new IPlugin() {
            @Override
            public String namespace() {
                return namespace;
            }

            @Override
            public void register(EngineRegistry registry) {
                register.accept(registry);
            }
        };
    }

    private static LifecycleHooks collect(List<String> calls) {
        return EngineRegistry.Collect(
                plugin(Identifier.ENGINE_NAMESPACE, registry -> registry.lifecycle()
                        .onStarted(() -> calls.add("engine started"))
                        .onShutdown(() -> calls.add("engine shutdown"))),
                List.of(plugin("game", registry -> registry.lifecycle()
                                .onStarted(() -> calls.add("game started"))
                                .onSceneChanged((context, previous, next) -> calls.add("game scene " + previous))
                                .onWindowOpened(window -> calls.add("game opened"))
                                .onWindowClosed(window -> calls.add("game closed"))),
                        plugin("mod", registry -> registry.lifecycle()
                                .onStarted(() -> calls.add("mod started"))
                                .onShutdown(() -> calls.add("mod shutdown")))))
                .lifecycle();
    }

    @Test
    void hooksAreCalledInTheOrderOfThePlugins() {
        List<String> calls = new ArrayList<>();
        LifecycleHooks hooks = collect(calls);

        hooks.started();
        hooks.windowOpened(null);
        hooks.sceneChanged(null, null, null);
        hooks.windowClosed(null);
        hooks.shutdown();

        assertEquals(List.of("engine started", "game started", "mod started", "game opened", "game scene null",
                "game closed", "engine shutdown", "mod shutdown"), calls);
    }

    @Test
    void failingHookDoesNotStopTheOthers() {
        List<String> calls = new ArrayList<>();
        LifecycleHooks hooks = new LifecycleHooks();
        hooks.addStarted("game", () -> {
            throw new IllegalStateException("broken on purpose (test)");
        });
        hooks.addStarted("mod", () -> calls.add("mod"));

        hooks.started();
        assertEquals(List.of("mod"), calls);
    }

    @Test
    void strictModeThrowsAgain() {
        LifecycleHooks hooks = new LifecycleHooks();
        hooks.addShutdown("game", () -> {
            throw new IllegalStateException("broken on purpose (test)");
        });
        hooks.setStrict(true);

        assertThrows(IllegalStateException.class, hooks::shutdown);
    }

    @Test
    void hooksAreRegisteredDuringThePhaseOnly() {
        EngineRegistry[] kept = new EngineRegistry[1];
        EngineRegistry.Collect(plugin(Identifier.ENGINE_NAMESPACE, registry -> {}),
                List.of(plugin("game", registry -> kept[0] = registry)));

        assertThrows(IllegalStateException.class, () -> kept[0].lifecycle());
    }
}
