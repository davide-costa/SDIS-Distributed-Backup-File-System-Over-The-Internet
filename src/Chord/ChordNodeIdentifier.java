package Chord;

import Utils.CommonUtils;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Objects;

public class ChordNodeIdentifier implements Serializable
{

    protected BigInteger numericIdentifier;
    protected String hashedIdentifier;

    protected InetAddress messageChannelAddress;
    protected int messageChannelPort;

    public ChordNodeIdentifier(BigInteger numericIdentifier, String hashedIdentifier, InetAddress address, int port)
    {
        this.numericIdentifier = numericIdentifier;
        this.hashedIdentifier = hashedIdentifier;
        this.messageChannelAddress = address;
        this.messageChannelPort = port;
    }

    public ChordNodeIdentifier(InetAddress address, int port)
    {
        this.messageChannelAddress = address;
        this.messageChannelPort = port;

        String peerId = address + ":" + port;
        ChordKey chordKey = new ChordKey(CommonUtils.getByteArraySHA256FromString(peerId));
        this.numericIdentifier = chordKey.getNumericIdentifier();
        this.hashedIdentifier = chordKey.getHashedIdentifier();
    }

    public BigInteger getNumericIdentifier()
    {
        return numericIdentifier;
    }

    public void setNumericIdentifier(BigInteger numericIdentifier)
    {
        this.numericIdentifier = numericIdentifier;
    }

    public String getHashedIdentifier()
    {
        return hashedIdentifier;
    }

    public void setHashedIdentifier(String hashedIdentifier)
    {
        this.hashedIdentifier = hashedIdentifier;
    }

    public InetAddress getMessageChannelAddress()
    {
        return messageChannelAddress;
    }

    public void setMessageChannelAddress(InetAddress messageChannelAddress)
    {
        this.messageChannelAddress = messageChannelAddress;
    }

    public int getMessageChannelPort()
    {
        return messageChannelPort;
    }

    public void setMessageChannelPort(int messageChannelPort)
    {
        this.messageChannelPort = messageChannelPort;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordNodeIdentifier that = (ChordNodeIdentifier) o;
        return messageChannelPort == that.messageChannelPort &&
                Objects.equals(numericIdentifier, that.numericIdentifier) &&
                Objects.equals(hashedIdentifier, that.hashedIdentifier) &&
                Objects.equals(messageChannelAddress, that.messageChannelAddress);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(numericIdentifier, hashedIdentifier, messageChannelAddress, messageChannelPort);
    }
}
