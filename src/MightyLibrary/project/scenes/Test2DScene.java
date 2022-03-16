package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.game.FullTileMapRenderer;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.graphics.renderer._2D.Animation2DRenderer;;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.graphics.texture.Animator;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.Vector2fTweening;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Test2DScene extends Scene {
    private Animation2DRenderer slimeRenderer;
    private Vector2fTweening slimeTextureTweening;

    private Text text;

    private TileMap map;
    private FullTileMapRenderer mapRenderer;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// RENDERERS ///

        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.switchToTextureMode("slime");
        //slimeRenderer.setVerticalFlip(true);


        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);

        slimeRenderer.init(animator);
        float scale = 720.f / 30.f / 2;
        slimeRenderer.setScale(new Vector2f(scale));
        slimeRenderer.setPosition(new Vector2f(400, mainContext.getWindow().getInfo().getSizeCopy().y));
        slimeRenderer.setVerticalFlip(false);

        slimeTextureTweening = new Vector2fTweening();

        slimeTextureTweening.setTweeningValues(ETweeningType.Back, ETweeningBehaviour.InOut)
                .initTwoValue(1, new Vector2f(400, 400), new Vector2f(400 + 150, 400 - 150))
                .setTweeningOption(ETweeningOption.LoopReversed).setAdditionnalArguments(3f,  null);


        text = new Text();

        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();

        text.setFont("arial")
                .setFontSize(60)
                .setReference(EDirection.RightDown)
                .setAlignment(ETextAlignment.Right)
                .setPosition(new Vector2f(size.x, size.y))
                .setText("Test d'écriture de texte c'est super cool");

        map = Resources.getInstance().getResource(TileMap.class, "map2");
        mapRenderer = new FullTileMapRenderer("texture2D", false);
        mapRenderer.setTileMap(map);
    }


    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();


        slimeTextureTweening.update();
        //slimeRenderer.setPosition(slimeTextureTweening.value());

        slimeRenderer.update();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        mapRenderer.drawBackLayers();
        slimeRenderer.display();
        mapRenderer.drawForLayers();

        text.display();


        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();
        slimeRenderer.unload();

        text.unload();

        mapRenderer.unload();
    }
}
