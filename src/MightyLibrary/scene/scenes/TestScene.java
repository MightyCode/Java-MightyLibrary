package MightyLibrary.scene.scenes;

import MightyLibrary.main.ManagerContainer;
import MightyLibrary.main.Window;
import MightyLibrary.render.texture.TextureParameters;
import MightyLibrary.scene.Camera;
import MightyLibrary.render.shape.Renderer.ColoredCubeRenderer;
import MightyLibrary.render.shape.Shape;
import MightyLibrary.util.Id;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;

public class TestScene extends Scene {
    private Matrix4f model = new Matrix4f();
    private FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    private Shape sBlock;
    private Shape hudBar;
    private Shape screenShape;
    // Textures
    private Id block, displacementMap;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private ColoredCubeRenderer light;

    private float counter = 0;

    // VARIABLES TEST
    int fbo, rbo;
    int screen;
    Window window;

    public TestScene(String[] args){
        window = ManagerContainer.getInstance().window;
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        screen = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, screen);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, window.virtualSize.x, window.virtualSize.y, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        TextureParameters.realisticParameters();
        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, screen, 0);

        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, window.virtualSize.x, window.virtualSize.y);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);

        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            System.err.println("ARCHTUNG DIE FRAMEBUFFER IST KAPUTT");
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }


    public void init(){
        light = new ColoredCubeRenderer(new Vector3f(3.0f, 3.0f, -3.0f), 0.5f);
        light.setColor(new Vector3f(1.0f, 0.2f, 0.4f));

        sBlock = new Shape("texture3D", false, false);

        // Textures
        block = manContainer.texManager.getIdShaderFromString("container");
        displacementMap = manContainer.texManager.getIdShaderFromString("dispMap1");


        // init cube
        float vertex0[] = createCrates(16 * 16);

        sBlock.setReading(new int[]{3, 2});
        sBlock.setVbo(vertex0);

        model.identity();
        model.get(fb);
        manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("model", fb);
        //manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("displacementMap", 1);
        fb.clear();

        hudBar = new Shape("colorShape2D", false, true);
        float vertex1[] = new float[]{
            1f, 0.75f,
            1f, 1f,
            0.75f, 1f,

            1f, 0.75f,
            0.75f, 0.75f,
            0.75f, 1f
        };
        manContainer.shadManager.getShader(hudBar.getShaderId()).glUniform("color", 0.5f, 0.5f, 0.5f, 1f);

        hudBar.setReading(new int[]{2});
        hudBar.setVbo(vertex1);

        screenShape = new Shape("texture2D", false, true);
        float vertex2[] = new float[]{
                -1.0f,  1.0f,  0.0f, 1.0f,
                -1.0f, -1.0f,  0.0f, 0.0f,
                1.0f, -1.0f,  1.0f, 0.0f,

                -1.0f,  1.0f,  0.0f, 1.0f,
                1.0f, -1.0f,  1.0f, 0.0f,
                1.0f,  1.0f,  1.0f, 1.0f
        };

        screenShape.setReading(new int[]{2, 2});
        screenShape.setVbo(vertex2);

        manContainer.mouseManager.setCursor(false);
        setClearColor(52, 189, 235, 1f);
    }


    public void update() {
        if(manContainer.keyManager.getKeyState(GLFW_KEY_A)){
            manContainer.cam.speedAngX(Camera.speed.x);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_D)){
            manContainer.cam.speedAngX(-Camera.speed.x);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_W)){
            manContainer.cam.speedAngZ(-Camera.speed.z);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_S)) {
            manContainer.cam.speedAngZ(Camera.speed.z);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_SPACE)) {
            manContainer.cam.setY(manContainer.cam.camPos.y += Camera.speed.y);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_LEFT_SHIFT)) {
            manContainer.cam.setY(manContainer.cam.camPos.y -= Camera.speed.y);
        }

        if(manContainer.keyManager.keyPressed(GLFW_KEY_ESCAPE)) {
            //manContainer.texManager.reload();
            manContainer.cam.invertLockViewCursor();
            manContainer.mouseManager.invertCursorState();
            //manContainer.screenManager.exit();
        }

        if(manContainer.keyManager.keyPressed(GLFW_KEY_F5)) {
            manContainer.texManager.reload();
        }

        light.setColor(new Vector3f(counter / 360.0f));
        light.setColor(new Vector3f(counter / 360.0f));
        light.updateColor();
        //manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("time", counter / 720);

        counter += 1f;
        if(counter > 720) counter = 0;
        manContainer.cam.updateView();
    }


    public void display() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        window.setVirtualViewport();

        clear();
        light.display();
        manContainer.texManager.bind(block);
        manContainer.texManager.bind(displacementMap, 1);
        sBlock.display();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        window.setRealViewport();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, screen);
        screenShape.display();
        hudBar.display();
    }


    public void unload(){
        sBlock.unload();
        light.unload();
        hudBar.unload();
        screenShape.unload();

        glDeleteFramebuffers(fbo);
        glDeleteTextures(screen);
    }


    public float [] createCrates(int size){
        float[] array = new float[size * size * boxSize];

        for(int i = 0; i < size; i++) {
            for(int a = 0; a < size; a++) {
                placeCrates(array, a, 1, -i, a, i, size);
            }
        }

        return array;
    }


    public void placeCrates(float[] array, float realPosX, float realPosZ, float realPosY, int posX, int posY, int maxSizeY){
        int index = boxSize * (posY * maxSizeY + posX);
        placeCoord(array, realPosX, realPosZ, realPosY, index);
        placeText(array, index);
        //placeNormal(array, index);

               /* -0.5f, -0.5f, -0.5f,   0.0f, 0.0f,   0.0f, 0.0f, -1.0f,
                0.5f, -0.5f, -0.5f,    1.0f, 0.0f,   0.0f, 0.0f, -1.0f,
                0.5f,  0.5f, -0.5f,    1.0f, 1.0f,   0.0f, 0.0f, -1.0f,
                0.5f,  0.5f, -0.5f,    1.0f, 1.0f,   0.0f, 0.0f, -1.0f,
                -0.5f,  0.5f, -0.5f,   0.0f, 1.0f,   0.0f, 0.0f, -1.0f,
                -0.5f, -0.5f, -0.5f,   0.0f, 0.0f,   0.0f, 0.0f, -1.0f,


                -0.5f, -0.5f,  0.5f,   0.0f, 0.0f,   0.0f, 0.0f, 1.0f,
                0.5f, -0.5f,  0.5f,    1.0f, 0.0f,   0.0f, 0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f,   0.0f, 0.0f, 1.0f,
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f,   0.0f, 0.0f, 1.0f,
                -0.5f,  0.5f,  0.5f,   0.0f, 1.0f,   0.0f, 0.0f, 1.0f,
                -0.5f, -0.5f,  0.5f,   0.0f, 0.0f,   0.0f, 0.0f, 1.0f,

                -0.5f,  0.5f,  0.5f,   0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,   1.0f, 0.0f,  -1.0f, 0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,   1.0f, 1.0f,  -1.0f, 0.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,   1.0f, 1.0f,  -1.0f, 0.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,   0.0f, 1.0f,  -1.0f, 0.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,   0.0f, 0.0f,  -1.0f, 0.0f, 0.0f,

                0.5f,  0.5f,  0.5f,    0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
                0.5f,  0.5f, -0.5f,    1.0f, 0.0f,  1.0f, 0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,    1.0f, 1.0f,  1.0f, 0.0f, 0.0f,
                0.5f, -0.5f, -0.5f,    1.0f, 1.0f,  1.0f, 0.0f, 0.0f,
                0.5f, -0.5f,  0.5f,    0.0f, 1.0f,  1.0f, 0.0f, 0.0f,
                0.5f,  0.5f,  0.5f,    0.0f, 0.0f,  1.0f, 0.0f, 0.0f,

                -0.5f, -0.5f, -0.5f,   0.0f, 0.0f,   0.0f, -1.0f, 0.0f,
                0.5f, -0.5f, -0.5f,    1.0f, 0.0f,   0.0f, -1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,    1.0f, 1.0f,   0.0f, -1.0f, 0.0f,
                0.5f, -0.5f,  0.5f,    1.0f, 1.0f,   0.0f, -1.0f, 0.0f,
                -0.5f, -0.5f,  0.5f,   0.0f, 1.0f,   0.0f, -1.0f, 0.0f,
                -0.5f, -0.5f, -0.5f,   0.0f, 0.0f,   0.0f, -1.0f, 0.0f,

                -0.5f,  0.5f, -0.5f,   0.0f, 0.0f,   0.0f, 1.0f, 0.0f,
                0.5f,  0.5f, -0.5f,    1.0f, 0.0f,   0.0f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f,   0.0f, 1.0f, 0.0f,
                0.5f,  0.5f,  0.5f,    1.0f, 1.0f,   0.0f, 1.0f, 0.0f,
                -0.5f,  0.5f,  0.5f,   0.0f, 1.0f,   0.0f, 1.0f, 0.0f,
                -0.5f,  0.5f, -0.5f,   0.0f, 0.0f,   0.0f, 1.0f, 0.0f*/
    }


    public void placeCoord(float[] array, float realPosX, float realPosZ, float realPosY, int index){
        // Right
        placeValueToCoords(array, new float[]{realPosX , realPosZ , realPosY  }, index  + 0 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + 1 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 2 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 3 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 4 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 5 * lineSize);

        // Down
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 0 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 1 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 4 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 5 * lineSize + faceSize);

        // Left
        placeValueToCoords(array, new float[]{realPosX, realPosZ , realPosY}, index  + 0 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 1 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 4 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 5 * lineSize + 2 * faceSize);

        // Up
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 0 * lineSize +  3* faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 1 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + 2 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + 3 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 4 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 5 * lineSize + 3 * faceSize);

        // Bottom
        placeValueToCoords(array, new float[]{realPosX , realPosZ , realPosY}, index  + 0 * lineSize +  4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + 1 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 2 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY + 1}, index  + 3 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 4 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX , realPosZ, realPosY}, index  + 5 * lineSize + 4 * faceSize);

        // Top
        placeValueToCoords(array, new float[]{realPosX , realPosZ + 1 , realPosY}, index  + 0 * lineSize +  5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 1 * lineSize + 5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + 5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + 5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 4 * lineSize + 5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX , realPosZ + 1, realPosY}, index  + 5 * lineSize + 5 * faceSize);
    }


    public void placeText(float[] array, int index){
        for(int i = 0; i < 6; i++){
            placeValueToCoords(array, new float[]{0.0f, 0.0f}, index + (i) * faceSize + 3);
            placeValueToCoords(array, new float[]{1.0f, 0.0f}, index + (i) * faceSize + 3 + lineSize);
            placeValueToCoords(array, new float[]{1.0f, 1.0f}, index + (i) * faceSize + 3 + 2 * lineSize);
            placeValueToCoords(array, new float[]{1.0f, 1.0f}, index + (i) * faceSize + 3 + 3 * lineSize);
            placeValueToCoords(array, new float[]{0.0f, 1.0f}, index + (i) * faceSize + 3 + 4 * lineSize);
            placeValueToCoords(array, new float[]{0.0f, 0.0f}, index + (i) * faceSize + 3 + 5 * lineSize);
        }
    }


    public void placeNormal(float[] array, int index){
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{0.0f, 0.0f, -1.0f}, index + (i) * lineSize + 5);
        }
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{0.0f, 0.0f, 1.0f}, index + (i) * lineSize + 5 + faceSize);
        }
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{-1.0f, 0.0f, 0.0f}, index + (i) * lineSize + 5 + 2 * faceSize);
        }
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{1.0f, 0.0f, 0.0f}, index + (i) * lineSize + 5 + 3 * faceSize);
        }
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{0.0f, -1.0f, 0.0f}, index + (i) * lineSize + 5 + 4 * faceSize);
        }
        for(int i = 0; i < 6; i ++){
            placeValueToCoords(array, new float[]{0.0f, 1.0f, 0.0f}, index + (i) * lineSize + 5 + 5 * faceSize);
        }

    }


    public void placeValueToCoords(float[] array, float[] value, int coordStart){
        for(int i = 0; i < value.length; i++){
            array[coordStart + i] = value[i];
        }
    }
}