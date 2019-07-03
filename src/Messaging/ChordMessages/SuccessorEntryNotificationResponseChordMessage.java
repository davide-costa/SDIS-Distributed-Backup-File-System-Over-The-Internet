package Messaging.ChordMessages;

import Chord.ChordNodeIdentifier;

import java.math.BigInteger;
import java.net.InetAddress;

public class SuccessorEntryNotificationResponseChordMessage extends PeerKeysTransferChordMessage
{
    /*
     * Contains predecessor of the peer sending
     * this chunk. That will be the predecessor of the node that receives this message.
     * */
    protected ChordNodeIdentifier predecessorChordNodeIdentifier;

    /*
     * The hashedIdentifier and numericIdentifier for this message contain those values for
     * the predecessor of the node that received the notify message
     * */
    public SuccessorEntryNotificationResponseChordMessage(String hashedIdentifier, BigInteger numericIdentifier, int[] keySizes,
                                                          String[] keyHashedIdentifiers, BigInteger[] keyNumericIdentifiers,
                                                          InetAddress dataTransferChannelAddress, int dataTransferChannelPort,
                                                          ChordNodeIdentifier predecessorChordNodeIdentifier)
    {
        super(hashedIdentifier, numericIdentifier, keySizes, keyHashedIdentifiers,
                keyNumericIdentifiers, dataTransferChannelAddress, dataTransferChannelPort);
        this.predecessorChordNodeIdentifier = predecessorChordNodeIdentifier;
    }

    public ChordNodeIdentifier getPredecessorChordNodeIdentifier()
    {
        return predecessorChordNodeIdentifier;
    }

    public void setPredecessorChordNodeIdentifier(ChordNodeIdentifier predecessorChordNodeIdentifier)
    {
        this.predecessorChordNodeIdentifier = predecessorChordNodeIdentifier;
    }
}
