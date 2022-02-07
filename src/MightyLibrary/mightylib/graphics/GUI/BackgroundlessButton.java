package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.main.Context;
import org.joml.Vector2f;

public class BackgroundlessButton {
    private final Context context;
    public final Text Text, OverlapsText;

    public BackgroundlessButton(Context context){
        this(context, null);
    }

    public BackgroundlessButton(Context context, Text text){
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

    public void display() {
        if (buttonSelected()) {
            OverlapsText.display();
        } else {
            Text.display();
        }
    }

    public boolean buttonSelected(){
        Vector2f position = Text.rightLeftPosition();
        Vector2f size = Text.size();
        Vector2f mousePosition = context.getMouseManager().pos();

        return (mousePosition.x >= position.x - (size.x * 0.1f) && mousePosition.x <= position.x + (size.x * 1.1f)
                && mousePosition.y >= position.y - (size.y * 0.1f) && mousePosition.y <= position.y + (size.y * 1.1f));
    }

    public void unload(){
        Text.unload();
        OverlapsText.text();
    }
}
