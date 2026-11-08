package io.github.mightycode.mightylib.graphics.utils;

import io.github.mightycode.mightylib.graphics.GLContext;
import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.valueDebug.TableDebug;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.lwjgl.opengl.GL30.*;

public abstract class GenDeleteResources {
    private static boolean Initialized = false;
    private static ArrayList<Integer> VBOs;
    private static ArrayList<Integer> EBOs;
    private static ArrayList<Integer> PBOs;

    private static ArrayList<Integer> Textures;


    private GenDeleteResources(){}

    public enum EGraphicResources {
        VertexArrayBuffer,
        VertexBufferObject,
        ElementBufferObject,
        PixelBufferObject,
        Texture,
        FramebufferObject,
        RenderBufferObject
    }

    public static void Init() {
        if (Initialized)
            return;

        VBOs = new ArrayList<>();
        EBOs = new ArrayList<>();
        PBOs = new ArrayList<>();

        Textures = new ArrayList<>();


        Initialized = true;
    }

    // GL objects only exist in the context of the main thread: a call from a worker fails or corrupts the driver state
    private static void CheckMainThread(String operation) {
        if (Logger.DO_CHECK && !MainThread.isMainThread())
            throw new IllegalStateException(operation + " called from thread " + Thread.currentThread().getName()
                    + ", GL calls must run on the main thread (MainThread.execute)");
    }

    // Vertex arrays, framebuffers and renderbuffers belong to the context that created them (the current one)
    public static int GenVertexArrays() {
        CheckMainThread("GenVertexArrays");
        int vao = GL30.glGenVertexArrays();

        GLContext.current().addVertexArray(vao);

        return vao;
    }

    // Deleted at once when the owner is the current context, at its next makeCurrent otherwise
    public static void DeleteVertexArrays(GLContext owner, Integer vao) {
        CheckMainThread("DeleteVertexArrays");
        DeleteInOwner(owner, () -> {
            owner.removeVertexArray(vao);
            GL30.glDeleteVertexArrays(vao);
        });
    }

    private static void DeleteInOwner(GLContext owner, Runnable deletion) {
        // Destroyed with its window: the driver freed its objects
        if (owner.isDestroyed())
            return;

        if (owner == GLContext.currentOrNull())
            deletion.run();
        else
            owner.deleteLater(deletion);
    }

    public static int GenBuffers() {
        CheckMainThread("GenBuffers");
        int vbo = glGenBuffers();
        VBOs.add(vbo);
        return vbo;
    }

    public static void DeleteBuffers(Integer vbo) {
        CheckMainThread("DeleteBuffers");
        VBOs.remove(vbo);
        glDeleteBuffers(vbo);
    }

    public static int GenBuffersEBO() {
        CheckMainThread("GenBuffersEBO");
        int ebo = glGenBuffers();
        EBOs.add(ebo);
        return ebo;
    }

    public static void DeleteBuffersEBO(Integer ebo) {
        CheckMainThread("DeleteBuffersEBO");
        EBOs.remove(ebo);
        glDeleteBuffers(ebo);
    }

    // Buffer used to send pixels to a texture (GL_PIXEL_UNPACK_BUFFER)
    public static int GenPixelBuffer() {
        CheckMainThread("GenPixelBuffer");
        int pbo = glGenBuffers();
        PBOs.add(pbo);
        return pbo;
    }

    public static void DeletePixelBuffer(Integer pbo) {
        CheckMainThread("DeletePixelBuffer");
        PBOs.remove(pbo);
        glDeleteBuffers(pbo);
    }

    public static int GenTexture() {
        CheckMainThread("GenTexture");
        int texture = glGenTextures();
        Textures.add(texture);
        return texture;
    }

    public static void DeleteTexture(Integer texture) {
        CheckMainThread("DeleteTexture");
        Textures.remove(texture);
        glDeleteTextures(texture);
    }

    public static int GenFramebuffers() {
        CheckMainThread("GenFramebuffers");
        int fbo = glGenFramebuffers();
        GLContext.current().addFramebuffer(fbo);
        return fbo;
    }

    public static void DeleteFramebuffers(GLContext owner, Integer fbo) {
        CheckMainThread("DeleteFramebuffers");
        DeleteInOwner(owner, () -> {
            owner.removeFramebuffer(fbo);
            glDeleteFramebuffers(fbo);
        });
    }

    public static int GenRenderBuffers() {
        CheckMainThread("GenRenderBuffers");
        int rbo = glGenRenderbuffers();
        GLContext.current().addRenderbuffer(rbo);
        return rbo;
    }

    public static void DeleteRenderBuffers(GLContext owner, Integer rbo) {
        CheckMainThread("DeleteRenderBuffers");
        DeleteInOwner(owner, () -> {
            owner.removeRenderbuffer(rbo);
            glDeleteRenderbuffers(rbo);
        });
    }

    // What the contexts alive own, of one kind (ids of different contexts can be equal)
    private static ArrayList<Integer> PerContext(Function<GLContext, List<Integer>> owned) {
        ArrayList<Integer> all = new ArrayList<>();
        for (GLContext context : GLContext.live())
            all.addAll(owned.apply(context));

        return all;
    }

    public static void PrintList(EGraphicResources graphicResources) {
        switch (graphicResources) {
            case VertexArrayBuffer:
                System.out.print("VertexArrayObjects (VAOs): ");
                TableDebug.printi(PerContext(GLContext::vertexArrays));
                break;
            case VertexBufferObject:
                System.out.print("VertexBufferObjects (VBOs): ");
                TableDebug.printi(VBOs);
                break;
            case ElementBufferObject:
                System.out.print("ElementBufferObjects (EBOs): ");
                TableDebug.printi(EBOs);
                break;
            case PixelBufferObject:
                System.out.print("PixelBufferObjects (PBOs): ");
                TableDebug.printi(PBOs);
                break;
            case Texture:
                System.out.print("Textures: ");
                TableDebug.printi(Textures);
                break;
            case FramebufferObject:
                System.out.print("FramebufferObjects (FBOs): ");
                TableDebug.printi(PerContext(GLContext::framebuffers));
                break;
            case RenderBufferObject:
                System.out.print("RenderBufferObjects (RBOs): ");
                TableDebug.printi(PerContext(GLContext::renderbuffers));
                break;
            default:
                System.out.print("Unknown resource type.");
                break;
        }
    }

    // Number of objects of this type currently alive
    public static int Count(EGraphicResources graphicResources) {
        switch (graphicResources) {
            case VertexArrayBuffer:
                return PerContext(GLContext::vertexArrays).size();
            case VertexBufferObject:
                return VBOs.size();
            case ElementBufferObject:
                return EBOs.size();
            case PixelBufferObject:
                return PBOs.size();
            case Texture:
                return Textures.size();
            case FramebufferObject:
                return PerContext(GLContext::framebuffers).size();
            case RenderBufferObject:
                return PerContext(GLContext::renderbuffers).size();
            default:
                return 0;
        }
    }

    public static void PrintAll() {
        for (EGraphicResources g : EGraphicResources.values()) {
            PrintList(g);
        }
    }
}
