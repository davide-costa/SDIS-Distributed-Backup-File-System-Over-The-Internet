package Utils;

import LoggingAndSettings.Logging;
import Protocols.ServerlessDistributedBackupService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class FileUtils
{
    //Returns 0 on success and if all files have been assembled successfully; 1 if file doesn't exist; 2 on reading exception; 3 if couldn't close the the output file (not big problem, just a warning)
    public static int reassembleChunksToFile(List<String> chunksPaths, String filePath)
    {
        if (chunksPaths.size() == 0)
            return 1;

        //open stream to file to store backed up file on
        FileOutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream(filePath);
        } catch (IOException ex)
        {
            return 2;
        }

        for (String chunkPath : chunksPaths)
        {
            byte[] data = new byte[ServerlessDistributedBackupService.CHUNK_MAX_SIZE];
            try
            {
                File f = new File(chunkPath);
                if (!f.exists())
                {
                    outputStream.close();
                    return 1;
                }
                //Read data from the current chunk
                FileInputStream inputStream = new FileInputStream(chunkPath);
                int readSize = inputStream.read(data);
                if (readSize < ServerlessDistributedBackupService.CHUNK_MAX_SIZE)
                {
                    byte[] smallerData = Arrays.copyOfRange(data, 0, readSize);
                    data = smallerData;
                }
                inputStream.close();

                //write the current chunk data to the file that will contain data from all chunks
                outputStream.write(data);
            } catch (IOException ex)
            {
                try
                {
                    outputStream.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                deleteFileFromDisk(filePath);
                return 2;
            }
        }

        try
        {
            outputStream.close();
        }
        catch (IOException ex)
        {
            return 3;
        }

        return 0;
    }

    public static byte[] readFileFromDisk(String filepath)
    {
        File file = new File(filepath);
        if (!file.exists())
            return null;
        byte[] chunkData = new byte[(int) file.length()];

        try
        {
            FileInputStream fileIn = new FileInputStream(filepath);
            fileIn.read(chunkData);
            fileIn.close();
        }
        catch (IOException e)
        {
            Logging.LogError("Error reading file '" + filepath + "' from disk");
            return null;
        }

        return chunkData;
    }

    public static Object readObjectFromDisk(String filepath)
    {
        try
        {
            FileInputStream fileIn = new FileInputStream(filepath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Object object = in.readObject();
            in.close();
            fileIn.close();
            return object;
        }
        catch (ClassNotFoundException | IOException e)
        {
            return null;
        }
    }

    public static boolean saveFileIntoDisk(String filepath, byte[] data)
    {
        try
        {
            FileOutputStream outputStream = new FileOutputStream(filepath);
            outputStream.write(data);
            outputStream.close();
        }
        catch (IOException ex)
        {
           Logging.LogError("Error writing file '" + filepath + "' to disk");
           return false;
        }

        return true;
    }

    public static boolean deleteFileFromDisk(String filepath)
    {
        File file = new File(filepath);
        return file.delete();
    }

}
