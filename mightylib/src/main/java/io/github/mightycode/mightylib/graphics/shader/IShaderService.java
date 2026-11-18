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
     * Written before the source of a shader, ending with a line break: #version first, then the defines. Called by a
     * worker for each source read; must not fail.
     *
     * @param entryDefines the defines of the entry of the shader (name to value, "" for a define without value)
     */
    String prelude(Map<String, String> entryDefines);
}
