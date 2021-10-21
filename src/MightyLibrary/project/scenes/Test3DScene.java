package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shape._2D.RectangleRenderer;
import MightyLibrary.mightylib.graphics.texture.Animation;
import MightyLibrary.mightylib.graphics.texture.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.graphics.texture.Texture;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.graphics.shape.Renderer;
import MightyLibrary.mightylib.graphics.shape._3D.ModelRenderer;
import MightyLibrary.mightylib.scene.Camera;
import MightyLibrary.mightylib.graphics.shape._3D.CubeRenderer;
import MightyLibrary.mightylib.graphics.shape.Shape;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.Id;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Test3DScene extends Scene {

    private Renderer sBlock;
    private ModelRenderer stand;
    private RectangleRenderer hudBar;
    private float move = 150f;

    // Textures
    private Texture displacementMap;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private CubeRenderer light;

    private float counter = 0;

    private Animator animator;

    public void init(String[] args){
        /// SCENE INFORMATIONS ///

        manContainer.mouseManager.setCursor(false);
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        // Cube
        light = new CubeRenderer("colorShape3D", new Vector3f(-1f, 1.0f, -4f), 1f);

        // Platform of cubes
        sBlock = new Renderer("textureComplex3D", false, false);
        float[] cratesInfo = createCrates(10);
        sBlock.getShape().addAllVbo(cratesInfo, new int[]{3, 2}, Shape.STATIC_STORE, Shape.STATIC_STORE);
        sBlock.setPosition(new Vector3f(0.0f));
        sBlock.setTexture("container");
            // Displacement texture for cubes/crate
        displacementMap = manContainer.resources.getResource(Texture.class,"dispMap1");
        manContainer.shadManager.getShader(sBlock.getShape().getShaderId()).glUniform("displacementMap", 1);

        // 3D Model
        stand = new ModelRenderer("texture3D", "stand/stall", "stall");
        stand.setPosition(new Vector3f(0.0f, 4.0f,0.0f));

        // Grey Rect in Hud
        /*hudBar = new RectangleRenderer("colorShape2D").setSizePix( window.size.x * 0.3f, window.size.y * 0.3f);
        hudBar.setPosition(window.size.x * 0.7f, window.size.y * 0.7f);
        hudBar.setColor(new Color4f(0.5f, 0.5f, 0.5f, 1.0f));*/
        hudBar = new RectangleRenderer("texture2D");
        hudBar.setTexture("slime");
        hudBar.setSizePix( 150, 150);//window.size.x * 0.3f, window.size.y * 0.3f);
        hudBar.setPosition(move, 150); //window.size.x * 0.7f, window.size.y * 0.7f);

        animator = new Animator(new Vector2f());
        animator.addAndInitAnimation("first", manContainer.resources.getResource(AnimationData.class, "slime"), true);

        System.out.println(cratesInfo.length);
        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }


    public void update() {
        super.update();

        InputManager inputManager = manContainer.inpManager;

        int speed = 1;
        if (inputManager.inputPressed(ActionId.SHIFT)) {
            speed = 3;
        }

        if(inputManager.input(ActionId.MOVE_LEFT)){
            manContainer.cam.speedAngX(Camera.speed.x * speed);
        }

        if(inputManager.input(ActionId.MOVE_RIGHT)){
            manContainer.cam.speedAngX(-Camera.speed.x * speed);
        }

        if(inputManager.input(ActionId.MOVE_FORWARD)){
            manContainer.cam.speedAngZ(-Camera.speed.z * speed);
        }

        if(inputManager.input(ActionId.MOVE_BACKWARD)) {
            manContainer.cam.speedAngZ(Camera.speed.z * speed);
        }

        if(inputManager.input(ActionId.MOVE_UP)) {
            manContainer.cam.setY(manContainer.cam.camPos.y += Camera.speed.y);
        }

        if(inputManager.input(ActionId.MOVE_DOWN)) {
            manContainer.cam.setY(manContainer.cam.camPos.y -= Camera.speed.y);
        }

        if(inputManager.inputPressed(ActionId.ESCAPE)) {
            manContainer.cam.invertLockViewCursor();
            manContainer.mouseManager.invertCursorState();

        }


        light.setColor(new Color4f(counter / 360.0f));
        manContainer.shadManager.getShader(sBlock.getShape().getShaderId()).glUniform("time", counter / 720);

        counter += 1f;

        if(counter > 720) counter = 0f;

        manContainer.cam.updateView();

        animator.update();
        hudBar.setTexture(animator.getCurrentAnimation().getData().getTextureName());
        hudBar.setTexturePosition(animator.getCurrentAnimation().currentTexturePosition());
        hudBar.updateShape();
    }


    public void display() {
        super.setVirtualScene();
        clear();
        // Better to draw the world here
        //light.display();
        displacementMap.bind(1);
        stand.display();

        sBlock.display();

        // Better to draw the hud here and be not affected by the post processing shader
        hudBar.display();
        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();
        sBlock.unload();
        light.unload();
        hudBar.unload();
        stand.unload();
    }


    public float [] createCrates(int size){
        float[] array = new float[size * size * boxSize];

        for(int i = 0; i < size; ++i) {
            for(int a = 0; a < size; ++a) {
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
        for(int i = 0; i < 6; ++i){
            placeValueToCoords(array, new float[]{0.0f, 1.0f}, index + (i) * faceSize + 3);
            placeValueToCoords(array, new float[]{1.0f, 1.0f}, index + (i) * faceSize + 3 + lineSize);
            placeValueToCoords(array, new float[]{1.0f, 0.0f}, index + (i) * faceSize + 3 + 2 * lineSize);
            placeValueToCoords(array, new float[]{1.0f, 0.0f}, index + (i) * faceSize + 3 + 3 * lineSize);
            placeValueToCoords(array, new float[]{0.0f, 0.0f}, index + (i) * faceSize + 3 + 4 * lineSize);
            placeValueToCoords(array, new float[]{0.0f, 1.0f}, index + (i) * faceSize + 3 + 5 * lineSize);
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
        for(int i = 0; i < value.length; ++i){
            array[coordStart + i] = value[i];
        }
    }
}