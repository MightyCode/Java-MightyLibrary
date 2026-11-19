package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.color.ERenderColorSpace;
import io.github.mightycode.mightylib.graphics.lighting.materials.BasicMaterial;
import io.github.mightycode.mightylib.graphics.lighting.materials.Material;
import io.github.mightycode.mightylib.graphics.renderer._3D.shape.CubeRenderer;
import io.github.mightycode.mightylib.graphics.renderer.RendererUtils;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.Camera3DCreationInfo;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.FreeCamera3DComponent;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.AxisId;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * The same lit 3D scene drawn twice with the same camera, linear on the left half and gamma on the right half (the
 * view changes its mode between the two draws, the scissor keeps one half each). The lighting is softer on the left;
 * move the camera (WASD, the mouse) to see the cubes cross the seam.
 */
public class ColorSpaceSplit3D extends Scene {
    private static final Camera3DCreationInfo CAMERA = new Camera3DCreationInfo(90, new Vector3f(0, 3, 5));

    private CubeRenderer light, colored, material, textured;
    private FreeCamera3DComponent cameraComponent;

    public ColorSpaceSplit3D() {
        super(CAMERA);
    }

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "litcrates3d" };
    }

    @Override
    public void launch(String[] args) {
        context().getMouseManager().setCursor(false);
        setClearColor(ColorList.DarkGrey().getR(), ColorList.DarkGrey().getG(), ColorList.DarkGrey().getB(), 1f);

        cameraComponent = new FreeCamera3DComponent(context().getInputManager(), camera3D(), AxisId.freeCamera3DInputs());
        addUpdatable(cameraComponent);
        camera3D().lookAt(new Vector3f(0, 2, 0));

        BasicMaterial lightMaterial = new BasicMaterial(new Vector3f(0.2f), new Vector3f(0.5f), new Vector3f(1));

        light = new CubeRenderer("colorShape3D");
        light.setPosition(new Vector3f(0, 4, 3));
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
        lightMaterial.addToRenderer(material, "light");
        Material.Emerald().addToRenderer(material, "material");

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
        lightMaterial.addToRenderer(textured, "light");
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

        colored.updateShaderValue("viewPos", camera3D().getCamPosRef());
        material.updateShaderValue("viewPos", camera3D().getCamPosRef());
        textured.updateShaderValue("viewPos", camera3D().getCamPosRef());

        camera3D().updateView();
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        Vector2i size = context().getWindow().getInfo().getVirtualSizeRef();
        int half = size.x / 2;

        // Each half forces its mode, the mode of the view comes back after (commands colorspace, F7)
        ERenderColorSpace viewMode = colorSpace();
        setScissor(0, 0, half, size.y);
        drawAll(ERenderColorSpace.LINEAR);
        setScissor(half, 0, size.x - half, size.y);
        drawAll(ERenderColorSpace.GAMMA);
        clearScissor();
        setColorSpace(viewMode);

        setAndDisplayRealScene();
    }

    // The view takes the mode of the half
    private void drawAll(ERenderColorSpace mode) {
        setColorSpace(mode);

        light.display();
        colored.display();
        material.display();
        textured.display();
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
