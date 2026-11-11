package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.WindowContext;
import org.joml.Vector2f;

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
    public boolean GUIMouseSelected(){
        Vector2f position = text.leftUpPosition();
        Vector2f size = text.size();
        Vector2f mousePosition = referenceCamera.transformPosition(context.getMouseManager().pos());

        return (mousePosition.x >= position.x - (size.x * 0.1f) && mousePosition.x <= position.x + (size.x * 1.1f)
                && mousePosition.y >= position.y - (size.y * 0.1f) && mousePosition.y <= position.y + (size.y * 1.1f));
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
