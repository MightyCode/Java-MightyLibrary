package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.resources.sound.SoundData;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the same sound in each supported format (ogg, wav, mp3, flac) through the batch "soundformats".
 * Each line shows the state of a sound and plays it when the mouse enters it (ENTER plays the next one), ESCAPE goes back to the menu.
 * Check without a window: python tools/scenario.py "scene SoundFormatsScene" wait-loaded "?resources SoundData"
 */
public class SoundFormatsScene extends Scene {
    private static final String[] SOUNDS = {"selectogg", "selectwav", "selectmp3", "selectflac"};
    private static final String[] FORMATS = {"ogg", "wav", "mp3", "flac"};

    private final List<Text> lines = new ArrayList<>();
    private Text help;
    private int next;
    private int lastPlayed = -1;
    // Line under the mouse at the previous tick, a sound is played when it changes
    private int hovered = -1;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "soundformats" };
    }

    @Override
    public void launch(String[] args) {
        setClearColor(0.2f, 0.2f, 0.2f);
        Vector2f windowSize = new Vector2f(mainContext.getWindow().getInfo().getVirtualSizeRef());

        for (int i = 0; i < SOUNDS.length; ++i)
            lines.add(createText(new Vector2f(windowSize.x / 2, windowSize.y * (0.25f + 0.12f * i)), 40));

        help = createText(new Vector2f(windowSize.x / 2, windowSize.y * 0.85f), 30);
        help.setText("Survolez une ligne ou ENTREE : jouer, ECHAP : menu");
    }

    private Text createText(Vector2f position, int size) {
        Text text = new Text();
        text.setFont("bahnschrift")
                .setFontSize(size)
                .setReference(EDirection.Up)
                .setAlignment(ETextAlignment.Center)
                .setPosition(position)
                .setColor(new Color4f(1f))
                .setText(" ");
        return text;
    }

    private String state(int index) {
        SoundData data = resources.getResource(SoundData.class, SOUNDS[index]);
        // Ascii only: the font may not have accents
        String state = data.hasFailed() ? " (ECHEC)" : data.isLoaded() ? "" : " (chargement)";
        return (index == lastPlayed ? "> " : "") + "Jouer son " + FORMATS[index] + state;
    }

    // Same test as BackgroundlessButton: the box of the text with a margin, in the coordinates of the 2D camera
    private int lineUnderMouse() {
        Vector2f mouse = main2DCamera.transformPosition(mainContext.getMouseManager().pos());

        for (int i = 0; i < lines.size(); ++i) {
            Vector2f position = lines.get(i).leftUpPosition();
            Vector2f size = lines.get(i).size();

            if (mouse.x >= position.x - size.x * 0.1f && mouse.x <= position.x + size.x * 1.1f
                    && mouse.y >= position.y - size.y * 0.1f && mouse.y <= position.y + size.y * 1.1f)
                return i;
        }

        return -1;
    }

    private void play(int index) {
        SoundSourceCreationInfo info = new SoundSourceCreationInfo();
        info.name = SOUNDS[index];
        info.gainNode = "noise";
        info.gain = 0.2f;
        engine().soundManager().createSoundSource(info);

        lastPlayed = index;
    }

    @Override
    public void update() {
        super.update();

        if (mainContext.getInputManager().inputPressed(ActionId.ESCAPE)) {
            sceneManagerInterface.setNewScene(new MenuScene(), new String[]{});
            return;
        }

        if (mainContext.getInputManager().inputPressed(ActionId.ENTER)) {
            play(next);
            next = (next + 1) % SOUNDS.length;
        }

        int under = lineUnderMouse();
        if (under != hovered && under != -1)
            play(under);
        hovered = under;

        for (int i = 0; i < lines.size(); ++i)
            lines.get(i).setText(state(i));
    }

    @Override
    public void display() {
        setVirtualScene();
        clear();

        for (Text line : lines)
            line.display();
        help.display();

        setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        for (Text line : lines)
            line.unload();
        lines.clear();

        help.unload();
        help = null;
    }
}
