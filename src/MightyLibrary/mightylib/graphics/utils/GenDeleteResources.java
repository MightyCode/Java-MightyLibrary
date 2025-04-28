package MightyLibrary.mightylib.graphics.utils;

import MightyLibrary.mightylib.utils.valueDebug.TableDebug;
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

    public static int GenVertexArrays() {
        int vao = GL30.glGenVertexArrays();

        VAOs.add(vao);

        return vao;
    }

    public static void DeleteVertexArrays(Integer vao) {
        VAOs.remove(vao);
        GL30.glDeleteVertexArrays(vao);
    }

    public static int GenBuffers() {
        int vbo = glGenBuffers();
        VBOs.add(vbo);
        return vbo;
    }

    public static void DeleteBuffers(Integer vbo) {
        VBOs.remove(vbo);
        glDeleteBuffers(vbo);
    }

    public static int GenBuffersEBO() {
        int ebo = glGenBuffers();
        EBOs.add(ebo);
        return ebo;
    }

    public static void DeleteBuffersEBO(Integer ebo) {
        EBOs.remove(ebo);
        glDeleteBuffers(ebo);
    }

    public static int GenTexture() {
        int texture = glGenTextures();
        Textures.add(texture);
        return texture;
    }

    public static void DeleteTexture(Integer texture) {
        Textures.add(texture);
        glDeleteTextures(texture);
    }

    public static int GenFramebuffers() {
        int fbo = glGenFramebuffers();
        FBOs.add(fbo);
        return fbo;
    }

    public static void DeleteFramebuffers(Integer fbo) {
        FBOs.remove(fbo);
        glDeleteFramebuffers(fbo);
    }

    public static int GenRenderBuffers() {
        int rbo = glGenRenderbuffers();
        RBOs.add(rbo);
        return rbo;
    }

    public static void DeleteRenderBuffers(Integer rbo) {
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

    public static void PrintAll() {
        for (EGraphicResources g : EGraphicResources.values()) {
            PrintList(g);
        }
    }
}
