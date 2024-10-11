package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.MouseManager;
import MightyLibrary.mightylib.scenes.camera.Camera2D;
import MightyLibrary.mightylib.utils.math.MightyMath;
import org.joml.Vector2f;

@SuppressWarnings("SuspiciousNameCombination")
public class VerticalSlider extends Slider {
    public VerticalSlider(Camera2D referenceCamera, InputManager inputManager, MouseManager mouseManager,
                            Vector2f position, Vector2f size, float minValue, float maxValue){
        super(referenceCamera, inputManager, mouseManager, position, size, minValue, maxValue,
                1);
    }

    protected void setButtonPositionDrag(RectangleRenderer button,
                                         Vector2f buttonBoundaries, Vector2f mousePosition){
        Vector2f newPosition = new Vector2f(
                button.position().x,
                mousePosition.y - button.scale().y * 0.5f
        );

        if (newPosition.y < buttonBoundaries.x){
            newPosition.y = buttonBoundaries.x;
        } else if (newPosition.y > buttonBoundaries.y) {
            newPosition.y = buttonBoundaries.y;
        }

        button.setPosition(newPosition);
    }

    @Override
    protected void setBarSize(RectangleRenderer bar, Vector2f sliderSize){
        bar.setSizePix(sliderSize.x * 0.1f,sliderSize.y);
    }

    @Override
    protected void setBarPosition(RectangleRenderer bar, Vector2f sliderPosition, Vector2f sliderSize){
        bar.setPosition(
                new Vector2f(sliderPosition.x + sliderSize.x * 0.5f - bar.scale().x * 0.5f, sliderPosition.y)
        );
    }


    @Override
    protected void setButtonSize(RectangleRenderer button, Vector2f sliderSize){
        float ref = Math.min(sliderSize.x, sliderSize.y);

        button.setSizePix(ref *  0.32f, ref * 0.28f);
    }

    @Override
    protected void setBasicButtonPosition(RectangleRenderer button, Vector2f sliderPosition, Vector2f sliderSize){
        button.setPosition(
                new Vector2f(
                        sliderPosition.x + sliderSize.x * 0.5f - button.scale().x * 0.5f,
                        sliderPosition.y + sliderSize.y * 0.5f - button.scale().y * 0.5f));
    }

    @Override
    protected void setButtonPositionWithCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries){
        button.setPosition(
                new Vector2f(
                        button.position().x,
                        (float)MightyMath.Mapd(getCurrentValue(),
                                getMinValue(), getMaxValue(), buttonBoundaries.x, buttonBoundaries.y)
                )
        );
    }

    @Override
    protected double returnCurrentValue(RectangleRenderer button, Vector2f buttonBoundaries) {
        return MightyMath.Mapd(button.position().y, buttonBoundaries.x, buttonBoundaries.y, getMinValue(), getMaxValue());
    }
}
