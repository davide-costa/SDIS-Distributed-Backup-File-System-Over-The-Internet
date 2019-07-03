package Messaging.P2PMessages;

import Chord.ChordKey;

import java.net.InetAddress;

public class BackupRequestP2PMessage extends RequestP2PMessage
{
    private int chunkNumBytes;

    public BackupRequestP2PMessage(InetAddress address, int port, ChordKey chunkId, int chunkNumBytes)
    {
        super(port, address, chunkId);
        this.chunkNumBytes = chunkNumBytes;
    }

    public int getChunkNumBytes()
    {
        return chunkNumBytes;
    }

    public void setChunkNumBytes(int chunkNumBytes)
    {
        this.chunkNumBytes = chunkNumBytes;
    }
}
