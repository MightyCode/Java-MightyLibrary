package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.lighting.MultiLightingManager;
import io.github.mightycode.mightylib.graphics.lighting.lights.DirectionalLight;
import io.github.mightycode.mightylib.graphics.lighting.lights.LightDecrease;
import io.github.mightycode.mightylib.graphics.lighting.lights.SpotLight;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial;
import io.github.mightycode.mightylib.graphics.lighting.lights.PointLight;
import io.github.mightycode.mightylib.graphics.lighting.materials.Material;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DebugInfoCamera3D;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.AxisId;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Scene3DBis extends Scene {
    private FreeCamera3DComponent cameraComponent;

    private final static Camera3DCreationInfo SCENE_CCI = new Camera3DCreationInfo(120, new Vector3f(0, 4, 0));
    private CubeRenderer[] cubeTexturedMaterialsDirectional;

    private CubeRenderer[] cubeTexturedMaterialsPoint;

    private CubeRenderer[] cubeMultiLights;

    private CubeRenderer lightPoint;

    private MultiLightingManager lightManager;

    private CubeRenderer[] lightPoints;

    public Scene3DBis(){
        super(SCENE_CCI);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "litcrates3d"
        };
    }

    public void launch(String[] args){
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(false);
        cameraComponent = new FreeCamera3DComponent(mainContext.getInputManager(), main3DCamera, AxisId.freeCamera3DInputs());
        addUpdatable(cameraComponent);
        main3DCamera.setSpeed(new Vector3f(8));
        cameraComponent.setQuickFactor(2);
        Color4f clearColor = ColorList.DarkGrey();
        setClearColor(clearColor.getR(), clearColor.getG(), clearColor.getB(), clearColor.getA());

        addUpdatableAndDisplayable(
                new DebugInfoCamera3D().init(main3DCamera, new Vector2f(5, 5))
                        .addInfo(DebugInfoCamera3D.POSITION)
                        .addInfo(DebugInfoCamera3D.LOOK)
                        .addInfo(DebugInfoCamera3D.SPEED)
        );

        /// RENDERERS ///

        LightDecrease lightDecrease = new LightDecrease(1.0f, 0.09f, 0.032f);
        BasicMaterial directionalReference = Material.Ruby();

        BasicMaterial pointLightReference = new BasicMaterial(
                new Vector3f(0.05f), new Vector3f( 0.8f), new Vector3f( 1.0f)
        );

        BasicMaterial spotLightReference = Material.YellowPlastic();

        DirectionalLight directionalLight = new DirectionalLight(directionalReference)
                .setDirection(new Vector3f(-0.2f, -1.0f, -0.3f));

        Vector3f[] positions = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.0f,  5.0f, -15.0f),
            new Vector3f(-1.5f, -2.2f, -2.5f),
            new Vector3f(-3.8f, -2.0f, -12.3f),
            new Vector3f( 2.4f, -0.4f, -3.5f),
            new Vector3f(-1.7f,  3.0f, -7.5f),
            new Vector3f( 1.3f, -2.0f, -2.5f),
            new Vector3f( 1.5f,  2.0f, -2.5f),
            new Vector3f( 1.5f,  0.2f, -1.5f),
            new Vector3f(-1.3f,  1.0f, -1.5f)
        };

        cubeTexturedMaterialsDirectional = new CubeRenderer[positions.length];

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i){
            CubeRenderer cubeTexturedMaterial = new CubeRenderer("textureMaterial3D");
            cubeTexturedMaterial.setTexturePosition();
            cubeTexturedMaterial.setNormal();
            cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
            cubeTexturedMaterial.addTextureChannel("container2_specular",
                    "material.specular", 1);

            float angle = MightyMath.toRads(20.0f * i);
            cubeTexturedMaterial.setPosition(positions[i]);
            cubeTexturedMaterial.setRotation(angle, new Vector3f(1.0f, 0.3f, 0.5f).normalize());

            this.cubeTexturedMaterialsDirectional[i] = cubeTexturedMaterial;

            cubeTexturedMaterial
                    .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                    .addShaderValue("light.vector", Vector4f.class,
                            RendererUtils.ToLightDirection(directionalLight.getDirection()))
                    .addShaderValue("material.shininess", Float.class, 64f);

            directionalLight.cloneBasicMaterial().addToRenderer(cubeTexturedMaterial, "light");
        }

        Vector3f shiftToPreviousChunk = new Vector3f(20, 0, 0);

        lightPoint = new CubeRenderer("colorShape3D");
        lightPoint.setPosition(new Vector3f(20, 2, 0));
        lightPoint.setScale(new Vector3f(0.3f));
        lightPoint.setColorMode(new Color4f(pointLightReference.Diffuse, 1));
        lightPoint.setNormal();

        cubeTexturedMaterialsPoint = new CubeRenderer[positions.length];

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i){
            CubeRenderer cubeTexturedMaterial = new CubeRenderer("textureMaterial3D");
            cubeTexturedMaterial.setTexturePosition();
            cubeTexturedMaterial.setNormal();
            cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
            cubeTexturedMaterial.addTextureChannel("container2_specular", "material.specular", 1);

            float angle = MightyMath.toRads(20.0f * i);
            cubeTexturedMaterial.setPosition(positions[i].add(shiftToPreviousChunk, new Vector3f()));
            cubeTexturedMaterial.setRotation(angle, new Vector3f(1.0f, 0.3f, 0.5f).normalize());

            this.cubeTexturedMaterialsPoint[i] = cubeTexturedMaterial;

            cubeTexturedMaterial
                    .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                    .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(lightPoint.position()))
                    .addShaderValue("material.shininess", Float.class, 64f)
                    .addShaderValue("light.useDistance", Integer.class, 1);

            lightDecrease.addToRenderer(cubeTexturedMaterial, "light");
            pointLightReference.addToRenderer(cubeTexturedMaterial, "light");
        }

        shiftToPreviousChunk = new Vector3f(0, 0, -30);

        Vector3f[] lightPositions = {
                new Vector3f( 0.7f,  0.2f,  2.0f),
                new Vector3f( 2.3f, -3.3f, -4.0f),
                new Vector3f(-4.0f,  2.0f, -12.0f),
                new Vector3f( 0.0f,  0.0f, -3.0f)
        };

        lightManager = new MultiLightingManager(
                "dirLight", "pointLights", "spotLight"
        );

        lightManager.setDirectionalLight(directionalLight.clone());
        lightManager.setSpotLight(
                new SpotLight(spotLightReference, lightDecrease)
                        .setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector())
                        .enable()
        );

        lightPoints = new CubeRenderer[lightPositions.length];

        for (int i = 0; i < lightPoints.length; ++i){
            lightPoints[i] = new CubeRenderer("colorShape3D");
            lightPoints[i].setPosition(lightPositions[i].add(shiftToPreviousChunk, new Vector3f()));
            lightPoints[i].setScale(new Vector3f(0.3f));
            lightPoints[i].setColorMode(new Color4f(pointLightReference.Diffuse, 1));
            lightPoints[i].setNormal();

            lightManager.addPointLight(
                    new PointLight(pointLightReference, lightDecrease)
                            .setPosition(lightPositions[i].add(shiftToPreviousChunk, new Vector3f()))
            );
        }

        cubeMultiLights  = new CubeRenderer[positions.length];
        for (int i = 0; i < cubeMultiLights.length; ++i){
            CubeRenderer cubeTexturedMaterial = new CubeRenderer("textureMultipleLights3D");
            cubeTexturedMaterial.setTexturePosition();
            cubeTexturedMaterial.setNormal();
            cubeTexturedMaterial.setMainTextureChannel("container2", "material.diffuse");
            cubeTexturedMaterial.addTextureChannel("container2_specular", "material.specular", 1);

            float angle = MightyMath.toRads(20.0f * i);
            cubeTexturedMaterial.setPosition(positions[i].add(new Vector3f(0, 0, -30), new Vector3f()));
            cubeTexturedMaterial.setRotation(angle, new Vector3f(1.0f, 0.3f, 0.5f).normalize());

            this.cubeMultiLights[i] = cubeTexturedMaterial;

            cubeTexturedMaterial
                    .addShaderValue("viewPos", Vector3f.class, main3DCamera.getCamPosRef())
                    .addShaderValue("material.shininess", Float.class, 64f);

            lightManager.addRenderer(cubeTexturedMaterial);
        }
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();


        if (inputManager.inputPressed(ActionId.TAB)) {
            cameraComponent.toggleLookLocked();
            mainContext.getMouseManager().invertCursorState();
        }

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].updateShaderValue("viewPos", main3DCamera.getCamPosRef());
            //cubeTexturedMaterial.updateShaderValue("light.position", light.position());

            cubeTexturedMaterialsPoint[i].updateShaderValue("viewPos", main3DCamera.getCamPosRef());

            cubeMultiLights[i].updateShaderValue("viewPos", main3DCamera.getCamPosRef());
        }

        lightManager.getSpotLight().setPosition(main3DCamera.getCamPosRef()).setDirection(main3DCamera.getLookAtVector());
        lightManager.update();

        main3DCamera.updateView();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        // Better to draw the world here
        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].display();
            cubeTexturedMaterialsPoint[i].display();

            cubeMultiLights[i].display();
        }

        lightPoint.display();

        for (CubeRenderer point : lightPoints) {
            point.display();
        }

        super.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload(){
        super.unload();

        for (int i = 0; i < cubeTexturedMaterialsDirectional.length; ++i) {
            cubeTexturedMaterialsDirectional[i].unload();
            cubeTexturedMaterialsPoint[i].unload();

            cubeMultiLights[i].unload();
        }

        lightPoint.unload();

        for (CubeRenderer point : lightPoints) {
            point.unload();
        }
    }
}