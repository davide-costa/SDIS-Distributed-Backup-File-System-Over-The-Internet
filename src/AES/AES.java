package AES;

import LoggingAndSettings.Logging;

import java.io.*;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AES
{
    private static SecretKeySpec secretKey;
    private static byte[] key;

    private static byte[] processBytesWithAES(int cipherMode, String key, byte[] bytes)
    {
        try
        {
            setKey(key);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKey);
            return cipher.doFinal(bytes);
        }
        catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException
                | BadPaddingException | IllegalBlockSizeException e)
        {
            Logging.LogError("Error " + e.getCause() + " encrypting bytes");
            return null;
        }
    }

    public static byte[] encryptBytesWithAES(String key, byte[] bytes)
    {
        return processBytesWithAES(Cipher.ENCRYPT_MODE, key, bytes);
    }

    public static byte[] decryptBytesWithAES(String key, byte[] bytes)
    {
        return processBytesWithAES(Cipher.DECRYPT_MODE, key, bytes);
    }

    public static void setKey(String myKey)
    {
        try
        {
            key = myKey.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public static String encrypt(String strToEncrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            System.out.println("Error while encrypting: " + e.toString());
        }
        return null;
    }

    public static String decrypt(String strToDecrypt, String secret)
    {
        try
        {
            setKey(secret);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}