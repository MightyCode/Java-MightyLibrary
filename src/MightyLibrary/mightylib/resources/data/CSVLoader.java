package MightyLibrary.mightylib.resources.data;

import MightyLibrary.mightylib.resources.DataType;
import MightyLibrary.mightylib.resources.ResourceLoader;
import MightyLibrary.mightylib.resources.Resources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CSVLoader extends ResourceLoader {
    @Override
    public Class<? extends DataType> getType() {
        return CSVFile.class;
    }

    @Override
    public String getResourceNameType() {
        return "CSVFile";
    }

    @Override
    public void create(Map<String, DataType> data){
        exploreResourcesFile(data, Resources.FOLDER);
    }

    public String filterFile(String path) {
        String ending = getFileExtension(path);

        if (ending != null && ending.equals(".csv")) return getFileName(path);

        return null;
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {
        data.put(name, new CSVFile(name, currentPath));
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof CSVFile))
            return;

        CSVFile csv = (CSVFile) dataType;

        String line;
        String csvSplitBy = ",";
        String csvHandleData = "\"";

        String[] columnsName = null;
        boolean header = true, lineHeader = true;
        List<String[]> dataList = new ArrayList<>();
        String[] data;

        try (BufferedReader br = new BufferedReader(new FileReader(csv.path()))) {
            while ((line = br.readLine()) != null) {
                data = line.split(csvSplitBy);

                if (lineHeader && data.length > 0 && data[0].charAt(0) == '\"'){
                    columnsName = new String[data.length];

                    for (int i = 0; i < data.length; ++i) {
                        columnsName[i] = data[i].substring(1, data[i].length() - 1);
                    }
                } else {
                    if (lineHeader) {
                        columnsName = new String[data.length];
                        header = false;
                    } else if (!line.equals("\n") && !line.equals("")) {
                        dataList.add(parseCsvLine(line, csvSplitBy));
                    }
                }

                lineHeader = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (columnsName == null)
            return;

        csv.init(columnsName, header, dataList.size());
        for (String[] a : dataList){
            csv.addDataRow(a);
        }
    }

    private String[] parseCsvLine(String line, String delimiter) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder fieldBuilder = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(fieldBuilder.toString());
                fieldBuilder.setLength(0);
            } else {
                fieldBuilder.append(c);
            }
        }

        fields.add(fieldBuilder.toString());
        return fields.toArray(new String[0]);
    }
}
