package MightyLibrary.mightylib.main.procedures;

import MightyLibrary.mightylib.main.Context;
import MightyLibrary.mightylib.resources.Resources;

public interface IProjectLoading {
    void init(Resources resources);

    void contextLoading(Context context);
}
