package io.github.mightycode.mightylib.graphics.gui;

import io.github.mightycode.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import io.github.mightycode.mightylib.inputs.InputAction;
import io.github.mightycode.mightylib.inputs.IInputManager;
import io.github.mightycode.mightylib.inputs.MouseManager;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.scenes.camera.Camera2D;
import io.github.mightycode.mightylib.utils.math.color.ColorList;
import org.joml.Vector2f;

/**
 * A bar and a button dragged along it, between a minimum and a maximum value. A subclass gives the direction
 * (HorizontalSlider, VerticalSlider): its hooks place and size the bar and the button and convert the position of
 * the button to a value. The size and position hooks are called by the constructor: the fields of the subclass are
 * not set yet.
 */
public abstract class Slider {
    private static final int MAX_DIGIT_PRECISION = 9;

    private final RectangleRenderer bar;
    private final RectangleRenderer button;
    private final Vector2f buttonBoundaries;
    private double minValue, maxValue;
    private double currentValue;
    private final IInputManager inputManager;
    private final MouseManager mouseManager;
    private final Camera2D referenceCamera;
    private boolean isDragging;

    // Maximum number of digits after the decimal point
    private int maxPrecision;


    private InputAction clickActionId;

    public Slider(Camera2D referenceCamera, IInputManager inputManager, MouseManager mouseManager,
                            Vector2f position, Vector2f size, double minValue, double maxValue,
                  int dimensionComponent){
        this.referenceCamera = referenceCamera;
        this.inputManager = inputManager;
        this.mouseManager = mouseManager;
        this.minValue = minValue;
        this.maxValue = maxValue;

        Vector2f referencePosition = new Vector2f(position);
        Vector2f referenceSize = new Vector2f(size);

        maxPrecision = -1;

        button = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
        button.setColorMode(ColorList.Grey());
        setButtonSize(button, referenceSize);

        setBasicButtonPosition(button, referencePosition, referenceSize);

        button.setReferenceCamera(referenceCamera);

        bar = new RectangleRenderer(EngineResources.COLOR_SHAPE_2D);
        bar.setColorMode(ColorList.DarkGrey());
        bar.setReferenceCamera(referenceCamera);
        setBarSize(bar, size);
        setBarPosition(bar, position, size);

        if (dimensionComponent == 0)
            buttonBoundaries = new Vector2f(
                    referencePosition.x - button.scale().x * 0.5f,
                    referencePosition.x + referenceSize.x - button.scale().x * 0.5f
            );
        else {
            buttonBoundaries = new Vector2f(
                    referencePosition.y - button.scale().y * 0.5f,
                    referencePosition.y + referenceSize.y - button.scale().y * 0.5f
            );
        }

        setValue(minValue);
    }

    public final void init(InputAction clickActionId){
        this.clickActionId = clickActionId;
    }

    public final void update(){
        boolean pressed = inputManager.inputPressed(clickActionId);
        if (!pressed && !isDragging)
            return;

        // The mouse in the world of the reference camera, where the bar and the button are (read only when used: an
        // inverted matrix and a few vectors)
        Vector2f position = referenceCamera.transformPosition(mouseManager.pos());
        if (pressed){
            if (button.position().x < position.x && button.position().x + button.scale().x > position.x &&
                    button.position().y < position.y && button.position().y + button.scale().y > position.y){
                isDragging = true;
            }
        } else if (inputManager.inputReleased(clickActionId))
            isDragging = false;

        if (isDragging){
            setButtonPositionDrag(button, buttonBoundaries, position);
            currentValue = RoundToPrecision(returnCurrentValue(button, buttonBoundaries), maxPrecision);
        }
    }

    // Rounds to the nearest value with this number of digits after the decimal point, a tie upwards
    // (negative: no rounding)
    static double RoundToPrecision(double value, int precision) {
        if (precision < 0)
            return value;

        double scale = Math.pow(10, precision);
        return Math.floor(value * scale + 0.5) / scale;
    }

    // A negative value disables the rounding, a too large one is capped
    static int CapPrecision(int precision) {
        return precision < 0 ? -1 : Math.min(precision, MAX_DIGIT_PRECISION);
    }


    public final void display(){
        bar.display();
        button.display();
    }

    // Hooks called by the constructor: size and place the bar and the button from the slider's
    protected abstract void setBarSize(RectangleRenderer bar, Vector2f sliderSize);

    protected abstract void setBarPosition(RectangleRenderer bar, Vector2f sliderPosition, Vector2f sliderSize);

    protected abstract void setButtonSize(RectangleRenderer button, Vector2f sliderSize);

    protected abstract void setBasicButtonPosition(RectangleRenderer button, Vector2f sliderPosition, Vector2f sliderSize);

    // Hook: places the button for getCurrentValue() (setValue, invertMinMax)
    protected abstract void setButtonPositionWithCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries);

    public final void setValue(double value){
        currentValue = Math.min(Math.max(minValue, value), maxValue);

        setButtonPositionWithCurrentValue(button, buttonBoundaries);
    }

    // Hook: the value at the position of the button, while it is dragged
    protected abstract double returnCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries);

    // Hook: moves the button to the mouse (in the world of the reference camera), within the boundaries, while it is
    // dragged
    protected abstract void setButtonPositionDrag(RectangleRenderer button,
                                                  Vector2f buttonBoundaries, Vector2f mousePosition);

    public final void invertMinMax(){
        double temp = minValue;
        minValue = maxValue;
        maxValue = temp;

        setButtonPositionWithCurrentValue(button, buttonBoundaries);
    }

    public final void setMaxPrecision(int value){
        maxPrecision = CapPrecision(value);
    }

    public final int getMaxPrecision(){
        return maxPrecision;
    }

    public final double getCurrentValue(){
        return currentValue;
    }

    public final double getMaxValue(){
        return maxValue;
    }

    public final double getMinValue(){
        return minValue;
    }

    public final boolean isGettingDragged() {
        return isDragging;
    }

    public final void unload(){
        bar.unload();
        button.unload();
    }
}
