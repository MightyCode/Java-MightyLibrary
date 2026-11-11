package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.GUI.EnumSelector;
import io.github.mightycode.mightylib.graphics.GUI.HorizontalSlider;
import io.github.mightycode.mightylib.graphics.GUI.VerticalSlider;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.graphics.utils.Shaking2D;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import io.github.mightycode.mightylib.physics.tweenings.type.FloatTweening;
import io.github.mightycode.mightylib.physics.tweenings.type.Vector2fTweening;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

/**
 * GUI widgets and Shaking2D. The square goes back and forth with the tweening type chosen in the EnumSelector (top
 * right). The horizontal slider sets its size, the vertical slider the amplitude of the shake that Enter starts
 * (Shaking2D driven by a Vector2fTweening going from the amplitude to zero).
 */
public class GUIWidgetsScene extends Scene {
    private static final float LEFT = 200f;
    private static final float RIGHT = 1000f;
    private static final float TRAVEL_TIME = 2f;
    private static final float SHAKE_TIME = 0.7f;

    private EnumSelector<ETweeningType> typeSelector;
    private HorizontalSlider sizeSlider;
    private VerticalSlider amplitudeSlider;

    private RectangleRenderer square;
    private RectangleRenderer track;
    private Text info;

    private FloatTweening travel;
    private Shaking2D shaking;

    @Override
    public void launch(String[] args) {
        setClearColor(0.1f, 0.1f, 0.12f);
        mainContext.getMouseManager().setCursor(true);

        InputManager input = mainContext.getInputManager();

        typeSelector = new EnumSelector<>(ETweeningType.values(), mainContext);
        typeSelector.init(ActionId.LEFT_CLICK);

        sizeSlider = new HorizontalSlider(main2DCamera, input, mainContext.getMouseManager(),
                new Vector2f(LEFT, 600), new Vector2f(RIGHT - LEFT, 60), 20f, 200f);
        sizeSlider.init(ActionId.LEFT_CLICK);
        sizeSlider.setValue(100);

        amplitudeSlider = new VerticalSlider(main2DCamera, input, mainContext.getMouseManager(),
                new Vector2f(1100, 200), new Vector2f(60, 360), 0f, 40f);
        amplitudeSlider.init(ActionId.LEFT_CLICK);
        amplitudeSlider.setValue(15);

        track = new RectangleRenderer("colorShape2D");
        track.setColorMode(new Color4f(0.2f, 0.22f, 0.28f, 1f));
        track.setPosition(new Vector2f(LEFT, 345));
        track.setSizePix(RIGHT - LEFT, 10);

        square = new RectangleRenderer("colorShape2D");
        square.setColorMode(new Color4f(0.95f, 0.8f, 0.2f, 1f));

        info = new Text();
        info.setFont("bahnschrift")
                .setFontSize(24)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(new Color4f(1, 1, 1, 1))
                .setPosition(new Vector2f(20, 100));

        startTravel();
    }

    private void startTravel() {
        travel = new FloatTweening();
        travel.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(typeSelector.getSelected(), ETweeningBehaviour.InOut)
                .initTwoValue(TRAVEL_TIME, LEFT, RIGHT);
    }

    private void startShake() {
        float amplitude = (float) amplitudeSlider.getCurrentValue();

        Vector2fTweening decay = new Vector2fTweening();
        decay.setTweeningOption(ETweeningOption.Direct)
                .setTweeningValues(ETweeningType.Linear, ETweeningBehaviour.In)
                .initTwoValue(SHAKE_TIME, new Vector2f(amplitude, amplitude), new Vector2f(0, 0));

        shaking = new Shaking2D(decay);
    }

    @Override
    public void update() {
        super.update();

        InputManager input = mainContext.getInputManager();

        if (input.inputPressed(ActionId.ESCAPE))
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});

        typeSelector.update();
        sizeSlider.update();
        amplitudeSlider.update();

        if (typeSelector.isUpdated())
            startTravel();

        if (input.inputPressed(ActionId.ENTER))
            startShake();

        travel.update();

        if (shaking != null) {
            shaking.update();

            if (shaking.isFinished())
                shaking = null;
        }
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        float size = (float) sizeSlider.getCurrentValue();
        Vector2f shake = shaking == null ? new Vector2f() : shaking.value();

        track.display();

        square.setSizePix(size, size);
        // The tweening goes from LEFT to RIGHT : the square stays inside the track whatever its size
        float progress = (travel.value() - LEFT) / (RIGHT - LEFT);
        float x = LEFT + progress * (RIGHT - LEFT - size);
        square.setPosition(new Vector2f(x + shake.x, 360 - size / 2f + shake.y));
        square.display();

        info.setText("Type : " + typeSelector.getSelectedName()
                + "    size : " + Math.round(sizeSlider.getCurrentValue())
                + "    amplitude : " + Math.round(amplitudeSlider.getCurrentValue())
                + "    (Enter : shake)");
        info.display();

        sizeSlider.display();
        amplitudeSlider.display();
        typeSelector.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (square == null)
            return;

        typeSelector.unload();
        sizeSlider.unload();
        amplitudeSlider.unload();
        square.unload();
        track.unload();
        info.unload();
    }
}
