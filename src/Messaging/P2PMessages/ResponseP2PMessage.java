package Messaging.P2PMessages;

import Chord.ChordKey;

public class ResponseP2PMessage extends P2PMessage
{
    boolean success;

    public ResponseP2PMessage(boolean success, ChordKey chunkId)
    {
        super(chunkId);
        this.success = success;
    }

    public boolean succeeded()
    {
        return success;
    }
}
