package MightyLibrary.mightylib.util;

import java.io.*;
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

    public BufferedReader getFileReader(String localPath){
        if (!fileExists(localPath))
            return null;

        try {
            return new BufferedReader(new FileReader(DATA_FOLDER_PATH + localPath));
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

    public static boolean emptyDataFolder(){
        File folder = new File(DATA_FOLDER_PATH);
        File[] files = folder.listFiles();
        if (files != null) {
            // Iterate over the files and delete them
            for (File file : files) {
                if (file.isFile()) {
                    if (file.delete()) {
                        System.out.println("Deleted file: " + file.getName());
                    } else {
                        System.out.println("Failed to delete file: " + file.getName());
                    }
                }
            }

            return true;
        }

        return false;
    }


    public static boolean deleteFile(String localPath){
        if (!fileExists(localPath))
            return false;

        File myObj = new File(DATA_FOLDER_PATH + localPath);
        return myObj.delete();
    }
}
