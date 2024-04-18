package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.resources.texture.IGLBindable;
import MightyLibrary.mightylib.resources.texture.TextureParameters;

import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer implements IRenderTextureBindable {

    private int fbo;
    private int rbo;

    private int width, height;

    private int renderTextureId;

    public FrameBuffer(IGLBindable bindable, int width, int height){
        this.width = width;
        this.height = height;

        fbo = 0;
        rbo = 0;
        renderTextureId = 0;

        fbo = glGenFramebuffers();
        bindFrameBuffer();
        update(bindable);
        unbindFrameBuffer();
    }

    public void update(IGLBindable bindable){
        renderTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, renderTextureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);

        TextureParameters.applyParameters(bindable);

        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTextureId, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
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

    @Override
    public void bindRenderTexture(int position){
        glActiveTexture(GL_TEXTURE0 + position);
        glBindTexture(GL_TEXTURE_2D, renderTextureId);
    }

    public void unbindRenderTexture(){
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public int getWidth() { return width; }

    @Override
    public int getHeight() { return height; }

    @Override
    public int getRenderTextureId(){
        return renderTextureId;
    }

    public void unload(){
        glDeleteFramebuffers(fbo);
        glDeleteTextures(renderTextureId);
        glDeleteRenderbuffers(rbo);
    }
}
