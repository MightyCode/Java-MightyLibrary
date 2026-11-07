package io.github.mightycode.mightylib.graphics.utils;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.valueDebug.TableDebug;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;

public abstract class GenDeleteResources {
    private static boolean Initialized = false;
    private static ArrayList<Integer> VAOs;
    private static ArrayList<Integer> VBOs;
    private static ArrayList<Integer> EBOs;

    private static ArrayList<Integer> Textures;

    private static ArrayList<Integer> FBOs;
    private static ArrayList<Integer> RBOs;

    private GenDeleteResources(){}

    public enum EGraphicResources {
        VertexArrayBuffer,
        VertexBufferObject,
        ElementBufferObject,
        Texture,
        FramebufferObject,
        RenderBufferObject
    }

    public static void Init() {
        if (Initialized)
            return;

        VAOs = new ArrayList<>();
        VBOs = new ArrayList<>();
        EBOs = new ArrayList<>();

        Textures = new ArrayList<>();

        FBOs = new ArrayList<>();
        RBOs = new ArrayList<>();

        Initialized = true;
    }

    // GL objects only exist in the context of the main thread: a call from a worker fails or corrupts the driver state
    private static void CheckMainThread(String operation) {
        if (Logger.DO_CHECK && !MainThread.isMainThread())
            throw new IllegalStateException(operation + " called from thread " + Thread.currentThread().getName()
                    + ", GL calls must run on the main thread (MainThread.execute)");
    }

    public static int GenVertexArrays() {
        CheckMainThread("GenVertexArrays");
        int vao = GL30.glGenVertexArrays();

        VAOs.add(vao);

        return vao;
    }

    public static void DeleteVertexArrays(Integer vao) {
        CheckMainThread("DeleteVertexArrays");
        VAOs.remove(vao);
        GL30.glDeleteVertexArrays(vao);
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
        FBOs.add(fbo);
        return fbo;
    }

    public static void DeleteFramebuffers(Integer fbo) {
        CheckMainThread("DeleteFramebuffers");
        FBOs.remove(fbo);
        glDeleteFramebuffers(fbo);
    }

    public static int GenRenderBuffers() {
        CheckMainThread("GenRenderBuffers");
        int rbo = glGenRenderbuffers();
        RBOs.add(rbo);
        return rbo;
    }

    public static void DeleteRenderBuffers(Integer rbo) {
        CheckMainThread("DeleteRenderBuffers");
        RBOs.remove(rbo);
        glDeleteRenderbuffers(rbo);
    }

    public static void PrintList(EGraphicResources graphicResources) {
        switch (graphicResources) {
            case VertexArrayBuffer:
                System.out.print("VertexArrayObjects (VAOs): ");
                TableDebug.printi(VAOs);
                break;
            case VertexBufferObject:
                System.out.print("VertexBufferObjects (VBOs): ");
                TableDebug.printi(VBOs);
                break;
            case ElementBufferObject:
                System.out.print("ElementBufferObjects (EBOs): ");
                TableDebug.printi(EBOs);
                break;
            case Texture:
                System.out.print("Textures: ");
                TableDebug.printi(Textures);
                break;
            case FramebufferObject:
                System.out.print("FramebufferObjects (FBOs): ");
                TableDebug.printi(FBOs);
                break;
            case RenderBufferObject:
                System.out.print("RenderBufferObjects (RBOs): ");
                TableDebug.printi(RBOs);
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
                return VAOs.size();
            case VertexBufferObject:
                return VBOs.size();
            case ElementBufferObject:
                return EBOs.size();
            case Texture:
                return Textures.size();
            case FramebufferObject:
                return FBOs.size();
            case RenderBufferObject:
                return RBOs.size();
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
