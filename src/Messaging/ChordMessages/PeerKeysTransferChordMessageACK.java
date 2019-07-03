package Messaging.ChordMessages;


import java.math.BigInteger;

public class PeerKeysTransferChordMessageACK extends ChordMessage
{
    public PeerKeysTransferChordMessageACK(String hashedIdentifier, BigInteger numericIdentifier)
    {
        super(hashedIdentifier, numericIdentifier);
    }
}
