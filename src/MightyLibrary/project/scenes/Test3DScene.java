package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer.RendererUtils;
import MightyLibrary.mightylib.graphics.renderer.utils.BasicMaterial;
import MightyLibrary.mightylib.graphics.renderer.utils.Material;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.graphics.shader.ShaderValue;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer._3D.ModelRenderer;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import MightyLibrary.mightylib.graphics.renderer.Shape;
import MightyLibrary.mightylib.scene.Camera3DCreationInfo;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.ColorList;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11C.*;

public class Test3DScene extends Scene {
    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));
    private Renderer sBlock;
    private Renderer sphere;

    private ModelRenderer stand;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private CubeRenderer light, cubeColorLightning, cubeColorMaterial;

    private CubeRenderer cubeTexturedMaterial;

    private FloatTweening displacementMapTweening;

    private FloatTweening rotationTweening;

    private ShaderValue timeShaderValue;

    public Test3DScene(){
        super(SCENE_CCI);
    }

    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        Color4f clearColor = ColorList.DarkGrey();
        setClearColor(clearColor.getR(), clearColor.getG(), clearColor.getB(), clearColor.getA());

        timeShaderValue = new ShaderValue("time", Float.class, 0f);
        main3DCamera.setSpeed(new Vector3f(10));

        /// RENDERERS ///

        // Cube
        cubeColorLightning = new CubeRenderer("colorLightning3D");
        cubeColorLightning.setPosition(new Vector3f(-1f, 3.0f, -4f));
        cubeColorLightning.setColorMode(ColorList.Red());
        cubeColorLightning.setNormal();

        light = new CubeRenderer("colorShape3D");
        light.setPosition(new Vector3f(-1f, 10.0f, -4f));
        light.setScale(new Vector3f(0.3f));
        light.setColorMode(ColorList.White());
        light.setNormal();

        BasicMaterial lightMaterial = new BasicMaterial(
                new Vector3f(0.2f), new Vector3f(0.5f), new Vector3f(1)
        );

        cubeColorLightning.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("worldColor", Vector3f.class, new Vector3f( 0.2f, 0.5f, 0.2f))
                .addShaderValue("lightPos", Vector3f.class, light.position())
                .addShaderValue("lightColor", Vector3f.class, lightMaterial.Diffuse);

        // Platform of cubes
        sBlock = new Renderer("textureDisplacement3D", false);
        float[] cratesInfo = createCrates(10);
        sBlock.getShape().addAllVbo(cratesInfo, new int[]{3, 2}, Shape.STATIC_STORE, Shape.STATIC_STORE);
        sBlock.setMainTextureChannel("container");
        // Displacement texture for cubes/crate
        sBlock.addTextureChannel("dispMap1", "displacementMap", 1);

        sphere = new Renderer("colorLightning3D", true);
        computeSphere(sphere);
        sphere.setScale(new Vector3f(10, 10, 10));
        sphere.setPosition(new Vector3f(20, 10, 10));

        sphere.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("lightPos", Vector3f.class, light.position());
        cubeColorLightning.copyShaderValuesTo(sphere, false);

        cubeColorMaterial = new CubeRenderer("colorMaterial3D");
        cubeColorMaterial.setPosition(new Vector3f(-1f, 3.0f, -8f));
        cubeColorMaterial.setColorMode(ColorList.Red());
        cubeColorMaterial.setNormal();

        cubeColorMaterial.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(light.position()));
        lightMaterial.addToRenderer(cubeColorMaterial, "light");
        Material.Emerald().addToRenderer(cubeColorMaterial, "material");

        cubeTexturedMaterial = new CubeRenderer("textureMaterial3D");
        cubeTexturedMaterial.setPosition(new Vector3f(3f, 3.0f, -8f));
        cubeTexturedMaterial.setTexturePosition();
        cubeTexturedMaterial.setNormal();
        cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
        cubeTexturedMaterial.addTextureChannel("container2_specular", "material.specular", 1);

        cubeTexturedMaterial
                .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(light.position()))
                .addShaderValue("material.shininess", Float.class, 64f);

        lightMaterial.addToRenderer(cubeTexturedMaterial, "light");

        // 3D Model
        stand = new ModelRenderer("texture3D", "stand/stall", "stall");
        stand.setPosition(new Vector3f(0.0f, 4.0f,5.0f));
        stand.setScale(new Vector3f(0.75f, 0.75f, 0.75f));

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

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

        int speed = 1;
        if (inputManager.getState(ActionId.SHIFT))
            speed = 3;

        if (inputManager.getState(ActionId.MOVE_LEFT))
            main3DCamera.speedAngX(main3DCamera.getSpeed().x * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_RIGHT))
            main3DCamera.speedAngX(-main3DCamera.getSpeed().x * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_FORWARD))
            main3DCamera.speedAngZ(-main3DCamera.getSpeed().z * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_BACKWARD))
            main3DCamera.speedAngZ(main3DCamera.getSpeed().z * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_UP))
            main3DCamera.setY(main3DCamera.getCamPosRef().y + main3DCamera.getSpeed().y * speed * GameTime.DeltaTime());

        if (inputManager.getState(ActionId.MOVE_DOWN))
            main3DCamera.setY(main3DCamera.getCamPosRef().y - main3DCamera.getSpeed().y * speed * GameTime.DeltaTime());


        if (inputManager.inputPressed(ActionId.TAB)) {
            main3DCamera.invertLockViewCursor();
            mainContext.getMouseManager().invertCursorState();
        }

        displacementMapTweening.update();

        timeShaderValue.setObject(displacementMapTweening.value());
        sBlock.getShape().getShader().sendValueToShader(timeShaderValue);

        rotationTweening.update();

        light.setX(rotationTweening.value() * 2);

        //light.setZ(rotationTweening.value() * 2);

        cubeColorLightning.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeColorLightning.updateShaderValue("lightPos", light.position());

        cubeColorMaterial.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeColorMaterial.updateShaderValue("light.vector", RendererUtils.ToLightPosition(light.position()));

        cubeTexturedMaterial.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeTexturedMaterial.updateShaderValue("light.vector", RendererUtils.ToLightPosition(light.position()));

        main3DCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        light.display();

        cubeColorLightning.display();
        cubeColorMaterial.display();

        cubeTexturedMaterial.display();

        sBlock.display();

        stand.display();

        sphere.display();

        super.setAndDisplayRealScene();
    }


    public void unload(){
        super.unload();
        sBlock.unload();
        cubeColorLightning.unload();
        cubeColorMaterial.unload();
        cubeTexturedMaterial.unload();
        light.unload();
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