package Protocols.Requesters.ChordRequesters;


import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.OnLeaveTransferSuccessorChordMessage;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import com.sun.security.ntlm.Server;

import java.io.IOException;


public class RequestTransferSuccessor
{
    ChordNodeIdentifier predecessorNodeIdentifier;
    ChordNodeIdentifier successorNodeIdentifier;

    public RequestTransferSuccessor(ChordNodeIdentifier predecessorNodeIdentifier, ChordNodeIdentifier successorNodeIdentifier)
    {
        this.predecessorNodeIdentifier = predecessorNodeIdentifier;
        this.successorNodeIdentifier = successorNodeIdentifier;
    }

    public void run()
    {
        Logging.Log("RequestTransferSuccessor, thread:" + Thread.currentThread().getId());
        predecessorNodeIdentifier = ServerlessDistributedBackupService.systemData.getChordNode().getPredecessor();
        successorNodeIdentifier = ServerlessDistributedBackupService.systemData.getChordNode().getSuccessor();
        OnLeaveTransferSuccessorChordMessage onLeaveTransferSuccessorChordMessage = new OnLeaveTransferSuccessorChordMessage(
                ServerlessDistributedBackupService.systemData.getChordNode().getSelf(),
                successorNodeIdentifier);

        TCPClientMessenger sslMessenger = null;
        try
        {
            sslMessenger = new TCPClientMessenger(predecessorNodeIdentifier.getMessageChannelAddress(),
                    predecessorNodeIdentifier.getMessageChannelPort());
        } catch (IOException e)
        {
            Logging.LogError("Error requesting transfer successor");
            e.printStackTrace();
        }
        Logging.Log("RequestTransferSuccessor sending onLeaveTransferSuccessorChordMessage, thread:" + Thread.currentThread().getId());
        sslMessenger.writeObject(onLeaveTransferSuccessorChordMessage);
        Logging.FatalSuccessLog("Finished Transfer Succesor Request");
    }

}
