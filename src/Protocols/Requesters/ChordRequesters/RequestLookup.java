package Protocols.Requesters.ChordRequesters;

import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.LookupChordMessage;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;

import java.io.IOException;

public class RequestLookup
{
    private ChordKey key;
    private ChordNodeIdentifier baseSearchNode;
    private LookupResponseChordMessage response;

    public RequestLookup(ChordKey key, ChordNodeIdentifier baseSearchNode)
    {
      this.key = key;
      this.baseSearchNode = baseSearchNode;
    }

    public boolean sendRequest()
    {
        Logging.Log("Starting lookup request");
        ChordNode self = ServerlessDistributedBackupService.systemData.getChordNode();
        LookupChordMessage msg = new LookupChordMessage(key.getHashedIdentifier(),key.getNumericIdentifier(),self.getSelf());

        Logging.Log("Starting to send lookup message");
        TCPClientMessenger clientMessenger;
        try
        {
            clientMessenger = new TCPClientMessenger(baseSearchNode.getMessageChannelAddress(),
                    baseSearchNode.getMessageChannelPort());
            Logging.LogSuccess("Aquired TCP client messeger");
        }
        catch (IOException e)
        {
            Logging.LogError("Failed to aquired TCP client messeger");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return false;
        }
        clientMessenger.writeObject(msg);
        Logging.LogSuccess("Send Lookup Message");

        Logging.Log("Attempting to read lookup responde");
        try
        {
            response = (LookupResponseChordMessage) clientMessenger.readObject();
            Logging.LogSuccess("Read Lookup Responde");
            clientMessenger.closeSocket();
        }
        catch (IOException e)
        {
            Logging.FatalErrorLog("IOException on read LookupResponseChordMessage, failed to read response.");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return false;
        }
        catch (ClassNotFoundException e)
        {
            Logging.FatalErrorLog("ClassNotFoundException on read LookupResponseChordMessage.");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return false;
        }

        Logging.Log("Updating Finger Table After Lookup");
        ChordNodeIdentifier source = response.getSource();
        self.update(source);
        Logging.FatalSuccessLog("Finished Chord Lookup Request");
        return true;
    }

    public LookupResponseChordMessage getRequestResponse()
    {
        return response;
    }
}
