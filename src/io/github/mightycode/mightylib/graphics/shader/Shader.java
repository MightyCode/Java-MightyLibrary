package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.main.utils.EngineStats;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32C.GL_GEOMETRY_SHADER;

/**
 * The program of a ShaderData on the card, compiled and linked in steps (a stage per step, then the link).
 * A program that does not compile at its first loading makes the element failed (its users get the fallback);
 * one that does not compile on a hot reload keeps the previous program.
 * The programs are shared between the windows; the program in use is a state of each context (GLContext).
 */
public class Shader extends GLElement {
    // Reused to send matrices without allocating a direct buffer for each uniform
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    private final ShaderData data;

    private int program = -1;
    // Version of the data the program was built from
    private int programVersion;
    // Version whose hot reload failed: not tried again until the data changes again
    private int rejectedVersion = -1;
    // Program being built (load or reload), from the sources taken at its start
    private Build build;

    // These belong to the current program: emptied with each new program
    // Values sent, to skip those unchanged
    private final HashMap<String, ShaderValue> lastValues = new HashMap<>();
    // Uniform locations never change for a linked program
    private final HashMap<String, Integer> uniformLocations = new HashMap<>();
    // Uniforms already reported missing
    private final Set<String> missingUniforms = new HashSet<>();

    public Shader(ShaderData data) {
        this.data = data;
        addResourceDependency(data);
    }

    public ShaderData getData() {
        return data;
    }

    public boolean isDimension2DShader() {
        return data.isDimension2DShader();
    }

    // ---- Build, in steps ----

    // The sources and their version taken at the start: never a stage of a version with a stage of another
    private static final class Build {
        static final int VERTEX = 0, FRAGMENT = 1, GEOMETRY = 2, LINK = 3;

        final String vertex, fragment, geometry;
        // Files of the #line ids of each source, to translate the compiler log
        final List<String> vertexFiles, fragmentFiles, geometryFiles;
        final int version;
        int stage = VERTEX;
        int vertexId = -1, fragmentId = -1, geometryId = -1;

        Build(ShaderData data) {
            vertex = data.getVertexSource();
            fragment = data.getFragmentSource();
            geometry = data.getGeometrySource();
            version = data.getVersion();
            vertexFiles = data.getStageFiles(0);
            fragmentFiles = data.getStageFiles(1);
            geometryFiles = data.getStageFiles(2);
        }

        void delete() {
            vertexId = deleteStage(vertexId);
            fragmentId = deleteStage(fragmentId);
            geometryId = deleteStage(geometryId);
        }

        private static int deleteStage(int id) {
            if (id != -1)
                glDeleteShader(id);
            return -1;
        }
    }

    /**
     * One step of the build: the compilation of a stage, then the link. The data changed since the start: started
     * again with its new sources. A source that does not compile or link throws a ResourceException, the build deleted.
     * @return the linked program, -1 while not done
     */
    private int buildStep() {
        if (build != null && build.version != data.getVersion() && data.isLoaded()) {
            build.delete();
            build = null;
        }

        if (build == null)
            build = new Build(data);

        try {
            switch (build.stage) {
                case Build.VERTEX:
                    build.vertexId = compile(GL_VERTEX_SHADER, "Vertex", build.vertex, build.vertexFiles);
                    build.stage = Build.FRAGMENT;
                    return -1;
                case Build.FRAGMENT:
                    build.fragmentId = compile(GL_FRAGMENT_SHADER, "Fragment", build.fragment, build.fragmentFiles);
                    build.stage = build.geometry != null ? Build.GEOMETRY : Build.LINK;
                    return -1;
                case Build.GEOMETRY:
                    build.geometryId = compile(GL_GEOMETRY_SHADER, "Geometry", build.geometry, build.geometryFiles);
                    build.stage = Build.LINK;
                    return -1;
                default:
                    return link();
            }
        } catch (ResourceException e) {
            build.delete();
            build = null;
            throw e;
        }
    }

    private int compile(int type, String name, String source, List<String> files) {
        if (source == null)
            throw new ResourceException(name + " source missing");

        int id = glCreateShader(type);
        Logger.CheckOpenGLError("Create " + name + " shader of " + getName() + " (id : " + id + ")");

        glShaderSource(id, source);
        glCompileShader(id);
        Logger.CheckOpenGLError("Compile " + name + " shader of " + getName() + " (id : " + id + ")");

        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = ShaderPreprocessor.translateLog(glGetShaderInfoLog(id).trim(), files);
            glDeleteShader(id);
            throw new ResourceException(name + " shader does not compile:\n\t" + log);
        }

        return id;
    }

    private int link() {
        int id = glCreateProgram();
        Logger.CheckOpenGLError("Create program of " + getName() + " (id : " + id + ")");

        glAttachShader(id, build.vertexId);
        if (build.geometryId != -1)
            glAttachShader(id, build.geometryId);
        glAttachShader(id, build.fragmentId);
        glLinkProgram(id);
        Logger.CheckOpenGLError("Link program of " + getName() + " (id : " + id + ")");

        // Once linked, the program keeps what it needs
        build.delete();

        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(id);
            glDeleteProgram(id);
            throw new ResourceException("program does not link:\n\t" + log.trim());
        }

        return id;
    }

    // The new program replaces the current one: what was sent to the previous one is not in it
    private void setProgram(int newProgram, int version) {
        deleteProgram();
        program = newProgram;
        programVersion = version;
        build = null;
        afterLink();
    }

    // Called once a new program is in use (first load, hot reload)
    protected void afterLink() {}

    private void deleteProgram() {
        if (program != -1) {
            // The id can be reused by a new program, it must not be considered bound anymore
            GLContext.forgetProgram(program);
            glDeleteProgram(program);
            Logger.CheckOpenGLError("Delete program of " + getName() + " (id : " + program + ")");
            program = -1;
        }

        lastValues.clear();
        uniformLocations.clear();
        missingUniforms.clear();
    }

    @Override
    public boolean load(int remainingMilliseconds) {
        int built = buildStep();
        if (built == -1)
            return false;

        setProgram(built, build.version);
        return true;
    }

    // Its data was reloaded: built again by GLResources, the current program is used meanwhile
    private void checkVersion() {
        int version = data.getVersion();
        if (version != programVersion && version != rejectedVersion && data.isLoaded())
            requestReload();
    }

    @Override
    protected boolean reload(int remainingMilliseconds) {
        // Nothing being built, and the data being read again in place: the current program stays until it is loaded
        if (build == null) {
            int version = data.getVersion();
            if (!data.isLoaded() || version == programVersion || version == rejectedVersion)
                return true;
        }

        int built;
        try {
            built = buildStep();
        } catch (ResourceException e) {
            // The version that failed: buildStep starts again on the current version when the data changed
            rejectedVersion = data.getVersion();
            throw e;
        }

        if (built == -1)
            return false;

        setProgram(built, build.version);
        return true;
    }

    @Override
    public void unload(int remainingMilliseconds) {
        if (build != null) {
            build.delete();
            build = null;
        }

        deleteProgram();
        rejectedVersion = -1;
    }

    // ---- Use ----

    public void use() {
        checkVersion();

        GLContext context = GLContext.current();
        if (context.currentProgram() == program)
            return;

        glUseProgram(program);
        context.setCurrentProgram(program);
        EngineStats.countProgramBind();

        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Use program of " + getName() + " (id : " + program + ")");
    }

    public void use(String message) {
        glUseProgram(program);
        GLContext.current().setCurrentProgram(program);
        Logger.CheckOpenGLError(message);
    }

    // Location of the uniform in the current program, -1 when absent (or no program)
    public int getLink(String valueName) {
        if (program == -1)
            return -1;

        Integer location = uniformLocations.get(valueName);
        if (location == null) {
            location = glGetUniformLocation(program, valueName);
            uniformLocations.put(valueName, location);
        }

        return location;
    }

    public int getShaderId() {
        return program;
    }

    // False for the fallback: it has none of the uniforms of the shaders it replaces
    protected boolean reportsMissingUniforms() {
        return true;
    }

    /**
     * Send the value to the current program (skipped when unchanged). Only this program gets it: a new one
     * (hot reload, fallback) has none of the values sent before, so a renderer keeps its values and sends them
     * at each draw (Renderer.shaderValues).
     */
    public void sendValueToShader(ShaderValue value) {
        ShaderValue lastValue = lastValues.get(value.getName());

        if (lastValue != null && value.equals(lastValue) && !value.shouldForceUpdate())
            return;

        value.resetForceUpdate();

        // Remember the sent value, in place when possible to avoid allocating
        if (lastValue == null || !lastValue.copyValueFrom(value))
            lastValues.put(value.getName(), value.clone());

        use();
        EngineStats.countUniformUpload();
        int link = getLink(value.getName());

        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Get uniform location : " + value.getName());

        // Removed by the GLSL compiler when unused: reported once, ignored
        if (link == -1) {
            if (reportsMissingUniforms() && missingUniforms.add(value.getName()))
                System.err.println("--Uniform " + value.getName() + " not found in shader " + getName()
                        + " (unused or absent), its values are ignored");
            return;
        }

        if (value.getType() == Float.class) {
            Float v = value.getObjectTyped(Float.class);
            glUniform1f(link, v);
        } else if (value.getType() == Vector2f.class) {
            Vector2f v = value.getObjectTyped(Vector2f.class);
            glUniform2f(link, v.x, v.y);
        } else if (value.getType() == Vector3f.class) {
            Vector3f v = value.getObjectTyped(Vector3f.class);
            glUniform3f(link, v.x, v.y, v.z);
        } else if (value.getType() == Vector4f.class) {
            Vector4f v = value.getObjectTyped(Vector4f.class);
            glUniform4f(link, v.x, v.y, v.z, v.w);
        } else if (value.getType() == Color4f.class) {
            glUniform1i(link, value.getObjectTyped(Color4f.class).toIntRGBA());
        } else if (value.getType() == Matrix4f.class) {
            value.getObjectTyped(Matrix4f.class).get(MATRIX_BUFFER);
            glUniformMatrix4fv(link, false, MATRIX_BUFFER);
        } else if (value.getType() == Integer.class || value.getType() == Boolean.class) {
            Integer v = value.getObjectTyped(Integer.class);
            glUniform1i(link, v);
        } else {
            System.err.println("Unknown shader value type : " + value.getType());
        }
    }
}
