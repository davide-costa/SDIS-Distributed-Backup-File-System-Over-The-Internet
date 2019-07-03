package Utils;

import LoggingAndSettings.Logging;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class CommonUtils
{
    public static void waitRandomTimeMiliseconds(int minTime, int maxTime)
    {
        try
        {
            int randomTime = ThreadLocalRandom.current().nextInt(minTime, maxTime + 1);
            Thread.sleep(randomTime);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public static int generateRandomNumberNotInList(ArrayList<Integer> notNumbers, int min, int max)
    {
        Integer randomNumber;
        do
        {
            randomNumber = ThreadLocalRandom.current().nextInt(min, max + 1);
        } while (notNumbers.contains(randomNumber));

        return randomNumber;
    }

    public static byte[] getByteArraySHA256FromString(String string)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(string.getBytes());
        }
        catch (NoSuchAlgorithmException e)
        {
            Logging.LogError("Error generating string SHA-256.");
            return null;
        }
    }

    public static byte[] getByteArraySHA256FromByteArray(byte[] bytes)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(bytes);
        }
        catch (NoSuchAlgorithmException e)
        {
            Logging.LogError("Error generating string SHA-256.");
            return null;
        }
    }

}
