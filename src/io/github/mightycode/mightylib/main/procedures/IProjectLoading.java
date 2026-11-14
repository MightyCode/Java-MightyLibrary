package io.github.mightycode.mightylib.main.procedures;

import io.github.mightycode.mightylib.main.WindowContext;

public interface IProjectLoading {
    // Binds the inputs of the game to the window (each window whose WindowConfig.hasProjectInputs)
    void contextLoading(WindowContext context);
}
