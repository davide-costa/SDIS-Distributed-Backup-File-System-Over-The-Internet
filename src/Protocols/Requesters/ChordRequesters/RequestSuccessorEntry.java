package Protocols.Requesters.ChordRequesters;

import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Messaging.ChordMessages.SuccessorEntryNotificationChordMessage;
import Messaging.ChordMessages.SuccessorEntryNotificationResponseChordMessage;
import Protocols.PeerKeysTransferrer;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;

public class RequestSuccessorEntry
{
    private ChordKey chordKey;
    private ChordNodeIdentifier wellKnownNode;
    private ChordNodeIdentifier predecessor;

    public RequestSuccessorEntry(ChordKey chordKey, ChordNodeIdentifier wellKnownNode)
    {
        this.chordKey = chordKey;
        this.wellKnownNode = wellKnownNode;
    }

    public boolean sendRequest()
    {
        Logging.Log("Starting Request Successor Entry Request");
        ChordNode selfChordNode = ServerlessDistributedBackupService.systemData.getChordNode();
        String hashedIdentifier = selfChordNode.getSelf().getHashedIdentifier();
        BigInteger numericIdentifier = selfChordNode.getSelf().getNumericIdentifier();

        Logging.Log("Looking Up Successor For Successor Entry Notification");
        RequestLookup requester = new RequestLookup(chordKey, wellKnownNode);
        if(requester.sendRequest() == false) {
            Logging.FatalErrorLog("Failed To Send Lookup request for sucessor entry request");
            return false;
        }

        Logging.LogSuccess("Read Lookup Response");
        LookupResponseChordMessage response = requester.getRequestResponse();
        InetAddress successorPeerAddress = response.getNodeMessageChannelAddress();
        int successorPeerPort = response.getNodeMessageChannelPort();

        SuccessorEntryNotificationChordMessage chordMessage = new SuccessorEntryNotificationChordMessage(hashedIdentifier, numericIdentifier,
                ServerlessDistributedBackupService.address, ServerlessDistributedBackupService.messagePort);
        TCPClientMessenger messenger = null;
        Logging.Log("Attempting To Aquired TCP Client Messeger For Succesor Entry Request");
        try
        {
            messenger = new TCPClientMessenger(successorPeerAddress, successorPeerPort);
        } catch (IOException e)
        {
            Logging.LogError("Error on sending request for successor entry");
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            e.printStackTrace();
        }
        messenger.writeObject(chordMessage);
        Logging.LogSuccess("Succesor Entry Message Sent");
        SuccessorEntryNotificationResponseChordMessage responseMessage;
        try
        {
            Logging.Log("Attempting To Read Succesor Entry Response");
            responseMessage = (SuccessorEntryNotificationResponseChordMessage) messenger.readObject();
            if(responseMessage.getHashedIdentifier().equals(hashedIdentifier))
            {
                Logging.LogError("Received unmatching response for SuccessorEntryNotificationChordMessage for peer " +  hashedIdentifier);
                return false;
            }

        }
        catch (IOException e)
        {
            Logging.LogError("IOException on reading SuccessorEntryNotificationChordMessageACK for peer " +  hashedIdentifier);
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return false;
        }
        catch (ClassNotFoundException e)
        {
            Logging.LogError("IOException on reading SuccessorEntryNotificationChordMessageACK for peer " +  hashedIdentifier);
            Logging.ErrorDebug(e.getMessage()+e.getStackTrace());
            return false;
        }

        Logging.Log("receiving keys from succesor");
        PeerKeysTransferrer.receiveKeys(responseMessage, messenger);
        Logging.LogSuccess("keys received from successor");

        Logging.Log("Updating Finger Table After Succesor Entry");
        predecessor = responseMessage.getPredecessorChordNodeIdentifier();
        ChordNodeIdentifier successor = new ChordNodeIdentifier(successorPeerAddress, successorPeerPort);
        ServerlessDistributedBackupService.systemData.getChordNode().setSuccessor(successor);
        ServerlessDistributedBackupService.systemData.getChordNode().update(responseMessage.getPredecessorChordNodeIdentifier());
        ServerlessDistributedBackupService.systemData.getChordNode().update(requester.getRequestResponse().getSource());
        Logging.FatalSuccessLog("Succesor Entry Request Finished");
        return true;
    }

    public ChordNodeIdentifier getPredecessor()
    {
        return predecessor;
    }

}
