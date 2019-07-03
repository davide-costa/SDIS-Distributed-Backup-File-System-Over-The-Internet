package Protocols.RequestHandlers.ChordRequestHandlers;

import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.PredecessorEntryNotificationChordMessage;
import Messaging.ChordMessages.PredecessorEntryNotificationChordMessageACK;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPMessenger;

public class PredecessorEntryNotificationHandler
{
    private PredecessorEntryNotificationChordMessage message;
    private TCPMessenger messenger;

    public PredecessorEntryNotificationHandler(PredecessorEntryNotificationChordMessage message, TCPMessenger messenger)
    {
        this.message = message;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        Logging.Log("PredecessorEntryNotificationChordMessage, thread:" + Thread.currentThread().getId());
        ChordNode self = ServerlessDistributedBackupService.systemData.getChordNode();
        ChordNodeIdentifier successor = message.getSource();
        self.setSuccessor(successor);
        self.update(successor);

        Logging.Log("sending PredecessorEntryNotificationChordMessageACK, thread:" + Thread.currentThread().getId());
        PredecessorEntryNotificationChordMessageACK ack = new PredecessorEntryNotificationChordMessageACK(self.getSelf().getHashedIdentifier(),
                self.getSelf().getNumericIdentifier());
        messenger.writeObject(ack);
    }
}
