package Messaging.ChordMessages;

import Chord.ChordNodeIdentifier;

import java.math.BigInteger;

public class LookupChordMessage extends ChordMessage
{
    protected ChordNodeIdentifier source;
    /*
    * The identifiers for this message contain the identifier of the key being looked up
    * */
    public LookupChordMessage(String hashedIdentifier, BigInteger numericIdentifier, ChordNodeIdentifier source)
    {
        super(hashedIdentifier, numericIdentifier);
        this.source = source;
    }

    public ChordNodeIdentifier getSource() {
        return source;
    }

    public void setSource(ChordNodeIdentifier source) {
        this.source = source;
    }
}
