package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.main.WindowInfo;
import MightyLibrary.mightylib.resources.texture.IGLBindable;
import MightyLibrary.mightylib.resources.texture.TextureParameters;
import MightyLibrary.mightylib.util.Id;

import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {

    private int fbo;
    private int rbo;

    private final Id renderTextureId;

    private final WindowInfo windowInfo;

    public FrameBuffer(WindowInfo window, IGLBindable bindable){
        fbo = 0;
        rbo = 0;
        renderTextureId = new Id(0);
        this.windowInfo = window;

        fbo = glGenFramebuffers();
        bindFrameBuffer();
        update(bindable);
        unbindFrameBuffer();
    }

    public void update(IGLBindable bindable){
        renderTextureId.id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, renderTextureId.id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, windowInfo.getVirtualSizeRef().x, windowInfo.getVirtualSizeRef().y, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);

        TextureParameters.applyParameters(bindable);

        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTextureId.id, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, windowInfo.getVirtualSizeRef().x, windowInfo.getVirtualSizeRef().y);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Make attention, the framebuffer doesn't work properly");
    }

    public void bindFrameBuffer(){
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    }


    public void unbindFrameBuffer(){
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }


    public void bindRenderTexture(){
        bindRenderTexture(0);
    }

    public void bindRenderTexture(int position){
        glActiveTexture(GL_TEXTURE0 + position);
        glBindTexture(GL_TEXTURE_2D, renderTextureId.id);
    }

    public void unbindRenderTexture(){
        glBindTexture(GL_TEXTURE_2D, 0);
    }


    public Id getTexture(){
        return renderTextureId;
    }


    public void unload(){
        glDeleteFramebuffers(fbo);
        glDeleteTextures(renderTextureId.id);
        glDeleteRenderbuffers(rbo);
    }
}
