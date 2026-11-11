package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.main.WindowContext;
import org.joml.Vector2f;

public class StrSelector extends ValueSelector<String> {
    /** Selector at the top right of the window (default position). */
    public StrSelector(String[] values, WindowContext context){
        super(values, context);
    }

    /**
     * @param position top left corner of the selector, in virtual window pixels
     * @param width width of the selector in pixels (the height is 8% of the window height)
     */
    public StrSelector(String[] values, WindowContext context, Vector2f position, float width){
        super(values, context, position, width);
    }

    public boolean setIfExists(String value){
        String[] values = getValues();
        for (int i = 0; i < values.length; ++i){
            if (values[i].equals(value)) {
                setIndex(i);
                return true;
            }
        }

        return false;
    }

    @Override
    protected String nameOf(String value){
        return value;
    }
}
