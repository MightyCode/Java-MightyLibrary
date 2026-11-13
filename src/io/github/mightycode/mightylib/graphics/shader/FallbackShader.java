package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

/**
 * Program of the fallback shader (FallbackShaderData), shared by the renderers whose program failed or is not linked yet.
 * Its matrices start as identity, then each renderer sends it its generic matrices (projection, view, model), kept
 * until another renderer sends its own. A quad already in screen coordinates (VirtualSceneRenderer, post processing)
 * is therefore shrunk to a few pixels: the screen stays black rather than magenta, during the first frames too
 * (drawn before the post processing program is linked). It has none of the other uniforms of the shaders it
 * replaces: they are not reported missing.
 */
public class FallbackShader extends Shader {
    private static final String[] MATRICES = {
            ShaderManager.GENERIC_PROJECTION_FIELD_NAME,
            ShaderManager.GENERIC_VIEW_FIELD_NAME,
            ShaderManager.GENERIC_MODEL_FIELD_NAME
    };

    public FallbackShader(FallbackShaderData data) {
        super(data);
    }

    @Override
    protected void afterLink() {
        FloatBuffer identity = new Matrix4f().get(BufferUtils.createFloatBuffer(16));
        use();
        for (String matrix : MATRICES) {
            int link = getLink(matrix);
            if (link != -1)
                glUniformMatrix4fv(link, false, identity);
        }
        Logger.CheckOpenGLError("Fallback shader matrices");
    }

    @Override
    protected boolean reportsMissingUniforms() {
        return false;
    }
}
