package Protocols.Requesters.P2PRequesters;

import AES.AES;
import Chord.ChordKey;
import Chord.ChordNode;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Messaging.P2PMessages.RestoreRequestP2PMessage;
import Messaging.P2PMessages.RestoreResponseP2PMessage;
import Protocols.Chunk;
import Protocols.Requesters.ChordRequesters.RequestLookup;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPServerMessenger;
import Utils.FileUtils;

import java.io.IOException;
import java.net.InetAddress;

import java.net.SocketTimeoutException;
import java.util.List;

public class RequestRestoreChunk implements Runnable
{
    private Chunk chunk;
    private int currChunkBackupPeer = 0;
    private int chunkSequentialNumber;

    public RequestRestoreChunk(Chunk chunk, int chunkSequentialNumber)
    {
        this.chunk = chunk;
        this.chunkSequentialNumber = chunkSequentialNumber;
    }

    @Override
    public void run()
    {
        TCPClientMessenger sslClientMessenger = getBackedUpPeerOfChunkMessenger();
        if(sslClientMessenger == null)
        {
            Logging.FatalErrorLog("Cannot connect to any of the backup peers of chunk " + chunk.getId() + ". Cannot restore it");
            return;
        }

        int port;
        try
        {
            port = ServerlessDistributedBackupService.availablePorts.take();
        }
        catch (InterruptedException e)
        {
            Logging.LogError("Failed to restore chunk with id " + this.chunk.getId() + " because the take from the available ports was interrupted.");
            return;
        }

        try
        {
            InetAddress address = ServerlessDistributedBackupService.address;
            sendRequestMessage(port, address, sslClientMessenger);

            TCPServerMessenger sslServerMessenger = new TCPServerMessenger(port);
            sslServerMessenger.acceptConnection();
            byte[] chunkData = sslServerMessenger.readMessage(chunk.getChunkSize());
            chunkData = AES.decryptBytesWithAES(ServerlessDistributedBackupService.privateKey, chunkData);
            saveRestoredChunk(chunkData);

            sendAckMessage(true, sslClientMessenger);
            sslClientMessenger.closeSocket();
            sslServerMessenger.closeSocket();
        }
        catch (IOException e)
        {
            if(e instanceof SocketTimeoutException)
            {
                currChunkBackupPeer++;
                if(currChunkBackupPeer == chunk.getNumBackupPeers())
                    Logging.LogError("Failed to restore chunk with id " + this.chunk.getId() + " because no backup peer have responded.");
                else
                    run();

                return;
            }
            try
            {
                ServerlessDistributedBackupService.availablePorts.put(port);
            }
            catch (InterruptedException e2)
            {
                Logging.LogError("Failed to restore chunk with id " + this.chunk.getId() + " because the put to the available ports was interrupted.");
                return;
            }
            Logging.LogError("Failed to restore chunk with id " + this.chunk.getId() + " because the socket could not be open.");
            return;
        }
        try
        {
            ServerlessDistributedBackupService.availablePorts.put(port);
        }
        catch (InterruptedException e)
        {
            Logging.LogError("Failed to restore chunk with id " + this.chunk.getId() + " because the put to the available ports was interrupted.");
            return;
        }

    }

    private TCPClientMessenger getBackedUpPeerOfChunkMessenger()
    {
        ChordNode self = ServerlessDistributedBackupService.systemData.getChordNode();
        List<ChordKey> backupPeersIds = chunk.getBackupPeersIds();
        for(ChordKey peerKey : backupPeersIds)
        {
            Logging.Log("Try restore chunk with id " + chunk.getId().getNumericIdentifier() + "!!!!" +
            "with peer id: " + peerKey.getNumericIdentifier());
            RequestLookup requestLookup = new RequestLookup(peerKey, self.getClosestNode(peerKey));
            requestLookup.sendRequest();
            LookupResponseChordMessage lookupResponseChordMessage = requestLookup.getRequestResponse();
            TCPClientMessenger messenger;
            try
            {
                messenger = new TCPClientMessenger(lookupResponseChordMessage.getNodeMessageChannelAddress(),
                        lookupResponseChordMessage.getNodeMessageChannelPort());
            } catch (IOException e)
            {
                Logging.LogError("Failed to restore chunk with id " + chunk.getId());
                continue;
            }
            return messenger;
        }

        return null;
    }

    private void sendRequestMessage(int port, InetAddress address, TCPClientMessenger sslClientMessenger)
    {
        RestoreRequestP2PMessage restoreRequestP2PMessage = new RestoreRequestP2PMessage(port, address, chunk.getId());
        sslClientMessenger.writeObject(restoreRequestP2PMessage);
    }

    private void sendAckMessage(boolean succeeded, TCPClientMessenger sslClientMessenger)
    {
        RestoreResponseP2PMessage restoreResponseP2PMessage = new RestoreResponseP2PMessage(succeeded, chunk.getId());
        sslClientMessenger.writeObject(restoreResponseP2PMessage);
    }

    private void saveRestoredChunk(byte[] chunkData)
    {
        String filename = chunk.getId().getHashedIdentifier();
        String filepath = ServerlessDistributedBackupService.restoredChunksPath + filename + "-" + chunkSequentialNumber;
        FileUtils.saveFileIntoDisk(filepath, chunkData);
        Logging.Log("Saved chunk " + chunk.getId() + " data in disk");
    }

}
