package io.github.mightycode.mightylib.utils.math;

import java.util.concurrent.atomic.AtomicInteger;

public class UUID {
    // Objects are also created by the workers (data types): the counter is atomic
    private static final AtomicInteger COUNT = new AtomicInteger();
    private final int id;

    public UUID(){
        id = COUNT.getAndIncrement();
    }

    public int id(){
        return id;
    }
}
