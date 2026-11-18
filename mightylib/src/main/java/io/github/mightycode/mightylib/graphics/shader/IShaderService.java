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
    /**
     * The declared shaders by name (the "shaders" object of the merged shaders.json): "files" (vertex, fragment, and
     * geometry when there are three, relative to folder()), "tags", optional "defines". Read-only.
     */
    JSONObject declarations();

    // Logical folder of the shaders in the sources of the resources, ending with '/' (an include not found next to
    // its file is searched there)
    String folder();

    /**
     * Written before the sources of a shader, ending with a line break: #version first, then the global defines
     * sorted by name, then those of the entry (they win over a global one of the same name). Called by a worker once
     * per reading of a shader; must not fail.
     *
     * @param entryDefines the defines of the entry of the shader (name to value, "" for a define without value),
     *                     already checked (ShaderService.CheckDefine)
     */
    String prelude(Map<String, String> entryDefines);

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
