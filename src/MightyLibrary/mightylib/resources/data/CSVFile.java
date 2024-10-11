package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.SingleSourceDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class CSVFile extends SingleSourceDataType {
    private String[][] data;
    private boolean containsHeader;
    private String[] names;
    private final HashMap<String, Integer> nameToIndex;
    private int counter;
    public CSVFile(String dataName, String path) {
        super(TYPE_SET_UP.THREAD_CONTEXT, dataName, path);

        nameToIndex = new HashMap<>();
    }

    /**
     * Init the object in order to fill it properly later
     *
     * @param columnsName    Set with columns name
     *                       Even without header, set with array of empty string to define the column size
     * @param containsHeader Indicates if the file contains a header ("...", "...", ...)
     * @param size Number of row
     */
    public void init(String[] columnsName, boolean containsHeader, int size){
        this.containsHeader = containsHeader;
        data = new String[columnsName.length][];
        names = new String[columnsName.length];

        for (int i = 0; i < columnsName.length; ++i){
            nameToIndex.put(columnsName[i], i);
            names[i] = columnsName[i];
            data[i] = new String[size];
        }

        counter = 0;
    }

    public void addDataRow(String[] data){
        for (int i = 0; i < columnSize(); ++i)
            this.data[i][counter] = data[i];

        ++counter;
    }

    public int size(){
        if (data == null || data.length == 0)
            return -1;

        return data[0].length;
    }

    public String getData(int row, int column){
        return data[column][row];
    }

    public int columnSize(){
        if (data == null)
            return -1;

        return data.length;
    }

    public Set<String> columnsName() {
        Set<String> unmodifiableSet = new HashSet<>();

        for (String name : names)
            System.out.println(name);

        Collections.addAll(unmodifiableSet, names);
        unmodifiableSet.remove(null);

        return Collections.unmodifiableSet(unmodifiableSet);
    }

    public String getColumnName(int index){
        return names[index];
    }

    public int getColumnIndex(String columnName){
        if (!containsHeader)
            return -1;

        return nameToIndex.get(columnName);
    }

    @Override
    protected boolean internLoad() {
        return true;
    }

    public boolean isContainingHeader(){
        return containsHeader;
    }

    @Override
    public void internUnload() {
        data = null;
        names = null;

        nameToIndex.clear();
    }
}
