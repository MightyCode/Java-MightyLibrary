package io.github.mightycode.mightylibdemo.scenes;

import io.github.mightycode.mightylib.graphics.GUI.BackgroundlessButton;
import io.github.mightycode.mightylib.graphics.GUI.GUI;
import io.github.mightycode.mightylib.graphics.GUI.ScrollList;
import io.github.mightycode.mightylib.graphics.GUI.TextField;
import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningBehaviour;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningOption;
import io.github.mightycode.mightylib.physics.tweenings.ETweeningType;
import io.github.mightycode.mightylib.physics.tweenings.type.FloatTweening;
import io.github.mightycode.mightylib.resources.data.JSONFile;
import io.github.mightycode.mightylib.scenes.DefaultLoadingScene;
import io.github.mightycode.mightylib.scenes.Scene;
import io.github.mightycode.mightylib.scenes.TemplateSceneLoading;
import io.github.mightycode.mightylib.sounds.SoundSourceCreationInfo;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.math.MightyMath;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import io.github.mightycode.mightylibdemo.main.ActionId;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;

/**
 * The menu of the demo: a field that filters the scenes, the list of the scenes, a panel that describes the one
 * chosen, and Quit. It shows how a game talks to the engine with its commands: the scenes are the candidates of the
 * completion of the "scene" command, and a choice runs "scene <name>" (or "exit") through Commands.execute, which
 * works on the window of this scene. A command that fails writes its message in red in the panel.
 *
 * The hidden scenes and the descriptions are data (resources/data/menuScenes.json, batch "menu").
 * Keys : up and down (W, S) go from the field to the list then Quit, Enter chooses (in the field: starts typing),
 * Escape quits ; in the field Enter launches the first scene of the list, Escape or a click elsewhere give the
 * focus back (InputFocus).
 */
public class MenuScene extends Scene {
    private static final SoundSourceCreationInfo CREATION_INFO_SELECT = new SoundSourceCreationInfo();
    static {
        CREATION_INFO_SELECT.name = "selectogg";
        CREATION_INFO_SELECT.gainNode = "noise";
        CREATION_INFO_SELECT.gain = 0.2f;
    }

    private static final Color4f PANEL_COLOR = new Color4f(0.1f, 0.25f, 0.55f, 1f);
    private static final Color4f TEXT_COLOR = new Color4f(1f, 1f, 1f, 1f);
    private static final Color4f ERROR_COLOR = new Color4f(1f, 0.4f, 0.35f, 1f);

    // The part of the menu the keys act on
    private enum EZone {
        FIELD, LIST, QUIT
    }

    private SceneChoices choices;
    private Map<String, String> descriptions;
    private String defaultDescription;
    // The scenes of the list now (the filter applied)
    private List<SceneChoices.Entry> shown;

    private TextField field;
    private ScrollList sceneList;
    private BackgroundlessButton buttonQuit;
    private RectangleRenderer panel;
    private Text panelTitle;
    private Text panelText;
    private Text title;

    private FloatTweening rotation;

    private EZone zone = EZone.FIELD;
    // Written in red in the panel while the panel is about the scene it happened for (errorKey)
    private String error;
    private String errorKey;
    private boolean errorShown;

    // For the sound of the selection
    private EZone lastZone;
    private Integer lastSelected;

    @Override
    protected String[] getInvolvedBatch() {
        return new String[]{ "menu" };
    }

    @Override
    protected TemplateSceneLoading createLoadingScene(String[] args) {
        // Plain grey screen at the start of the program
        if (args.length > 0 && args[0].equals("start"))
            return new DefaultLoadingScene();

        return super.createLoadingScene(args);
    }

    public void launch(String[] args) {
        /// SCENE INFORMATION ///

        mainContext.getMouseManager().setCursor(true);

        main3DCamera.setPos(new Vector3f(0, 0, 0));
        setClearColor(52, 189, 235, 1f);

        /// DATA ///

        JSONObject config = resources.getResource(JSONFile.class, "menuScenes").getObject();
        Set<String> hidden = new HashSet<>();
        JSONArray hiddenNames = config.getJSONArray("hidden");
        for (int i = 0; i < hiddenNames.length(); ++i)
            hidden.add(hiddenNames.getString(i));

        descriptions = new HashMap<>();
        JSONObject descriptionsJson = config.getJSONObject("descriptions");
        for (String name : descriptionsJson.keySet())
            descriptions.put(name, descriptionsJson.getString(name));
        defaultDescription = config.optString("defaultDescription", "No description.");

        // The scenes are the ones the command "scene" suggests: nothing to maintain here when one is added
        choices = new SceneChoices(engine().commands().complete("scene ").getCandidates(), hidden);
        shown = choices.entries();

        /// RENDERERS ///

        Vector2i windowSize = mainContext.getWindow().getInfo().getVirtualSizeCopy();

        title = new Text();
        title.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setFontSize(80)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.09f))
                .setText("MightyLibrary");

        float left = windowSize.x * 0.07f;
        float top = windowSize.y * 0.22f;
        float listWidth = windowSize.x * 0.42f;
        float fieldHeight = 54f;
        float listTop = top + fieldHeight + 10f;
        float bottom = windowSize.y * 0.86f;

        field = new TextField(mainContext, left, top, listWidth, fieldHeight, "bahnschrift", 30);
        field.setHint("Filter the scenes...");

        sceneList = new ScrollList(mainContext, left, listTop, listWidth, bottom - listTop, 50f, "bahnschrift", 30);
        sceneList.setupActivationActions(ActionId.LEFT_CLICK);
        sceneList.setItems(labelsOf(shown));

        float panelLeft = left + listWidth + 40f;
        float panelWidth = windowSize.x - left - panelLeft;
        float padding = 20f;

        panel = new RectangleRenderer("colorShape2D");
        panel.setColorMode(PANEL_COLOR);
        panel.setPosition(new Vector2f(panelLeft, top));
        panel.setSizePix(panelWidth, bottom - top);

        panelTitle = new Text();
        panelTitle.setFont("bahnschrift")
                .setFontSize(38)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(TEXT_COLOR)
                .setPosition(new Vector2f(panelLeft + padding, top + padding));

        panelText = new Text();
        panelText.setFont("bahnschrift")
                .setFontSize(28)
                .setReference(EDirection.LeftUp)
                .setAlignment(ETextAlignment.Left)
                .setColor(TEXT_COLOR)
                .setMaxWidth(panelWidth - 2 * padding)
                .setPosition(new Vector2f(panelLeft + padding, top + padding + 70f));

        buttonQuit = new BackgroundlessButton(mainContext);
        buttonQuit.text().setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.None)
                .setFontSize(40)
                .setPosition(new Vector2f(windowSize.x * 0.5f, windowSize.y * 0.93f))
                .setText("Quit");
        buttonQuit.text().copyTo(buttonQuit.overlapsText());
        buttonQuit.overlapsText().setColor(new Color4f(0.3f)).setText("->Quit<-");

        rotation = new FloatTweening();
        rotation.setTweeningOption(ETweeningOption.LoopReversed)
                .setTweeningValues(ETweeningType.Sinusoidal, ETweeningBehaviour.InOut)
                .initTwoValue(2, 0f, MightyMath.PI_FLOAT * 2f);

        refreshPanel();
    }

    private static List<String> labelsOf(List<SceneChoices.Entry> entries) {
        List<String> labels = new ArrayList<>();
        for (SceneChoices.Entry entry : entries)
            labels.add(entry.label());

        return labels;
    }

    // The filter changed: the list shows the scenes that match, the first one is chosen
    private void refilter() {
        shown = choices.filter(field.getText());
        sceneList.setItems(labelsOf(shown));
        error = null;
    }

    @Override
    public void update() {
        super.update();

        InputManager input = mainContext.getInputManager();
        MouseManager mouse = mainContext.getMouseManager();

        // While the field has the focus the scene sees no input: only the field and the mouse position work
        field.update();
        sceneList.update();

        boolean filterChanged = field.isChanged();
        if (filterChanged)
            refilter();

        // The mouse chooses the zone it moves over
        if (!mouse.oldPos().equals(mouse.pos())) {
            if (field.GUIMouseSelected())
                zone = EZone.FIELD;
            else if (sceneList.getHovered() != null)
                zone = EZone.LIST;
            else if (buttonQuit.GUIMouseSelected())
                zone = EZone.QUIT;
        }

        // What the field says when it gives the focus back
        if (field.getLeftWith() == GLFW_KEY_DOWN)
            navigateDown();
        if (field.isSubmitted())
            launchScene(shown.isEmpty() ? null : shown.get(0));

        if (input.inputPressed(ActionId.SELECT_DOWN))
            navigateDown();
        else if (input.inputPressed(ActionId.SELECT_UP))
            navigateUp();

        if (input.inputPressed(ActionId.ENTER))
            activateZone();

        Integer clicked = sceneList.getActivated();
        if (clicked != null)
            launchScene(shown.get(clicked));

        if (buttonQuit.GUIMouseSelected() && input.inputPressed(ActionId.LEFT_CLICK))
            quit();

        if (input.inputPressed(ActionId.ESCAPE))
            quit();

        setSelected(field, zone == EZone.FIELD);
        setSelected(buttonQuit, zone == EZone.QUIT);
        refreshPanel();

        rotation.update();
        buttonQuit.text().setRotation(rotation.value(), new Vector3f(0, 0, 1));

        // A sound when the choice changes, not when the filter changes it
        Integer selected = sceneList.getSelected();
        if (lastZone != null && !filterChanged && (zone != lastZone || !Objects.equals(selected, lastSelected)))
            engine().soundManager().createSoundSource(CREATION_INFO_SELECT);
        lastZone = zone;
        lastSelected = selected;
    }

    private void navigateDown() {
        switch (zone) {
            case FIELD:
                zone = shown.isEmpty() ? EZone.QUIT : EZone.LIST;
                break;
            case LIST:
                if (!sceneList.moveSelection(1))
                    zone = EZone.QUIT;
                break;
            default:
                break;
        }
    }

    private void navigateUp() {
        switch (zone) {
            case QUIT:
                zone = shown.isEmpty() ? EZone.FIELD : EZone.LIST;
                break;
            case LIST:
                if (sceneList.isSelectionAtStart())
                    zone = EZone.FIELD;
                else
                    sceneList.moveSelection(-1);
                break;
            default:
                break;
        }
    }

    private void activateZone() {
        switch (zone) {
            case FIELD:
                field.requestFocus();
                break;
            case LIST:
                Integer selected = sceneList.getSelected();
                if (selected != null)
                    launchScene(shown.get(selected));
                break;
            case QUIT:
                quit();
                break;
        }
    }

    // The engine's way to change scenes: its command, as the console would run it
    private void launchScene(SceneChoices.Entry entry) {
        if (entry == null) {
            fail("No scene to launch: nothing matches \"" + field.getText() + "\".");
            return;
        }

        CommandResult result = engine().commands().execute("scene " + entry.command());
        if (!result.isSuccess())
            fail(result.getMessage());
    }

    private void quit() {
        CommandResult result = engine().commands().execute("exit");
        if (!result.isSuccess())
            fail(result.getMessage());
    }

    // In the panel, and in the log
    private void fail(String message) {
        error = message;
        errorKey = currentKey();
        System.err.println("--Menu: " + message);
    }

    // The scene the panel is about : the one under the mouse, otherwise the selected one
    private String currentKey() {
        Integer row = sceneList.getActive();
        return row == null ? "" : shown.get(row).command();
    }

    // The part of the menu the keys act on is shown selected
    private static void setSelected(GUI gui, boolean selected) {
        if (selected)
            gui.forceSelect(true);
        else
            gui.forceUnselect(true);
    }

    private void refreshPanel() {
        // Another choice: the error is old news
        if (error != null && !Objects.equals(errorKey, currentKey()))
            error = null;

        Integer row = sceneList.getActive();
        SceneChoices.Entry entry = row == null ? null : shown.get(row);

        boolean inError = error != null;
        if (inError != errorShown) {
            errorShown = inError;
            panelText.setColor(inError ? ERROR_COLOR : TEXT_COLOR);
        }

        if (inError) {
            panelTitle.setText("Error");
            panelText.setText(error);
        } else if (entry == null) {
            panelTitle.setText("No scene");
            panelText.setText("No scene matches the filter.");
        } else {
            panelTitle.setText(entry.label());
            panelText.setText(descriptions.getOrDefault(entry.command(), defaultDescription));
        }
    }

    @Override
    public void display() {
        super.setVirtualScene();
        clear();
        super.display();

        title.display();
        panel.display();
        panelTitle.display();
        panelText.display();
        field.display();
        sceneList.display();
        buttonQuit.display();

        super.setAndDisplayRealScene();
    }

    @Override
    public void unload() {
        super.unload();

        if (title != null)
            title.unload();
        if (panel != null)
            panel.unload();
        if (panelTitle != null)
            panelTitle.unload();
        if (panelText != null)
            panelText.unload();
        // Gives the focus back if the scene is left while typing
        if (field != null)
            field.unload();
        if (sceneList != null)
            sceneList.unload();
        if (buttonQuit != null)
            buttonQuit.unload();
    }
}
