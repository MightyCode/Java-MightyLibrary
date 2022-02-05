package MightyLibrary.mightylib.resources;

import java.io.*;
import java.nio.charset.StandardCharsets;


public abstract class FileMethods {
    public static boolean copy(File source, File dest) {
        try (InputStream sourceFile = new FileInputStream(source);
             OutputStream destinationFile = new FileOutputStream(dest)) {
            // Lecture par segment de 0.5Mo
            byte[] buffer = new byte[512 * 1024];
            int nbLecture;
            while ((nbLecture = sourceFile.read(buffer)) != -1){
                destinationFile.write(buffer, 0, nbLecture);
            }
        } catch (IOException e){
            e.printStackTrace();
            return false; // Error
        }

        return true; // Result OK
    }


    public static boolean copy(String source, String dest) {
        return copy(new File(source), new File(dest));
    }


    public static String readFileAsString(String filePath){
        StringBuilder source = new StringBuilder();
        try {

            BufferedReader reader;
            FileInputStream in = new FileInputStream(filePath);
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }

            reader.close();

            in.close();

        } catch (FileNotFoundException e) {
            System.err.println("Can't find the file" + filePath);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("reader can't read a part of the file");
            e.printStackTrace();
        }

        return source.toString();
    }
}
