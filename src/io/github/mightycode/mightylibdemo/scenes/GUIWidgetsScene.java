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
import io.github.mightycode.mightylib.sounds.SoundSource;
import io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

/**
 * GUI widgets, Shaking2D and positional sound. The square goes back and forth with the tweening type chosen in the
 * EnumSelector (top right); a looping chime follows it (SoundListener at the middle of the track: use headphones).
 * The lower horizontal slider sets its size, the upper one the volume of the chime, the vertical slider the amplitude
 * of the shake that Enter starts (Shaking2D driven by a Vector2fTweening going from the amplitude to zero).
 */
public class GUIWidgetsScene extends Scene {
    private static final float LEFT = 200f;
    private static final float RIGHT = 1000f;
    private static final float TRAVEL_TIME = 2f;
    private static final float SHAKE_TIME = 0.7f;
    private static final float TRACK_Y = 360f;
    // Distance between the listener and the line of the square, in sound units
    private static final float SOUND_DEPTH = 2f;
    private static final float DEFAULT_VOLUME = 0.6f;

    private EnumSelector<ETweeningType> typeSelector;
    private HorizontalSlider sizeSlider;
    private VerticalSlider amplitudeSlider;
    private HorizontalSlider volumeSlider;

    private RectangleRenderer square;
    private RectangleRenderer track;
    private Text info;

    private FloatTweening travel;
    private Shaking2D shaking;

    private SoundSource ambience;
    private boolean ambienceConfigured;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "guiwidgets" };
    }

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

        volumeSlider = new HorizontalSlider(main2DCamera, input, mainContext.getMouseManager(),
                new Vector2f(LEFT, 180), new Vector2f(RIGHT - LEFT, 40), 0f, 1f);
        volumeSlider.init(ActionId.LEFT_CLICK);
        volumeSlider.setValue(DEFAULT_VOLUME);

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

        // Positional sound: the listener sits at the middle of the track, the looping chime follows the square
        engine().soundManager().getListener().setPosition(toSoundSpace(new Vector2f((LEFT + RIGHT) / 2f, TRACK_Y)));

        SoundSourceCreationInfo chime = new SoundSourceCreationInfo();
        chime.name = "chime";
        chime.gainNode = "music";
        chime.gain = DEFAULT_VOLUME;
        chime.loop = true;
        // Kept by the scene until unload, played again if the sound is reloaded
        chime.owned = true;
        chime.position.set(soundPosition());
        ambience = engine().soundManager().createSoundSource(chime);
    }

    // Pixels to sound units: the track is 8 units long
    private static Vector2f toSoundSpace(Vector2f pixels) {
        return new Vector2f(pixels).div(100f);
    }

    /*
     * OpenAL pans from the direction of the source, not its distance: on the listener's line, the direction is only
     * full left or full right. The source is put in front of the listener (it faces -z), so the angle changes smoothly
     * along the track (about -63 to 63 degrees).
     */
    private Vector3f soundPosition() {
        return new Vector3f(toSoundSpace(squareCenter()), -SOUND_DEPTH);
    }

    private float squareSize() {
        return (float) sizeSlider.getCurrentValue();
    }

    // The tweening goes from LEFT to RIGHT : the square stays inside the track whatever its size
    private float squareX() {
        float progress = (travel.value() - LEFT) / (RIGHT - LEFT);
        return LEFT + progress * (RIGHT - LEFT - squareSize());
    }

    private Vector2f squareCenter() {
        return new Vector2f(squareX() + squareSize() / 2f, TRACK_Y);
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
        volumeSlider.update();

        if (typeSelector.isUpdated())
            startTravel();

        if (input.inputPressed(ActionId.ENTER))
            startShake();

        travel.update();
        updateSound();

        if (shaking != null) {
            shaking.update();

            if (shaking.isFinished())
                shaking = null;
        }
    }

    private void updateSound() {
        // The source only exists once the sound manager has created it
        if (ambience == null || !ambience.isReady())
            return;

        if (!ambienceConfigured) {
            // Gain falls slowly with the distance: still audible at the ends of the track
            ambience.setProperty(AL10.AL_REFERENCE_DISTANCE, 2f);
            ambienceConfigured = true;
        }

        ambience.setPosition(soundPosition());

        float volume = (float) volumeSlider.getCurrentValue();
        if (volume != ambience.getSelfGain())
            ambience.setGain(volume);
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        float size = squareSize();
        Vector2f shake = shaking == null ? new Vector2f() : shaking.value();

        track.display();

        square.setSizePix(size, size);
        square.setPosition(new Vector2f(squareX() + shake.x, TRACK_Y - size / 2f + shake.y));
        square.display();

        info.setText("Type : " + typeSelector.getSelectedName()
                + "    size : " + Math.round(sizeSlider.getCurrentValue())
                + "    amplitude : " + Math.round(amplitudeSlider.getCurrentValue())
                + "    (Enter : shake)"
                + "    volume : " + Math.round(volumeSlider.getCurrentValue() * 100) + " %");
        info.display();

        sizeSlider.display();
        amplitudeSlider.display();
        volumeSlider.display();
        typeSelector.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (square == null)
            return;

        // The owned looping sound outlives the scene otherwise; the listener goes back to the origin
        if (ambience != null)
            ambience.release();
        ambience = null;
        engine().soundManager().getListener().setPosition(new Vector2f());

        typeSelector.unload();
        sizeSlider.unload();
        amplitudeSlider.unload();
        volumeSlider.unload();
        square.unload();
        track.unload();
        info.unload();
    }
}
