package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.lighting.MultiLightingManager;
import io.github.mightycode.mightylib.graphics.lighting.lights.DirectionalLight;
import io.github.mightycode.mightylib.graphics.lighting.lights.LightDecrease;
import io.github.mightycode.mightylib.graphics.lighting.lights.PointLight;
import io.github.mightycode.mightylib.graphics.lighting.lights.SpotLight;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial;
import io.github.mightycode.mightylib.graphics.lighting.materials.Material;
import io.github.mightycode.mightylib.graphics.renderer._3D.heightMap.HeightMapColorFunctions;
import io.github.mightycode.mightylib.graphics.renderer._3D.heightMap.HeightMapRenderer;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.graphics.shader.ShaderValue;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.graphics.renderer.MeshRenderer;
import io.github.mightycode.mightylib.graphics.renderer._3D.ModelRenderer;
import io.github.mightycode.mightylib.resources.models.Model;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import io.github.mightycode.mightylib.physics.tweenings.type.FloatTweening;
import io.github.mightycode.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import io.github.mightycode.mightylib.graphics.renderer.Mesh;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DebugInfoCamera3D;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.AxisId;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class Scene3D extends Scene {
    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));
    private MeshRenderer sBlock;
    private MeshRenderer sphere;

    private ModelRenderer stand;

    // Model of the demonstration package (not in the repository), behind the stand
    private static final String BACKPACK_MODEL = "backpack";
    private ModelRenderer backpack;
    private MultiLightingManager backpackLights;
    private CubeRenderer[] backpackLightCubes;
    private Text missingBackpackLabel;

    private final int lineSize = 5;
    private final int faceSize = lineSize * 6;
    private final int boxSize = faceSize * 6;

    private CubeRenderer light, cubeColorLighting, cubeColorMaterial;

    private CubeRenderer cubeTexturedMaterial;

    private HeightMapRenderer heightMapRenderer;

    private FloatTweening displacementMapTweening;

    private FloatTweening rotationTweening;

    private ShaderValue timeShaderValue;

    private Text originLabel;

    private FreeCamera3DComponent cameraComponent;

    public Scene3D(){
        super(SCENE_CCI);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "crates3d", "litcrates3d", "stall3d", "backpack3d"
        };
    }

    public void launch(String[] args){
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        Color4f clearColor = ColorList.DarkGrey();
        setClearColor(clearColor.getR(), clearColor.getG(), clearColor.getB(), clearColor.getA());

        timeShaderValue = new ShaderValue("time", Float.class, 0f);
        main3DCamera.setSpeed(new Vector3f(10));
        cameraComponent = new FreeCamera3DComponent(mainContext.getInputManager(), main3DCamera, AxisId.freeCamera3DInputs());
        addUpdatable(cameraComponent);

        addUpdatableAndDisplayable(
                new DebugInfoCamera3D().init(main3DCamera, new Vector2f(5, 5))
                        .addInfo(DebugInfoCamera3D.POSITION)
                        .addInfo(DebugInfoCamera3D.LOOK)
                        .addInfo(DebugInfoCamera3D.SPEED)
        );

        /// RENDERERS ///

        // Cube
        cubeColorLighting = new CubeRenderer("colorLighting3D");
        cubeColorLighting.setPosition(new Vector3f(-1f, 3.0f, -4f));
        cubeColorLighting.setColorMode(ColorList.Red());
        cubeColorLighting.setNormal();

        light = new CubeRenderer("colorShape3D");
        light.setPosition(new Vector3f(-1f, 10.0f, -4f));
        light.setScale(new Vector3f(0.3f));
        light.setColorMode(ColorList.White());
        light.setNormal();

        BasicMaterial lightMaterial = new BasicMaterial(
                new Vector3f(0.2f), new Vector3f(0.5f), new Vector3f(1)
        );

        cubeColorLighting.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("worldColor", Vector3f.class, new Vector3f( 0.2f, 0.5f, 0.2f))
                .addShaderValue("lightPos", Vector3f.class, light.position())
                .addShaderValue("lightColor", Vector3f.class, lightMaterial.Diffuse);

        // Platform of cubes
        sBlock = new MeshRenderer("textureDisplacement3D", false);
        float[] cratesInfo = createCrates(10);
        sBlock.getMesh().addAllVbo(cratesInfo, new int[]{3, 2}, Mesh.STATIC_STORE, Mesh.STATIC_STORE);
        sBlock.setMainTextureChannel("container");
        // Displacement texture for cubes/crate
        sBlock.addTextureChannel("dispMap1", "displacementMap", 1);
        sBlock.addShaderValue("time", Float.class, timeShaderValue.getObjectTyped(Float.class));

        sphere = new MeshRenderer("colorLighting3D", true);
        computeSphere(sphere);
        sphere.setScale(new Vector3f(10, 10, 10));
        sphere.setPosition(new Vector3f(20, 10, 10));

        cubeColorLighting.copyShaderValuesTo(sphere, false);

        cubeColorMaterial = new CubeRenderer("colorMaterial3D");
        cubeColorMaterial.setPosition(new Vector3f(-1f, 3.0f, -8f));
        cubeColorMaterial.setNoColorNoTextureMode();
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
        // stall.obj has no material: its texture is given here
        stand = new ModelRenderer("texture3D", "stall");
        stand.setTextureUniforms("ourTexture", null);
        stand.setDiffuseOverride("stall");
        stand.setPosition(new Vector3f(0.0f, 4.0f,5.0f));
        stand.setScale(new Vector3f(0.75f, 0.75f, 0.75f));

        createBackpack();

        displacementMapTweening = new FloatTweening();
        displacementMapTweening.setTweeningValues(ETweeningType.Linear, ETweeningBehaviour.InOut)
                .setTweeningOption(ETweeningOption.LoopMirrored)
                .initTwoValue(5f, 0f, 1f);

        rotationTweening = new FloatTweening();
        rotationTweening.setTweeningValues(ETweeningType.Quadratic, ETweeningBehaviour.InOut)
                .setTweeningOption(ETweeningOption.LoopMirrored)
                .initTwoValue(5f, 0f, (float)Math.PI * 2);

        heightMapRenderer = new HeightMapRenderer("dispMap1", true);
        heightMapRenderer.setPosition(new Vector3f(-4140, -920, -4640));
        heightMapRenderer.setScale(new Vector3f(new Vector3f(10000, 1000, 10000)));

        heightMapRenderer.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                .addShaderValue("worldColor", Vector3f.class, new Vector3f( 1f, 1f, 1f))
                .addShaderValue("lightPos", Vector3f.class, light.position())
                .addShaderValue("lightColor", Vector3f.class, lightMaterial.Diffuse);

        heightMapRenderer.setHeightMapper((height, min, max) -> {
            height -= timeShaderValue.getObjectTyped(Float.class);

            if (height < 0.7f) {
                return 0.7f; // Water / flat land
            } else if (height < 0.9f) {
                return height;
            } else {
                // Steep transition for mountains
                float t = (height - 0.9f) / 0.1f;
                return 0.7f + (float)Math.pow(t, 0.5) * 0.6f; // sharper raise, nonlinear
            }
        }).setColorMapper(HeightMapColorFunctions.HeightToColorMountain(0.7f, 0.75f, 0.9f));
        heightMapRenderer.load();

        originLabel = new Text();
        originLabel.setFont("arial")
                .setText("(0, 0, 0)")
                        .setFontSize(60);

        originLabel.setColor(ColorList.Red());
    }

    // Model loaded by Assimp with its material, lit by three coloured point lights and a spot light following the camera
    private void createBackpack() {
        Model data = resources.getResource(Model.class, BACKPACK_MODEL);
        if (data == null || !data.isLoaded()) {
            String message = "Model '" + BACKPACK_MODEL + "' missing: copy the demonstration package into resources/models/backpack/";
            System.err.println("--Scene3D : " + message);

            missingBackpackLabel = new Text();
            missingBackpackLabel.setFont("arial").setFontSize(20).setText(message);
            missingBackpackLabel.setColor(ColorList.Red());
            missingBackpackLabel.setPosition(new Vector2f(5, 120));
            return;
        }

        Vector3f center = new Vector3f(0f, 4f, 13f);

        backpack = new ModelRenderer(BACKPACK_MODEL);
        backpack.setPosition(center);
        backpack.addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef());

        LightDecrease lightDecrease = new LightDecrease(1.0f, 0.09f, 0.032f);

        backpackLights = new MultiLightingManager("dirLight", "pointLights", "spotLight");
        backpackLights.setDirectionalLight(new DirectionalLight(new BasicMaterial(
                new Vector3f(0.05f), new Vector3f(0.3f), new Vector3f(0.3f)))
                .setDirection(new Vector3f(-0.2f, -1.0f, -0.3f)));

        backpackLights.setSpotLight(new SpotLight(new BasicMaterial(
                new Vector3f(0f), new Vector3f(0.6f), new Vector3f(0.8f)), lightDecrease)
                .setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector())
                .enable());

        Vector3f[] offsets = {
                new Vector3f(3f, 1f, -2f),
                new Vector3f(-3f, 1f, -2f),
                new Vector3f(0f, 3f, 3f)
        };
        Vector3f[] colors = {
                new Vector3f(1f, 0.3f, 0.3f),
                new Vector3f(0.3f, 1f, 0.3f),
                new Vector3f(0.3f, 0.3f, 1f)
        };

        backpackLightCubes = new CubeRenderer[offsets.length];
        for (int i = 0; i < offsets.length; ++i) {
            Vector3f position = new Vector3f(center).add(offsets[i]);

            backpackLights.addPointLight(new PointLight(new BasicMaterial(
                    new Vector3f(colors[i]).mul(0.05f), colors[i], colors[i]), lightDecrease)
                    .setPosition(position));

            backpackLightCubes[i] = new CubeRenderer("colorShape3D");
            backpackLightCubes[i].setPosition(position);
            backpackLightCubes[i].setScale(new Vector3f(0.2f));
            backpackLightCubes[i].setColorMode(new Color4f(colors[i], 1));
            backpackLightCubes[i].setNormal();
        }

        backpackLights.addRenderer(backpack);
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();

        // The camera has been moved by its component (super.update)
        if (inputManager.inputPressed(ActionId.TAB)) {
            cameraComponent.toggleLookLocked();
            mainContext.getMouseManager().invertCursorState();
        }

        originLabel.setPosition(main3DCamera.worldPositionToScreen(new Vector3f(0), main2DCamera));

        displacementMapTweening.update();

        timeShaderValue.setObject(displacementMapTweening.value());
        sBlock.updateShaderValue("time", timeShaderValue.getObjectTyped(Float.class));

        rotationTweening.update();

        light.setX(rotationTweening.value() * 2);

        cubeColorLighting.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeColorLighting.updateShaderValue("lightPos", light.position());

        cubeColorMaterial.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeColorMaterial.updateShaderValue("light.vector", RendererUtils.ToLightPosition(light.position()));

        cubeTexturedMaterial.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        cubeTexturedMaterial.updateShaderValue("light.vector", RendererUtils.ToLightPosition(light.position()));

        if (backpack != null) {
            backpack.updateShaderValue("viewPos", main3DCamera.getCamPosRef());
            backpackLights.getSpotLight().setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector());
            backpackLights.update();
        }

        main3DCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        light.display();

        cubeColorLighting.display();
        cubeColorMaterial.display();

        heightMapRenderer.display();

        cubeTexturedMaterial.display();

        sBlock.display();

        stand.display();

        if (backpack != null) {
            backpack.display();
            for (CubeRenderer cube : backpackLightCubes)
                cube.display();
        }

        sphere.display();

        super.display();

        originLabel.display();

        if (missingBackpackLabel != null)
            missingBackpackLabel.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload(){
        super.unload();
        sBlock.unload();
        cubeColorLighting.unload();
        cubeColorMaterial.unload();
        cubeTexturedMaterial.unload();
        light.unload();
        stand.unload();

        if (backpack != null) {
            backpack.unload();
            for (CubeRenderer cube : backpackLightCubes)
                cube.unload();
        }

        if (missingBackpackLabel != null)
            missingBackpackLabel.unload();
        sphere.unload();
        heightMapRenderer.unload();
        originLabel.unload();
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

    private void computeSphere(MeshRenderer renderer){
        float radius = 1;
        int sector = 70;
        int stack  = 70;

        ArrayList<Integer> tempIndices = new ArrayList<>();
        ArrayList<Float> tempPosition = new ArrayList<>();
        ArrayList<Float> tempNormals = new ArrayList<>();

        float x, y, z, xy;
        float nx, ny, nz;
        float lengthInv = 3.0f * radius;

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

                nx = x * lengthInv;
                ny = y * lengthInv;
                nz = z * lengthInv;

                tempNormals.add(nx);
                tempNormals.add(ny);
                tempNormals.add(nz);

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
        float[] normals = new float[tempNormals.size()];

        int i = 0;

        for (Float f : tempPosition) {
            position[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        i = 0;

        for (Float f : tempNormals) {
            normals[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }

        renderer.getMesh().setEbo(tempIndices.stream().mapToInt(j -> j).toArray());
        renderer.getMesh().addVboFloat(position, 3, Mesh.STATIC_STORE);
        renderer.getMesh().addVboFloat(normals, 3, Mesh.STATIC_STORE);
    }
}