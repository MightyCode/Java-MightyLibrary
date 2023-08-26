package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.scene.Camera2D;
import MightyLibrary.mightylib.util.math.ColorList;
import org.joml.Vector2f;

/**
 * Abstract slider which can be used to create all customs slider.
 */
@SuppressWarnings("SuspiciousNameCombination")
public abstract class Slider {
    private static final int MAX_DIGIT_PRECISION = 9;

    private final Vector2f referencePosition, referenceSize;
    private final RectangleRenderer bar;
    private final RectangleRenderer button;
    private final Vector2f buttonBoundaries;
    private double minValue, maxValue;
    private double currentValue;
    private final InputManager inputManager;
    private final MouseManager mouseManager;
    private final Camera2D referenceCamera;
    private boolean isDragging;

    // Maximum number of digits after the decimal point
    private int maxPrecision;


    private int clickActionId;

    public Slider(Camera2D referenceCamera, InputManager inputManager, MouseManager mouseManager,
                            Vector2f position, Vector2f size, double minValue, double maxValue,
                  int dimensionComponent){
        this.referenceCamera = referenceCamera;
        this.inputManager = inputManager;
        this.mouseManager = mouseManager;
        this.minValue = minValue;
        this.maxValue = maxValue;

        referencePosition = new Vector2f(position);
        referenceSize = new Vector2f(size);

        maxPrecision = -1;

        button = new RectangleRenderer("colorShape2D");
        button.switchToColorMode(ColorList.Grey());
        setButtonSize(button, referenceSize);

        setBasicButtonPosition(button, referencePosition, referenceSize);

        button.setReferenceCamera(referenceCamera);

        bar = new RectangleRenderer("colorShape2D");
        bar.switchToColorMode(ColorList.DarkGrey());
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

    public void init(int clickActionId){
        this.clickActionId = clickActionId;
    }

    public void update(){
        if (inputManager.inputPressed(clickActionId)){
            Vector2f position = referenceCamera.getPosition(mouseManager.pos());

            if (button.position().x < position.x && button.position().x + button.scale().x > position.x &&
                    button.position().y < position.y && button.position().y + button.scale().y > position.y){
                isDragging = true;
            }
        } else if (inputManager.inputReleased(clickActionId))
            isDragging = false;

        if (isDragging){
            setButtonPositionDrag(button, buttonBoundaries, mouseManager.pos());
            currentValue = returnCurrentValue(button, buttonBoundaries);

            if (maxPrecision >= 0 && maxPrecision <= MAX_DIGIT_PRECISION)
                currentValue = (float)((int)(currentValue * Math.pow(10, maxPrecision)) * 1.0f / Math.pow(10, maxPrecision));
        }
    }


    public void display(){
        bar.display();
        button.display();
    }

    protected abstract void setBarSize(RectangleRenderer bar, Vector2f sliderSize);

    protected abstract void setBarPosition(RectangleRenderer bar, Vector2f sliderPosition, Vector2f sliderSize);

    protected abstract void setButtonSize(RectangleRenderer button, Vector2f sliderSize);

    protected abstract void setBasicButtonPosition(RectangleRenderer button, Vector2f sliderPosition, Vector2f sliderSize);

    protected abstract void setButtonPositionWithCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries);

    public void setValue(double value){
        currentValue = Math.min(Math.max(minValue, value), maxValue);

        setButtonPositionWithCurrentValue(button, buttonBoundaries);
    }

    protected abstract double returnCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries);

    protected abstract void setButtonPositionDrag(RectangleRenderer button,
                                                  Vector2f buttonBoundaries, Vector2f mousePosition);

    public void invertMinMax(){
        double temp = minValue;
        minValue = maxValue;
        maxValue = temp;

        setButtonPositionWithCurrentValue(button, buttonBoundaries);
    }

    public void setMaxPrecision(int value){
        if (maxPrecision >= 0 && maxPrecision < MAX_DIGIT_PRECISION)
            maxPrecision = value;
    }

    public int getMaxPrecision(){
        return maxPrecision;
    }

    public double getCurrentValue(){
        return currentValue;
    }

    public double getMaxValue(){
        return maxValue;
    }

    public double getMinValue(){
        return minValue;
    }

    public boolean isGettingDragged() {
        return isDragging;
    }

    public void unload(){
        bar.unload();
        button.unload();
    }
}
