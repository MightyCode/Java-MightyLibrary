package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.GLElement;
import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.internal.RefCount;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderData;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.texture.Texture;
import io.github.mightycode.mightylib.resources.texture.TextureData;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * How things are drawn, shared by the renderers that draw them alike: the program (its fallback while it loads or
 * when it failed), the values of uniforms common to its renderers, the textures (main texture on unit 0, channels on
 * units 1 to MAX_TEXTURE_CHANNELS) and the OpenGL state (RenderState: depth, culling, blend, color space).
 * <p>
 * A renderer created with a shader name has a material of its own. The material of a renderer is shared only
 * explicitly (Renderer.setMaterial, a constructor that takes a Material): changing a shared material changes all its
 * renderers, there is never an implicit copy. What differs between its renderers stays in each renderer (transform,
 * color, visibility, camera, and its own values, which win over those of the material).
 * <p>
 * Each renderer that draws it retains it, and releases it when unloaded; the last release frees it (the program and
 * the textures given back). A material created by the game starts without reference: the game retains it itself to
 * keep it after its renderers. Main thread only.
 */
@API(status = EXPERIMENTAL)
public final class Material implements IUniformTarget {
    // Texture units 1 to MAX_TEXTURE_CHANNELS of setTextureChannel (unit 0 is the main texture)
    public static final int MAX_TEXTURE_CHANNELS = 30;

    // Belong to the engine of the view that creates the material
    private final GLResources glResources = Views.engine().glResources();
    private final Resources resources = Views.engine().resources();

    // Program of the shader, held (urgent reference) until the material is freed
    private final Shader shader;
    private final boolean in2D;
    // Drawn with while shader is not linked, cached at the first use (see getShader)
    private Shader fallbackShader;
    private ShaderSink sink;
    private ShaderSink fallbackSink;

    private final UniformValues values = new UniformValues();
    private IRenderTextureBindable mainTexture;
    private IRenderTextureBindable[] textures;
    private final RenderState renderState;

    private final RefCount references = new RefCount("Material", this::free);
    private int version;

    /**
     * Takes the program of the shader (the fallback one while it loads or when it failed); the tag of the shader
     * chooses the default depth state (2D: neither test nor write).
     */
    public Material(String shaderName) {
        // Unknown or failed: the fallback data, whose program is linked from the start
        ShaderData shaderData = resources.getResourceOrFallback(ShaderData.class, shaderName);
        shader = (Shader) glResources.acquire(shaderData, true);
        in2D = shaderData.isDimension2DShader();

        renderState = new RenderState(!in2D, !in2D, false);
    }

    // ---- Program ----

    /** The program if linked, otherwise the fallback one (failed or still loading), always linked. */
    public Shader getShader() {
        if (shader.isLoaded())
            return shader;

        // Fixed once the fallback data are loaded: no lookup at each draw
        if (fallbackShader == null)
            fallbackShader = (Shader) glResources.fallback(ShaderData.class);
        return fallbackShader;
    }

    /** The tag of the shader: 2D (camera 2D, no depth by default) or 3D. */
    public boolean isDimension2D() {
        return in2D;
    }

    // The current program as the target of the values (the same object for a program: its locations are kept)
    IUniformSink sink() {
        Shader current = getShader();
        if (current == shader) {
            if (sink == null)
                sink = new ShaderSink(shader);
            return sink;
        }

        if (fallbackSink == null || fallbackSink.shader != current)
            fallbackSink = new ShaderSink(current);
        return fallbackSink;
    }

    // ---- Values ----

    @Override
    public Material addShaderValue(String name, Class<?> type, Object object) {
        values.put(new ShaderValue(name, type, object), false);
        changed();
        return this;
    }

    /** A value sent only when the program has its uniform (never reported missing). */
    public Material putOptionalValue(ShaderValue value) {
        values.put(value, true);
        changed();
        return this;
    }

    @Override
    public Material updateShaderValue(String name, Object object) {
        ShaderValue value = values.get(name);
        if (value == null)
            throw new IllegalArgumentException("No shader value " + name + " in the material: add it first with "
                    + "addShaderValue(name, type, object)");

        value.setObject(object);
        changed();
        return this;
    }

    @Override
    public Material removeShaderValue(String name) {
        if (values.remove(name))
            changed();
        return this;
    }

    @Override
    public boolean hasShaderValue(String name) {
        return values.has(name);
    }

    /** The value of this name, null when there is none. */
    public ShaderValue getShaderValue(String name) {
        return values.get(name);
    }

    // Sends the values of the material to its current program, but those that the renderer gives itself
    void sendValues(UniformValues overrides) {
        values.send(sink(), overrides);
    }

    // ---- Textures ----

    /**
     * The shared texture of the TextureData of this name, on unit 0 (an unknown name gives the fallback texture).
     */
    public Material setMainTexture(String name) {
        return setMainTexture(acquireTexture(name));
    }

    /**
     * The texture on unit 0. A GL element given here is referenced by the material: released when replaced or when
     * the material is freed.
     */
    public Material setMainTexture(IRenderTextureBindable texture) {
        if (mainTexture == texture) {
            // Already held: the new reference is not needed
            if (texture instanceof GLElement)
                glResources.releaseResource((GLElement) texture);
            return this;
        }

        releaseTexture(mainTexture);
        mainTexture = texture;
        changed();
        return this;
    }

    /** The texture on unit 0, null when none. */
    public IRenderTextureBindable getMainTexture() {
        return mainTexture;
    }

    /** The shared texture of this name on the unit (1 to MAX_TEXTURE_CHANNELS). */
    public Material setTextureChannel(String name, int unit) {
        checkTextureChannel(unit);
        return setTextureChannel(acquireTexture(name), unit);
    }

    /**
     * The texture on the unit (1 to MAX_TEXTURE_CHANNELS), the one already there released (when a GL element).
     * @throws IllegalArgumentException if unit is out of range
     */
    public Material setTextureChannel(IRenderTextureBindable texture, int unit) {
        checkTextureChannel(unit);

        if (textures == null)
            textures = new IRenderTextureBindable[MAX_TEXTURE_CHANNELS];
        else if (textures[unit - 1] == texture) {
            if (texture instanceof GLElement)
                glResources.releaseResource((GLElement) texture);
            return this;
        } else
            releaseTexture(textures[unit - 1]);

        textures[unit - 1] = texture;
        changed();
        return this;
    }

    /** The texture on the unit (1 to MAX_TEXTURE_CHANNELS), null when none. */
    public IRenderTextureBindable getTextureChannel(int unit) {
        checkTextureChannel(unit);
        return textures == null ? null : textures[unit - 1];
    }

    static void checkTextureChannel(int unit) {
        if (unit <= 0 || unit > MAX_TEXTURE_CHANNELS)
            throw new IllegalArgumentException("Texture channel " + unit + " out of 1.." + MAX_TEXTURE_CHANNELS
                    + " (unit 0 is the main texture: setMainTexture)");
    }

    // Binds the textures to their units (before the draw)
    void bindTextures() {
        if (mainTexture != null)
            mainTexture.bindRenderTexture(0);

        if (textures != null) {
            for (int i = 0; i < textures.length; ++i) {
                if (textures[i] != null)
                    textures[i].bindRenderTexture(i + 1);
            }
        }
    }

    private Texture acquireTexture(String name) {
        return (Texture) glResources.acquire(resources.getResourceOrFallback(TextureData.class, name), true);
    }

    private void releaseTexture(IRenderTextureBindable texture) {
        if (texture instanceof GLElement)
            glResources.releaseResource((GLElement) texture);
    }

    // ---- State ----

    // Applied before each draw of its renderers
    RenderState renderState() {
        return renderState;
    }

    public Material setDepthTest(boolean depthTest) {
        renderState.setDepthTest(depthTest);
        changed();
        return this;
    }

    public boolean isDepthTest() {
        return renderState.isDepthTest();
    }

    public Material setDepthWrite(boolean depthWrite) {
        renderState.setDepthWrite(depthWrite);
        changed();
        return this;
    }

    public boolean isDepthWrite() {
        return renderState.isDepthWrite();
    }

    public Material setFaceCulling(boolean faceCulling) {
        renderState.setFaceCulling(faceCulling);
        changed();
        return this;
    }

    public boolean isFaceCulling() {
        return renderState.isFaceCulling();
    }

    public Material setBlendMode(EBlendMode blendMode) {
        renderState.setBlendMode(blendMode);
        changed();
        return this;
    }

    public EBlendMode getBlendMode() {
        return renderState.getBlendMode();
    }

    // INHERIT (the default) takes the mode of the view; LINEAR or GAMMA fixes the mode of the material
    public Material setColorSpace(ERenderColorSpace colorSpace) {
        renderState.setColorSpace(colorSpace);
        changed();
        return this;
    }

    public ERenderColorSpace getColorSpace() {
        return renderState.getColorSpace();
    }

    // ---- Sharing ----

    /** One more user of this material (a renderer that draws with it, or the game to keep it). */
    public void retain() {
        references.retain();
    }

    /**
     * A user is done with this material: the last one frees it (program and textures given back).
     * @return true when this release freed the material
     * @throws IllegalStateException when released more times than retained
     */
    public boolean release() {
        return references.release();
    }

    /** Number of users that retained this material and did not release it. */
    public int references() {
        return references.references();
    }

    public boolean isFreed() {
        return references.isFreed();
    }

    private void free() {
        glResources.releaseResource(shader);
        releaseTexture(mainTexture);
        mainTexture = null;
        if (textures != null) {
            for (int i = 0; i < textures.length; ++i) {
                releaseTexture(textures[i]);
                textures[i] = null;
            }
        }
    }

    // ---- Sorting ----

    private void changed() {
        ++version;
    }

    /**
     * Incremented by each change made through the material (value added, changed or removed, texture, state): what
     * depends on its content (a batch) is built again when it changes. An object of a value changed in place by
     * the game is not seen.
     */
    public int version() {
        return version;
    }

    /**
     * The part of the material in the sort key of the draws: equal for two materials in the same state, so that their
     * draws follow each other. Bits, high to low: current program id (10), hash of the textures and their units (14),
     * blend mode (3), depth test and write (2), culling (1). A hint for sorting: two different states may collide.
     */
    public int sortKeyPart() {
        int program = getShader().getShaderId() & 0x3FF;
        int texturesHash = texturesHash() & 0x3FFF;
        int blend = renderState.getBlendMode().ordinal() & 0x7;
        int depth = (renderState.isDepthTest() ? 2 : 0) | (renderState.isDepthWrite() ? 1 : 0);
        int culling = renderState.isFaceCulling() ? 1 : 0;

        return program << 20 | texturesHash << 6 | blend << 3 | depth << 1 | culling;
    }

    private int texturesHash() {
        int hash = mainTexture == null ? 0 : System.identityHashCode(mainTexture);
        if (textures != null) {
            for (int i = 0; i < textures.length; ++i) {
                if (textures[i] != null)
                    hash = 31 * hash + (i + 1) * System.identityHashCode(textures[i]);
            }
        }

        // Folds the high bits into the kept ones
        return hash ^ (hash >>> 14) ^ (hash >>> 28);
    }

    // The values of the material sent to a program
    private static final class ShaderSink implements IUniformSink {
        private final Shader shader;

        ShaderSink(Shader shader) {
            this.shader = shader;
        }

        @Override
        public long programStamp() {
            return shader.programStamp();
        }

        @Override
        public int location(String name) {
            return shader.location(name);
        }

        @Override
        public void send(int location, ShaderValue value) {
            shader.sendValue(location, value);
        }

        @Override
        public void reportMissing(String name) {
            shader.reportMissingUniform(name);
        }
    }
}
