package Protocols.RequestHandlers.ChordRequestHandlers;

import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import Messaging.ChordMessages.OnLeaveTransferSuccessorChordMessage;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPMessenger;

import java.net.InetAddress;

public class TransferSuccessorHandler
{
    private OnLeaveTransferSuccessorChordMessage chordMessage;
    private TCPMessenger messenger;

    public TransferSuccessorHandler(OnLeaveTransferSuccessorChordMessage message, TCPMessenger messenger)
    {
        this.chordMessage = message;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        ChordNode chordNode = ServerlessDistributedBackupService.systemData.getChordNode();
        ChordNodeIdentifier newSuccessorChordNodeIdentifier = chordMessage.getNewSuccessorOfReceiverChordNodeIdentifier();
        ChordNodeIdentifier currSuccessorNodeIdentifier = chordNode.getSuccessor();

        //alter finger table
        chordNode.nodeLeft(currSuccessorNodeIdentifier, newSuccessorChordNodeIdentifier);

        //alter successor
        chordNode.setSuccessor(newSuccessorChordNodeIdentifier);

        messenger.closeSocket();
    }
}
