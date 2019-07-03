package Protocols.RequestHandlers.P2PRequestHandlers;

import Chord.ChordKey;
import LoggingAndSettings.Logging;
import Messaging.P2PMessages.BackupRequestP2PMessage;
import Messaging.P2PMessages.BackupResponseP2PMessage;
import Protocols.Chunk;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPMessenger;
import Utils.FileUtils;

import java.io.IOException;
import java.net.InetAddress;

public class BackupChunkRequestHandler
{
    private BackupRequestP2PMessage backupMessage;
    private TCPMessenger messenger;
    private ChordKey chunkId;
    private final static int MAX_NUM_OF_RETRIES = 5;

    public BackupChunkRequestHandler(BackupRequestP2PMessage backupMessage, TCPMessenger messenger)
    {
        this.backupMessage = backupMessage;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        Logging.Log("Handling Request Backup Chunk on class BackupChunkRequestHandler");
        //check if chunk is not already backed up in this peer
        chunkId = backupMessage.getChunkId();
        int chunkSize = backupMessage.getChunkNumBytes();
        Logging.Log("Received backup request for chunk with id " + chunkId.getNumericIdentifier());
        InetAddress address = backupMessage.getAddress();
        int port = backupMessage.getPort();
        TCPClientMessenger sslClientMessenger;
        try
        {
            sslClientMessenger = new TCPClientMessenger(address, port);
        }
        catch (IOException e)
        {
            Logging.LogError("Failed to open connection to " + backupMessage.getAddress() + ":" + backupMessage.getPort()
                    + ", to read chunk data.");
            return;
        }

        Logging.Log("BackupChunkRequestHandler - reading chunk, thread:" + Thread.currentThread().getId());
        byte[] chunkData = sslClientMessenger.readMessage(chunkSize);
        if (chunkData == null)
        {
            Logging.LogError("Error reading chunk data from socket to backup the chunk on BackupRequestHandler, thread:" + Thread.currentThread().getId());
            sendAckMessage(false);
        }
        else
            Logging.LogSuccess("Read chunk bytes from socket on BackupChunkRequestHandler, thread:" + Thread.currentThread().getId());
        String chunkFilepath = ServerlessDistributedBackupService.chunksPath + chunkId.getHashedIdentifier();
        boolean success = FileUtils.saveFileIntoDisk(chunkFilepath, chunkData);
        if(success == false)
            sendAckMessage(false);

        Logging.Log("BackupChunkRequestHandler - chunk read, saving in system, thread:" + Thread.currentThread().getId());
        Chunk chunk = new Chunk(chunkId, chunkSize);
        ServerlessDistributedBackupService.systemData.addToCommunityBackedUpChunks(chunk);
        ServerlessDistributedBackupService.systemData.getChordNode().addToKeyTable(chunk.getIdString(), chunk.getId());
        success = sendAckMessage(true);
        if(success == false)
        {
            FileUtils.deleteFileFromDisk(chunkFilepath);
            ServerlessDistributedBackupService.systemData.removeChunkFromCommunityBackedUpChunks(chunk);
            ServerlessDistributedBackupService.systemData.getChordNode().removeFromKeyTable(chunk.getIdString(), chunk.getId());
        }
        Logging.Log("BackupChunkRequestHandler - chunk saved, thread:" + Thread.currentThread().getId());
        sslClientMessenger.closeSocket();
        Logging.Log("Ending BackupChunkRequestHandler for chunk with id " + chunkId.getNumericIdentifier());

    }

    private boolean sendAckMessage(boolean success)
    {
        BackupResponseP2PMessage responseMessage = new BackupResponseP2PMessage(success, chunkId);
        boolean writeSuccessfully = messenger.writeObject(responseMessage);

        int numRetries = 0;
        while(writeSuccessfully == false && numRetries < MAX_NUM_OF_RETRIES)
        {
            writeSuccessfully = messenger.writeObject(responseMessage);
            numRetries++;
        }

        return numRetries < MAX_NUM_OF_RETRIES;
    }

}
