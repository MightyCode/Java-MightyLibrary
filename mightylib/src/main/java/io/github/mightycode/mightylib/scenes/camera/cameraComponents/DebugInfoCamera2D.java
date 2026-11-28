package io.github.mightycode.mightylib.scenes.camera.cameraComponents;

import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.utils.IUpdatableDisplayable;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.Logger;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DebugInfoCamera2D implements IUpdatableDisplayable {
    // Names accepted by addInfo
    static final List<String> KNOWN_INFO = Collections.unmodifiableList(Arrays.asList("position", "rotation", "zoom"));

    private Camera2D camera;
    private final Text text;
    // Shown in the order they were added
    private final Set<String> info = new LinkedHashSet<>();

    // Format of the numbers shown, from the precision
    private String numberFormat = "%.2f";

    public Text text() {
        return text;
    }

    public DebugInfoCamera2D(){
        text = new Text();
        text.setReference(EDirection.LeftUp).setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(0, 0))
                .setColor(ColorList.Purple())
                .setFontSize(25);
    }

    public DebugInfoCamera2D(Camera2D camera){
        this();
        text.setReferenceCamera(camera);
    }

    public DebugInfoCamera2D init(Camera2D camera){
        return init(camera, new Vector2f(0, 0));
    }

    public DebugInfoCamera2D init(Camera2D camera, Vector2f position){
        this.camera = camera;

        this.text.setPosition(position);

        return this;
    }

    /** Adds a line (position, rotation or zoom, any case); an unknown name is reported as a WARNING and ignored. */
    public DebugInfoCamera2D addInfo(String info){
        AddInfo(this.info, info);
        return this;
    }

    // Adds the name in lower case if it is known, else reports it; true if added
    static boolean AddInfo(Set<String> info, String requested) {
        String name = requested.toLowerCase(Locale.ROOT);
        if (!KNOWN_INFO.contains(name)) {
            Logger.Warn(Logger.CATEGORY_ENGINE, "DebugInfoCamera2D: unknown information '" + requested
                    + "', expected one of " + KNOWN_INFO);
            return false;
        }

        return info.add(name);
    }

    public DebugInfoCamera2D setPrecision(int precision){
        numberFormat = "%." + Math.max(0, precision) + "f";
        return this;
    }

    public String prec(float value){
        return Prec(numberFormat, value);
    }

    private static String Prec(String numberFormat, float value) {
        return String.format(numberFormat, value);
    }

    // The text shown for the camera: its name and id, then each information in the order given
    static String Describe(Camera2D camera, Collection<String> info, String numberFormat) {
        StringBuilder result = new StringBuilder();
        result.append("camera2D ").append(camera.getName()).append(" id : ").append(camera.getCameraId()).append("\n");

        for (String name : info) {
            switch (name) {
                case "position":
                    result.append("Position: x(")
                            .append(Prec(numberFormat, camera.getCamPosRef().x))
                            .append(") y(")
                            .append(Prec(numberFormat, camera.getCamPosRef().y))
                            .append(")\n");
                    break;
                case "rotation":
                    result.append("Rotation: ").append(Prec(numberFormat, camera.getRotation())).append("\n");
                    break;
                case "zoom":
                    result.append("Zoom: x(")
                            .append(Prec(numberFormat, camera.getZoomLevel().x))
                            .append(") y(")
                            .append(Prec(numberFormat, camera.getZoomLevel().y))
                            .append(")\n");
                    break;
                default:
                    // Refused by addInfo
                    break;
            }
        }

        return result.toString();
    }

    String describe() {
        return Describe(camera, info, numberFormat);
    }

    @Override
    public void update() {}

    @Override
    public void postUpdate() {
        text.setText(describe());
    }

    @Override
    public void display() {
        text.display();
    }

    @Override
    public void unload() {
        text.unload();
        info.clear();
    }
}
