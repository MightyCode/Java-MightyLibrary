package MightyLibrary.mightylib.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;

public class DataFolder {
    public static final String DATA_FOLDER_PATH = "data/";
    public static boolean checkDataFolderExists(){
        File file = new File(DATA_FOLDER_PATH);
        if (file.exists())
            return true;

        return file.mkdirs();
    }
    public static boolean saveFile (String content, String localPath){
        if (!checkDataFolderExists())
            return false;

        try {
            FileWriter myWriter = new FileWriter(DATA_FOLDER_PATH + localPath);
            myWriter.write(content);
            myWriter.close();

        } catch (Exception e) {
            e.printStackTrace();

            return false;
        }

        return true;
    }

    public static boolean fileExists(String localPath){
        if (!checkDataFolderExists())
            return false;

        return (new File(DATA_FOLDER_PATH + localPath)).exists();
    }

    public static String getFileContent(String localPath){
        if (!fileExists(localPath))
            return null;

        try {
            File myObj = new File(DATA_FOLDER_PATH + localPath);
            Scanner myReader = new Scanner(myObj);
            StringBuilder result = new StringBuilder();
            while (myReader.hasNextLine()) {
                result.append(myReader.nextLine());
            }
            myReader.close();

            return result.toString();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        return null;
    }

    public static File getFileInfo(String localPath){
        if (!fileExists(localPath))
            return null;

        return new File(localPath);
    }

    public static boolean deleteFile(String localPath){
        if (!fileExists(localPath))
            return false;

        File myObj = new File(DATA_FOLDER_PATH + localPath);
        return myObj.delete();
    }
}
