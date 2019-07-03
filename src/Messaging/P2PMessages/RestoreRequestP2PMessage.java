package Messaging.P2PMessages;

import Chord.ChordKey;

import java.net.InetAddress;

public class RestoreRequestP2PMessage extends RequestP2PMessage
{
    public RestoreRequestP2PMessage(int port, InetAddress address, ChordKey chunkId) {
        super(port, address, chunkId);
    }
}
