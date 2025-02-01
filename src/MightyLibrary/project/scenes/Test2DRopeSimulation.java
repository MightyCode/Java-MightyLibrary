package MightyLibrary.project.scenes;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.main.utils.GameTime;
import MightyLibrary.mightylib.main.utils.IUpdatableDisplayable;
import MightyLibrary.mightylib.scenes.Scene;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.DebugInfoCamera2D;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.DraggingCameraComponent;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.MovingCameraComponent;
import MightyLibrary.mightylib.scenes.camera.cameraComponents.ZoomingCameraComponent;
import MightyLibrary.mightylib.utils.math.MightyMath;
import MightyLibrary.mightylib.utils.math.color.Color4f;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import MightyLibrary.project.main.ActionId;
import MightyLibrary.project.scenes.loadingScenes.LoadingSceneImplementation;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class Test2DRopeSimulation extends Scene {

    interface CallBackUpdate {
        void update();
    }

    static class Point implements IUpdatableDisplayable {
        private final RectangleRenderer renderer;

        private final Vector2f position;
        private final Vector2f previousPosition;
        private final boolean locked;

        public Point(Vector2f position) {
            this(position, false);
        }

        public Point(Vector2f position, boolean locked) {
            this.previousPosition = new Vector2f(position);
            this.position = position;
            this.locked = locked;

            renderer = new RectangleRenderer("colorShape2D");
            renderer.load(0);
            renderer.setColorMode(ColorList.Grey());
            renderer.setSizePix(POINT_SIZE, POINT_SIZE);
            renderer.setReferenceDirection(EDirection.None);
            renderer.setPosition(position);

            if (locked) {
                renderer.setColorMode(POINT_LOCKED);
            } else {
                renderer.setColorMode(POINT_COLOR);
            }
        }

        public void select() {
            renderer.setColorMode(POINT_SELECTED);
        }

        public void unselect() {
            if (locked) {
                renderer.setColorMode(POINT_LOCKED);
            } else {
                renderer.setColorMode(POINT_COLOR);
                }
        }

        public Vector2f getPosition() {
            return position;
        }

        public Vector2f getPreviousPosition() {
            return previousPosition;
        }

        public void setNewPosition(Vector2f position) {
            this.previousPosition.set(this.position);
            this.position.set(position);
        }

        public void forcePosition(Vector2f position) {
            this.position.set(position);
        }

        public boolean isLocked() {
            return locked;
        }

        public boolean isPointOverriding(Vector2f point, float precision) {
            return point.distance(this.position) <= precision;
        }

        public void update(){
            renderer.setPosition(position);
        }

        @Override
        public void dispose() {}

        public void display(){
            renderer.display();
        }

        public void unload(){
            renderer.unload();
        }
    }

    static class Stick implements IUpdatableDisplayable {
        private final RectangleRenderer renderer;
        private final Point pointA;
        private final Point pointB;
        private final float designedLength;

        public Stick(Point pointA, Point pointB) {
            this.pointA = pointA;
            this.pointB = pointB;
            this.designedLength = pointA.getPosition().distance(pointB.getPosition());

            renderer = new RectangleRenderer("colorShape2D");
            renderer.load(0);
            renderer.setColorMode(ColorList.Yellow());
            renderer.setReferenceDirection(EDirection.None);

            update();
        }

        public Point getPointA() {
            return pointA;
        }

        public Point getPointB() {
            return pointB;
        }

        public float getDesignedLength() {
            return designedLength;
        }

        public boolean isPointOverriding(Vector2f point, float precision) {
            return MightyMath.isPositionOverridingLine(point,
                    new Vector4f(pointA.getPosition().x, pointA.getPosition().y, pointB.getPosition().x, pointB.getPosition().y), precision);
        }

        public void update(){
            // position = center of center
            Vector2f position = new Vector2f(
                    (pointA.getPosition().x + pointB.getPosition().x) / 2,
                    (pointA.getPosition().y + pointB.getPosition().y) / 2);

            // rotation = angle between both points
            float rotation = (float) Math.atan2(pointB.getPosition().y - pointA.getPosition().y, pointB.getPosition().x - pointA.getPosition().x);

            // size = distance between both points
            float sizeX = pointA.getPosition().distance(pointB.getPosition());
            float sizeY = POINT_SIZE / 2;

            renderer.setPosition(position);
            renderer.setRotation(rotation, new Vector3f(0, 0, 1));
            renderer.setSizePix(sizeX, sizeY);
        }

        @Override
        public void dispose() {}

        public void display(){
            renderer.display();
        }

        public void unload(){
            renderer.unload();
        }
    }

    private enum ClickingMode {
        ADD_POINT,
        ADD_POINT_CONNECTED_WITH_LAST,
        ADD_STICK,
        REMOVE_POINT,
        REMOVE_STICK
    }

    private final static float POINT_SIZE = 10;
    private final static float PRECISION = 10;
    private final static float GRAVITY = 9.81f;

    private final static Color4f POINT_COLOR = new Color4f(1f, 1f, 1f, 1f);
    private final static Color4f POINT_LOCKED = new Color4f(1f, 0, 0, 1f);
    private final static Color4f POINT_SELECTED = new Color4f(1f, 120f / 255, 120f / 255, 1f);

    private boolean simulationWorking;
    private boolean isDrawingStick, isDrawingPoint;
    private boolean controlWind;
    private ClickingMode clickingMode = ClickingMode.ADD_POINT;
    private int numberIteraction = 1;

    private Point selectedPoint;
    private Vector2f windForce;

    private ArrayList<Point> points;
    private ArrayList<Stick> sticks;

    private Camera2D hudCamera;
    private Text textCurrentState;
    private Text textCurrentModeClicking;
    private Text textIsDrawingPoint;
    private Text textIsDrawingStick;
    private Text textIsWindControlled;

    public void init(String[] args) {
        super.init(args, new LoadingSceneImplementation());

        windForce = new Vector2f(0, 0);

        simulationWorking = false;
        isDrawingStick = true;
        isDrawingPoint = true;
        controlWind = false;

        points = new ArrayList<>();
        sticks = new ArrayList<>();

        reset();
    }

    private void reset() {
        for (Point point : points)
            point.unload();
        points.clear();

        for (Stick stick : sticks)
            stick.unload();
        sticks.clear();
    }

    public void launch(String[] args) {
        super.launch(args);
        setClearColor(52, 189, 235, 1f);

        /// SCENE INFORMATION ///

        hudCamera = main2DCamera.copy();

        textCurrentState = new Text();
        textCurrentState.setReferenceCamera(hudCamera);

        Vector2i size = mainContext.getWindow().getInfo().getSizeRef();
        textCurrentState.setFont("bahnschrift")
                .setFontSize(20)
                .setReference(EDirection.RightUp)
                .setAlignment(ETextAlignment.Right)
                .setPosition(new Vector2f(size.x, 0))
                .setColor(new Color4f(0.5f, 0.4f, 0.3f, 1))
                .setText("Simulation working : " + simulationWorking + " ");

        textCurrentModeClicking = new Text();
        textCurrentState.copyTo(textCurrentModeClicking)
                .setPosition(new Vector2f(size.x, 24))
                .setText("ctrl - Clicking mode : " + clickingMode + " ");

        textIsDrawingPoint = new Text();
        textCurrentState.copyTo(textIsDrawingPoint)
                .setPosition(new Vector2f(size.x, 48))
                .setText("p - Drawing point : " + isDrawingPoint + " ");

        textIsDrawingStick = new Text();
        textCurrentState.copyTo(textIsDrawingStick)
                .setPosition(new Vector2f(size.x, 72))
                .setText("o - Drawing stick : " + isDrawingStick + " ");

        textIsWindControlled = new Text();
        textCurrentState.copyTo(textIsWindControlled)
                .setPosition(new Vector2f(size.x, 96))
                .setText("i - Wind controlled : " + controlWind + " ");

        DraggingCameraComponent draggingSceneComponent = new DraggingCameraComponent();
        draggingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(), main2DCamera);
        draggingSceneComponent.initActionId(ActionId.RIGHT_CLICK);
        addUpdatable(draggingSceneComponent);

        MovingCameraComponent movingSceneComponent = new MovingCameraComponent();
        movingSceneComponent.init(mainContext.getInputManager(), main2DCamera);
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
        zoomingSceneComponent.init(mainContext.getInputManager(), mainContext.getMouseManager(),
                main2DCamera, mainContext.getWindow().getInfo().getSizeRef());
        zoomingSceneComponent.initActionId(ActionId.SHIFT);

        addUpdatable(zoomingSceneComponent);

        addUpdatableAndDisplayable(
                new DebugInfoCamera2D(hudCamera).init(main2DCamera, new Vector2f(5, 5))
                        .addInfo("position")
                        .addInfo("rotation")
                        .addInfo("zoom")
        );

        main2DCamera.setZoomLevel(0.5f);
    }

    public void update() {
        super.update();

        InputManager inputManager = mainContext.getInputManager();
        KeyboardManager keyboardManager = mainContext.getKeyboardManager();

        if (inputManager.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        if (keyboardManager.keyPressed(GLFW_KEY_SPACE)) {
            simulationWorking = !simulationWorking;
            textCurrentState.setText("Simulation working : " + simulationWorking + " ");
        }

        if (keyboardManager.keyPressed(GLFW_KEY_LEFT_CONTROL)) {
            clickingMode = ClickingMode.values()[(clickingMode.ordinal() + 1) % ClickingMode.values().length];
            textCurrentModeClicking.setText("z - Clicking mode : " + clickingMode + " ");

            if (selectedPoint != null) {
                selectedPoint.unselect();
                selectedPoint = null;
            }
        }

        if (keyboardManager.keyPressed(GLFW_KEY_P)) {
            isDrawingPoint = !isDrawingPoint;
            textIsDrawingPoint.setText("p - Drawing point : " + isDrawingPoint + " ");
        }

        if (keyboardManager.keyPressed(GLFW_KEY_O)) {
            isDrawingStick = !isDrawingStick;
            textIsDrawingStick.setText("o - Drawing stick : " + isDrawingStick + " ");
        }

        if (keyboardManager.keyPressed(GLFW_KEY_I)) {
            controlWind = !controlWind;
            textIsWindControlled.setText("i - Wind controlled : " + controlWind + " ");
        }

        if (keyboardManager.keyPressed(GLFW_KEY_R)) {
            reset();
        }

        if (inputManager.getState(ActionId.LEFT_CLICK))
            mouseLeftClick();

        /// Update physic
        if (!simulationWorking)
            return;

        if (controlWind) {
            // Wind = 2 time the gravity if mouse on outer border
            // Wind 0 if mouse on center
            Vector2f mousePosition = main2DCamera.getPosition(inputManager.getMouseManager().pos());
            Vector2i windowSize = mainContext.getWindow().getInfo().getSizeCopy();

            windForce = new Vector2f(0, 0);
            windForce.x = (mousePosition.x - (float) windowSize.x / 2) / windowSize.x * GRAVITY * 2;
            windForce.y = (mousePosition.y - (float) windowSize.y / 2) / windowSize.y * GRAVITY * 2;

            System.out.println(windForce.x + " " + windForce.y);
        }

        // Update points
        for (Point point : points) {
            if (point.isLocked())
                continue;

            // Update using gravity
            Vector2f newPosition = new Vector2f(point.getPosition());
            Vector2f oldPosition = point.getPreviousPosition();
            Vector2f diff = new Vector2f(newPosition).sub(oldPosition);

            newPosition.add(diff);

            Vector2f force = new Vector2f(0, GRAVITY);
            force.add(windForce);
            newPosition.add(force.mul(GameTime.DeltaTime()));

            point.setNewPosition(newPosition);
        }

        // Update using sticks
        for (int i = 0; i < numberIteraction; ++i) {
            for (Stick stick : sticks) {
                Point pointA = stick.getPointA();
                Point pointB = stick.getPointB();

                Vector2f stickCenter = new Vector2f(pointA.getPosition()).add(pointB.getPosition()).mul(0.5f);
                Vector2f stickDirection = new Vector2f(pointB.getPosition()).sub(pointA.getPosition()).normalize();

                Vector2f stickCenterCopy = new Vector2f(stickCenter);
                Vector2f stickDirectionCopy = new Vector2f(stickDirection);

                if (!pointA.isLocked())
                    pointA.forcePosition(stickCenter.sub(stickDirection.mul(stick.getDesignedLength() / 2)));

                if (!pointB.isLocked())
                    pointB.forcePosition(stickCenterCopy.add(stickDirectionCopy.mul(stick.getDesignedLength() / 2)));
            }
        }


        // Update renderers
        for (Point point : points)
            point.update();

        for (Stick stick : sticks)
            stick.update();
    }

    public void mouseLeftClick() {
        InputManager inputManager = mainContext.getInputManager();
        KeyboardManager keyboardManager = mainContext.getKeyboardManager();

        Vector2f mousePositionInWorld = main2DCamera.getPosition(inputManager.getMouseManager().pos());
        boolean noOverridePoints;

        switch (clickingMode) {
            case ADD_POINT:
                noOverridePoints = true;
                for (Point point : points) {
                    if (point.isPointOverriding(mousePositionInWorld, PRECISION)) {
                        noOverridePoints = false;
                        break;
                    }
                }

                if (noOverridePoints)
                    points.add(new Point(mousePositionInWorld, keyboardManager.getKeyState(GLFW_KEY_LEFT_SHIFT)));
                break;
            case ADD_POINT_CONNECTED_WITH_LAST:
                noOverridePoints = true;
                for (Point point : points) {
                    if (point.isPointOverriding(mousePositionInWorld, PRECISION)) {
                        noOverridePoints = false;
                        break;
                    }
                }

                if (noOverridePoints) {
                    points.add(new Point(mousePositionInWorld, keyboardManager.getKeyState(GLFW_KEY_LEFT_SHIFT)));

                    if (points.size() >= 2)
                        sticks.add(new Stick(points.get(points.size() - 2), points.get(points.size() - 1)));
                }
                break;
            case ADD_STICK:
                for (Point point : points) {
                    if (point.isPointOverriding(mousePositionInWorld, PRECISION)) {
                        if (selectedPoint == null) {
                            selectedPoint = point;
                            selectedPoint.select();
                        } else if (selectedPoint != point) {
                            boolean noExists = true;
                            for (Stick stick : sticks) {
                                if ((stick.getPointA() == selectedPoint && stick.getPointB() == point) ||
                                        (stick.getPointA() == point && stick.getPointB() == selectedPoint)) {
                                    noExists = false;
                                    break;
                                }
                            }

                            if (noExists) {
                                sticks.add(new Stick(selectedPoint, point));
                                selectedPoint.unselect();
                                selectedPoint = null;
                            }
                        }

                        break;
                    }
                }

                break;
            case REMOVE_POINT:
                for (Point point : points) {
                    if (point.isPointOverriding(mousePositionInWorld, PRECISION)) {
                        for (Stick stick : sticks) {
                            if (stick.getPointA() == point || stick.getPointB() == point) {
                                stick.unload();
                                sticks.remove(stick);
                                break;
                            }
                        }

                        point.unload();
                        points.remove(point);
                        break;
                    }
                }
                break;
            case REMOVE_STICK:
                for (Stick stick : sticks) {
                    if (stick.isPointOverriding(mousePositionInWorld, PRECISION)) {
                        stick.unload();
                        sticks.remove(stick);
                        break;
                    }
                }
                break;
        }
    }

    public void display() {
        super.setVirtualScene();
        clear();

        if (isDrawingPoint) {
            for (Point point : points)
                point.display();
        }

        if (isDrawingStick) {
            for (Stick stick : sticks)
                stick.display();
        }

        super.display();

        textCurrentState.display();
        textCurrentModeClicking.display();
        textIsDrawingPoint.display();
        textIsDrawingStick.display();
        textIsWindControlled.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (Point point : points)
            point.unload();

        for (Stick stick : sticks)
            stick.unload();

        textCurrentState.unload();
        textCurrentModeClicking.unload();
        textIsDrawingPoint.unload();
        textIsDrawingStick.unload();
        textIsWindControlled.unload();
    }
}
