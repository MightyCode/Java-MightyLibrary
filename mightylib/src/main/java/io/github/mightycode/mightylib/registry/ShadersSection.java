package io.github.mightycode.mightylib.registry;

import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;

import java.util.Objects;

/**
 * What the shaders need from Java (EngineRegistry.shaders): the batch format of each batch program (a shader tagged
 * batch2D or batch2DInstanced in shaders.json), by shader name. A batch program without format is not used: the
 * shaders linked to it are drawn alone.
 */
public final class ShadersSection {
    private final Registrations registrations;
    private final String plugin;

    ShadersSection(Registrations registrations, String plugin) {
        this.registrations = registrations;
        this.plugin = plugin;
    }

    /**
     * The format the batch program of this shader name reads (several programs may read the same format).
     * @throws IllegalStateException if a format is registered for this shader
     */
    public ShadersSection batchFormat(String shaderName, BatchFormat format) {
        registrations.checkOpen();
        Objects.requireNonNull(shaderName, "shaderName");
        registrations.batchFormats.register(plugin, shaderName, Objects.requireNonNull(format, "format"));
        return this;
    }

    /**
     * Replaces the format of a batch program registered by a previous plugin (the engine included).
     * @throws IllegalStateException if no format is registered for this shader
     */
    public ShadersSection replaceBatchFormat(String shaderName, BatchFormat format) {
        registrations.checkOpen();
        Objects.requireNonNull(shaderName, "shaderName");
        registrations.batchFormats.replace(plugin, shaderName, Objects.requireNonNull(format, "format"));
        return this;
    }
}
