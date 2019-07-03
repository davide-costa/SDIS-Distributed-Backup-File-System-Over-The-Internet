package Protocols.RequestHandlers.ChordRequestHandlers;

import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.OnLeaveTransferOwnershipChordMessage;
import Protocols.PeerKeysTransferrer;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPMessenger;


public class TransferOwnershipHandler
{
    private OnLeaveTransferOwnershipChordMessage chordMessage;
    private TCPMessenger messenger;

    public TransferOwnershipHandler(OnLeaveTransferOwnershipChordMessage chordMessage, TCPMessenger messenger)
    {
        this.chordMessage = chordMessage;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        Logging.Log("TransferOwnershipHandler receiving keys, thread:" + Thread.currentThread().getId());
        PeerKeysTransferrer.receiveKeys(chordMessage, messenger);
        ServerlessDistributedBackupService.systemData.getChordNode().update(new ChordNodeIdentifier(chordMessage.getNumericIdentifier(),
                chordMessage.getHashedIdentifier(), chordMessage.getDataTransferChannelAddress(), chordMessage.getDataTransferChannelPort()));
    }

}
