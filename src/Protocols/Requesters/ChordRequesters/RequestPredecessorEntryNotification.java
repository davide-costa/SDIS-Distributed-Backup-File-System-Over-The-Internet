package Protocols.Requesters.ChordRequesters;

import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.PredecessorEntryNotificationChordMessage;
import Messaging.ChordMessages.PredecessorEntryNotificationChordMessageACK;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;

import java.io.IOException;

public class RequestPredecessorEntryNotification
{
    private ChordNodeIdentifier self;
    private ChordNodeIdentifier predecessor;

    public RequestPredecessorEntryNotification(ChordNodeIdentifier self, ChordNodeIdentifier predecessor)
    {
        this.self = self;
        this.predecessor = predecessor;
    }

    public void sendRequest()
    {
        Logging.Log("Starting Predecessor Entry Notification Request");
        TCPClientMessenger client = null;
        try
        {
            Logging.Log("Attempting To Aquire TCP Client Messeger");
            client = new TCPClientMessenger(predecessor.getMessageChannelAddress(), predecessor.getMessageChannelPort());
        }
        catch (IOException e)
        {
            Logging.LogError("Failed to open connection to " + predecessor.getMessageChannelAddress() + ":" +
                    predecessor.getMessageChannelPort() + ", to send Request Predecessor Entry Notification. Failed To Aquire TCP Client Messeger");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return;
        }
        sendPredecessorEntryNotificationChordMessage(client);
        Logging.LogSuccess("Aquired TCP Client Messeger And Sent Predecessor Entry Notification Message");

        try
        {
            while(true)
            {
                Logging.Log("Attempting To Read Predecessor Entry Notification Response");
                PredecessorEntryNotificationChordMessageACK ack = (PredecessorEntryNotificationChordMessageACK) client.readObject();
                if(ack.getHashedIdentifier().equals(predecessor.getHashedIdentifier()) == false)
                {
                    Logging.LogError("Received unmatching response for SuccessorEntryNotificationChordMessage of peer " +
                            ack.getHashedIdentifier());
                }else {
                    Logging.LogSuccess("Read Predecessor Entry Notification Response");
                    break;
                }
            }
        }
        catch (IOException e)
        {
            Logging.LogError("IOException on reading SuccessorEntryNotificationChordMessageACK for peer");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
        }
        catch (ClassNotFoundException e)
        {
            Logging.LogError("IOException on reading SuccessorEntryNotificationChordMessageACK for peer");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
        }
        Logging.Log("Closing Socket And Updating Finger Table After Predecessor Entry Notification Request");
        client.closeSocket();
        ServerlessDistributedBackupService.systemData.getChordNode().update(predecessor);
        Logging.FatalSuccessLog("finished predecessor entry notifcation request");
    }

    private TCPClientMessenger sendPredecessorEntryNotificationChordMessage(TCPClientMessenger client)
    {
        PredecessorEntryNotificationChordMessage message = new PredecessorEntryNotificationChordMessage(self.getHashedIdentifier(),
                self.getNumericIdentifier(), self);
        client.writeObject(message);
        return client;
    }

}
