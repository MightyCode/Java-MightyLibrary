package io.github.mightycode.mightylib.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * What a plugin registers, by section (resources, commands, inputs, scenes, services, shaders, lifecycle), during IPlugin.register only. Each plugin
 * gets its own registry, bound to its namespace: it registers identifiers of its namespace only, and replaces those of
 * the others explicitly (replace). Pure data: no GL, no engine.
 */
public final class EngineRegistry {
    private final Registrations registrations;
    private final String namespace;
    private final ResourcesSection resources;
    private final CommandsSection commands;
    private final InputsSection inputs;
    private final ScenesSection scenes;
    private final ServicesSection services;
    private final ShadersSection shaders;
    private final LifecycleSection lifecycle;

    private EngineRegistry(Registrations registrations, String namespace, ClassLoader classLoader) {
        this.registrations = registrations;
        this.namespace = namespace;
        resources = new ResourcesSection(registrations, namespace, classLoader);
        commands = new CommandsSection(registrations, this);
        inputs = new InputsSection(registrations, this);
        scenes = new ScenesSection(registrations, this);
        services = new ServicesSection(registrations, this);
        shaders = new ShadersSection(registrations, namespace);
        lifecycle = new LifecycleSection(registrations, namespace);
    }

    /**
     * The registration phase: the plugin of the engine (namespace "mightylib") then the others in their order, then
     * the registry is frozen.
     * @throws IllegalArgumentException if a namespace is invalid, or the engine's one is used by another plugin
     * @throws IllegalStateException    if two plugins have the same namespace, or a registration fails (collision...)
     */
    public static Registrations Collect(IPlugin enginePlugin, List<? extends IPlugin> plugins) {
        Objects.requireNonNull(enginePlugin, "enginePlugin");
        if (!Identifier.ENGINE_NAMESPACE.equals(enginePlugin.namespace()))
            throw new IllegalArgumentException("The plugin of the engine has the namespace "
                    + Identifier.ENGINE_NAMESPACE);

        Set<String> namespaces = new HashSet<>();
        namespaces.add(Identifier.ENGINE_NAMESPACE);
        for (IPlugin plugin : plugins) {
            String namespace = Objects.requireNonNull(plugin, "plugin").namespace();
            if (!Identifier.IsNamespace(namespace))
                throw new IllegalArgumentException("Invalid namespace ->" + namespace + "<- of the plugin "
                        + plugin.getClass().getName() + " (allowed : a-z 0-9 _ . -)");
            if (Identifier.ENGINE_NAMESPACE.equals(namespace))
                throw new IllegalArgumentException("The namespace " + namespace + " is the engine's (plugin "
                        + plugin.getClass().getName() + ")");
            if (!namespaces.add(namespace))
                throw new IllegalStateException("Two plugins have the namespace " + namespace);
        }

        Registrations registrations = new Registrations();
        try {
            register(registrations, enginePlugin);
            for (IPlugin plugin : plugins)
                register(registrations, plugin);

            registrations.freeze();
        } catch (RuntimeException e) {
            registrations.closeSources();
            throw e;
        }
        return registrations;
    }

    private static void register(Registrations registrations, IPlugin plugin) {
        registrations.addPlugin(plugin.namespace());
        plugin.register(new EngineRegistry(registrations, plugin.namespace(), plugin.getClass().getClassLoader()));
    }

    public String namespace() {
        return namespace;
    }

    // An identifier of the namespace of the plugin
    public Identifier id(String path) {
        return Identifier.of(namespace, path);
    }

    public ResourcesSection resources() {
        registrations.checkOpen();
        return resources;
    }

    public CommandsSection commands() {
        registrations.checkOpen();
        return commands;
    }

    public InputsSection inputs() {
        registrations.checkOpen();
        return inputs;
    }

    public ScenesSection scenes() {
        registrations.checkOpen();
        return scenes;
    }

    public ServicesSection services() {
        registrations.checkOpen();
        return services;
    }

    public ShadersSection shaders() {
        registrations.checkOpen();
        return shaders;
    }

    public LifecycleSection lifecycle() {
        registrations.checkOpen();
        return lifecycle;
    }

    // A plugin registers the identifiers of its namespace only (replace excepted)
    void checkNamespace(Identifier id) {
        if (!id.namespace().equals(namespace))
            throw new IllegalArgumentException("The plugin " + namespace + " can't register " + id
                    + ": outside of its namespace (replace an entry of another plugin with replace)");
    }
}
