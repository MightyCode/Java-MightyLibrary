package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.WindowContext;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Objects;

public class BackgroundlessButton extends GUI {
    private final WindowContext context;
    private final Text text, overlapsText;

    public Text text() {
        return text;
    }

    // Displayed instead of the text while the button is selected
    public Text overlapsText() {
        return overlapsText;
    }

    public BackgroundlessButton(WindowContext context){
        this(context, null);
    }

    public BackgroundlessButton(WindowContext context, Text text){
        super();
        this.context = context;

        this.text = Objects.requireNonNullElseGet(text, Text::new);

        if (text != null) {
            this.overlapsText = text.createCopy();
        } else {
            this.overlapsText = new Text();
        }
    }

    @Override
    public void display() {
        if ((GUIMouseSelected() || forceSelected()) && !forceUnselected()) {
            overlapsText.display();
        } else {
            text.display();
        }
    }

    @Override
    /**
     * True when the mouse is over the text as it is drawn : the rectangle of the text itself (padding excluded),
     * turned with the text when it is rotated.
     */
    public boolean GUIMouseSelected(){
        Vector2f mouse = referenceCamera.transformPosition(context.getMouseManager().pos());
        Vector3f origin = text.position();

        // The mouse relative to the point the text turns around, back in the frame of the unrotated text
        Vector3f local = new Vector3f(mouse.x - origin.x, mouse.y - origin.y, 0);
        Vector3f axis = text.rotationAxis();
        if (text.getRotationAngle() != 0 && axis.lengthSquared() > 0)
            local.rotate(new Quaternionf().fromAxisAngleRad(new Vector3f(axis).normalize(), -text.getRotationAngle()));

        // The box of the text, relative to the same point
        Vector2f leftUp = text.leftUpPosition();
        Vector2f size = text.size();
        Vector4f padding = text.getPadding();
        float left = leftUp.x - origin.x + padding.x;
        float up = leftUp.y - origin.y + padding.y;
        float right = leftUp.x - origin.x + size.x - padding.z;
        float down = leftUp.y - origin.y + size.y - padding.w;

        return local.x >= left && local.x <= right && local.y >= up && local.y <= down;
    }

    public BackgroundlessButton copy(){
        BackgroundlessButton temp = new BackgroundlessButton(context);
        text.copyTo(temp.text);
        overlapsText.copyTo(temp.overlapsText);

        return temp;
    }

    public BackgroundlessButton copyTextToOverlapping(){
        this.text.copyTo(overlapsText);

        return this;
    }

    @Override
    public void unload(){
        text.unload();
        overlapsText.unload();
    }

    @Override
    public boolean mouseDisableIt() {
        return true;
    }
}
