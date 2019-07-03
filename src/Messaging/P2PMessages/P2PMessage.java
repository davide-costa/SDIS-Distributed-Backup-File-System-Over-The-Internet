package Messaging.P2PMessages;

import Chord.ChordKey;
import Messaging.MessageSuperClass;

import java.net.InetAddress;

public abstract class P2PMessage extends MessageSuperClass
{
    protected ChordKey chunkId;

    public P2PMessage(ChordKey chunkId)
    {
        this.chunkId = chunkId;
    }

    public ChordKey getChunkId()
    {
        return chunkId;
    }

    public void setChunkId(ChordKey chunkId)
    {
        this.chunkId = chunkId;
    }
}
