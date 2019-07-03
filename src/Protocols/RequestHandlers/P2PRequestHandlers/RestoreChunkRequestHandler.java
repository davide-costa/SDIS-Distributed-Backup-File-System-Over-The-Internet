package Protocols.RequestHandlers.P2PRequestHandlers;

import LoggingAndSettings.Logging;
import Messaging.P2PMessages.RestoreRequestP2PMessage;
import Messaging.P2PMessages.RestoreResponseP2PMessage;
import Protocols.Chunk;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPMessenger;
import Utils.FileUtils;

import java.io.IOException;
import java.net.InetAddress;

public class RestoreChunkRequestHandler
{
    private RestoreRequestP2PMessage restoreMessage;
    private TCPMessenger messenger;
    private Chunk chunk;
    private final static int MAX_NUM_OF_RETRIES = 5;

    public RestoreChunkRequestHandler(RestoreRequestP2PMessage restoreMessage, TCPMessenger messenger)
    {
        this.restoreMessage = restoreMessage;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        String restoreChunkId = restoreMessage.getChunkId().getHashedIdentifier();
        chunk = ServerlessDistributedBackupService.systemData.getCommunityBackedUpChunkById(restoreChunkId);
        if (chunk == null)
            return;

        String restoreChunkFilepath = ServerlessDistributedBackupService.chunksPath + restoreChunkId;
        byte[] chunkData = FileUtils.readFileFromDisk(restoreChunkFilepath);

        InetAddress address = restoreMessage.getAddress();
        int port = restoreMessage.getPort();
        TCPClientMessenger sslClientDataMessenger;
        try
        {
            sslClientDataMessenger = new TCPClientMessenger(address, port);
        }
        catch (IOException e)
        {
            Logging.LogError("Failed to open connection to " + address + ":" + port + ", to send chunk " + chunk.getId() + ".");
            return;
        }

        boolean successfulWrite = false;
        int numOfRetries = 0;
        while (!successfulWrite && numOfRetries < MAX_NUM_OF_RETRIES)
        {
            numOfRetries++;
            try
            {
                successfulWrite = sslClientDataMessenger.writeMessage(chunkData);
                if(!successfulWrite)
                    continue;

                RestoreResponseP2PMessage responseMessage = (RestoreResponseP2PMessage) messenger.readObject();
                successfulWrite = responseMessage.succeeded();
                String responseMessageChunkId = responseMessage.getChunkId().getHashedIdentifier();
                if (!responseMessageChunkId.equals(restoreChunkId))
                    successfulWrite = false;
            }
            catch (java.io.IOException e)
            {
                successfulWrite = false;
            }
            catch (ClassNotFoundException e)
            {
                successfulWrite = false;
            }
        }

        sslClientDataMessenger.closeSocket();
        messenger.closeSocket();
    }

}
