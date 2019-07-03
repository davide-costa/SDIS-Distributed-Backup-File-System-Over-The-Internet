package Protocols;

import Chord.ChordNode;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class SystemManagement implements Serializable
{
    private static final long serialVersionUID = 4274720516046778792L;
    private static final int GIGABYTE = 1024 * 1024 * 1024;

    private ChordNode chordNode;
    private List<String> ownerBackedUpFilePaths = Collections.synchronizedList(new ArrayList<>());
    private List<Chunk> ownerBackedUpChunks = Collections.synchronizedList(new ArrayList<>());
    private List<Chunk> communityBackedUpChunks = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<String, ArrayList<Chunk>> ownerBackedUpFilepathToChunks = new ConcurrentHashMap<>();
    private AtomicLong currBackedUpSpace = new AtomicLong(0);
    private AtomicLong currCommunityBackedUpSpace = new AtomicLong(0);
    private AtomicLong maxAmountDiskSpaceForBackup = new AtomicLong(5 * GIGABYTE);


    public List<String> getOwnerBackedUpFilePaths()
    {
        return ownerBackedUpFilePaths;
    }

    public boolean ownerBackedUpFilePathsContainsFilePath(String filepath)
    {
        return ownerBackedUpFilePaths.contains(filepath);
    }

    public void setOwnerBackedUpFilePaths(List<String> ownerBackedUpFilePaths)
    {
        this.ownerBackedUpFilePaths = ownerBackedUpFilePaths;
    }

    public void addToOwnerBackedUpFilePaths(String filepath)
    {
        this.ownerBackedUpFilePaths.add(filepath);
    }

    public ArrayList<Chunk> getOwnerBackedUpChunkByFileId(String fileId)
    {
        return ownerBackedUpFilepathToChunks.get(fileId);
    }

    public void addToOwnerBackedUpFilepathToChunks(String filepath, ArrayList<Chunk> ownerBackedUpChunks)
    {
        this.ownerBackedUpFilepathToChunks.put(filepath, ownerBackedUpChunks);
    }

    public List<Chunk> getOwnerBackedUpChunks()
    {
        return ownerBackedUpChunks;
    }

    public List<Chunk> getOwnerBackedUpChunksOfFile(String filepath)
    {
        return ownerBackedUpFilepathToChunks.get(filepath);
    }

    public ConcurrentHashMap<String, ArrayList<Chunk>> getOwnerBackedUpFilepathToChunks()
    {
        return ownerBackedUpFilepathToChunks;
    }

    public void setOwnerBackedUpChunks(ArrayList<Chunk> ownerBackedUpChunks)
    {
        this.ownerBackedUpChunks = ownerBackedUpChunks;

        //reset currBackedUpSpace
        currBackedUpSpace.set(0);
        for (int i = 0; i < ownerBackedUpChunks.size(); i++)
            currBackedUpSpace.getAndAdd(ownerBackedUpChunks.get(i).getChunkSize());
    }

    public void removeOwnerBackedUpChunksOfFile(String filepath)
    {
        ownerBackedUpFilePaths.remove(filepath);
        ArrayList<Chunk> chunksRemoved = ownerBackedUpFilepathToChunks.remove(filepath);
        for(Chunk chunk : chunksRemoved)
        {
            for(Chunk backedUpChunk : ownerBackedUpChunks)
            {
                if(backedUpChunk.getId().getHashedIdentifier().equals(chunk.getId().getHashedIdentifier()))
                {
                    ownerBackedUpChunks.remove(backedUpChunk);
                    break;
                }
            }
        }
    }

    public void addToOwnerBackedUpChunks(Chunk ownerBackedUpChunk)
    {
        this.ownerBackedUpChunks.add(ownerBackedUpChunk);
        this.currBackedUpSpace.getAndAdd(ownerBackedUpChunk.getChunkSize());
    }

    public List<Chunk> getCommunityBackedUpChunks()
    {
        return communityBackedUpChunks;
    }

    public Chunk getCommunityBackedUpChunkById(String chunkId)
    {
        for (int i = 0; i < this.communityBackedUpChunks.size(); i++)
        {
            String currChunkId = this.communityBackedUpChunks.get(i).getId().getHashedIdentifier();
            if (currChunkId.equals(chunkId))
                return this.communityBackedUpChunks.get(i);
        }

        return null;
    }

    public void setCommunityBackedUpChunks(ArrayList<Chunk> communityBackedUpChunks)
    {
        this.communityBackedUpChunks = communityBackedUpChunks;

        //reset currCommunityBackedUpSpace
        currCommunityBackedUpSpace = new AtomicLong(0);
        for (int i = 0; i < communityBackedUpChunks.size(); i++)
            currCommunityBackedUpSpace.getAndAdd(communityBackedUpChunks.get(i).getChunkSize());
    }

    public void addToCommunityBackedUpChunks(Chunk chunk)
    {
        this.communityBackedUpChunks.add(chunk);
        this.incrementCurrCummunityBackedUpSpace(chunk.getChunkSize());
    }

    public void removeChunkFromCommunityBackedUpChunks(Chunk chunk)
    {
        this.communityBackedUpChunks.remove(chunk);
        this.decrementCurrCummunityBackedUpSpace(chunk.getChunkSize());
    }

    public synchronized void removeChunksFromCommunityBackedUpChunks(ArrayList<String> chunksIds)
    {
        for(String chunkId : chunksIds)
        {
            for(Chunk communityChunk : communityBackedUpChunks)
            {
                if(communityChunk.getIdString().equals(chunkId))
                {
                    communityBackedUpChunks.remove(communityChunk);
                    break;
                }
            }
        }
    }

    public void removeChunkFromCommunityBackedUpChunksByChunkId(String chunkId)
    {
        Chunk chunkToRemove = this.getCommunityBackedUpChunkById(chunkId);
        this.removeChunkFromCommunityBackedUpChunks(chunkToRemove);
    }

    public void removeChunkFromCommunityBackedUpChunksByIndex(int chunkIndex)
    {
        Chunk chunkRemoved = this.communityBackedUpChunks.remove(chunkIndex);
        this.decrementCurrCummunityBackedUpSpace(chunkRemoved.getChunkSize());
    }

    public void removeChunkFromCommunityBackedUpChunksByIterator(Iterator<Chunk> it, Chunk chunk)
    {
        this.decrementCurrCummunityBackedUpSpace(chunk.getChunkSize());
        it.remove();
    }

    public List<Chunk> removeAllChunksFromCommunityBackedUpChunksByFileId(String fileId)
    {
        for (int i = 0; i < communityBackedUpChunks.size(); i++)
        {
            Chunk chunk = communityBackedUpChunks.get(i);
            if (chunk.getId().getHashedIdentifier().equals(fileId))
            {
                communityBackedUpChunks.remove(chunk);
                this.decrementCurrCummunityBackedUpSpace(chunk.getChunkSize());
                i--;
            }
        }

        return  communityBackedUpChunks;
    }

    public long getMaxAmountDiskSpaceForBackup()
    {
        return maxAmountDiskSpaceForBackup.get();
    }

    public void setMaxAmountDiskSpaceForBackup(long maxAmountDiskSpaceForBackup)
    {
        this.maxAmountDiskSpaceForBackup = new AtomicLong(maxAmountDiskSpaceForBackup);
    }

    public long getCurrBackedUpSpace()
    {
        return currBackedUpSpace.get();
    }

    public void setCurrBackedUpSpace(long currBackedUpSpace)
    {
        this.currBackedUpSpace.set(currBackedUpSpace);
    }

    public void incrementCurrBackedUpSpace(long increment)
    {
        this.currBackedUpSpace.getAndAdd(increment);
    }

    public void decrementCurrBackedUpSpace(long decrement)
    {
        this.currBackedUpSpace.getAndAdd(-decrement);
    }

    public long getCurrCommunityBackedUpSpace()
    {
        return currCommunityBackedUpSpace.get();
    }

    public void setCurrCommunityBackedUpSpace(long currCommunityBackedUpSpace)
    {
        this.currCommunityBackedUpSpace.set(currCommunityBackedUpSpace);
    }

    public void incrementCurrCummunityBackedUpSpace(long increment)
    {
        this.currCommunityBackedUpSpace.getAndAdd(increment);
    }

    public void decrementCurrCummunityBackedUpSpace(long decrement)
    {
        this.currCommunityBackedUpSpace.getAndAdd(-decrement);
    }

    public long getAvailableSpaceForBackup()
    {
        return maxAmountDiskSpaceForBackup.get() - currCommunityBackedUpSpace.get();
    }

    public long getSystemOccupationPercentage()
    {
        return currCommunityBackedUpSpace.get() * 100 / maxAmountDiskSpaceForBackup.get();
    }

    public void saveSystemInfo()
    {
        try
        {
            File systemDataFile = new File(ServerlessDistributedBackupService.systemDataFilePath);
            if (!systemDataFile.exists())
                systemDataFile.createNewFile();
            FileOutputStream fileOut = new FileOutputStream(systemDataFile, false);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        } catch (IOException i)
        {
            i.printStackTrace();
        }
    }

    public String getSystemData()
    {
        String systemData = "System data:\n\n";

        //peer info
        systemData += "\tCurrently you have backed up " + ownerBackedUpChunks.size() + " chunks in the community.\n";
        systemData += "\tTotal of " + currBackedUpSpace.get() / 1000 + " Kbytes backed up.\n";

        systemData += "\n\n";

        //community info
        systemData += "\tCurrently are stored " + communityBackedUpChunks.size() + " chunks.\n";
        systemData += "\tTotal of " + currCommunityBackedUpSpace.get() / 1000 + " Kbytes stored.\n";
        systemData += "\tAt " + getSystemOccupationPercentage() + "% capacity.";

        return systemData;
    }

    public ScheduledExecutorService initiatePeriodicSaver(int period)
    {
        ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable retryTask = SystemManagement.this::saveSystemInfo;
        retryScheduler.scheduleAtFixedRate(retryTask, period, period, TimeUnit.SECONDS);

        return retryScheduler;
    }

    public ChordNode getChordNode()
    {
        return chordNode;
    }

    public void setChordNode(ChordNode chordNode)
    {
        this.chordNode = chordNode;
    }

}
