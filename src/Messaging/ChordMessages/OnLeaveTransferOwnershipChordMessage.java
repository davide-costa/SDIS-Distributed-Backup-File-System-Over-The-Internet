package Messaging.ChordMessages;

import java.math.BigInteger;
import java.net.InetAddress;

public class OnLeaveTransferOwnershipChordMessage extends PeerKeysTransferChordMessage
{
    /*
     * The hashedIdentifier and numericIdentifier for this message contain those values for
     * the new predecessor of the node that received the notify message
     * */
    public OnLeaveTransferOwnershipChordMessage(String predecessorHashedIdentifier, BigInteger predecessorNumericIdentifier,
                                                int[] keySizes, String[] keyHashedIdentifiers, BigInteger[] keyNumericIdentifiers,
                                                InetAddress dataTransferChannelAddress, int dataTransferChannelPort)
    {
        super(predecessorHashedIdentifier, predecessorNumericIdentifier, keySizes, keyHashedIdentifiers,
                keyNumericIdentifiers, dataTransferChannelAddress, dataTransferChannelPort);
    }
}
