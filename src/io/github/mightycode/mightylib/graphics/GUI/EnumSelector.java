package io.github.mightycode.mightylib.graphics.GUI;

import io.github.mightycode.mightylib.main.WindowContext;
import org.joml.Vector2f;

public class EnumSelector<T extends Enum<?>> extends ValueSelector<T> {
    /** Selector at the top right of the window (default position). */
    public EnumSelector(T[] values, WindowContext context){
        super(values, context);
    }

    /**
     * @param position top left corner of the selector, in virtual window pixels
     * @param width width of the selector in pixels (the height is 8% of the window height)
     */
    public EnumSelector(T[] values, WindowContext context, Vector2f position, float width){
        super(values, context, position, width);
    }

    @Override
    protected String nameOf(T value){
        return value.name();
    }
}
