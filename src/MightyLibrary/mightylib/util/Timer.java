package MightyLibrary.mightylib.util;

import MightyLibrary.mightylib.main.GameTime;

public class Timer {
    /**
     * Time of activation.
     */
    private float aimedTime;
    private float elapsedTime;

    private boolean stopped;
    private boolean finished;
    private boolean started;

    private boolean elapsedTimeCapped;


    public Timer() {
        aimedTime = 0;
        elapsedTime = 0;

        stopped = false;
        finished = false;
        started = false;

        elapsedTimeCapped = true;
    }


    public void update(){
        if (!stopped && !finished && started){
            elapsedTime += GameTime.DeltaTime();

            if (elapsedTime >= aimedTime){
                if (elapsedTimeCapped)
                    this.elapsedTime = this.aimedTime;

                finished = true;
            }
        }
    }


    public void start(float aimedTime){
        this.aimedTime = aimedTime;
        resetStart();
    }


    public void resetStart() {
        stopped = false;
        finished = false;
        started = true;
        elapsedTime = 0;
    }


    public void resetStop() {
        resetStart();
        stop();
        started = false;
    }


    public void stop() {
        stopped = true;
    }


    public void restart() {
        stopped = false;
    }


    public boolean isStopped(){
        return stopped;
    }


    public boolean isFinished(){
        return finished;
    }

    public boolean isStarted() { return started; }


    public float getElapsedTime(){
        return elapsedTime;
    }


    public float getAimedTime(){ return aimedTime; }


    public void forceTime(float newElaspedTime){
        if (this.elapsedTimeCapped && elapsedTime > aimedTime){
            elapsedTime = aimedTime;
        } else {
            elapsedTime = newElaspedTime;
        }
    }

    public void forceRandomTime(){
        elapsedTime = (float)Math.random() * this.aimedTime;
    }

    public void setElapsedTimeCapped(boolean value){
        elapsedTimeCapped = value;
    }
}