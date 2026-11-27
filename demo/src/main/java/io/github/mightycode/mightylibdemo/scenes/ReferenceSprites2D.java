package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.game.LayersTileMapRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.Animation2DRenderer;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.resources.animation.AnimationData;
import io.github.mightycode.mightylib.resources.animation.Animator;
import io.github.mightycode.mightylib.resources.map.TileMap;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import io.github.mightycode.mightylibdemo.main.DemoCamera;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference scene of the image tests (ReferenceImagesGLTest): textures, animations on a fixed frame and a tile map,
 * still (nothing moves with the time, no text, no counter). Do not change it without generating its references
 * again (tools/references.py --update).
 */
public class ReferenceSprites2D extends Scene {
    private LayersTileMapRenderer map;
    private final List<RectangleRenderer> textures = new ArrayList<>();
    private final List<Animation2DRenderer> animations = new ArrayList<>();

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{"anim", "map-assets", "litcrates3d"};
    }

    @Override
    public void launch(String[] args) {
        DemoCamera.free(this);
        setClearColor(52, 189, 235, 1f);

        // The map is drawn from the origin of the world; built once: its animated tiles stay on their first frame
        map = new LayersTileMapRenderer("texture2D", false);
        map.setTileMap(resources().getResource(TileMap.class, "map"));
        map.update();

        textures.add(texture("painting", 150, 10, 160, 120));
        textures.add(texture("container2", 330, 10, 120, 120));
        textures.add(texture("painting", 470, 150, 60, 45));

        animations.add(slime(0, 520, 70, 4, false, false));
        animations.add(slime(1, 110, 270, 6, true, false));
        animations.add(slime(2, 300, 270, 5, false, true));
    }

    private static RectangleRenderer texture(String name, float x, float y, float width, float height) {
        RectangleRenderer texture = new RectangleRenderer("texture2D");
        texture.setMainTextureChannel(name);
        texture.setSizePix(width, height);
        texture.setPosition(new Vector2f(x, y));
        return texture;
    }

    // An animation of the slime shown on one frame: never updated, it stays there
    private Animation2DRenderer slime(int frame, float x, float y, float scale, boolean horizontal, boolean vertical) {
        Animator animator = new Animator();
        animator.addAndInitAnimation("still", resources().getResource(AnimationData.class, "slime"), true);
        animator.getCurrentAnimation().setFrame(frame);

        Animation2DRenderer animation = new Animation2DRenderer("texture2D");
        animation.init(animator);
        animation.setShiftRotation(EDirection.None);
        animation.setScale(new Vector2f(scale));
        animation.setHorizontalFlip(horizontal);
        animation.setVerticalFlip(vertical);
        animation.setPosition(new Vector2f(x, y));
        return animation;
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        map.display();
        for (RectangleRenderer texture : textures)
            texture.display();
        for (Animation2DRenderer animation : animations)
            animation.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();
        map.unload();
        for (RectangleRenderer texture : textures)
            texture.unload();
        textures.clear();
        for (Animation2DRenderer animation : animations)
            animation.unload();
        animations.clear();
    }
}
