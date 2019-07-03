package Messaging.P2PMessages;


import Chord.ChordKey;

public class RestoreResponseP2PMessage extends ResponseP2PMessage
{
    public RestoreResponseP2PMessage(boolean success, ChordKey chunkId)
    {
        super(success, chunkId);
    }
}
