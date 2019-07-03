package Messaging.ChordMessages;

import Chord.ChordNodeIdentifier;

import java.math.BigInteger;

public class PredecessorEntryNotificationChordMessage extends ChordMessage
{
    private ChordNodeIdentifier source;

    /*
    * The identifiers for this message contain the identifier of the node that is the
    * new successor of the node that received this message
    * */
    public PredecessorEntryNotificationChordMessage(String hashedIdentifier, BigInteger numericIdentifier, ChordNodeIdentifier source)
    {
        super(hashedIdentifier, numericIdentifier);
        this.source = source;
    }

    public ChordNodeIdentifier getSource()
    {
        return source;
    }
}
