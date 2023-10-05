package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.main.GameTime;
import MightyLibrary.mightylib.scene.Camera2D;
import MightyLibrary.mightylib.scene.Scene;
import MightyLibrary.mightylib.util.math.ColorList;
import MightyLibrary.mightylib.util.math.EDirection;
import MightyLibrary.project.lib.ActionId;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.ArrayList;

public class TestBox2D extends Scene {
    public static class Box {
        RectangleRenderer renderer;
        Body box;
        public Box(World world, Vector2f position, Vector2f size){

            renderer = new RectangleRenderer("colorShape2D");
            renderer.switchToColorMode(ColorList.Grey());

            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyType.DYNAMIC;
            bodyDef.position.set(position.x, position.y);
            box = world.createBody(bodyDef);

            PolygonShape dynamicBox = new PolygonShape();
            dynamicBox.setAsBox(size.x, size.y);
            renderer.setSizePix(size.x * 2 + 0.02f, size.y * 2 + 0.02f);
            renderer.setReference(EDirection.None);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = dynamicBox;
            fixtureDef.density = 1f;
            fixtureDef.friction = 1f;

            box.createFixture(fixtureDef);
        }

        public void update(){
            Vec2 position = box.getPosition();
            float angle = box.getAngle();

            renderer.setPosition(new Vector2f(
                    position.x,
                    position.y));
            renderer.setRotation(angle, new Vector3f(0, 0, 1));
        }

        public void display(){
            renderer.display();
        }

        public void unload(){
            renderer.unload();
        }
    }


    public static final float MOVE_SPEED = 600;
    public static final float SHIFT_SPEED = MOVE_SPEED * 2f;


    private RectangleRenderer ground;
    private ArrayList<Box> boxes;

    private final int velocityIterations = 6;
    private final int positionIterations = 2;

    private World world;

    private boolean isDragging = false;

    @Override
    public void init(String[] args){
        super.init(args);
        /// SCENE INFORMATION ///

        main3DCamera.setPos(new Vector3f());
        main2DCamera.setPos(new Vector2f());
        main2DCamera.setZoomReference(EDirection.LeftUp);
        main2DCamera.setZoomLevel(30);
        main2DCamera.setX(-10f);
        setClearColor(52, 189, 235, 1f);

        ground = new RectangleRenderer("colorShape2D");
        ground.switchToColorMode(ColorList.Black());

        Vec2 gravity = new Vec2(0.8f, 9.81f);
        world = new World(gravity);
        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.position.set(10.0f, 30.0f);
        Body groundBody = world.createBody(groundBodyDef);
        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(10.f, 3.f);
        ground.setReference(EDirection.None);
        ground.setSizePix(10 * 2, 3 * 2);

        ground.setPosition(new Vector2f(
                groundBody.getPosition().x,
                groundBody.getPosition().y));

        FixtureDef groundFixture = new FixtureDef();
        groundFixture.shape = groundBox;

        groundBody.createFixture(groundFixture);

        boxes = new ArrayList<>();

        for (int i = 0; i < 4; ++i){
            boxes.add(
                    new Box(world, new Vector2f(10 - 0.4f * i, 0 - 4f * i), new Vector2f(0.9f, 2))
            );
        }
    }

    public void zoom(Vector2f factor){
        main2DCamera.setZoomLevel(new Vector2f(main2DCamera.getZoomLevel()).mul(factor));
    }

    @Override
    public void update() {
        super.update();

        world.step(GameTime.DeltaTime(), velocityIterations, positionIterations);

        MouseManager mouseManager = mainContext.getMouseManager();
        InputManager inputManager = mainContext.getInputManager();
        Camera2D mapCamera = main2DCamera;
        Vector2i windowSize = mainContext.getWindow().getInfo().getSizeRef();

        if (mouseManager.getMouseScroll().y != 0) {
            mapCamera.setZoomReference(
                    new Vector2f(
                            mouseManager.posX() / windowSize.x,
                            mouseManager.posY() / windowSize.y
                    ));

            if (inputManager.getState(ActionId.SHIFT))
                zoom(new Vector2f(1, 1 + mainContext.getMouseManager().getMouseScroll().y * 0.1f));
            else
                zoom(new Vector2f(1 + mainContext.getMouseManager().getMouseScroll().y * 0.1f));
        }

        if (inputManager.inputReleased(ActionId.RIGHT_CLICK))
            isDragging = false;

        if (inputManager.inputPressed(ActionId.LEFT_CLICK)){
            boxes.add(
                    new Box(world,
                            new Vector2f(
                                    mapCamera.getPosition(mouseManager.pos())),
                            new Vector2f(0.9f, 2))
            );
        }

        if (isDragging) {
            mapCamera.moveXinZoom( -mouseManager.posX() + mouseManager.oldPosX());
            mapCamera.moveYinZoom( -mouseManager.posY() + mouseManager.oldPosY());
        }

        if (inputManager.inputPressed(ActionId.RIGHT_CLICK)){
            isDragging = true;
        }

        if (inputManager.inputPressed(ActionId.SHIFT)) {
            mapCamera.setZoomReference(EDirection.None);
            zoom(new Vector2f(1.01f));
        }

        move(mapCamera, inputManager);


        for (Box box : boxes)
            box.update();
    }

    protected void move(Camera2D mapCamera, InputManager inputManager){
        float speed = MOVE_SPEED;
        if (inputManager.getState(ActionId.SHIFT))
            speed = SHIFT_SPEED;

        speed *= GameTime.DeltaTime();

        if (inputManager.getState(ActionId.MOVE_LEFT)){
            mapCamera.moveXinZoom(-speed);
        }

        if (inputManager.getState(ActionId.MOVE_RIGHT)){
            mapCamera.moveXinZoom(speed);
        }

        if (inputManager.getState(ActionId.MOVE_UP)){
            mapCamera.moveYinZoom(-speed);
        }

        if (inputManager.getState(ActionId.MOVE_DOWN)){
            mapCamera.moveYinZoom(speed);
        }
    }

    public void display(){
        super.setVirtualScene();
        clear();
        ground.display();

        for (Box box : boxes)
            box.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (Box box : boxes)
            box.unload();

        ground.unload();
    }
}
