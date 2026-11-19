package io.github.mightycode.mightylib.graphics.renderer._2D;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.color.IColorService;
import io.github.mightycode.mightylib.graphics.color.PictureSettings;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.shader.ShaderUniforms;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.main.IView;
import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.main.WindowInfo;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.resources.EngineResources;

import java.util.function.BiConsumer;

/**
 * The framebuffer of a view (sRGB) and its final pass to the window: the shader of the view, or of the engine
 * (IColorService.finalPassShader, EngineResources.POST_PROCESSING by default), with the settings of the picture.
 */
public final class VirtualSceneRenderer extends RectangleRenderer {
    private final FrameBuffer frameBuffer;
    private final PassValues values = new PassValues();
    // Final pass of another shader than the default one, created when the chosen shader changes
    private PassRenderer otherPass;

    public VirtualSceneRenderer(WindowInfo info, IGLBindable bindable){
        super(EngineResources.POST_PROCESSING);
        // Written to the window without the sRGB encoding: the final pass encodes itself
        setColorSpace(ERenderColorSpace.GAMMA);
        values.register(this::putOptionalValue);
        frameBuffer = new FrameBuffer(bindable, info.getVirtualSizeRef().x, info.getVirtualSizeRef().y);
    }

    // The whole window, in normalized device coordinates
    @Override
    protected float[] calculatePosition() {
        return WholeWindow();
    }

    private static float[] WholeWindow() {
        return new float[]{
                -1, 1,
                -1, -1,
                1, -1,
                1, 1
        };
    }

    // The pass of the current view (resolved at each frame: a change takes effect at once)
    @Override
    public void display() {
        IView view = Views.current();
        IColorService colors = view.engine().colors();
        String shaderName = colors.resolveFinalPassShader(view.finalPassShader());
        PictureSettings picture = colors.resolvePicture(view.picture());

        if (EngineResources.POST_PROCESSING.equals(shaderName)) {
            releaseOtherPass();
            values.update(picture);
            super.display();
            return;
        }

        if (otherPass == null || !otherPass.shaderName.equals(shaderName)) {
            releaseOtherPass();
            otherPass = new PassRenderer(shaderName);
        }
        otherPass.values.update(picture);
        otherPass.display();
    }

    // The shader drawing the final pass now
    public String finalPassShader() {
        return otherPass != null ? otherPass.shaderName : EngineResources.POST_PROCESSING;
    }

    public FrameBuffer getFrameBuffer(){
        return frameBuffer;
    }


    public void bindFrameBuff(){
        frameBuffer.bindFrameBuffer();
    }


    public void unbindFrameBuff(){
        frameBuffer.unbindFrameBuffer();
        frameBuffer.bindRenderTexture();
    }

    private void releaseOtherPass() {
        if (otherPass != null) {
            otherPass.unload();
            otherPass = null;
        }
    }

    @Override
    public void unload() {
        super.unload();
        releaseOtherPass();
        frameBuffer.unload();
    }

    // The settings of the picture, sent when the shader of the pass has them
    private static final class PassValues {
        private final ShaderValue gamma = new ShaderValue(ShaderUniforms.GAMMA, Float.class, 1f);
        private final ShaderValue brightness = new ShaderValue(ShaderUniforms.BRIGHTNESS, Float.class, 1f);
        private final ShaderValue contrast = new ShaderValue(ShaderUniforms.CONTRAST, Float.class, 1f);
        private PictureSettings sent;

        void register(BiConsumer<String, ShaderValue> putOptionalValue) {
            putOptionalValue.accept(gamma.getName(), gamma);
            putOptionalValue.accept(brightness.getName(), brightness);
            putOptionalValue.accept(contrast.getName(), contrast);
        }

        // Immutable settings: the same object changes nothing
        void update(PictureSettings picture) {
            if (picture == sent)
                return;

            gamma.setObject(picture.gamma());
            brightness.setObject(picture.brightness());
            contrast.setObject(picture.contrast());
            sent = picture;
        }
    }

    private static final class PassRenderer extends RectangleRenderer {
        private final String shaderName;
        private final PassValues values = new PassValues();

        PassRenderer(String shaderName) {
            super(shaderName);
            this.shaderName = shaderName;
            setColorSpace(ERenderColorSpace.GAMMA);
            values.register(this::putOptionalValue);
        }

        @Override
        protected float[] calculatePosition() {
            return WholeWindow();
        }
    }
}
