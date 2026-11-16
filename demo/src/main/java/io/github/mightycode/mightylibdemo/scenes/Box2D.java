package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.main.utils.GameTime;
import io.github.mightycode.mightylib.main.utils.IUpdatableDisplayable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DebugInfoCamera2D;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.DraggingCameraComponent;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.MovingCameraComponent;
import io.github.mightycode.mightylib.scenes.camera.cameraComponents.ZoomingCameraComponent;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;

public class Box2D extends Scene {
    public static class Box implements IUpdatableDisplayable {
        final RectangleRenderer renderer;
        final Body box;
        public Box(World world, Vector2f position, Vector2f size){
            renderer = new RectangleRenderer("colorShape2D");
            renderer.setColorMode(ColorList.Grey());

            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyType.DYNAMIC;
            bodyDef.position.set(position.x, position.y);
            box = world.createBody(bodyDef);

            PolygonShape dynamicBox = new PolygonShape();
            dynamicBox.setAsBox(size.x, size.y);
            renderer.setSizePix(size.x * 2 + 0.02f, size.y * 2 + 0.02f);
            renderer.setReferenceDirection(EDirection.None);

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

        @Override
        public void postUpdate() {}

        public void display(){
            renderer.display();
        }

        public void unload(){
            renderer.unload();
        }
    }

    private RectangleRenderer ground;
    private ArrayList<Box> boxes;

    private final static int VELOCITY_ITERATIONS = 6;
    private final static int POSITION_ITERATIONS = 2;
    private World world;


    @Override
    public void launch(String[] args){
        /// SCENE INFORMATION ///

        Camera2D hudCamera = camera2D().copy();

        camera3D().setPos(new Vector3f());
        camera2D().setPos(new Vector2f());
        camera2D().setZoomReference(EDirection.LeftUp);
        camera2D().setZoomLevel(30);
        camera2D().setX(-10f);
        setClearColor(52, 189, 235, 1f);

        ground = new RectangleRenderer("colorShape2D");
        ground.setColorMode(ColorList.Black());

        Vec2 gravity = new Vec2(0.8f, 9.81f);
        world = new World(gravity);
        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.position.set(10.0f, 30.0f);
        Body groundBody = world.createBody(groundBodyDef);
        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(10.f, 3.f);
        ground.setReferenceDirection(EDirection.None);
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

        DraggingCameraComponent draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(context().getInputManager(), context().getMouseManager(), camera2D());
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);
        addUpdatable(draggingSceneComponent);

        MovingCameraComponent movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(context().getInputManager(), camera2D());
        movingSceneComponent.initActionIds(
                new MovingCameraComponent.Inputs()
                .setMoveLeft(ActionId.MOVE_LEFT_2D)
                .setMoveRight(ActionId.MOVE_RIGHT_2D)
                .setMoveDown(ActionId.MOVE_DOWN_2D)
                .setMoveUp(ActionId.MOVE_UP_2D)
                .setQuickSpeed(ActionId.SHIFT)
        );

        addUpdatable(movingSceneComponent);

        ZoomingCameraComponent zoomingSceneComponent = new ZoomingCameraComponent();
        zoomingSceneComponent.init(context().getInputManager(), context().getMouseManager(),
                camera2D(), context().getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

        addUpdatable(zoomingSceneComponent);

        addUpdatableAndDisplayable(
                new DebugInfoCamera2D(hudCamera).init(camera2D(), new Vector2f(5, 5))
                        .addInfo("position")
                        .addInfo("rotation")
                        .addInfo("zoom")
        );
    }

    @Override
    public void update() {
        super.update();

        if (context().getInputManager().inputPressed(ActionId.ESCAPE))
            sceneManager().setNewScene(new MenuScene(), new String[]{});

        world.step(GameTime.FixedDeltaTime(), VELOCITY_ITERATIONS, POSITION_ITERATIONS);

        MouseManager mouseManager = context().getMouseManager();
        IInputManager inputManager = context().getInputManager();
        Camera2D mapCamera = camera2D();

        if (inputManager.inputPressed(ActionId.LEFT_CLICK)){
            boxes.add(
                    new Box(world,
                            new Vector2f(
                                    mapCamera.transformPosition(mouseManager.pos())),
                            new Vector2f(0.9f, 2))
            );
        }

        for (Box box : boxes)
            box.update();
    }

    public void display(){
        super.setVirtualScene();
        clear();

        ground.display();

        for (Box box : boxes)
            box.display();

        super.display();

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
