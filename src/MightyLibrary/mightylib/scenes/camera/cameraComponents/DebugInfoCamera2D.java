package MightyLibrary.mightylib.scenes.camera.cameraComponents;

import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.main.utils.IUpdatableDisplayable;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DebugInfoCamera2D implements IUpdatableDisplayable {
    protected Camera2D camera;
    public final Text Text;
    private final Set<String> info = new HashSet<>();

    private int precision = 2;

    public DebugInfoCamera2D(){
        Text = new Text();
        Text.setReference(EDirection.LeftUp).setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(0, 0))
                .setColor(ColorList.Purple())
                .setFont("arial").setFontSize(25);

        info.add("id");
    }

    public DebugInfoCamera2D(Camera2D camera){
        this();
        Text.setReferenceCamera(camera);
    }

    public DebugInfoCamera2D init(Camera2D camera){
        return init(camera, new Vector2f(0, 0));
    }

    public DebugInfoCamera2D init(Camera2D camera, Vector2f position){
        this.camera = camera;

        this.Text.setPosition(position);

        return this;
    }

    public DebugInfoCamera2D addInfo(String info){
        this.info.add(info.toLowerCase(Locale.ROOT));
        return this;
    }

    public DebugInfoCamera2D setPrecision(int precision){
        this.precision = precision;
        return this;
    }

    public String prec(float value){
        return String.format("%."+precision+"f", value);
    }

    @Override
    public void update() {}

    @Override
    public void dispose() {
        StringBuilder result = new StringBuilder();
        result.append("camera2D id : ").append(camera.id()).append("\n");

        for (String info : this.info) {
            if (info.equals("position"))
                result.append("Position: x(")
                        .append(prec(camera.getCamPosRef().x))
                        .append(") y(")
                        .append(prec(camera.getCamPosRef().y))
                        .append(")\n");
            else if (info.equals("rotation"))
                result.append("Rotation: ").append(camera.getRotation()).append("\n");
            else if (info.equals("zoom"))
                result.append("Zoom: x(")
                        .append(prec(camera.getZoomLevel().x))
                        .append(") y(")
                        .append(prec(camera.getZoomLevel().y))
                        .append(")\n");

        }

        Text.setText(result.toString());
    }

    @Override
    public void display() {
        Text.display();
    }


    @Override
    public void unload() {
        Text.unload();
        info.clear();
        info.add("id");
    }
}
