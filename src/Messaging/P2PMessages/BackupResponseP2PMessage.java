package Messaging.P2PMessages;

import Chord.ChordKey;

import java.net.InetAddress;

public class BackupResponseP2PMessage extends ResponseP2PMessage
{
    public BackupResponseP2PMessage(boolean success, ChordKey chunkId) {
        super(success, chunkId);

    }
}
