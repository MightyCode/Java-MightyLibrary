package io.github.mightycode.mightylib.graphics.renderer._2D;

import io.github.mightycode.mightylib.graphics.internal.GLContext;
import io.github.mightycode.mightylib.graphics.surface.IGLBindable;
import io.github.mightycode.mightylib.graphics.surface.TextureParameters;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.Logger;

import static org.lwjgl.opengl.GL30.*;

public final class FrameBuffer implements IRenderTextureBindable {
    // Framebuffers are not shared between contexts: it is used and deleted in the context that created it
    private final GLContext owner = GLContext.current();
    private int fbo;
    private int rbo;

    private final int width;
    private final int height;

    private int renderTextureId;

    public FrameBuffer(IGLBindable bindable, int width, int height){
        this.width = width;
        this.height = height;

        fbo = 0;
        rbo = 0;
        renderTextureId = 0;

        fbo = GenDeleteResources.GenFramebuffers();
        Logger.CheckOpenGLError("Generate FrameBuffer : " + fbo);

        bindFrameBuffer();
        update(bindable);
        unbindFrameBuffer();
    }

    private void update(IGLBindable bindable){
        renderTextureId = GenDeleteResources.GenTexture();

        GLContext.currentState().bindTexture(GL_TEXTURE_2D, renderTextureId);
        Logger.CheckOpenGLError("Generate Texture : " + renderTextureId);

        // Always sRGB: a linear draw is encoded by the card (GL_FRAMEBUFFER_SRGB), a gamma draw is stored as written;
        // read back in linear (the final pass encodes it again)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        Logger.CheckOpenGLError("Texture Image 2D : " + renderTextureId);

        TextureParameters.applyParameters(bindable);
        glTexParameteri(bindable.getTextureType(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        Logger.CheckOpenGLError("Texture Parameters : " + renderTextureId);

        GLContext.currentState().bindTexture(GL_TEXTURE_2D, 0);
        Logger.CheckOpenGLError("Bind Texture : " + renderTextureId);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTextureId, 0);
        Logger.CheckOpenGLError("FrameBuffer Texture 2D : " + renderTextureId);

        rbo = GenDeleteResources.GenRenderBuffers();
        Logger.CheckOpenGLError("Generate RenderBuffer : " + rbo);
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        Logger.CheckOpenGLError("Bind RenderBuffer : " + rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height);
        Logger.CheckOpenGLError("RenderBuffer Storage : " + rbo);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);
        Logger.CheckOpenGLError("FrameBuffer RenderBuffer : " + rbo);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            Logger.Error(Logger.CATEGORY_GL, "The framebuffer " + fbo + " is not complete");
    }

    public void bindFrameBuffer(){
        if (Logger.isGLErrorCheck() && GLContext.current() != owner)
            throw new IllegalStateException("The framebuffer " + fbo + " belongs to the OpenGL context " + owner.getName()
                    + ", the current one is " + GLContext.current().getName());

        // The batched draws asked before go to the framebuffer they were asked for
        GLContext.FlushCurrentDraws(GLContext.CAUSE_FRAMEBUFFER);
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        Logger.CheckOpenGLError("Bind FrameBuffer : " + fbo);
    }


    public void unbindFrameBuffer(){
        GLContext.FlushCurrentDraws(GLContext.CAUSE_FRAMEBUFFER);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        Logger.CheckOpenGLError("Unbind FrameBuffer : " + fbo);
    }


    public void bindRenderTexture(){
        bindRenderTexture(0);
    }

    @Override
    public void bindRenderTexture(int position){
        GLContext.currentState().activeTexture(position);
        GLContext.currentState().bindTexture(GL_TEXTURE_2D, renderTextureId);

        // Called for each draw: avoid building the message when checks are disabled
        if (Logger.isGLErrorCheck())
            Logger.CheckOpenGLError("Bind Texture : " + renderTextureId);
    }

    public void unbindRenderTexture(){
        GLContext.currentState().bindTexture(GL_TEXTURE_2D, 0);
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
        GenDeleteResources.DeleteFramebuffers(owner, fbo);
        Logger.CheckOpenGLError("Delete FrameBuffer : " + fbo);

        GenDeleteResources.DeleteTexture(renderTextureId);
        Logger.CheckOpenGLError("Delete Texture : " + renderTextureId);

        GenDeleteResources.DeleteRenderBuffers(owner, rbo);
        Logger.CheckOpenGLError("Delete RenderBuffer : " + rbo);
    }
}
