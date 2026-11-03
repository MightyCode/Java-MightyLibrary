package MightyLibrary.mightylib.physics.tweenings.type;

import MightyLibrary.mightylib.physics.tweenings.Tweening;

public class FloatTweening extends Tweening<Float> {
    private float beginningValue;
    private float endingValue;
    private float valuesRange;

    private Float computedValue;

    public FloatTweening(){
        beginningValue = 0.0f;
        endingValue = 0.0f;
        valuesRange = 0.0f;

        computedValue = 0.0f;
    }

    @Override
    public void update() {
        float beginningValue = this.beginningValue;
        float valuesRange = this.valuesRange;

       updateBase();

       if (inverseThisFrame){
           beginningValue = this.endingValue;
           valuesRange = -valuesRange;
       }

        if (aimedTime() == 0)
            computedValue = endingValue;
        else
            computedValue = evaluate(beginningValue, valuesRange);
    }

    @Override
    public Tweening<Float> initTwoValue(float time, Float beginningValue, Float endValue) {
        return initRangeValue(time, beginningValue, endValue - beginningValue);
    }

    @Override
    public Tweening<Float> initRangeValue(float time, Float beginningValue, Float range) {
        this.beginningValue = beginningValue;
        this.valuesRange = range;
        this.endingValue = beginningValue + range;

        this.computedValue = beginningValue;

        startTimerAfterInit(time);
        return this;
    }

    @Override
    public Float value() {
        return computedValue;
    }

    @Override
    public Float goalValue() {
        return null;
    }
}
