package bgu.spl.net.impl.tftp;


import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;

public class FileManager {
    private static HashMap<String, FileLock> fileLocks;
    private final static String directoryPath = "server\\Files";

    public static boolean init() {
        fileLocks = new HashMap<>();

        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory path: " + directoryPath);
            return false;
        }

        File[] files = directory.listFiles();

        if (files == null) {
            System.err.println("Failed to list files in directory: " + directoryPath);
            return false;
        }

        for (File file : files) {
            if (file.isFile()) {
                try {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");
                    FileChannel channel = raf.getChannel();
                    FileLock lock = channel.lock();
                    lock.release();
                    fileLocks.put(file.getName(), lock);
                    raf.close();
                } catch (IOException e) {
                    System.err.println("Failed to acquire lock for file: " + file.getName());
                    return false;
                }
            }
        }
        return true;
    }

    public static byte[] readFile(String filename) throws IOException {
        //FileLock lock = lockFile(filename);
        FileInputStream file = new FileInputStream(directoryPath + "\\" + filename);
        byte[] fileContent = new byte[(int) file.getChannel().size()];

        try {
            file.read(fileContent, 0, fileContent.length);
        } catch (IOException e) {
            file.close();
            //unlockFile(filename, lock);
            return null;
        }
        file.close();
        //unlockFile(filename, lock);
        //System.out.println("RRQ" +filename +"complete");
        return fileContent;
    }

    public static boolean writeFile(String filename, byte[] data) throws IOException {
        //FileLock lock = lockFile(filename);
        if (fileExists(filename))
            return false;

        FileOutputStream file = new FileOutputStream(directoryPath + "\\" + filename);
        try {
            file.write(data);
            System.out.println("Data write complete: " + filename);
            fileLocks.put(filename, null);
        } catch (IOException e) {
            System.out.println("Error writing file: " + filename);
            return false;
        } finally {
            file.close();
            //unlockFile(filename, lock);
        }

        //System.out.println("WRQ" +filename +"complete");
        return true;
    }

    public static boolean deleteFile(String filename) throws IOException {

        boolean deleted;
        // Acquire a lock for the file
        //FileLock lock = lockFile(filename);
        try {
            File file = new File(directoryPath + "\\" + filename);
            deleted = file.delete();

        } catch (Exception e) {
            return false;
        } finally {
            // Release the lock
            //unlockFile(filename, lock);
            fileLocks.remove(filename);
        }
        return deleted;
    }

    private static synchronized FileLock lockFile(String filename) throws IOException {
        // Open the file
        RandomAccessFile file = new RandomAccessFile(filename, "rw");
        FileChannel channel = file.getChannel();

        // Lock the file
        FileLock lock = channel.lock();

        // Store the lock in the map
        fileLocks.put(filename, lock);

        // Return the lock
        return lock;
    }

    private static synchronized void unlockFile(String filename, FileLock lock) throws IOException {
        // Release the lock
        lock.release();

        // Remove the lock from the map
        fileLocks.remove(filename);
    }


    public static String[] getAllFileNames() {
        String[] allFiles = new String[fileLocks.size()];
        int i = 0;
        for (String fileName : fileLocks.keySet()) {
            allFiles[i] = fileName;
            i++;
        }
        return allFiles;
    }

    public static boolean fileExists(String file) {
        return fileLocks.containsKey(file);
    }
}
