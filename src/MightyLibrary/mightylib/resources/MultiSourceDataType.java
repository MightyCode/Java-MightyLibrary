package MightyLibrary.mightylib.resources;

import java.util.Arrays;

public abstract class MultiSourceDataType extends DataType {
    private final String[] sourcesPath;
    protected final Boolean[] sourceLoaded;

    public MultiSourceDataType(TYPE_SET_UP typeSetUp, String dataName, String ... paths) {
        super(typeSetUp, dataName);

        this.sourcesPath = paths;
        sourceLoaded = new Boolean[paths.length];
    }

    public String getSourcePath(int index){
        return sourcesPath[index];
    }
    public int numberSource(){
        return sourcesPath.length;
    }

    protected void setAllSourceUnloaded(){
        Arrays.fill(sourceLoaded, false);
    }

    public void isPartLoaded(int index){
        sourceLoaded[index] = true;
    }

    public boolean validPaths() {
        for (String path : sourcesPath){
            if (path == null || path.isEmpty()){
                return false;
            }
        }

        return true;
    }
}
