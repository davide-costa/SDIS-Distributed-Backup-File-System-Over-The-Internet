package Protocols.Requesters.P2PRequesters;

import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Messaging.P2PMessages.BackupRequestP2PMessage;
import Messaging.P2PMessages.BackupResponseP2PMessage;
import Protocols.Chunk;
import Protocols.Requesters.ChordRequesters.RequestLookup;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPServerMessenger;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestBackupChunk implements Runnable
{
    private Chunk chunk;
    private byte[] chunkData;
    private final static int MAX_NUM_OF_RETRIES = 5;

    public RequestBackupChunk(Chunk chunk, byte[] chunkData)
    {
        this.chunk = chunk;
        this.chunkData = chunkData;
    }

    public Chunk getChunk()
    {
        return chunk;
    }

    public void setChunk(Chunk chunk)
    {
        this.chunk = chunk;
    }

    public byte[] getChunkData()
    {
        return chunkData;
    }

    public void setChunkData(byte[] chunkData)
    {
        this.chunkData = chunkData;
    }

    @Override
    public void run()
    {
        List<ChordKey> peersIds = chunk.getBackupPeersIds();
        ExecutorService executor = Executors.newFixedThreadPool(peersIds.size());
        for (int i = 0; i < peersIds.size(); i++)
        {
            RequestBackupChunkToPeer peerRequester = new RequestBackupChunkToPeer(peersIds.get(0), chunk);
            Logging.Log("Starting thread pool to request backup to each peer, instances of RequestBackupChunkToPeer class");
            executor.execute(peerRequester);
            Logging.Log("thread pool to request backup to each peer, instances of RequestBackupChunkToPeer class started");

        }
        try
        {
            executor.awaitTermination(20000, TimeUnit.MILLISECONDS);
            Logging.Log("awating termination of thread pool to request backup to each peer, instances of RequestBackupChunkToPeer");

            if(chunk.getCurrReplicationDegree() > 1)
            {
                if(chunk.getCurrReplicationDegree() >= chunk.getExpectedReplicationDegree())
                    ServerlessDistributedBackupService.systemData.addToOwnerBackedUpChunks(chunk);
                else
                    Logging.LogError("Chunk " + chunk.getId().getNumericIdentifier() + " has not been backed up with expected replication degree" +
                            ", replication achieved was: " + chunk.getCurrReplicationDegree());
            }

        } catch (InterruptedException e)
        {
            Logging.LogError("Not all threads the request the backup for each peer for the chunk with id " + chunk.getId() + " terminated successfully");
        }
    }

    private BackupResponseP2PMessage readBackupResponseP2PMessage(TCPClientMessenger messenger)
    {
        try
        {
            return (BackupResponseP2PMessage) messenger.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e)
        {
            Logging.LogError("Error reading backup response message on chunk " + chunk.getId() + ".");
            return null;
        }
    }

    public class RequestBackupChunkToPeer implements Runnable
    {
        ChordKey peerId;
        private Chunk chunk;

        public RequestBackupChunkToPeer(ChordKey peerId, Chunk chunk)
        {
            this.peerId = peerId;
            this.chunk = chunk;
        }

        @Override
        public void run()
        {
            Logging.Log("Backup chunk with id " + chunk.getId().getNumericIdentifier() + "!!!!");
            RequestLookup requestLookup = new RequestLookup(chunk.getId(), ServerlessDistributedBackupService.systemData.getChordNode().getSelf());
            if (!requestLookup.sendRequest())
                Logging.LogError("Error doing lookup for initiating backup to peer");
            LookupResponseChordMessage lookupResult = requestLookup.getRequestResponse();


            //TODO DEBIG
            Logging.Log("Starting Backup Chunk request to specific peer ");
            Logging.Log("Finger table");
            ChordNodeIdentifier[] fingerTable = ServerlessDistributedBackupService.systemData.getChordNode().getFingerTable();
            for(int i = 0; i < fingerTable.length; i++)
            {
                Logging.Log(i + ": " + fingerTable[i].getNumericIdentifier());
            }



            TCPClientMessenger messenger = getBackupPeerMessenger();
            if(messenger == null)
                return;

            int dataPort;
            try
            {
                dataPort = ServerlessDistributedBackupService.availablePorts.take();
            } catch (InterruptedException e)
            {
                Logging.LogError("Failed to backup chunk with id " + this.chunk.getId() + " because the take from the available ports was interrupted.");
                return;
            }

            Logging.Log("sending BackupRequestP2PMessage");
            if (!sendBackupRequestP2PMessage(messenger, ServerlessDistributedBackupService.address, dataPort))
            {
                Logging.LogError("Failed to backup chunk with id " + RequestBackupChunk.this.chunk.getId());
                return;
            }

            Logging.Log("sendChunk");
            if (!sendChunk(dataPort))
            {
                Logging.LogError("Failed to backup chunk with id " + RequestBackupChunk.this.chunk.getId());
                return;
            }

            Logging.Log("readAckBackupMessage");
            //read final ack message
            if (!readAckBackupMessage(messenger))
                return;
            messenger.closeSocket();

            Logging.Log("storeInformationAboutPeersWhoStoredTheChunk");
            chunk.incrementCurrReplicationDegree();
        }

        private TCPClientMessenger getBackupPeerMessenger()
        {
            ChordNode self = ServerlessDistributedBackupService.systemData.getChordNode();
            List<ChordKey> backupPeersIds = chunk.getBackupPeersIds();
            for(ChordKey peerKey : backupPeersIds)
            {
                RequestLookup requestLookup = new RequestLookup(peerKey, self.getClosestNode(peerKey));
                requestLookup.sendRequest();
                LookupResponseChordMessage lookupResponseChordMessage = requestLookup.getRequestResponse();
                TCPClientMessenger messenger;
                try
                {
                    messenger = new TCPClientMessenger(lookupResponseChordMessage.getNodeMessageChannelAddress(),
                            lookupResponseChordMessage.getNodeMessageChannelPort());
                }
                catch (IOException e)
                {
                    Logging.LogError("Failed to restore chunk with id " + RequestBackupChunk.this.chunk.getId()
                            + "from " + peerKey.getHashedIdentifier());
                    continue;
                }

                return messenger;
            }

            return null;
        }

        private boolean sendBackupRequestP2PMessage(TCPClientMessenger messenger, InetAddress dataAddress, int dataPort)
        {
            BackupRequestP2PMessage requestMessage = new BackupRequestP2PMessage(dataAddress, dataPort, chunk.getId(), chunkData.length);

            boolean writeSuccessful = messenger.writeObject(requestMessage);
            int numRetries = 0;
            while (!writeSuccessful && numRetries < MAX_NUM_OF_RETRIES)
            {
                writeSuccessful = messenger.writeObject(requestMessage);
                numRetries++;
            }

            return numRetries < MAX_NUM_OF_RETRIES;
        }

        private boolean readAckBackupMessage(TCPClientMessenger messenger)
        {
            BackupResponseP2PMessage backupAcceptBackupMessage = readBackupResponseP2PMessage(messenger);
            if (backupAcceptBackupMessage == null)
                return false;
            if (!backupAcceptBackupMessage.succeeded())
            {
                Logging.LogError("Failed to backup chunk with id " + this.chunk.getId());
                return false;
            }
            return true;
        }

        private boolean sendChunk(int dataPort)
        {
            int numRetries = 0;
            try
            {
                TCPServerMessenger dataMessenger = new TCPServerMessenger(dataPort);
                dataMessenger.acceptConnection();
                boolean writeSuccessful = dataMessenger.writeMessage(chunkData);
                while (!writeSuccessful && numRetries < MAX_NUM_OF_RETRIES)
                {
                    writeSuccessful = dataMessenger.writeMessage(chunkData);
                    numRetries++;
                }
                dataMessenger.closeSocket();
            } catch (IOException e)
            {
                Logging.LogError("Failed to backup chunk with id " + this.chunk.getId() + " because could not create server socket for data connection");
                return false;
            }
            try
            {
                ServerlessDistributedBackupService.availablePorts.put(dataPort);
            } catch (InterruptedException e)
            {
                Logging.LogError("Failed to backup chunk with id " + this.chunk.getId() + " because the put to the available ports was interrupted.");
                return false;
            }
            return numRetries < MAX_NUM_OF_RETRIES;
        }
    }
}
