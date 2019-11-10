package MightyLibrary.render.shape._2D;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.main.Window;
import MightyLibrary.render.texture.TextureParameters;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_COMPLETE;

public class FrameBuffer {

    private int fbo;
    private int rbo;

    private int renderTexture;

    private Window window;

    public FrameBuffer(){
        fbo = 0;
        rbo = 0;
        renderTexture = 0;
        window = ManagerContainer.getInstance().window;

        fbo = glGenFramebuffers();
        bindFrameBuffer();
        update();
        unbindFrameBuffer();
    }

    public void update(){
        renderTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, renderTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, window.virtualSize.x, window.virtualSize.y, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        TextureParameters.realisticParameters();
        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, renderTexture, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, window.virtualSize.x, window.virtualSize.y);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("ARCHTUNG DIE FRAMEBUFFER IST KAPUTT");
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
        glBindTexture(GL_TEXTURE_2D, renderTexture);
    }

    public void unbindRenderTexture(){
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void unload(){
        glDeleteFramebuffers(fbo);
        glDeleteTextures(renderTexture);
        glDeleteRenderbuffers(rbo);
    }
}
