package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.lighting.materials.BasicPhongMaterial;
import io.github.mightycode.mightylib.graphics.lighting.materials.PhongMaterial;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.AxisId;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Reference scene of the image tests (ReferenceImagesGLTest): primitive cubes lit by one light, a Phong material
 * and a texture of the demo (no demonstration pack). Still; WASD moves the camera, TAB frees the look (the mouse).
 * Do not change it without generating its references again (tools/references.py --update).
 */
public class Reference3D extends Scene {
    private static final Camera3DCreationInfo CAMERA = new Camera3DCreationInfo(70f, new Vector3f(0, 3, 7));

    private CubeRenderer light, colored, material, textured;
    private FreeCamera3DComponent cameraComponent;

    public Reference3D() {
        super(CAMERA);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"litcrates3d"};
    }

    @Override
    public void launch(String[] args) {
        setClearColor(ColorList.DarkGrey().getR(), ColorList.DarkGrey().getG(), ColorList.DarkGrey().getB(), 1f);

        // The look follows the mouse only after TAB: the cursor stays free
        cameraComponent = new FreeCamera3DComponent(context().getInputManager(), camera3D(), AxisId.freeCamera3DInputs());
        cameraComponent.setLookLocked(true);
        addUpdatable(cameraComponent);
        camera3D().lookAt(new Vector3f(0, 2, 0));

        BasicPhongMaterial lightMaterial = new BasicPhongMaterial(new Vector3f(0.2f), new Vector3f(0.5f), new Vector3f(1));

        light = new CubeRenderer("colorShape3D");
        light.setPosition(new Vector3f(1, 4, 3));
        light.setScale(new Vector3f(0.3f));
        light.setColorMode(ColorList.White());
        light.setNormal();

        colored = new CubeRenderer("colorLighting3D");
        colored.setPosition(new Vector3f(-3f, 2f, 0f));
        colored.setColorMode(ColorList.Red());
        colored.setScale(new Vector3f(2f));
        colored.setNormal();
        colored.addShaderValue("viewPos", Vector3f.class, camera3D().getCamPosRef())
                .addShaderValue("worldColor", Vector3f.class, new Vector3f(0.2f, 0.5f, 0.2f))
                .addShaderValue("lightPos", Vector3f.class, light.position())
                .addShaderValue("lightColor", Vector3f.class, lightMaterial.Diffuse);

        material = new CubeRenderer("colorMaterial3D");
        material.setPosition(new Vector3f(0f, 2f, 0f));
        material.setNoColorNoTextureMode();
        material.setScale(new Vector3f(2f));
        material.setNormal();
        material.addShaderValue("viewPos", Vector3f.class, camera3D().getCamPosRef())
                .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(light.position()));
        lightMaterial.addTo(material, "light");
        PhongMaterial.Emerald().addTo(material, "material");

        textured = new CubeRenderer("textureMaterial3D");
        textured.setPosition(new Vector3f(3f, 2f, 0f));
        textured.setTexturePosition();
        textured.setScale(new Vector3f(2f));
        textured.setNormal();
        textured.setMainTextureChannel("container2", "material.diffuse");
        textured.addTextureChannel("container2_specular", "material.specular", 1);
        textured.addShaderValue("viewPos", Vector3f.class, camera3D().getCamPosRef())
                .addShaderValue("light.vector", Vector4f.class, RendererUtils.ToLightPosition(light.position()))
                .addShaderValue("material.shininess", Float.class, 64f);
        lightMaterial.addTo(textured, "light");
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});
        if (context().getInputManager().inputPressed(ActionId.TAB)) {
            cameraComponent.toggleLookLocked();
            context().getMouseManager().invertCursorState();
        }
    }

    @Override
    public void display() {
        // At the display: a camera moved by a command between two ticks is seen
        camera3D().updateView();
        colored.updateShaderValue("viewPos", camera3D().getCamPosRef());
        material.updateShaderValue("viewPos", camera3D().getCamPosRef());
        textured.updateShaderValue("viewPos", camera3D().getCamPosRef());

        setVirtualScene();
        clear();

        light.display();
        colored.display();
        material.display();
        textured.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();
        light.unload();
        colored.unload();
        material.unload();
        textured.unload();
    }
}
