package MightyLibrary.mightylib.scenes.camera.cameraComponents;

import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.main.utils.IUpdatableDisplayable;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.scenes.camera.Camera3D;
import MightyLibrary.mightylib.utils.math.color.ColorList;
import MightyLibrary.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

import java.util.HashSet;
import java.util.Set;

public class DebugInfoCamera3D implements IUpdatableDisplayable {
    public final static int POSITION = 1;
    public final static int LOOK = 2;
    public final static int SPEED = 3;


    protected Camera3D camera;
    public final Text Text;
    private final Set<Integer> info = new HashSet<>();

    private int precision = 2;

    public DebugInfoCamera3D(){
        Text = new Text();
        Text.setReference(EDirection.LeftUp).setAlignment(ETextAlignment.Left)
                .setPosition(new Vector2f(0, 0))
                .setColor(ColorList.Purple())
                .setFont("arial").setFontSize(25);
    }

    public DebugInfoCamera3D(Camera2D camera){
        this();
        Text.setReferenceCamera(camera);
    }

    public DebugInfoCamera3D init(Camera3D camera){
        return init(camera, new Vector2f(0, 0));
    }

    public DebugInfoCamera3D init(Camera3D camera, Vector2f position){
        this.camera = camera;

        this.Text.setPosition(position);

        return this;
    }

    public DebugInfoCamera3D setPrecision(int precision){
        this.precision = precision;
        return this;
    }

    public String prec(float value){
        return String.format("%."+precision+"f", value);
    }

    public DebugInfoCamera3D addInfo(int info){
        this.info.add(info);
        return this;
    }

    @Override
    public void display() {
        Text.display();
    }

    @Override
    public void update() {
    }

    @Override
    public void dispose() {
        StringBuilder result = new StringBuilder();
        result.append("camera3D id : ").append(camera.id()).append("\n");

        for (Integer info : this.info) {
            switch (info) {
                case POSITION:
                    result.append("Position: x(")
                            .append(prec(camera.getCamPosRef().x))
                            .append(") y(")
                            .append(prec(camera.getCamPosRef().y))
                            .append(") z(")
                            .append(prec(camera.getCamPosRef().z))
                            .append(")\n");
                    break;
                case LOOK:
                    result.append("Look at: x(")
                            .append(prec(camera.getLookAtVector().x))
                            .append(") y(")
                            .append(prec(camera.getLookAtVector().y))
                            .append(") z(")
                            .append(prec(camera.getLookAtVector().z))
                            .append(")\n");
                    break;
                case SPEED:
                    result.append("Speed : x(")
                            .append(prec(camera.getSpeed().x))
                            .append(") y(")
                            .append(prec(camera.getSpeed().y))
                            .append(") z(")
                            .append(prec(camera.getSpeed().z))
                            .append(")\n");
                    break;
            }
        }

        Text.setText(result.toString());
    }

    @Override
    public void unload() {
        Text.unload();
    }
}
