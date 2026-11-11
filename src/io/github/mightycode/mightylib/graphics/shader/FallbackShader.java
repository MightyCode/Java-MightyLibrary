package io.github.mightycode.mightylib.graphics.shader;

import io.github.mightycode.mightylib.utils.Logger;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;

/**
 * Program of the fallback shader (FallbackShaderData). The matrices start as identity, so a full screen quad
 * without matrices (post processing) is magenta too. It has none of the uniforms of the shaders it replaces:
 * they are not reported missing.
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
