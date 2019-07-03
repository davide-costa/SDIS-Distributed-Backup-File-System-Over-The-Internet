package Messaging.ChordMessages;

import Messaging.MessageSuperClass;

import java.math.BigInteger;

public class ChordMessage extends MessageSuperClass
{
    protected String        hashedIdentifier;
    protected BigInteger    numericIdentifier;

    public ChordMessage(String hashedIdentifier,BigInteger numericIdentifier)
    {
        this.numericIdentifier = numericIdentifier;
        this.hashedIdentifier = hashedIdentifier;
    }

    public String getHashedIdentifier()
    {
        return this.hashedIdentifier;
    }

    public void setHashedIdentifier(String hashedIdentifier)
    {
        this.hashedIdentifier = hashedIdentifier;
    }

    public BigInteger getNumericIdentifier()
    {
        return this.numericIdentifier;
    }

    public void setNumericIdentifier(BigInteger numericIdentifier)
    {
        this.numericIdentifier = numericIdentifier;
    }
}
