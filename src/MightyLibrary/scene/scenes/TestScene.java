package MightyLibrary.scene.scenes;

import MightyLibrary.render.shape._3D.OBJLoader;
import MightyLibrary.scene.Camera;
import MightyLibrary.render.shape._3D.ColoredCubeRenderer;
import MightyLibrary.render.shape.Shape;
import MightyLibrary.util.Id;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class TestScene extends Scene {
    private Matrix4f model = new Matrix4f();
    private FloatBuffer fb = BufferUtils.createFloatBuffer(16);

    private Shape sBlock;
    private Shape hudBar;
    private Shape _3DModels;
    // Textures
    private Id block, displacementMap, stall;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private ColoredCubeRenderer light;

    private float counter = 0;

    public TestScene(String[] args){
        super();
    }


    public void init(){
        light = new ColoredCubeRenderer(new Vector3f(3.0f, 3.0f, -3.0f), 0.5f);
        light.setColor(new Vector3f(1.0f, 0.2f, 0.4f));

        sBlock = new Shape("textureComplex3D", false, false);

        // Textures
        block = manContainer.texManager.getIdShaderFromString("container");
        displacementMap = manContainer.texManager.getIdShaderFromString("dispMap1");


        // init cube
        float[] vertex0 = createCrates(16 * 16);

        sBlock.setReading(new int[]{3, 2});
        sBlock.setVbo(vertex0);

        model.identity();
        model.get(fb);
        manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("model", fb);
        manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("displacementMap", 1);

        hudBar = new Shape("colorShape2D", true, true);
        float vertex1[] = new float[]{
            1f, 1f,
            0.0f, 1f,
            1f, 0.0f,
            0.0f, 0.0f

        };

        int ebo1[] = new int[]{
                0, 1, 2, 1, 2, 3
        };

        manContainer.shadManager.getShader(hudBar.getShaderId()).glUniform("color", 0.5f, 0.5f, 0.5f, 1f);

        hudBar.setEbo(ebo1);
        hudBar.setVbo(vertex1);
        hudBar.setReading(new int[]{2});

        manContainer.mouseManager.setCursor(false);
        setClearColor(52, 189, 235, 1f);

        _3DModels = OBJLoader.loadObjModel("stand/stall");
        manContainer.shadManager.getShader(_3DModels.getShaderId()).glUniform("model", fb);
        stall = manContainer.texManager.getIdShaderFromString("stall");
    }


    public void update() {
        int speed = 1;
        if (manContainer.keyManager.getKeyState(GLFW_KEY_LEFT_SHIFT)) {
            speed = 3;
        }


        if(manContainer.keyManager.getKeyState(GLFW_KEY_A)){
            manContainer.cam.speedAngX(Camera.speed.x * speed);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_D)){
            manContainer.cam.speedAngX(-Camera.speed.x * speed);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_W)){
            manContainer.cam.speedAngZ(-Camera.speed.z * speed);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_S)) {
            manContainer.cam.speedAngZ(Camera.speed.z * speed);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_SPACE)) {
            manContainer.cam.setY(manContainer.cam.camPos.y += Camera.speed.y);
        }
        if(manContainer.keyManager.getKeyState(GLFW_KEY_LEFT_CONTROL)) {
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
        manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("time", counter / 720);

        counter += 1f;
        if(counter > 720) counter = 0;
        manContainer.cam.updateView();
    }


    public void display() {
        super.setVirtualScene();
        // Better to draw the world here
        clear();
        light.display();
        manContainer.texManager.bind(block);
       // manContainer.texManager.bind(displacementMap, 1);
       // sBlock.display();
        manContainer.texManager.bind(stall);
       _3DModels.display();

        super.setAndDisplayRealScene();
        // Better to draw the hud here and be not affected by the post processing shader
        //hudBar.display();
    }


    public void unload(){
        super.unload();
        sBlock.unload();
        light.unload();
        hudBar.unload();
        _3DModels.unload();
        fb.clear();
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