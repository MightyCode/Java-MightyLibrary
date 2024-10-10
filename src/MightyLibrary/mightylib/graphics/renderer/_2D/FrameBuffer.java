package MightyLibrary.mightylib.graphics.renderer._2D;

import MightyLibrary.mightylib.graphics.surface.IGLBindable;
import MightyLibrary.mightylib.graphics.surface.TextureParameters;
import MightyLibrary.mightylib.utils.Logger;

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
        Logger.CheckOpenGLError("Generate FrameBuffer : " + fbo);

        bindFrameBuffer();
        update(bindable);
        unbindFrameBuffer();
    }

    protected void update(IGLBindable bindable){
        renderTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, renderTextureId);
        Logger.CheckOpenGLError("Generate Texture : " + renderTextureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        Logger.CheckOpenGLError("Texture Image 2D : " + renderTextureId);

        TextureParameters.applyParameters(bindable);
        Logger.CheckOpenGLError("Texture Parameters : " + renderTextureId);

        glBindTexture(GL_TEXTURE_2D, 0);
        Logger.CheckOpenGLError("Bind Texture : " + renderTextureId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTextureId, 0);
        Logger.CheckOpenGLError("FrameBuffer Texture 2D : " + renderTextureId);

        rbo = glGenRenderbuffers();
        Logger.CheckOpenGLError("Generate RenderBuffer : " + rbo);
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        Logger.CheckOpenGLError("Bind RenderBuffer : " + rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        Logger.CheckOpenGLError("RenderBuffer Storage : " + rbo);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);
        Logger.CheckOpenGLError("FrameBuffer RenderBuffer : " + rbo);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Make attention, the framebuffer doesn't work properly");
    }

    public void bindFrameBuffer(){
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        Logger.CheckOpenGLError("Bind FrameBuffer : " + fbo);
    }


    public void unbindFrameBuffer(){
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        Logger.CheckOpenGLError("Unbind FrameBuffer : " + fbo);
    }


    public void bindRenderTexture(){
        bindRenderTexture(0);
    }

    @Override
    public void bindRenderTexture(int position){
        glActiveTexture(GL_TEXTURE0 + position);
        Logger.CheckOpenGLError("Active Texture : " + position);
        glBindTexture(GL_TEXTURE_2D, renderTextureId);
        Logger.CheckOpenGLError("Bind Texture : " + renderTextureId);
    }

    public void unbindRenderTexture(){
        glBindTexture(GL_TEXTURE_2D, 0);
        Logger.CheckOpenGLError("Unbind Texture : " + renderTextureId);
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
        Logger.CheckOpenGLError("Delete FrameBuffer : " + fbo);

        glDeleteTextures(renderTextureId);
        Logger.CheckOpenGLError("Delete Texture : " + renderTextureId);

        glDeleteRenderbuffers(rbo);
        Logger.CheckOpenGLError("Delete RenderBuffer : " + rbo);
    }
}
