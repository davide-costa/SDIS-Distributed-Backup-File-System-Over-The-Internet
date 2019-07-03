package Messaging.ChordMessages;

import Chord.ChordNodeIdentifier;

public class OnLeaveTransferSuccessorChordMessage extends ChordMessage
{

    protected ChordNodeIdentifier senderChordNodeIdentifier;
    protected ChordNodeIdentifier newSuccessorOfReceiverChordNodeIdentifier;

    /*
    * The identifier values for this message contain the identifier for the
    * successor for the node that receives this message
    * */
    public OnLeaveTransferSuccessorChordMessage(ChordNodeIdentifier senderChordNodeIdentifier, ChordNodeIdentifier newSuccessorOfReceiverChordNodeIdentifier)
    {
        super(newSuccessorOfReceiverChordNodeIdentifier.getHashedIdentifier(), newSuccessorOfReceiverChordNodeIdentifier.getNumericIdentifier());
        this.senderChordNodeIdentifier = senderChordNodeIdentifier;
        this.newSuccessorOfReceiverChordNodeIdentifier = newSuccessorOfReceiverChordNodeIdentifier;
    }

    public ChordNodeIdentifier getSenderChordNodeIdentifier()
    {
        return senderChordNodeIdentifier;
    }

    public ChordNodeIdentifier getNewSuccessorOfReceiverChordNodeIdentifier()
    {
        return newSuccessorOfReceiverChordNodeIdentifier;
    }
}
