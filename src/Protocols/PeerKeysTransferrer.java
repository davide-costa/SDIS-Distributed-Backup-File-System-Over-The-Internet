package Protocols;

import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.OnLeaveTransferOwnershipChordMessage;
import Messaging.ChordMessages.PeerKeysTransferChordMessageACK;
import Messaging.ChordMessages.PeerKeysTransferChordMessage;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPMessenger;
import TCPMessengers.TCPServerMessenger;
import Utils.FileUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PeerKeysTransferrer
{
    public static void sendKeysAndDeleteThemFromDisk(ChordNodeIdentifier chordNodeSuccessor, ChordNodeIdentifier chordNodePredecessor)
    {
        ChordNode chordNode = ServerlessDistributedBackupService.systemData.getChordNode();
        TCPClientMessenger messenger = null;
        try
        {
            messenger = new TCPClientMessenger(chordNodeSuccessor.getMessageChannelAddress(),
                    chordNodeSuccessor.getMessageChannelPort());
        }
        catch (IOException e)
        {
            Logging.LogError("Failed to open connection to " + chordNodeSuccessor.getMessageChannelAddress()
                    + ":" + chordNodePredecessor.getMessageChannelPort() + ", to send keys to successor");
            return;
        }

        ArrayList<ChordKey> chordKeys = chordNode.getChordTableKeysList();
        String[] keyHashedIdentifiers = new String[chordKeys.size()];
        BigInteger[] keyNumericIdentifiers = new BigInteger[chordKeys.size()];

        int i = 0;
        for(ChordKey chordKey : chordKeys)
        {
            keyHashedIdentifiers[i] = chordKey.getHashedIdentifier();
            keyNumericIdentifiers[i] = chordKey.getNumericIdentifier();
            i++;
        }
        int keySizes[] = getKeySizes();
        int port = 0;
        try
        {
            port = ServerlessDistributedBackupService.availablePorts.take();
        }
        catch (InterruptedException e)
        {
            Logging.FatalErrorLog("Error getting data Port from available Ports");
        }

        OnLeaveTransferOwnershipChordMessage onLeaveTransferOwnershipChordMessage = new OnLeaveTransferOwnershipChordMessage(
                chordNodePredecessor.getHashedIdentifier(), chordNodePredecessor.getNumericIdentifier(), keySizes,
                keyHashedIdentifiers, keyNumericIdentifiers, ServerlessDistributedBackupService.address, port);
        messenger.writeObject(onLeaveTransferOwnershipChordMessage);


        try
        {
            TCPServerMessenger sslServerMessenger = new TCPServerMessenger(port);
            sslServerMessenger.acceptConnection();
            ArrayList<String> chunksIds = getChunksIdsOfChunks();
            if(sendKeysAndDeleteThemFromDisk(sslServerMessenger, chunksIds) == false)
            {
                sslServerMessenger.closeSocket();
                return;
            }
            sslServerMessenger.closeSocket();

            PeerKeysTransferChordMessageACK ack = (PeerKeysTransferChordMessageACK) messenger.readObject();
            if(ack.getHashedIdentifier().equals(chordNodeSuccessor.getHashedIdentifier()) == false ||
                    ack.getNumericIdentifier().equals(chordNodeSuccessor.getNumericIdentifier()) == false)
            {
                Logging.LogError("Transfer ownership failed");
            }
            messenger.closeSocket();
        }
        catch (IOException e)
        {
            Logging.FatalErrorLog("Error opening connection to send chunks to successor");
        }
        catch (ClassNotFoundException e)
        {
            Logging.FatalErrorLog("Error (ClassNotFoundException) on ack message from RequestTransferOwnership");
        }

        try
        {
            ServerlessDistributedBackupService.availablePorts.put(port);
        }
        catch (InterruptedException e)
        {
            Logging.FatalErrorLog("Error getting data Port from available Ports");
        }

    }

    private static ArrayList<String> getChunksIdsOfChunks()
    {
        List<Chunk> chunks = ServerlessDistributedBackupService.systemData.getCommunityBackedUpChunks();
        ArrayList<String> chunksIds = new ArrayList<>();
        for (Chunk chunk : chunks)
        {
            chunksIds.add(chunk.getId().getHashedIdentifier());
        }
        return chunksIds;
    }

    public static void receiveKeys(PeerKeysTransferChordMessage chordMessage, TCPMessenger messenger)
    {
        InetAddress addr = chordMessage.getDataTransferChannelAddress();
        int port = chordMessage.getDataTransferChannelPort();
        int[] chunksSizes = chordMessage.getKeySizes();
        if(chunksSizes.length == 0)
        {
            PeerKeysTransferChordMessageACK ack = new PeerKeysTransferChordMessageACK(
                    ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getHashedIdentifier(),
                    ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getNumericIdentifier());
            messenger.writeObject(ack);
            messenger.closeSocket();
            return;
        }
        BigInteger[] chunksNumericIds = chordMessage.getKeyNumericIdentifiers();
        String[] chunksStringIds = chordMessage.getKeyHashedIdentifiers();
        TCPClientMessenger dataMessenger = null;
        try
        {
            dataMessenger = new TCPClientMessenger(addr, port);
        } catch (IOException e)
        {
            Logging.LogError("Error on transferring keys between peers");
            e.printStackTrace();
        }

        Logging.Log("PeerKeysTransferrer - receiving chunks data, thread:" + Thread.currentThread().getId());
        ArrayList<byte[]> chunksData = new ArrayList<>();
        for (int chunkSize : chunksSizes)
        {
            byte[] chunkData = dataMessenger.readMessage(chunkSize);
            chunksData.add(chunkData);
        }
        ArrayList<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < chunksData.size(); i++)
        {
            BigInteger chunkNumericId = chunksNumericIds[i];
            String chunkStringId = chunksStringIds[i];
            ChordKey chordKey = new ChordKey(chunkNumericId, chunkStringId);
            int chunkSize = chunksSizes[i];
            Chunk chunk = new Chunk(chordKey, chunkSize);
            chunks.add(chunk);
        }

        for (int i = 0; i < chunks.size(); i++)
        {
            byte[] chunkData = chunksData.get(i);
            if (chunkData == null) //only add the chunk to the community backed up chunk internal info if it has been successfully received from the peer
                continue;
            Chunk chunk = chunks.get(i);
            ServerlessDistributedBackupService.systemData.addToCommunityBackedUpChunks(chunk);
            ServerlessDistributedBackupService.systemData.getChordNode().addToKeyTable(chunk.getIdString(), chunk.getId());
            String chunkFilepath = ServerlessDistributedBackupService.chunksPath + chunk.getIdString();
            FileUtils.saveFileIntoDisk(chunkFilepath, chunkData);
        }
        dataMessenger.closeSocket();

        Logging.Log("PeerKeysTransferrer - sending PeerKeysTransferChordMessageACK, thread:" + Thread.currentThread().getId());
        //send ack
        PeerKeysTransferChordMessageACK ack = new PeerKeysTransferChordMessageACK(
                ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getHashedIdentifier(),
                ServerlessDistributedBackupService.systemData.getChordNode().getSelf().getNumericIdentifier());
        messenger.writeObject(ack);

        messenger.closeSocket();
    }

    public static boolean sendKeysAndDeleteThemFromDisk(TCPServerMessenger messenger, ArrayList<String> chunksStringIds)
    {
        Logging.Log("PeerKeysTransferrer -sendKeysAndDeleteThemFromDisk begin, thread:" + Thread.currentThread().getId());
        for(String chunkId : chunksStringIds)
        {
            String chunkFilepath = ServerlessDistributedBackupService.chunksPath + chunkId;
            byte chunkData[] = FileUtils.readFileFromDisk(chunkFilepath);
            FileUtils.deleteFileFromDisk(chunkFilepath);
            if (chunkData == null)
            {
                Logging.LogError("Error reading chunks from disk to send keys (chunks) to successor");
                return false;
            }
            try
            {
                messenger.writeMessage(chunkData);
            }
            catch (IOException e)
            {
                Logging.FatalErrorLog("Error sending chunks to successor");
                return false;
            }
        }
        Logging.Log("PeerKeysTransferrer -sendKeysAndDeleteThemFromDisk end, thread:" + Thread.currentThread().getId());

        ServerlessDistributedBackupService.systemData.removeChunksFromCommunityBackedUpChunks(chunksStringIds);
        ServerlessDistributedBackupService.systemData.getChordNode().removeKeysFromKeyTableByChunkId(chunksStringIds);
        return  true;
    }

    private static int[] getKeySizes()
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
