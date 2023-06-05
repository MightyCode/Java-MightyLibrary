package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.shader.ShaderManager;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.resources.texture.Texture;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer._3D.ModelRenderer;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.scene.Camera3D;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.scene.Camera3DCreationInfo;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.project.lib.ActionId;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Test3DScene extends Scene {

    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120f, new Vector3f(0, 4, 0));

    private Renderer sBlock;

    private Renderer sphere;

    private ModelRenderer stand;
    private RectangleRenderer hudBar;

    // Textures
    private Texture displacementMap;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private CubeRenderer light;

    private FloatTweening displacementMapTweening;

    private FloatTweening rotationTweening;

    public Test3DScene(){
        super(SCENE_CCI);
    }

    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        // Cube
        light = new CubeRenderer("colorComplex3D", new Vector3f(-1f, 3.0f, -4f), 1f);
        light.switchToColorMode(new Color4f(1f, 0f, 0f, 1f));
        light.addNormal();

        ShaderManager.getInstance().getShader(light.getShape().getShaderId()).glUniform("worldColor", 0.2f, 0.5f, 0.2f);
        Vector3f lightDir = new Vector3f(0.1f, 1f, 0.1f).normalize();
        ShaderManager.getInstance().getShader(light.getShape().getShaderId()).glUniform("lightDir", lightDir.x, lightDir.y, lightDir.z);
        ShaderManager.getInstance().getShader(light.getShape().getShaderId()).glUniform("lightColor", 0.8f, 0.8f, 0.8f);


        // Platform of cubes
        sBlock = new Renderer("textureComplex3D", false, false);
        float[] cratesInfo = createCrates(10);
        sBlock.getShape().addAllVbo(cratesInfo, new int[]{3, 2}, Shape.STATIC_STORE, Shape.STATIC_STORE);
        sBlock.switchToTextureMode("container");
            // Displacement texture for cubes/crate
        displacementMap = Resources.getInstance().getResource(Texture.class,"dispMap1");
        ShaderManager.getInstance().getShader(sBlock.getShape().getShaderId()).glUniform("displacementMap", 1);


        sphere = new Renderer("colorComplex3D", true, false);
        computeSphere(sphere);
        sphere.setScale(new Vector3f(10, 10, 10));
        sphere.setPosition(new Vector3f(20, 10, 10));

        // 3D Model
        stand = new ModelRenderer("texture3D", "stand/stall", "stall");
        stand.setPosition(new Vector3f(0.0f, 4.0f,0.0f));
        stand.setScale(new Vector3f(0.75f, 0.75f, 0.75f));

        // Grey Rect in Hud
        hudBar = new RectangleRenderer("texture2D");
        hudBar.switchToTextureMode("error");
        hudBar.setSizePix( 150, 150);//window.size.x * 0.3f, window.size.y * 0.3f);
        hudBar.setPosition(new Vector2f(150, 150)); //window.size.x * 0.7f, window.size.y * 0.7f);

        displacementMapTweening = new FloatTweening();
        displacementMapTweening.setTweeningValues(ETweeningType.Linear, ETweeningBehaviour.InOut)
                .setTweeningOption(ETweeningOption.LoopMirrored)
                .initTwoValue(5f, 0f, 1f);

        rotationTweening = new FloatTweening();
        rotationTweening.setTweeningValues(ETweeningType.Quadratic, ETweeningBehaviour.InOut)
                .setTweeningOption(ETweeningOption.LoopMirrored)
                .initTwoValue(5f, 0f, (float)Math.PI * 2);
    }


    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();

        int speed = 1;
        if (inputManager.getState(ActionId.SHIFT)) {
            speed = 3;
        }

        if(inputManager.getState(ActionId.MOVE_LEFT)){
            main3DCamera.speedAngX(Camera3D.speed.x * speed);
        }

        if(inputManager.getState(ActionId.MOVE_RIGHT)){
            main3DCamera.speedAngX(-Camera3D.speed.x * speed);
        }

        if(inputManager.getState(ActionId.MOVE_FORWARD)){
            main3DCamera.speedAngZ(-Camera3D.speed.z * speed);
        }

        if(inputManager.getState(ActionId.MOVE_BACKWARD)) {
            main3DCamera.speedAngZ(Camera3D.speed.z * speed);
        }

        if(inputManager.getState(ActionId.MOVE_UP)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y += Camera3D.speed.y);
        }

        if(inputManager.getState(ActionId.MOVE_DOWN)) {
            main3DCamera.setY(main3DCamera.getCamPosRef().y -= Camera3D.speed.y);
        }

        if(inputManager.inputPressed(ActionId.ESCAPE)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();

        }

        displacementMapTweening.update();

        ShaderManager.getInstance().getShader(sBlock.getShape().getShaderId()).glUniform("time", displacementMapTweening.value());

        rotationTweening.update();
        light.setRotation(rotationTweening.value(), new Vector3f(0, 1f, 1f).normalize());

        main3DCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        light.display();
        displacementMap.bind(1);
        stand.display();

        sBlock.display();

        sphere.display();

        // Better to draw the hud here and be not affected by the postProcessing shader
        hudBar.display();

        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();
        sBlock.unload();
        light.unload();
        hudBar.unload();
        stand.unload();
        sphere.unload();
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
        placeValueToCoords(array, new float[]{realPosX , realPosZ , realPosY  }, index);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 2 * lineSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + 3 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 4 * lineSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 5 * lineSize);

        // Down
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 4 * lineSize + faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 5 * lineSize + faceSize);

        // Left
        placeValueToCoords(array, new float[]{realPosX, realPosZ , realPosY}, index + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 2 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY + 1}, index  + 3 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ + 1, realPosY}, index  + 4 * lineSize + 2 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY}, index  + 5 * lineSize + 2 * faceSize);

        // Up
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY}, index  + 2 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + 3 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 4 * lineSize + 3 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY + 1}, index  + 5 * lineSize + 3 * faceSize);

        // Bottom
        placeValueToCoords(array, new float[]{realPosX , realPosZ , realPosY}, index + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY}, index  + lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ, realPosY + 1}, index  + 2 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ , realPosY + 1}, index  + 3 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX, realPosZ, realPosY + 1}, index  + 4 * lineSize + 4 * faceSize);
        placeValueToCoords(array, new float[]{realPosX , realPosZ, realPosY}, index  + 5 * lineSize + 4 * faceSize);

        // Top
        placeValueToCoords(array, new float[]{realPosX , realPosZ + 1 , realPosY}, index + 5 * faceSize);
        placeValueToCoords(array, new float[]{realPosX + 1, realPosZ + 1, realPosY}, index  + lineSize + 5 * faceSize);
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
        System.arraycopy(value, 0, array, coordStart , value.length);
    }

    private void computeSphere(Renderer renderer){
        float radius = 1;
        int sector = 70;
        int stack  = 70;

        ArrayList<Integer> tempIndices = new ArrayList<>();
        ArrayList<Float> tempPosition = new ArrayList<>();
        ArrayList<Float> tempTextures = new ArrayList<>();
        ArrayList<Float> tempNormales = new ArrayList<>();

        float x, y, z, xy;
        float nx, ny, nz;
        float lenghtInv = 3.0f * radius;
        float s, t;

        float sectorStep = 2f * (float)Math.PI / sector;
        float stackStep = (float)Math.PI / stack;
        float sectorAngle, stackAngle;

        int k1, k2;

        for (int i = 0; i <= stack; ++i){
            stackAngle = (float)Math.PI * 0.5f - i * stackStep;
            xy = radius * (float)Math.cos(stackAngle);


            z = radius * (float)Math.sin(stackAngle);

            for (int j = 0; j <= sector; ++j){
                sectorAngle = j * sectorStep;

                x = xy * (float)Math.cos(sectorAngle);
                y = xy * (float)Math.sin(sectorAngle);

                tempPosition.add(x);
                tempPosition.add(y);
                tempPosition.add(z);

                nx = x * lenghtInv;
                ny = y * lenghtInv;
                nz = z * lenghtInv;

                tempNormales.add(nx);
                tempNormales.add(ny);
                tempNormales.add(nz);

                s = (float)j / sector;
                t = (float)i / stack;

                tempTextures.add(s);
                tempTextures.add(t);
            }
        }

        for (int i = 0; i < stack; ++i){
            k1 = i * (sector + 1);
            k2 = k1 + sector + 1;

            for (int j = 0; j < sector; ++j, ++k1, ++k2){
                if (i != 0){
                    tempIndices.add(k1);
                    tempIndices.add(k2);
                    tempIndices.add(k1 + 1);
                }

                if (i != (stack - 1)){
                    tempIndices.add(k1 + 1);
                    tempIndices.add(k2);
                    tempIndices.add(k2 + 1);
                }
            }
        }

        float[] position = new float[tempPosition.size()];
        float[] normals = new float[tempNormales.size()];

        int i = 0;

        for (Float f : tempPosition) {
            position[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        i = 0;

        for (Float f : tempNormales) {
            normals[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        renderer.getShape().setEbo(tempIndices.stream().mapToInt(j -> j).toArray());
        renderer.getShape().addVboFloat(position, 3, Shape.STATIC_STORE);
        renderer.getShape().addVboFloat(normals, 3, Shape.STATIC_STORE);
    }
}