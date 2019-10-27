package MightyLibrary.scene.scenes;

import MightyLibrary.scene.Camera;
import MightyLibrary.render.shape.Renderer.ColoredCubeRenderer;
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
    private Shape textHudBlock;
    // Texture
    private Id block;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private ColoredCubeRenderer light;

    private float counter = 0;

    public TestScene(String[] args){
    }

    public void init(){
        light = new ColoredCubeRenderer(new Vector3f(3.0f, 3.0f, -3.0f), 0.5f);
        light.setColor(new Vector3f(1.0f, 0.2f, 0.4f));

        sBlock = new Shape("texture3D", false, false);
        // Texture
        block = manContainer.texManager.getIdShaderFromString("container");

        // init cube
        float vertex0[] = createCrates(16 * 16);

        sBlock.setReading(new int[]{3, 2});
        sBlock.setVbo(vertex0);

        model.identity();
        model.get(fb);
        manContainer.shadManager.getShader(sBlock.getShaderId()).glUniform("model", fb);
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

        textHudBlock = new Shape("texture2D", false, true);
        float vertex2[] = new float[]{
                -1f, -0.75f,        0.0f, 1.0f,
                -1f, -1f,           0.0f, 0.0f,
                -0.75f, -1f,        1.0f, 0.0f,

                -1f, -0.75f,        0.0f, 1.0f,
                -0.75f, -0.75f,     1.0f, 1.0f,
                -0.75f, -1f,        1.0f, 0.0f,
        };
        textHudBlock.setReading(new int[]{2, 2});
        textHudBlock.setVbo(vertex2);

        manContainer.mouseManager.setCursor(false);
        setClearColor(52, 189, 235, 1f);
    }

    public void update() {
        manContainer.cam.setToCursor();

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
            manContainer.mouseManager.invertCursorState();
           //manContainer.screenManager.exit();
        }

        light.setColor(new Vector3f(counter / 360.0f));
        light.updateColor();

        counter += 2f;
        if(counter > 360) counter = 0;
        manContainer.cam.updateView();
    }

    public void display() {
        clear();
        light.display();
        manContainer.texManager.bind(block);

        sBlock.display();
        hudBar.display();
        textHudBlock.display();
    }

    public void unload(){
        sBlock.unload();
        light.unload();
        hudBar.unload();
        textHudBlock.unload();
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
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 0 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + 1 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 2 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 3 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 4 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 5 * lineSize);

        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 0 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 1 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 4 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 5 * lineSize + faceSize);

        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 0 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 1 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 2 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ , realPosY}, index  + 3 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 4 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 5 * lineSize + 2 * faceSize);

        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 0 * lineSize +  3* faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 1 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + 2 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + 3 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 4 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 5 * lineSize + 3 * faceSize);

        placeValueToCoords(array, new float[]{realPosX , realPosZ , realPosY}, index  + 0 * lineSize +  4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + 1 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 2 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY + 1}, index  + 3 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 4 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX , realPosZ, realPosY}, index  + 5 * lineSize + 4 * faceSize);

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