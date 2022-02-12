package MightyLibrary.mightylib.sounds;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.EDataType;

public class Sound extends DataType {

    public Sound(String dataName, String path) {
        super(EDataType.Sound, dataName, path);
    }

    @Override
    public boolean load() {
        return false;
    }

    @Override
    public boolean unload() {
        return false;
    }
}
