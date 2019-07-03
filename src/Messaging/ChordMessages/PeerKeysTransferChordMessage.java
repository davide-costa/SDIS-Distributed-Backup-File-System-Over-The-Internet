package Messaging.ChordMessages;

import java.math.BigInteger;
import java.net.InetAddress;

public class PeerKeysTransferChordMessage extends ChordMessage
{
    /*
     * The contain an array with the size of each data chunk in bytes
     * An array with the identifier of each chunk in the string format
     * and an array with the identifier of each chunk in the numeric format
     * */
    protected int[] keySizes;
    protected String[] keyHashedIdentifiers;
    protected BigInteger[] keyNumericIdentifiers;

    /*
     * Contains the address and messagePort of the channel that will be used to
     * transfer the chunks to their new owner
     * */
    protected InetAddress dataTransferChannelAddress;
    protected int dataTransferChannelPort;

    /*
     * The hashedIdentifier and numericIdentifier for this message contain those values for
     * the new predecessor of the node that received the notify message
     * */
    public PeerKeysTransferChordMessage(String predecessorHashedIdentifier, BigInteger predecessorNumericIdentifier, int[] keySizes, String[] keyHashedIdentifiers, BigInteger[] keyNumericIdentifiers, InetAddress dataTransferChannelAddress, int dataTransferChannelPort)
    {
        super(predecessorHashedIdentifier, predecessorNumericIdentifier);

        this.dataTransferChannelAddress = dataTransferChannelAddress;
        this.dataTransferChannelPort = dataTransferChannelPort;
        this.keySizes = keySizes;
        this.keyHashedIdentifiers = keyHashedIdentifiers;
        this.keyNumericIdentifiers = keyNumericIdentifiers;
    }

    public int[] getKeySizes()
    {
        return keySizes;
    }

    public void setKeySizes(int[] keySizes)
    {
        this.keySizes = keySizes;
    }

    public String[] getKeyHashedIdentifiers()
    {
        return keyHashedIdentifiers;
    }

    public void setKeyHashedIdentifiers(String[] keyHashedIdentifiers)
    {
        this.keyHashedIdentifiers = keyHashedIdentifiers;
    }

    public BigInteger[] getKeyNumericIdentifiers()
    {
        return keyNumericIdentifiers;
    }

    public void setKeyNumericIdentifiers(BigInteger[] keyNumericIdentifiers)
    {
        this.keyNumericIdentifiers = keyNumericIdentifiers;
    }

    public InetAddress getDataTransferChannelAddress()
    {
        return dataTransferChannelAddress;
    }

    public void setDataTransferChannelAddress(InetAddress dataTransferChannelAddress)
    {
        this.dataTransferChannelAddress = dataTransferChannelAddress;
    }

    public int getDataTransferChannelPort()
    {
        return dataTransferChannelPort;
    }

    public void setDataTransferChannelPort(int dataTransferChannelPort)
    {
        this.dataTransferChannelPort = dataTransferChannelPort;
    }
}
