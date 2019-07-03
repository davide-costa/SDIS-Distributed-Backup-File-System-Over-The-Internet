package Messaging.P2PMessages;

import Chord.ChordKey;

public class DeleteResponseP2PMessage extends ResponseP2PMessage
{
    public DeleteResponseP2PMessage(boolean success, ChordKey chunkId)
    {
        super(success, chunkId);
    }
}
