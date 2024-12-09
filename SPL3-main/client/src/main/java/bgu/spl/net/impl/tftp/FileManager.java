package bgu.spl.net.impl.tftp;


import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;

public class FileManager {
    //private static HashMap<String, FileLock> fileLocks;
    private final static String directoryPath = "client";


    public static byte[] readFile(String filename) throws IOException {

        FileInputStream file = new FileInputStream(directoryPath + "\\" + filename);
        byte[] fileContent = new byte[(int) file.getChannel().size()];

        try {
            file.read(fileContent, 0, fileContent.length);
        } catch (IOException e) {
            file.close();
            return null;
        }
        file.close();
        return fileContent;
    }

    public static boolean writeFile(String filename, byte[] data) throws IOException {

        if (fileExists(filename))
            return false;

        FileOutputStream file = new FileOutputStream(directoryPath + "\\" + filename);
        try {
            file.write(data);
            System.out.println("Data write complete: " + filename);
        } catch (IOException e) {
            System.out.println("Error writing file: " + filename);
            return false;
        } finally {
            file.close();
        }
        return true;
    }

    public static boolean fileExists(String fileName) {
       return new File(directoryPath+ "\\" + fileName).exists();
    }
}
