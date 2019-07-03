package Messaging.ChordMessages;

import Chord.ChordNodeIdentifier;

import java.math.BigInteger;
import java.net.InetAddress;

public class LookupResponseChordMessage extends ChordMessage
{
    protected ChordNodeIdentifier source;
    /*
    * These Contain the address and messagePort of the message channel for the node
    * that currently own the key that is being looked up
    * */
    protected InetAddress   nodeMessageChannelAddress;
    protected int           nodeMessageChannelPort;

    /*
    * The identifier for this message contains the identifier of the node
    * that currently owns the key that was looked up
    * */
    public LookupResponseChordMessage(String hashedIdentifier, BigInteger numericIdentifier, InetAddress nodeMessageChannelAddress, int nodeMessageChannelPort, ChordNodeIdentifier self) {
        super(hashedIdentifier, numericIdentifier);

        this.source = self;
        this.nodeMessageChannelAddress = nodeMessageChannelAddress;
        this.nodeMessageChannelPort = nodeMessageChannelPort;
    }

    public ChordNodeIdentifier getSource()
    {
        return source;
    }

    public void setSource(ChordNodeIdentifier source)
    {
        this.source = source;
    }

    public InetAddress getNodeMessageChannelAddress() {
        return nodeMessageChannelAddress;
    }

    public void setNodeMessageChannelAddress(InetAddress nodeMessageChannelAddress) {
        this.nodeMessageChannelAddress = nodeMessageChannelAddress;
    }

    public int getNodeMessageChannelPort() {
        return nodeMessageChannelPort;
    }

    public void setNodeMessageChannelPort(int nodeMessageChannelPort) {
        this.nodeMessageChannelPort = nodeMessageChannelPort;
    }
}
