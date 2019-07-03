package Protocols.RequestHandlers.ChordRequestHandlers;

import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.PeerKeysTransferChordMessageACK;
import Messaging.ChordMessages.SuccessorEntryNotificationChordMessage;
import Messaging.ChordMessages.SuccessorEntryNotificationResponseChordMessage;
import Protocols.Chunk;
import Protocols.PeerKeysTransferrer;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPMessenger;
import TCPMessengers.TCPServerMessenger;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuccessorEntryRequestHandler
{
    SuccessorEntryNotificationChordMessage message;
    TCPMessenger messenger;

    public SuccessorEntryRequestHandler(SuccessorEntryNotificationChordMessage message, TCPMessenger messenger)
    {
        this.message = message;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        Logging.Log("SuccessorEntryRequestHandler, thread:" + Thread.currentThread().getId());

        InetAddress address = message.getRecentlyEnteredNodeChannelAddress();
        int messagePort = message.getRecentlyEnteredNodeChannelPort();
        BigInteger newPredecessorNumericIdentifier = message.getNumericIdentifier();
        String newPredecessorHashedIdentifier = message.getHashedIdentifier();

        List<Chunk> communityBackedUpChunks = Collections.synchronizedList(new ArrayList<>(ServerlessDistributedBackupService.systemData.getCommunityBackedUpChunks()));
        ArrayList<Integer> chunksSizes = new ArrayList<>();
        ArrayList<String> chunksStringIds = new ArrayList<>();
        ArrayList<BigInteger> chunksNumericIds = new ArrayList<>();
        fillListsOfChunksInfo(communityBackedUpChunks, chunksSizes, chunksStringIds, chunksNumericIds);
        int[] chunksSizesArray = new int[chunksSizes.size()];
        String[] chunkStringsIdsArray = new String[chunksStringIds.size()];
        BigInteger[] chunksNumericIdsArray = new BigInteger[chunksNumericIds.size()];
        for (int i = 0; i < chunksSizes.size(); i++)
        {
            chunksSizesArray[i] = chunksSizes.get(i);
        }
        for (int i = 0; i < chunksStringIds.size(); i++)
        {
            chunkStringsIdsArray[i] = chunksStringIds.get(i);
        }
        for (int i = 0; i < chunksNumericIds.size(); i++)
        {
            chunksNumericIdsArray[i] = chunksNumericIds.get(i);
        }



        //TCPClientMessenger messenger = new TCPClientMessenger(predecessor.getMessageChannelAddress(), predecessor.getMessageChannelPort());

        int dataPort;
        try
        {
            dataPort = ServerlessDistributedBackupService.availablePorts.take();
        }
        catch (InterruptedException e)
        {
            Logging.FatalErrorLog("Error getting port for data transfer from available Ports on SuccessorEntryRequestHandler to transfer the keys");
            return;
        }

        String selfHashString = ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getHashedIdentifier();
        BigInteger selfNumericHash = ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getNumericIdentifier();

        ChordNodeIdentifier previousPredecessor = ServerlessDistributedBackupService.systemData.getChordNode().getPredecessor();
        SuccessorEntryNotificationResponseChordMessage successorEntryNotificationResponseChordMessage = new SuccessorEntryNotificationResponseChordMessage(
                selfHashString, selfNumericHash, chunksSizesArray,
                chunkStringsIdsArray, chunksNumericIdsArray, ServerlessDistributedBackupService.address, dataPort, previousPredecessor);

        Logging.Log("sending SuccessorEntryNotificationResponseChordMessage, thread:" + Thread.currentThread().getId());
        messenger.writeObject(successorEntryNotificationResponseChordMessage);


        ChordNodeIdentifier predecessor = new ChordNodeIdentifier(newPredecessorNumericIdentifier, newPredecessorHashedIdentifier, address, messagePort);
        ServerlessDistributedBackupService.systemData.getChordNode().setPredecessor(predecessor);
        ServerlessDistributedBackupService.systemData.getChordNode().update(predecessor);
        if (chunksSizesArray.length == 0)
        {
            messenger.closeSocket();
            try
            {
                ServerlessDistributedBackupService.availablePorts.put(dataPort);
            } catch (InterruptedException e)
            {
                Logging.FatalErrorLog("Error putting back the port for data transfer from available Ports on SuccessorEntryRequestHandler to transfer the keys");
                return;
            }
            return;
        }


        try
        {
            TCPServerMessenger sslServerMessenger = new TCPServerMessenger(dataPort);
            Logging.Log("accepting connection to send keys to predecessor, thread:" + Thread.currentThread().getId());
            sslServerMessenger.acceptConnection();
            if(PeerKeysTransferrer.sendKeysAndDeleteThemFromDisk(sslServerMessenger, chunksStringIds) == false)
            {
                sslServerMessenger.closeSocket();
                return;
            }
            sslServerMessenger.closeSocket();

            Logging.Log("PeerKeysTransferChordMessageACK, thread:" + Thread.currentThread().getId());
            PeerKeysTransferChordMessageACK ack = (PeerKeysTransferChordMessageACK) messenger.readObject();
            if(ack.getHashedIdentifier().equals(message.getHashedIdentifier()) == false ||
                    ack.getNumericIdentifier().equals(message.getNumericIdentifier()) == false)
            {
                Logging.LogError("Transfer ownership failed");
            }
            messenger.closeSocket();
        }
        catch (IOException e)
        {
            Logging.FatalErrorLog("Error opening connection to send chunks to succesor");
        }
        catch (ClassNotFoundException e)
        {
            Logging.FatalErrorLog("Error (ClassNotFoundException) on ack message from RequestTransferOwnership");
        }

        messenger.closeSocket();
        try
        {
            ServerlessDistributedBackupService.availablePorts.put(dataPort);
        } catch (InterruptedException e)
        {
            Logging.FatalErrorLog("Error putting back the port for data transfer from available Ports on SuccessorEntryRequestHandler to transfer the keys");
            return;
        }
    }

    private void fillListsOfChunksInfo(List<Chunk> communityBackedUpChunks, ArrayList<Integer> chunksSizes, ArrayList<String> chunksStringIds, ArrayList<BigInteger> chunksNumericIds)
    {
        for (Chunk chunk : communityBackedUpChunks)
        {
            BigInteger selfId = ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getNumericIdentifier();
            BigInteger newSuccessorId = message.getNumericIdentifier();
            BigInteger chunkNumericId = chunk.getId().getNumericIdentifier();
            BigInteger predecessorId = ServerlessDistributedBackupService.systemData.getChordNode().getPredecessor().getNumericIdentifier();

            //chunkNumericId is less than newSuccessorId or my predecessor is higher than me and that means a total lap has been given to chord circle
            if (chunkNumericId.compareTo(newSuccessorId) < 0 || predecessorId.compareTo(selfId) >= 0)
            {
                    //chunkNumericId is equal or higher than predecessorId or my predecessor is higher than me and that means a total lap has been given to chord circle
                if (chunkNumericId.compareTo(predecessorId) >= 0 || predecessorId.compareTo(selfId) >= 0)
                {
                    //if both statements are true, the chunk must be sent to the new predecessor
                    chunksSizes.add(chunk.getChunkSize());
                    chunksStringIds.add(chunk.getId().getHashedIdentifier());
                    chunksNumericIds.add(chunkNumericId);
                    ServerlessDistributedBackupService.systemData.removeChunkFromCommunityBackedUpChunks(chunk);
                }
            }
        }
    }

    private int[] getKeySizes()
    {
        List<Chunk> communityBackedUpChunks = ServerlessDistributedBackupService.systemData.getCommunityBackedUpChunks();
        int keySizes[] = new int[communityBackedUpChunks.size()];
        int i = 0;
        for(Chunk chunk : communityBackedUpChunks)
        {
            keySizes[i] = chunk.getChunkSize();
            i++;
        }

        return keySizes;
    }

}
