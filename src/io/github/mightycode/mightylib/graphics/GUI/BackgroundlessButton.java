package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.main.WindowContext;
import org.joml.Vector2f;

public class BackgroundlessButton extends GUI {
    private final WindowContext context;
    public final Text Text, OverlapsText;

    public BackgroundlessButton(WindowContext context){
        this(context, null);
    }

    public BackgroundlessButton(WindowContext context, Text text){
        super();
        this.context = context;

        if (text == null)
            this.Text = new Text();
        else
            this.Text = text;

        if (text != null) {
            this.OverlapsText = text.createCopy();
        } else {
            this.OverlapsText = new Text();
        }
    }

    @Override
    public void display() {
        if ((GUIMouseSelected() || forceSelected()) && !forceUnselected()) {
            OverlapsText.display();
        } else {
            Text.display();
        }
    }

    @Override
    public boolean GUIMouseSelected(){
        Vector2f position = Text.leftUpPosition();
        Vector2f size = Text.size();
        Vector2f mousePosition = referenceCamera.transformPosition(context.getMouseManager().pos());

        return (mousePosition.x >= position.x - (size.x * 0.1f) && mousePosition.x <= position.x + (size.x * 1.1f)
                && mousePosition.y >= position.y - (size.y * 0.1f) && mousePosition.y <= position.y + (size.y * 1.1f));
    }

    public BackgroundlessButton copy(){
        BackgroundlessButton temp = new BackgroundlessButton(context);
        Text.copyTo(temp.Text);
        OverlapsText.copyTo(temp.OverlapsText);

        return temp;
    }

    public BackgroundlessButton copyTextToOverlapping(){
        this.Text.copyTo(OverlapsText);

        return this;
    }

    @Override
    public void unload(){
        Text.unload();
        OverlapsText.unload();
    }

    @Override
    public boolean mouseDisableIt() {
        return true;
    }
}
