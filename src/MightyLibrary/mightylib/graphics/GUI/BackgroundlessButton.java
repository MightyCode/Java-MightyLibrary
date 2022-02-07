package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.main.Context;
import org.joml.Vector2f;

public class BackgroundlessButton extends GUI {
    private final Context context;
    public final Text Text, OverlapsText;

    public BackgroundlessButton(Context context){
        this(context, null);
    }

    public BackgroundlessButton(Context context, Text text){
        super();
        this.context = context;

        if(text == null)
            this.Text = new Text(context.getWindow().getInfo());
        else
            this.Text = text;

        if (text != null) {
            this.OverlapsText = text.createCopy();
        } else {
            this.OverlapsText = new Text(context.getWindow().getInfo());
        }
    }

    @Override
    public void display() {
        if (GUISelected()) {
            OverlapsText.display();
        } else {
            Text.display();
        }
    }

    @Override
    public boolean GUISelected(){
        if (userSelect)
            return true;

        Vector2f position = Text.rightLeftPosition();
        Vector2f size = Text.size();
        Vector2f mousePosition = context.getMouseManager().pos();

        return (mousePosition.x >= position.x - (size.x * 0.1f) && mousePosition.x <= position.x + (size.x * 1.1f)
                && mousePosition.y >= position.y - (size.y * 0.1f) && mousePosition.y <= position.y + (size.y * 1.1f));
    }

    @Override
    public void unload(){
        Text.unload();
        OverlapsText.text();
    }

    @Override
    public boolean mouseDeableIt() {
        return true;
    }
}
