package Protocols;

import Chord.ChordKey;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Protocols.Requesters.ChordRequesters.RequestLookup;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Chunk implements Comparable<Object>, Serializable
{
    private static final long serialVersionUID = -1803682345293214003L;
    private ChordKey id;
    private int chunkSize;
    private int expectedReplicationDegree;
    private int currReplicationDegree = 0;
    private List<ChordKey> backupPeersIds = Collections.synchronizedList(new ArrayList<>());


    public Chunk(ChordKey id, int chunkSize)
    {
        this.id = id;
        this.chunkSize = chunkSize;
    }

    public Chunk(ChordKey id, int chunkSize, int expectedReplicationDegree)
    {
        this.id = id;
        this.chunkSize = chunkSize;
        this.expectedReplicationDegree = expectedReplicationDegree;
    }

    public Chunk(ChordKey id, int chunkSize, int expectedReplicationDegree, List<ChordKey> backupPeersIds)
    {
        this.id = id;
        this.chunkSize = chunkSize;
        this.expectedReplicationDegree = expectedReplicationDegree;
        this.backupPeersIds = backupPeersIds;
    }

    public Chunk(Chunk chunk)
    {
        this.id = chunk.getId();
        this.chunkSize = chunk.getChunkSize();
        this.expectedReplicationDegree = chunk.getExpectedReplicationDegree();
        this.currReplicationDegree = chunk.getCurrReplicationDegree();
        this.backupPeersIds = chunk.getBackupPeersIds();
    }

    public ChordKey getId()
    {
        return id;
    }

    public String getIdString()
    {
        return id.getHashedIdentifier();
    }

    public void setId(ChordKey id)
    {
        this.id = id;
    }

    public int getChunkSize()
    {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize)
    {
        this.chunkSize = chunkSize;
    }

    public int getExpectedReplicationDegree()
    {
        return expectedReplicationDegree;
    }

    public void setExpectedReplicationDegree(int expectedReplicationDegree)
    {
        this.expectedReplicationDegree = expectedReplicationDegree;
    }

    public int getCurrReplicationDegree()
    {
        return currReplicationDegree;
    }

    public void setCurrReplicationDegree(int currReplicationDegree)
    {
        this.currReplicationDegree = currReplicationDegree;
    }

    public List<ChordKey> getBackupPeersIds()
    {
        return backupPeersIds;
    }

    public void setBackupPeersIds(ArrayList<ChordKey> backupPeersIds)
    {
        this.backupPeersIds = backupPeersIds;
    }

    public void addBackupPeers(ChordKey backupPeerId)
    {
        this.backupPeersIds.add(backupPeerId);
    }

    public boolean alreadyContainsBackupPeer(String backupPeerId)
    {
        return backupPeersIds.contains(backupPeerId);
    }

    public int getNumBackupPeers()
    {
        return backupPeersIds.size();
    }

    @Override
    public int compareTo(Object object)
    {
        return id.getHashedIdentifier().compareTo(((Chunk) object).getId().getHashedIdentifier());
    }

    public void incrementCurrReplicationDegree()
    {
        currReplicationDegree++;
    }

    public ArrayList<InetSocketAddress> getBackupPeersAddressesAndPorts()
    {
        ArrayList<InetSocketAddress> peersAddressesAndPorts = new ArrayList<>();

        for(ChordKey chordKey : backupPeersIds)
        {
            RequestLookup requestLookup = new RequestLookup(chordKey, ServerlessDistributedBackupService.systemData.getChordNode().getSelf());
            requestLookup.sendRequest();
            LookupResponseChordMessage responseChordMessage = requestLookup.getRequestResponse();
            peersAddressesAndPorts.add(new InetSocketAddress(responseChordMessage.getNodeMessageChannelAddress(),
                    responseChordMessage.getNodeMessageChannelPort()));
        }

        return peersAddressesAndPorts;
    }
}
