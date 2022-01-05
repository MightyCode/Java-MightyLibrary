package MightyLibrary.mightylib.util.tweenings;

import MightyLibrary.mightylib.util.Timer;
import MightyLibrary.mightylib.util.tweenings.types.*;

public class Tweening {
    /**
     *
     * @param type Type of extrapolation
     * @param behaviour Behaviour of curves we want
     * @param t Current Time
     * @param b Beginning value
     * @param c Change in value - range
     * @param d Duration of extrapolation
     * @return the extrapolate value
     */
    public static float Evaluate(ETweeningType type, ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d) {
        return Tweening.Evaluate(type, behaviour, t, b, c, d, null, null);
    }

    /**
     *
     * @param type Type of extrapolation
     * @param behaviour Behaviour of curves we want
     * @param t Current Time
     * @param b Beginning value
     * @param c Change in value - range
     * @param d Duration of extrapolation
     * @param additionalValue1 like constant values
     * @param additionalValue2 like constant values
     * @return the extrapolate value
     */
    public static float Evaluate(ETweeningType type, ETweeningBehaviour behaviour,
                                 float t, float b, float c, float d,
                                 Float additionalValue1, Float additionalValue2){
        switch(type){
            case Linear:
                return Linear.Evaluate(behaviour, t, b, c, d);
            case Quadratic:
                return Quadratic.Evaluate(behaviour, t, b, c, d);
            case Cubic:
                return Cubic.Evaluate(behaviour, t, b, c, d);
            case Quartic:
                return Quartic.Evaluate(behaviour, t, b, c, d);
            case Quintic:
                return Quintic.Evaluate(behaviour, t, b, c, d);
            case Sinusoidal:
                return Sinusoidal.Evaluate(behaviour, t, b, c, d);
            case Exponential:
                return Exponential.Evaluate(behaviour, t, b, c, d);
            case Circular:
                return Cicular.Evaluate(behaviour, t, b, c, d);
            case Elastic:
                return Elastic.Evaluate(behaviour, t, b, c, d,
                        additionalValue1, additionalValue2);
            case Back:
                return Back.Evaluate(behaviour, t, b, c, d,
                        additionalValue1);
            case Bounce:
                return Bounce.Evaluate(behaviour, t, b, c, d);
            default:
                return 0.f;
        }
    }

    private ETweeningType type;
    private ETweeningBehaviour behaviour;
    private ETweeningOption option;

    private Timer timer;

    private float beginningValue = 0;
    private float endingValue = 0;
    private float valuesRange = 0;

    private float extrapolateValue = 0;

    private boolean reversed = false;
    private boolean mirrored = false;

    private Float additionalValue1;
    private Float additionalValue2;


    public Tweening(){
        type = ETweeningType.Linear;
        behaviour = ETweeningBehaviour.In;
        option = ETweeningOption.Direct;

        timer = new Timer();

        beginningValue = 0.f;
        endingValue = 0.f;
        valuesRange = 0.f;


        additionalValue1 = null;
        additionalValue2 = null;

        extrapolateValue = 0.f;
    }


    public void update(){
        this.timer.update();

        if (this.timer.isFinished()){
            if (this.option == ETweeningOption.DirectMirrored) {
                if (!this.mirrored){
                    this.mirrored = true;
                    this.timer.resetStart();
                }
            } else if (this.option == ETweeningOption.DirectReversed) {
                if (!this.reversed){
                    this.reversed = true;
                    this.timer.resetStart();
                }
            } else if (this.option == ETweeningOption.Loop){
                float plus = this.timer.getElapsedTime() - this.timer.getAimedTime();
                this.timer.resetStart();
                this.timer.forceTime(plus);
            } else if (this.option == ETweeningOption.LoopReversed){
                float plus = this.timer.getElapsedTime() - this.timer.getAimedTime();
                this.reversed = !this.reversed;
                this.timer.resetStart();
                this.timer.forceTime(plus);
            } else if(this.option == ETweeningOption.LoopMirrored) {
                float plus = this.timer.getElapsedTime() - this.timer.getAimedTime();
                this.mirrored = !this.mirrored;
                this.timer.resetStart();
                this.timer.forceTime(plus);
            }
        }

        float tmpTimeValue = this.timer.getElapsedTime();
        float beginValue = this.beginningValue;
        float rangeValue = this.valuesRange;

        if (this.reversed){
            tmpTimeValue = this.timer.getAimedTime() - tmpTimeValue;
        }

        if (this.mirrored){
            beginValue = this.endingValue;
            rangeValue = -this.valuesRange;
        }



        extrapolateValue = Tweening.Evaluate(this.type, this.behaviour, tmpTimeValue, beginValue, rangeValue, this.timer.getAimedTime(),
                additionalValue1, additionalValue2);
    }


    public Tweening setTweeningOption(ETweeningOption tweeningOption){
        this.option = tweeningOption;

        this.timer.setElapsedTimeCapped((
                tweeningOption == ETweeningOption.Loop
                        || tweeningOption == ETweeningOption.LoopMirrored
                        || tweeningOption == ETweeningOption.LoopReversed));

        return this;
    }


    public Tweening setTweeningValues(ETweeningType type, ETweeningBehaviour behaviour){
        this.type = type;
        this.behaviour = behaviour;

        return this;
    }


    public Tweening initTwoValue(float time, float beginningValue, float endValue){
        return this.initRangeValue(time, beginningValue, endValue - beginningValue);
    }


    public Tweening initRangeValue(float time, float beginningValue, float range){
        this.timer.start(time);
        this.beginningValue = beginningValue;
        this.valuesRange = range;

        this.endingValue = range + beginningValue;

        this.extrapolateValue = this.beginningValue;

        return this;
    }


    public Tweening setAdditionnalArguments(Float arg1, Float arg2){
        this.additionalValue1 = arg1;
        this.additionalValue2 = arg2;

        return this;
    }


    public Tweening initRandomTweening(){
        this.timer.forceRandomTime();

        if (this.option == ETweeningOption.DirectMirrored
                || this.option == ETweeningOption.LoopMirrored){
            if (Math.random() >= 0.5){
                this.mirrored = true;
            }
        } else if (this.option == ETweeningOption.DirectReversed
                || this.option == ETweeningOption.LoopReversed){
            if (Math.random() >= 0.5){
                this.reversed = true;
            }
        }

        return this;
    }


    public float value (){
        return this.extrapolateValue;
    }

    // Not is in infinityLoop
    public boolean finished(){
        if (this.option == ETweeningOption.Direct){
            return this.timer.isFinished();
        } else if (this.option == ETweeningOption.DirectReversed){
            return this.timer.isFinished() && this.reversed;
        }  else if (this.option == ETweeningOption.DirectMirrored){
            return this.timer.isFinished() && this.mirrored;
        }
        return false;
    }


    public float addArg1(){
        return this.additionalValue1;
    }

    public float addArg2(){
        return this.additionalValue2;
    }
}
