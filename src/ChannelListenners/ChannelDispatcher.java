package ChannelListenners;

import LoggingAndSettings.Logging;
import Messaging.ChordMessages.*;
import Messaging.MessageSuperClass;
import Messaging.P2PMessages.*;
import Protocols.RequestHandlers.ChordRequestHandlers.*;
import Protocols.RequestHandlers.P2PRequestHandlers.BackupChunkRequestHandler;
import Protocols.RequestHandlers.P2PRequestHandlers.DeleteChunkRequestHandler;
import Protocols.RequestHandlers.P2PRequestHandlers.RestoreChunkRequestHandler;
import TCPMessengers.TCPMessenger;

import java.io.IOException;

public class ChannelDispatcher implements Runnable
{
    private TCPMessenger messenger;
    private MessageSuperClass messageObj;

    ChannelDispatcher(TCPMessenger messenger)
    {
        this.messenger = messenger;
    }

    @Override
    public void run()
    {
        //TODO ver o q fazer quando o socket é fechado do outro lado, para isso tem q se deixar a exception vir pa cima e nao ser apanhada no messenger
        Object readObj = null;
        try
        {
            readObj = messenger.readObject();
        }
        catch (IOException e)
        {
            Logging.FatalErrorLog("IOException on ChannelDispatcher read.");
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            Logging.FatalErrorLog("ClassNotFoundException on ChannelDispatcher read.");
            e.printStackTrace();
        }

        if (!(readObj instanceof MessageSuperClass)) //ignore if does not correspond to valid data
            return;

        this.messageObj = (MessageSuperClass) readObj;
        if (messageObj instanceof BackupRequestP2PMessage) {
            Logging.Log("received backup message");
            handleBackupTask();
        }
        else if (messageObj instanceof RestoreRequestP2PMessage) {
            Logging.Log("received restore request message");
            handleRestoreTask();
        }
        else if (messageObj instanceof DeleteRequestP2PMessage) {
            Logging.Log("received delete message");
            handleDeleteTask();
        }
        else if (messageObj instanceof LookupChordMessage) {
            Logging.Log("received lookup message");
            handleLookupChordMessage();
        }
        else if (messageObj instanceof OnLeaveTransferOwnershipChordMessage) {
            Logging.Log("received on leave transfer ownership message");
            handleOnLeaveTransferOwnershipChordMessage();
        }
        else if (messageObj instanceof OnLeaveTransferSuccessorChordMessage) {
            Logging.Log("received on leave transfer successor message");
            handleOnLeaveTransferSuccessorChordMessage();
        }
        else if (messageObj instanceof PredecessorEntryNotificationChordMessage) {
            Logging.Log("received predessecor entry notification message");
            handlePredecessorEntryNotificationChordMessage();
        }
        else if (messageObj instanceof SuccessorEntryNotificationChordMessage) {
            Logging.Log("received successor entry notification message");
            handleSuccessorEntryNotificationChordMessage();
        }
        else
        {
            //ignore invalid messages ensuring it is harder to DoS a peer
            Logging.Log("Received P2PMessages that is not an instance of expected types");
            return;
        }
    }

    private void handleBackupTask()
    {
        BackupRequestP2PMessage backupMessage = (BackupRequestP2PMessage) messageObj;
        BackupChunkRequestHandler handler = new BackupChunkRequestHandler(backupMessage, messenger);
        handler.handleRequest();
    }

    private void handleRestoreTask()
    {
        RestoreRequestP2PMessage restoreMessage = (RestoreRequestP2PMessage) messageObj;
        RestoreChunkRequestHandler handler = new RestoreChunkRequestHandler(restoreMessage, messenger);
        handler.handleRequest();
    }

    private void handleDeleteTask()
    {
        DeleteRequestP2PMessage deleteMessage = (DeleteRequestP2PMessage) messageObj;
        DeleteChunkRequestHandler handler = new DeleteChunkRequestHandler(deleteMessage, messenger);
        handler.handleRequest();
    }

    private void handleLookupChordMessage()
    {
        LookupChordMessage message = (LookupChordMessage) messageObj;
        LookupHandler handler = new LookupHandler(message, messenger);
        handler.handleRequest();
    }

    private void handleOnLeaveTransferOwnershipChordMessage()
    {
        OnLeaveTransferOwnershipChordMessage message = (OnLeaveTransferOwnershipChordMessage) messageObj;
        TransferOwnershipHandler handler = new TransferOwnershipHandler(message, messenger);
        handler.handleRequest();
    }

    private void handleOnLeaveTransferSuccessorChordMessage()
    {
        OnLeaveTransferSuccessorChordMessage message = (OnLeaveTransferSuccessorChordMessage) messageObj;
        TransferSuccessorHandler handler = new TransferSuccessorHandler(message, messenger);
        handler.handleRequest();
    }

    private void handlePredecessorEntryNotificationChordMessage()
    {
        PredecessorEntryNotificationChordMessage message = (PredecessorEntryNotificationChordMessage) messageObj;
        PredecessorEntryNotificationHandler handler = new PredecessorEntryNotificationHandler(message, messenger);
        handler.handleRequest();
    }

    private void handleSuccessorEntryNotificationChordMessage()
    {
        SuccessorEntryNotificationChordMessage message = (SuccessorEntryNotificationChordMessage) messageObj;
        SuccessorEntryRequestHandler handler = new SuccessorEntryRequestHandler(message, messenger);
        handler.handleRequest();
    }

}
