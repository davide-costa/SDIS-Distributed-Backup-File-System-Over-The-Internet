package Protocols.RequestHandlers.P2PRequestHandlers;

import Chord.ChordKey;
import LoggingAndSettings.Logging;
import Messaging.P2PMessages.DeleteRequestP2PMessage;
import Messaging.P2PMessages.DeleteResponseP2PMessage;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPMessenger;
import Utils.FileUtils;

public class DeleteChunkRequestHandler
{
    private DeleteRequestP2PMessage deleteRequestP2PMessage;
    private TCPMessenger messenger;
    private final static int MAX_NUM_OF_RETRIES = 5;

    public DeleteChunkRequestHandler(DeleteRequestP2PMessage deleteRequestP2PMessage, TCPMessenger messenger)
    {
        this.deleteRequestP2PMessage = deleteRequestP2PMessage;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        ChordKey deleteChunkChordkey = deleteRequestP2PMessage.getChunkId();
        String deleteChunkId = deleteChunkChordkey.getHashedIdentifier();
        String deleteChunkFilepath = ServerlessDistributedBackupService.chunksPath + deleteChunkId;
        if (!FileUtils.deleteFileFromDisk(deleteChunkFilepath))
        {
            Logging.LogError("Failed to delete file from disk on DeleteChunkRequestHandler");
            return;
        }
        ServerlessDistributedBackupService.systemData.removeChunkFromCommunityBackedUpChunksByChunkId(deleteChunkId);
        ServerlessDistributedBackupService.systemData.getChordNode().removeFromKeyTable(deleteChunkChordkey.getHashedIdentifier(), deleteChunkChordkey);

        DeleteResponseP2PMessage deleteResponseP2PMessage = new DeleteResponseP2PMessage(true, deleteChunkChordkey);
        int numRetries = 0;
        boolean sendSuccessfully = messenger.writeObject(deleteResponseP2PMessage);
        while(sendSuccessfully == false && numRetries < MAX_NUM_OF_RETRIES)
        {
            sendSuccessfully = messenger.writeObject(deleteResponseP2PMessage);
            numRetries++;
        }

        messenger.closeSocket();
    }
}
