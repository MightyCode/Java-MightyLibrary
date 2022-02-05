package MightyLibrary.mightylib.util.tweenings;

import MightyLibrary.mightylib.util.Timer;
import MightyLibrary.mightylib.util.tweenings.tweeningmath.*;

public abstract class Tweening<T> {
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

    private final Timer timer;

    private boolean reversed = false;
    private boolean mirrored = false;

    private Float additionalValue1;
    private Float additionalValue2;

    protected float timeValueForFrame;
    protected boolean inverseThisFrame;

    public Tweening(){
        type = ETweeningType.Linear;
        behaviour = ETweeningBehaviour.In;
        option = ETweeningOption.Direct;

        timer = new Timer();

        additionalValue1 = null;
        additionalValue2 = null;
    }


    protected void updateBase(){
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

        timeValueForFrame = this.timer.getElapsedTime();

        if (this.reversed){
            timeValueForFrame = this.timer.getAimedTime() - timeValueForFrame;
        }

        inverseThisFrame = this.mirrored;
    }

    protected float aimedTime(){
        return timer.getAimedTime();
    }

    public abstract void update();
    public abstract Tweening<T> initTwoValue(float time, T beginningValue, T endValue);

    public abstract Tweening<T> initRangeValue(float time, T beginningValue, T range);
    public abstract T value();

    protected void startTimerAfterInit(float time){
        this.timer.start(time);
    }

    protected float evaluate(float beginningValue, float valuesRange){
        return Tweening.Evaluate(this.type, this.behaviour, timeValueForFrame, beginningValue, valuesRange, this.timer.getAimedTime(),
                additionalValue1, additionalValue2);
    }


    public Tweening<T> setTweeningOption(ETweeningOption tweeningOption){
        this.option = tweeningOption;

        this.timer.setElapsedTimeCapped((
                tweeningOption == ETweeningOption.Loop
                        || tweeningOption == ETweeningOption.LoopMirrored
                        || tweeningOption == ETweeningOption.LoopReversed));

        return this;
    }


    public Tweening<T> setTweeningValues(ETweeningType type, ETweeningBehaviour behaviour){
        this.type = type;
        this.behaviour = behaviour;

        return this;
    }

    public Tweening<T> setAdditionnalArguments(Float arg1, Float arg2){
        this.additionalValue1 = arg1;
        this.additionalValue2 = arg2;

        return this;
    }


    public Tweening<T> initRandomTweening(){
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
