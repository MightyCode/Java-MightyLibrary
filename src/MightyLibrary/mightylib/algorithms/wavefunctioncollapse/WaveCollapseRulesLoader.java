package MightyLibrary.mightylib.algorithms.wavefunctioncollapse;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;
import MightyLibrary.mightylib.resources.data.JSONFile;

import java.util.Map;

public class WaveCollapseRulesLoader extends ResourceLoader {
    public WaveCollapseRulesLoader(){
    }

    @Override
    public Class<? extends DataType> getType() {
        return WaveCollapseRules.class;
    }

    @Override
    public String getResourceNameType() {
        return "WaveCollapseRules";
    }

    @Override
    public void create(Map<String, DataType> data) {
        exploreResourcesFile(data, Resources.FOLDER);
    }

    @Override
    public void fileDetected(final Map<String, DataType> data, final String currentPath, final String name) {
        String reducedName = name.replace(".wavecollapse", "");
        data.put(reducedName, new WaveCollapseRules(reducedName));
    }

    @Override
    public String filterFile(String path) {
        String ending = getFileExtension(path);
        String fileName = getFileName(path);

        if (ending != null && fileName.toLowerCase().contains(".wavecollapse"))
            return getFileName(path);

        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof WaveCollapseRules))
            return;

        WaveCollapseRules rules = (WaveCollapseRules) dataType;
        rules.setData(Resources.getInstance().getResource(JSONFile.class, rules.getDataName() + ".wavecollapse"));
    }
}
