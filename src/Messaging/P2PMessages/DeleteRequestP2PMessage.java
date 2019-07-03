package Messaging.P2PMessages;

import Chord.ChordKey;

public class DeleteRequestP2PMessage extends RequestP2PMessage
{
    public DeleteRequestP2PMessage(ChordKey chunkId)
    {
        super(chunkId);
    }
}
