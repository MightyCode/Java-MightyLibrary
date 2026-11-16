package io.github.mightycode.mightylib.main.utils;

public interface IUpdatable {
    void update();
    // End of tick, after update() (e.g. reset the "pressed" input states); unload() frees the resources
    void postUpdate();
    void unload();
}
