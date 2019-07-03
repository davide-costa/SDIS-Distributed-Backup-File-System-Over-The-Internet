package Messaging.ChordMessages;

import java.math.BigInteger;
import java.net.InetAddress;

public class SuccessorEntryNotificationChordMessage extends ChordMessage
{
    protected InetAddress recentlyEnteredNodeChannelAddress;
    protected int recentlyEnteredNodeChannelPort;

    /*
    * For this message the identifiers contain the id of the node that is the new predecessor
    * of the node that receives this message
    * */
    public SuccessorEntryNotificationChordMessage(String hashedIdentifier, BigInteger numericIdentifier,
                                                  InetAddress recentlyEnteredNodeChannelAddress, int recentlyEnteredNodeChannelPort)
    {
        super(hashedIdentifier, numericIdentifier);
        this.recentlyEnteredNodeChannelAddress = recentlyEnteredNodeChannelAddress;
        this.recentlyEnteredNodeChannelPort = recentlyEnteredNodeChannelPort;
    }

    public InetAddress getRecentlyEnteredNodeChannelAddress()
    {
        return recentlyEnteredNodeChannelAddress;
    }

    public int getRecentlyEnteredNodeChannelPort()
    {
        return recentlyEnteredNodeChannelPort;
    }

}
