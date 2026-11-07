package io.github.mightycode.mightylib.utils;

public abstract class ThreadOrCurrent extends Thread {
    private boolean separateThread = false;
    public boolean IsSeparateThread(){ return separateThread; }

    public abstract void content();

    public final void run() {
        content();
        separateThread = true;
    }
}
