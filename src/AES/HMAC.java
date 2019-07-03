package AES;

import LoggingAndSettings.Logging;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMAC
{
    private final static String HMAC_SHA512 = "HmacSHA512";

    public static byte[] getHMAC(byte[] bytes, String privateKey)
    {
        try
        {
            byte[] byteKey = privateKey.getBytes("UTF-8");
            Mac sha512_HMAC = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, HMAC_SHA512);
            sha512_HMAC.init(keySpec);
            return sha512_HMAC.doFinal(bytes);
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e)
        {
            Logging.FatalErrorLog("Getting HMAC.");
            System.exit(-1);
        }

        return null;
    }
}