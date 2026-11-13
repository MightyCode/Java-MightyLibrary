package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.graphics.text.ETextAlignment;
import io.github.mightycode.mightylib.graphics.text.Text;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.InputManager;
import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.utils.math.color.Color4f;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import io.github.mightycode.mightylib.utils.math.geometry.EDirection;
import org.joml.Vector2f;

/**
 * Left / right selector over an array of values, shown by name. Subclasses give the name of a value.
 */
public abstract class ValueSelector<T> {
    private final T[] values;

    private boolean updated;
    private int selected;
    private boolean canLoop;

    private final Text typeSelectedName;
    private final RectangleRenderer background;
    private final BackgroundlessButton chooseLeft, chooseRight;

    private final InputManager inputManager;

    private InputAction clickActionId;

    /** Selector at the top right of the window (default position). */
    protected ValueSelector(T[] values, WindowContext context){
        this(values, context, DefaultPosition(context), DefaultWidth(context));
    }

    /**
     * @param position top left corner of the selector, in virtual window pixels
     * @param width width of the selector in pixels (the height is 8% of the window height)
     */
    protected ValueSelector(T[] values, WindowContext context, Vector2f position, float width){
        this.values = values;
        this.inputManager = context.getInputManager();
        float height = context.getWindow().getInfo().getVirtualSizeCopy().y * 0.08f;
        float textY = position.y + height * 0.125f;

        typeSelectedName = new Text();
        typeSelectedName.setFont("bahnschrift")
                .setColor(ColorList.Blue())
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.Up)
                .setPosition(new Vector2f(position.x + width * 0.515f, textY))
                .setFontSize(20);

        chooseLeft = new BackgroundlessButton(context);
        chooseLeft.text().setFont("bahnschrift")
                .setAlignment(ETextAlignment.Center)
                .setReference(EDirection.Up)
                .setPosition(new Vector2f(position.x + width * 0.21f, textY))
                .setFontSize(30)
                .setText("<-");

        chooseLeft.copyTextToOverlapping().overlapsText().setColor(new Color4f(0.5f));

        chooseRight = chooseLeft.copy();
        chooseRight.text().setPosition(new Vector2f(position.x + width * 0.82f, textY))
                .setText("->");

        chooseRight.copyTextToOverlapping().overlapsText().setColor(new Color4f(0.8f));

        background = new RectangleRenderer("colorShape2D");
        background.setColorMode(ColorList.White());
        background.setPosition(new Vector2f(position));
        background.setSizePix(width, height);

        canLoop = true;

        updated = false;

        setSelected(0);
    }

    private static Vector2f DefaultPosition(WindowContext context) {
        return new Vector2f(context.getWindow().getInfo().getVirtualSizeCopy().x * 0.835f, 0f);
    }

    private static float DefaultWidth(WindowContext context) {
        return context.getWindow().getInfo().getVirtualSizeCopy().x * 0.165f;
    }

    public void init(InputAction clickActionId){
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

    public ValueSelector<T> setCanLoop(){
        canLoop = true;

        return setIndex(selected);
    }

    public ValueSelector<T> setIndex(int i){
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
        return nameOf(values[selected]);
    }

    protected abstract String nameOf(T value);

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