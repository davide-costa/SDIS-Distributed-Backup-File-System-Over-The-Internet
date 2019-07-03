package Messaging.P2PMessages;

import Chord.ChordKey;

import java.net.InetAddress;

public class RequestP2PMessage extends P2PMessage
{
    protected int port;
    protected InetAddress address;

    public RequestP2PMessage(ChordKey chunkId)
    {
        super(chunkId);
    }

    public RequestP2PMessage(int port, InetAddress address, ChordKey chunkId)
    {
        super(chunkId);
        this.port = port;
        this.address = address;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public void setAddress(InetAddress address)
    {
        this.address = address;
    }
}
