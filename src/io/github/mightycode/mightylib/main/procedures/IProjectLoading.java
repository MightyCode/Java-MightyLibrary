package io.github.mightycode.mightylib.main.procedures;

import io.github.mightycode.mightylib.main.WindowContext;
import io.github.mightycode.mightylib.resources.Resources;

public interface IProjectLoading {
    void init(Resources resources);

    void contextLoading(WindowContext context);
}
