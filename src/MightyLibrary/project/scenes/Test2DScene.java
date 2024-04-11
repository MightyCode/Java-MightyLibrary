package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.game.LayersTileMapRenderer;
import MightyLibrary.mightylib.graphics.renderer.Renderer;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.HexagonRenderer;
import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
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
import MightyLibrary.mightylib.resources.texture.TextureAtlas;
import MightyLibrary.mightylib.scenes.Camera2D;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.cameracomponents.DraggingCameraComponent;
import MightyLibrary.mightylib.scenes.cameracomponents.MovingCameraComponent;
import MightyLibrary.mightylib.scenes.cameracomponents.ZoomingCameraComponent;
import MightyLibrary.mightylib.utils.math.Color4f;
import MightyLibrary.mightylib.utils.math.ColorList;
import MightyLibrary.mightylib.utils.math.EDirection;
import MightyLibrary.mightylib.physics.tweenings.ETweeningBehaviour;
import MightyLibrary.mightylib.physics.tweenings.ETweeningOption;
import MightyLibrary.mightylib.physics.tweenings.ETweeningType;
import MightyLibrary.mightylib.physics.tweenings.type.Vector2fTweening;
import MightyLibrary.mightylib.utils.math.MightyMath;
import MightyLibrary.project.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

public class Test2DScene extends Scene {
    private Animation2DRenderer slimeRenderer;
    private Vector2fTweening slimeTextureTweening;
    private Text text;
    private TileMap map;
    private LayersTileMapRenderer mapRenderer;
    private TextureAtlas atlas;
    private RectangleRenderer atlasRenderer;

    private Camera2D testCamera;

    private FloatTweening rotation;

    private DraggingCameraComponent draggingSceneComponent;
    private MovingCameraComponent movingSceneComponent;
    private ZoomingCameraComponent zoomingSceneComponent;

    private HexagonRenderer hexagonRenderer1, hexagonRenderer2;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{
                "anim", "map-assets"
        };
    }

    public void init(String[] args) {
        super.init(args);
        /// SCENE INFORMATION ///

        atlas = new TextureAtlas("atlas1");
        atlas.addTexture("tileset");
        atlas.addTexture("painting");

        atlasRenderer = new RectangleRenderer("texture2D");
        atlasRenderer.setMainTextureChannel(atlas);
        atlasRenderer.setSizePix(atlas.getWidth(), atlas.getHeight());
        atlasRenderer.setPosition(new Vector2f(500, 10));

        main3DCamera.setPos(new Vector3f(0, 0, 0));

        setClearColor(52, 189, 235, 1f);
        //setClearColor(0, 0, 0, 1f);

        /// RENDERERS ///

        slimeRenderer = new Animation2DRenderer("texture2D");
        slimeRenderer.setMainTextureChannel("slime");
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

        mapRenderer = new LayersTileMapRenderer("texture2D", false);
        mapRenderer.setTileMap(map);

        testCamera = new Camera2D(mainContext.getWindow().getInfo(), new Vector2f(0, 0));

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);

        draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);

        movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        movingSceneComponent.initActionIds(
                new MovingCameraComponent.Inputs()
                        .setMoveLeft(ActionId.MOVE_LEFT_2D)
                        .setMoveRight(ActionId.MOVE_RIGHT_2D)
                        .setMoveDown(ActionId.MOVE_DOWN_2D)
                        .setMoveUp(ActionId.MOVE_UP_2D)
                        .setQuickSpeed(ActionId.SHIFT)
        );

        zoomingSceneComponent = new ZoomingCameraComponent();
        zoomingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(),
                main2DCamera, mainContext.getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

        main2DCamera.setZoomLevel(0.5f);

        hexagonRenderer1 = new HexagonRenderer("colorShape2D");
        hexagonRenderer1.setColorMode(ColorList.Gold());
        hexagonRenderer1.setSizePix(100, 100);
        hexagonRenderer1.setPosition(new Vector2f(-100, 400));

        hexagonRenderer2 = new HexagonRenderer("texture2D");
        hexagonRenderer2.setMainTextureChannel("hexagon");
        hexagonRenderer2.setSizePix(100, 100 * HexagonRenderer.STRETCH_RATIO);
        hexagonRenderer2.setPosition(new Vector2f(0,0));
    }


    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        InputManager inputManager = mainContext.getInputManager();
        if (inputManager.getState(ActionId.MOVE_DOWN)){
            testCamera.moveX(mainContext.getWindow().getInfo().getSizeRef().x * 0.3f * GameTime.DeltaTime());
        }

        if (inputManager.inputPressed(ActionId.MOVE_LEFT_2D)) {
            map.setTileType(0, 0, 0, 78);
        }

        rotation.update();
        slimeRenderer.setRotation(rotation.value(), new Vector3f(0, 0, 1));
        slimeTextureTweening.update();
        slimeRenderer.update();

        movingSceneComponent.update();
        draggingSceneComponent.update();
        zoomingSceneComponent.update();

        //map.setTileType(0, 0, 0, 560);

        mapRenderer.update();

        map.dispose();
    }


    public void display() {
        super.setVirtualScene();
        clear();

        if (mapRenderer.containsCategory("Foreground"))
            mapRenderer.getTileMapRenderer("Foreground").setPosition(new Vector3f(0, 0, 0));
        mapRenderer.getTileMapRenderer("Background").setPosition(new Vector3f(0, 0, 0));

        atlasRenderer.display();

        mapRenderer.display();

        slimeRenderer.display();

        if (mapRenderer.containsCategory("Foreground"))
            mapRenderer.getTileMapRenderer("Foreground").setPosition(new Vector3f(200, 200, 0));
        mapRenderer.getTileMapRenderer("Background").setPosition(new Vector3f(200, 200, 0));

        mapRenderer.display();

        text.display();

        hexagonRenderer1.display();

        Vector2f hexagonOrigin = new Vector2f(-500, 500);

        for (int x = 0; x < 4; ++x){
            for (int y = 0; y < 4; ++y){
                hexagonRenderer2.setPosition(
                        new Vector2f(hexagonOrigin.x + x * 150 + (y % 2) * 75, hexagonOrigin.y + y * 50 * HexagonRenderer.STRETCH_RATIO)
                );
                hexagonRenderer2.display();
            }
        }

        super.setAndDisplayRealScene();
    }


    public void unload() {
        super.unload();
        slimeRenderer.unload();

        text.unload();

        hexagonRenderer1.unload();
        hexagonRenderer2.unload();

        mapRenderer.unload();
    }
}
