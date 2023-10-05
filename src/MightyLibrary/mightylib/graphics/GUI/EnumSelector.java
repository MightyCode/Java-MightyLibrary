package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.ColorList;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector2i;

/**
 * No used
 */
public class EnumSelector<T extends Enum<?>> {
    private final T[] values;
    private boolean updated;
    private int selected;
    private boolean canLoop;

    private final Text typeSelectedName;
    private final RectangleRenderer background;
    private final BackgroundlessButton chooseLeft, chooseRight;

    private final InputManager inputManager;

    private int clickActionId;

    public EnumSelector(T[] values, Context context){
        this.values = values;
        this.inputManager = context.getInputManager();
        Vector2i windowSize = context.getWindow().getInfo().getVirtualSizeCopy();

        typeSelectedName = new Text();
        typeSelectedName.setFont("bahnschrift")
                .setColor(ColorList.Blue())
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.Up)
                .setPosition(new Vector2f(windowSize.x * 0.92f, windowSize.y * 0.01f))
                .setFontSize(20);

        chooseLeft = new BackgroundlessButton(context);
        chooseLeft.Text.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.Up)
                .setPosition(new Vector2f(windowSize.x * 0.87f, windowSize.y * 0.01f))
                .setFontSize(30)
                .setText("<-");

        chooseLeft.copyTextToOverlapping().OverlapsText.setColor(new Color4f(0.5f));

        chooseRight = chooseLeft.copy();
        chooseRight.Text.setPosition(new Vector2f(windowSize.x * 0.97f, windowSize.y * 0.01f))
                .setText("->");

        chooseRight.copyTextToOverlapping().OverlapsText.setColor(new Color4f(0.8f));

        background = new RectangleRenderer("colorShape2D");
        background.switchToColorMode(ColorList.White());
        background.setPosition(new Vector2f(windowSize.x * 0.835f, windowSize.y * 0.0f));
        background.setSizePix(windowSize.x * 0.45f, windowSize.y * 0.08f);

        canLoop = true;

        updated = false;

        setSelected(0);
    }

    public void init(int clickActionId){
        this.clickActionId = clickActionId;
    }

    public void update()
    {
        if (chooseLeft.GUIMouseSelected() && inputManager.inputPressed(clickActionId)){
            setSelected(selected - 1);
            updated = true;
        } else if (chooseRight.GUIMouseSelected() && inputManager.inputPressed(clickActionId)){
            setSelected(selected + 1);
            updated = true;
        }
    }

    public boolean isUpdated(){
        return updated;
    }

    public void display(){
        updated = false;
        background.display();
        chooseLeft.display();
        chooseRight.display();
        typeSelectedName.display();
    }

    public EnumSelector<T> setCanLoop(){
        canLoop = true;

        return setIndex(selected);
    }

    public EnumSelector<T> setIndex(int i){
        setSelected(i);

        return this;
    }

    private void setSelected(int i){
        this.selected = i;

        if (selected < 0){
            if (canLoop)
                selected = size() - 1;
            else
                selected = 0;
        } else if (selected >= size()){
            if (canLoop)
                selected = 0;
            else
                selected = size() - 1;
        }

        typeSelectedName.setText(getSelectedName());
    }

    public int size() {
        return values.length;
    }

    public T getSelected(){
        return values[selected];
    }

    public String getSelectedName(){
        return values[selected].name();
    }

    public T[] getValues(){
        return values;
    }

    public void unload(){
        background.unload();
        chooseLeft.unload();
        chooseRight.unload();
        typeSelectedName.unload();
    }
}
