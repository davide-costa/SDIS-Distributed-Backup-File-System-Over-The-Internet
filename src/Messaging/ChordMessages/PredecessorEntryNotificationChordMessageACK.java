package Messaging.ChordMessages;

import java.math.BigInteger;

public class PredecessorEntryNotificationChordMessageACK extends ChordMessage
{
    public PredecessorEntryNotificationChordMessageACK(String hashedIdentifier, BigInteger numericIdentifier)
    {
        super(hashedIdentifier, numericIdentifier);
    }
}
