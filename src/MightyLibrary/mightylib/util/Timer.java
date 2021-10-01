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



    public Timer() {
        aimedTime = 0;
        elapsedTime = 0;

        stopped = false;
        finished = false;
    }


    public void update(){
        if (!stopped && !finished){
            elapsedTime += GameTime.DeltaTime();

            if (elapsedTime >= aimedTime){
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
        elapsedTime = 0;
    }


    public void resetStop() {
        resetStart();
        stop();
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


    public float getElapsedTime(){
        return elapsedTime;
    }
}