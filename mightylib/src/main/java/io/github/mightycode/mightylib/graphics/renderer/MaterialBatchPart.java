package io.github.mightycode.mightylib.graphics.renderer;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.renderer._2D.IRenderTextureBindable;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.BatchFormat;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.Batches2D;
import io.github.mightycode.mightylib.graphics.renderer._2D.batch.internal.IBatchMaterial;
import io.github.mightycode.mightylib.graphics.shader.Shader;
import io.github.mightycode.mightylib.graphics.shader.ShaderUniforms;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.Objects;

/**
 * What a material gives to its batched draws, taken when its version or its program changes: the batch program,
 * the textures by unit (the white texture on unit 0 when its program reads none, or it has none), the OpenGL state,
 * and the material itself when it has common values (sent to the batch program). Equal content, equal parts:
 * materials that draw alike share their batched draws.
 */
final class MaterialBatchPart implements IBatchMaterial {
    private final Batches2D batches;
    private final Shader shader;
    private final BatchFormat format;
    // Of the runs of quads drawn by instances, null when the shader has none ("batchInstanced")
    private final Shader instancedShader;
    private final BatchFormat instancedFormat;
    private final IRenderTextureBindable mainTexture;
    // Units 1 to MAX_TEXTURE_CHANNELS, null when none
    private final IRenderTextureBindable[] channels;
    private final boolean depthTest;
    private final boolean depthWrite;
    private final boolean faceCulling;
    private final EBlendMode blendMode;
    // With common values: the material, at this version (another version is another part)
    private final Material values;
    private final int valuesVersion;
    // Margin of the view culling of its draws in world units ("cullMargin" of the shaders)
    private final float cullMargin;
    private final int hash;

    private final RenderState renderState;
    // Sent at each run, reused
    private final ShaderValue projection = new ShaderValue(ShaderUniforms.PROJECTION, Matrix4f.class, null);
    private final ShaderValue view = new ShaderValue(ShaderUniforms.VIEW, Matrix4f.class, null);
    private final ShaderValue linearColors = new ShaderValue(ShaderUniforms.LINEAR_COLORS, Integer.class, 0);
    private final Vector2f viewportSize = new Vector2f();
    private final ShaderValue viewport = new ShaderValue(ShaderUniforms.VIEWPORT_SIZE, Vector2f.class, viewportSize);

    MaterialBatchPart(Batches2D batches, Shader shader, BatchFormat format, Shader instancedShader,
                      BatchFormat instancedFormat, IRenderTextureBindable mainTexture,
                      IRenderTextureBindable[] channels, RenderState state, Material values, float cullMargin) {
        this.batches = Objects.requireNonNull(batches, "batches");
        this.shader = Objects.requireNonNull(shader, "shader");
        this.format = Objects.requireNonNull(format, "format");
        this.instancedShader = instancedShader;
        this.instancedFormat = instancedFormat;
        this.mainTexture = Objects.requireNonNull(mainTexture, "mainTexture");
        this.channels = channels;
        depthTest = state.isDepthTest();
        depthWrite = state.isDepthWrite();
        faceCulling = state.isFaceCulling();
        blendMode = state.getBlendMode();
        this.values = values;
        valuesVersion = values == null ? 0 : values.version();
        this.cullMargin = cullMargin;

        renderState = new RenderState(depthTest, depthWrite, faceCulling);
        renderState.setBlendMode(blendMode);

        int h = System.identityHashCode(shader);
        h = 31 * h + System.identityHashCode(format);
        h = 31 * h + System.identityHashCode(instancedShader);
        h = 31 * h + System.identityHashCode(mainTexture);
        h = 31 * h + (channels == null ? 0 : identityHash(channels));
        h = 31 * h + (depthTest ? 4 : 0) + (depthWrite ? 2 : 0) + (faceCulling ? 1 : 0);
        h = 31 * h + blendMode.ordinal();
        h = 31 * h + System.identityHashCode(values);
        h = 31 * h + Float.floatToIntBits(cullMargin);
        hash = 31 * h + valuesVersion;
    }

    private static int identityHash(Object[] objects) {
        int h = 1;
        for (Object object : objects)
            h = 31 * h + System.identityHashCode(object);
        return h;
    }

    @Override
    public void apply(boolean linear, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        applyWith(shader, false, linear, projectionMatrix, viewMatrix);
    }

    @Override
    public float cullMargin() {
        return cullMargin;
    }

    @Override
    public BatchFormat format() {
        return format;
    }

    @Override
    public BatchFormat instancedFormat() {
        return instancedFormat;
    }

    // Its program loaded and reading its format (otherwise the quads are drawn by vertices)
    @Override
    public boolean hasInstanced() {
        return instancedShader != null && instancedShader.isLoaded()
                && batches.readsFormat(instancedShader, instancedFormat);
    }

    @Override
    public void applyInstanced(boolean linear, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        applyWith(instancedShader, true, linear, projectionMatrix, viewMatrix);
    }

    private void applyWith(Shader program, boolean instanced, boolean linear, Matrix4f projectionMatrix,
                           Matrix4f viewMatrix) {
        renderState.apply(linear);
        program.use();

        mainTexture.bindRenderTexture(0);
        if (channels != null) {
            for (int i = 0; i < channels.length; ++i) {
                if (channels[i] != null)
                    channels[i].bindRenderTexture(i + 1);
            }
        }

        if (values != null)
            values.sendBatchValues(instanced);

        // Optional: a batch program of a game may not have them
        projection.setObject(projectionMatrix);
        view.setObject(viewMatrix);
        linearColors.setObject(linear ? 1 : 0);
        SendIfPresent(program, projection);
        SendIfPresent(program, view);
        SendIfPresent(program, linearColors);
        GLContext context = GLContext.currentOrNull();
        if (context != null) {
            viewportSize.set(context.viewportWidth(), context.viewportHeight());
            SendIfPresent(program, viewport);
        }
    }

    private static void SendIfPresent(Shader program, ShaderValue value) {
        int location = program.location(value.getName());
        if (location != -1)
            program.sendValue(location, value);
    }

    // Compared by identity: the objects, not their content (a texture is the same one whatever it holds)
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (!(other instanceof MaterialBatchPart))
            return false;

        MaterialBatchPart part = (MaterialBatchPart) other;
        return hash == part.hash && shader == part.shader && format == part.format
                && instancedShader == part.instancedShader
                && mainTexture == part.mainTexture
                && sameObjects(channels, part.channels) && depthTest == part.depthTest
                && depthWrite == part.depthWrite && faceCulling == part.faceCulling && blendMode == part.blendMode
                && values == part.values && valuesVersion == part.valuesVersion
                && Float.compare(cullMargin, part.cullMargin) == 0;
    }

    private static boolean sameObjects(Object[] first, Object[] second) {
        if (first == null || second == null)
            return first == second;
        if (first.length != second.length)
            return false;

        for (int i = 0; i < first.length; ++i) {
            if (first[i] != second[i])
                return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "MaterialBatchPart[" + shader.getName() + ", " + format.name() + ", " + mainTexture.getClass().getSimpleName()
                + (channels == null ? "" : ", channels " + Arrays.stream(channels).filter(Objects::nonNull).count())
                + ", " + blendMode + (values == null ? "" : ", values v" + valuesVersion) + "]";
    }
}
