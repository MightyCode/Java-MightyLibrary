package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.game.FullTileMapRenderer;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.physics.tweenings.type.FloatTweening;
import MightyLibrary.mightylib.resources.map.TileMap;
import MightyLibrary.mightylib.graphics.renderer._2D.Animation2DRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.animation.AnimationData;
import MightyLibrary.mightylib.resources.animation.Animator;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.scene.Camera2D;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.Vector2fTweening;
import MightyLibrary.mightylib.util.math.MightyMath;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Test2DScene extends Scene {
    private Animation2DRenderer slimeRenderer;
    private Vector2fTweening slimeTextureTweening;

    private Text text;

    private TileMap map;
    private FullTileMapRenderer mapRenderer;

    private Camera2D testCamera;

    private FloatTweening rotation;

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f(0, 0, 0));

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///

        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.switchToTextureMode("slime");
        //slimeRenderer.setVerticalFlip(true);


        Animator animator = new Animator();
        animator.addAndInitAnimation("first", resources.getResource(AnimationData.class, "slime"), true);

        slimeRenderer.init(animator);
        slimeRenderer.setShiftRotation(EDirection.None);

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

        text.setFont("bahnschrift")
                .setFontSize(60)
                .setReference(EDirection.RightDown)
                .setAlignment(ETextAlignment.Right)
                .setPosition(new Vector2f(size.x, size.y))
                .setColor(new Color4f(0.5f, 0.4f, 0.3f, 1))
                .setText("Test d'écriture de texte c'est super cool");

        map = Resources.getInstance().getResource(TileMap.class, "map");

        mapRenderer = new FullTileMapRenderer("texture2D", false);
        mapRenderer.setTileMap(map);

        testCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0, 0));

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();
        if (inputManager.getState(ActionId.MOVE_DOWN)){
            testCamera.moveX(mainContext.getWindow().getInfo().getSizeRef().x * 0.3f * GameTime.DeltaTime());
        }

        rotation.update();
        slimeRenderer.setRotation(rotation.value(), new Vector3f(0, 0, 1));
        slimeTextureTweening.update();
        slimeRenderer.update();

        //map.setTileType(0, 0, 0, 560);

        mapRenderer.update();

        map.dispose();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        mapRenderer.getForTileMapRenderer().setPosition(new Vector3f(0, 0, 0));
        mapRenderer.getBackTileMapRenderer().setPosition(new Vector3f(0, 0, 0));

        mapRenderer.drawBackLayers();
        mapRenderer.drawForLayers();

        slimeRenderer.display();

        mapRenderer.getForTileMapRenderer().setPosition(new Vector3f(200, 200, 0));
        mapRenderer.getBackTileMapRenderer().setPosition(new Vector3f(200, 200, 0));

        mapRenderer.drawBackLayers();
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
