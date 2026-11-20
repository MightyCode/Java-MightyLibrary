package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.services.IEngineService;
import org.apiguardian.api.API;
import org.json.JSONObject;

import java.util.Map;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * The shaders of the engine (service mightylib:shaders, Engine.shaders): their declarations (shaders.json merged
 * through the sources of the resources) and the prelude written before each of their sources. The GLSL files have no
 * #version: the service writes it, so a replaced service (registry.services().replace) can aim at another version of
 * GLSL. ShaderService by default. Without GL: read by the workers of the resources while they preprocess the shaders.
 */
@API(status = EXPERIMENTAL)
public interface IShaderService extends IEngineService {
    // Tag of a shader that draws a 2D batch (fixed vertex format: position, UV, color RGBA8)
    String BATCH_2D_TAG = "batch2D";
    // Tag of a shader that draws a 2D batch of sprite quads by instances
    String BATCH_2D_INSTANCED_TAG = "batch2DInstanced";

    /**
     * The declared shaders by name (the "shaders" object of the merged shaders.json): "files" (vertex, fragment, and
     * geometry when there are three, relative to folder()), "tags", optional "defines". A copy: changing it changes
     * nothing in the service.
     */
    JSONObject declarations();

    /**
     * The batch equivalent of a shader: the "batch" field of its entry, when it names a declared shader tagged
     * BATCH_2D_TAG; null otherwise (no field, unknown or untagged target: the shader is drawn alone). Any thread.
     */
    String batchShader(String shaderName);

    /**
     * The instanced batch equivalent of a shader: the "batchInstanced" field of its entry, when it names a declared
     * shader tagged BATCH_2D_INSTANCED_TAG; null otherwise. Any thread.
     */
    String batchInstancedShader(String shaderName);

    // Logical folder of the shaders in the sources of the resources, ending with '/' (an include not found next to
    // its file is searched there)
    String folder();

    /**
     * Written before the sources of a shader, ending with a line break: #version first, then the global defines
     * sorted by name, then those of the entry (they win over a global one of the same name). Called by a worker once
     * per reading of a shader; must not fail.
     *
     * @param entryDefines the defines of the entry of the shader (name to value, "" for a define without value),
     *                     already checked (checkDefine)
     */
    String prelude(Map<String, String> entryDefines);

    /**
     * The rule of the defines, global (setDefine) or of an entry (checked by the loader before reading the shader):
     * by default the name is a GLSL identifier not starting with GL_ (reserved), the value is on one line (a value from
     * a command or a mod can't slip a directive into the prelude). A replaced service may change it, the prelude must
     * then accept what it accepts. Any thread.
     *
     * @throws IllegalArgumentException if the define is refused
     */
    default void checkDefine(String name, String value) {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*"))
            throw new IllegalArgumentException("Invalid define name \"" + name + "\": expected a GLSL identifier");
        if (name.startsWith("GL_"))
            throw new IllegalArgumentException("Invalid define name " + name + ": GL_ is reserved by GLSL");
        if (value == null)
            throw new IllegalArgumentException("The value of the define " + name
                    + " is not a string, a number or a boolean (\"\" for none)");
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0)
            throw new IllegalArgumentException("The value of the define " + name + " has a line break");
    }

    /**
     * Global define written in the prelude of every shader ("" for "#define NAME"); every shader is read again when
     * it changes, nothing happens when the value is the same. Main thread.
     *
     * @throws IllegalArgumentException if the name is not a GLSL identifier, starts with GL_, or the value has a line
     *                                  break
     */
    void setDefine(String name, String value);

    // Removes a global define (every shader is read again); nothing if there is none of this name. Main thread
    void removeDefine(String name);

    // The global defines sorted by name: an immutable snapshot, not changed by a later setDefine
    Map<String, String> defines();
}
