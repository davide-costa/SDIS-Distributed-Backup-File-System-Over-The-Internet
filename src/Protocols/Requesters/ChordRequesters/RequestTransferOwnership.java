package Protocols.Requesters.ChordRequesters;

import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Protocols.PeerKeysTransferrer;

public class RequestTransferOwnership
{
    ChordNodeIdentifier chordNodeSuccessor;
    ChordNodeIdentifier chordNodePredecessor;

    public RequestTransferOwnership(ChordNodeIdentifier chordNodeSuccessor, ChordNodeIdentifier chordNodePredecessor)
    {
        this.chordNodePredecessor = chordNodePredecessor;
        this.chordNodeSuccessor = chordNodeSuccessor;
    }

    public void run()
    {
        Logging.Log("RequestTransferOwnership sending keys, thread:" + Thread.currentThread().getId());
        PeerKeysTransferrer.sendKeysAndDeleteThemFromDisk(chordNodeSuccessor, chordNodePredecessor);
        Logging.FatalSuccessLog("Finished Transfer Ownership Request");
    }

}
