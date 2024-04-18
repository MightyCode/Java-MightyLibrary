package MightyLibrary.mightylib.physics.tweenings.type;

import MightyLibrary.mightylib.physics.tweenings.Tweening;
import org.joml.Vector2f;

public class Vector2fTweening extends Tweening<Vector2f> {
    private Vector2f beginningValue;
    private Vector2f endingValue;
    private Vector2f valuesRange;

    private Vector2f computedValue;

    public Vector2fTweening(){
        beginningValue = null;
        endingValue = null;
        valuesRange = null;

        computedValue = new Vector2f();
    }

    @Override
    public void update() {
        Vector2f beginningValue = this.beginningValue;
        Vector2f valuesRange = this.valuesRange;

        updateBase();

        if (inverseThisFrame){
            beginningValue = this.endingValue;
            valuesRange = valuesRange.mul(-1f);
        }


        if (aimedTime() == 0) {
            computedValue = endingValue;
        } else {
            computedValue.x = evaluate(beginningValue.x, valuesRange.x);

            computedValue.y = evaluate(beginningValue.y, valuesRange.y);
        }
    }

    @Override
    public Vector2fTweening initTwoValue(float time, Vector2f beginningValue, Vector2f endValue) {
        return initRangeValue(time, beginningValue, new Vector2f(endValue).sub(beginningValue));
    }

    @Override
    public Vector2fTweening initRangeValue(float time, Vector2f beginningValue, Vector2f range) {
        this.beginningValue = new Vector2f(beginningValue);
        this.valuesRange = new Vector2f(range);
        this.endingValue = new Vector2f(beginningValue).add(range);

        this.computedValue = new Vector2f(beginningValue);

        startTimerAfterInit(time);
        return this;
    }

    @Override
    public Vector2f value() {
        return new Vector2f(computedValue);
    }

    @Override
    public Vector2f goalValue() {
        return new Vector2f(endingValue);
    }


}
