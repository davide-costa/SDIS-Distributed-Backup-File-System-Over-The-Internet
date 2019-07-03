package Chord;

import LoggingAndSettings.Logging;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Objects;

public class ChordKey implements Serializable
{

    protected BigInteger    numericIdentifier;
    protected String        hashedIdentifier;

    public ChordKey(BigInteger numericIdentifier, String hashedIdentifier)
    {
        this.numericIdentifier = numericIdentifier;
        this.hashedIdentifier = hashedIdentifier;
    }

    public ChordKey(byte[] bytes)
    {
        BigInteger id = new BigInteger(bytes);
        if(id.compareTo(new BigInteger(String.valueOf(0).getBytes()))==-1)
        {
            // for negative BigInteger, top byte is negative
            byte[] contents = id.toByteArray();

            // prepend byte of opposite sign
            byte[] result = new byte[contents.length + 1];
            System.arraycopy(contents, 0, result, 1, contents.length);
            result[0] = (contents[0] < 0) ? 0 : (byte)-1;

            // this will be two's complement
            id = new BigInteger(result);
        }
        this.numericIdentifier = id;

        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xff & bytes[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        this.hashedIdentifier = hexString.toString();

        Logging.Log("hashedIdentifier: " + this.hashedIdentifier);
        Logging.Log("numericIdentifier: " + this.numericIdentifier);
    }

    public BigInteger getNumericIdentifier() {
        return numericIdentifier;
    }

    public void setNumericIdentifier(BigInteger numericIdentifier) {
        this.numericIdentifier = numericIdentifier;
    }

    public String getHashedIdentifier() {
        return hashedIdentifier;
    }

    public void setHashedIdentifier(String hashedIdentifier) {
        this.hashedIdentifier = hashedIdentifier;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordKey chordKey = (ChordKey) o;
        return Objects.equals(numericIdentifier, chordKey.numericIdentifier) &&
                Objects.equals(hashedIdentifier, chordKey.hashedIdentifier);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(numericIdentifier, hashedIdentifier);
    }
}
