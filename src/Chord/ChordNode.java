package Chord;

import Protocols.Chunk;
import Protocols.ServerlessDistributedBackupService;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChordNode implements Serializable
{

    private static final short mBits = 256;

    protected ChordNodeIdentifier predecessor;
    protected ChordNodeIdentifier self;
    protected ChordNodeIdentifier successor;

    protected ChordNodeIdentifier[] fingerTable;
    protected BigInteger[] expectedFingerTableIDs;
    protected HashMap<String, ChordKey> keyTable;

    public ChordNode(ChordNodeIdentifier predecessor, ChordNodeIdentifier self, ChordNodeIdentifier successor)
    {
        this.predecessor = predecessor;
        this.self = self;
        this.successor = successor;

        fingerTable = new ChordNodeIdentifier[mBits];
        for (int i = 0; i < this.fingerTable.length; ++i)
        {
            this.fingerTable[i] = new ChordNodeIdentifier(this.self.numericIdentifier, this.self.hashedIdentifier, this.self.messageChannelAddress, this.self.messageChannelPort);
        }

        expectedFingerTableIDs = new BigInteger[mBits];
        keyTable = new HashMap<>();
        this.initializeExpectedFingerTableValues();
    }

    private void initializeExpectedFingerTableValues()
    {
        BigInteger base = this.self.numericIdentifier;
        for (int i = 0; i < mBits; ++i)
        {
            //n
            BigInteger result = base;
            //2^i
            BigInteger increment = new BigInteger("2".getBytes());
            increment.pow(i);
            //(n+2^i)
            result.add(increment);
            //(n+2^i) mod (2^m)
            BigInteger modulos = new BigInteger("2".getBytes());
            modulos.pow(mBits);
            this.expectedFingerTableIDs[i] = result.mod(modulos);
        }
    }

    /**
     * Looks for the node that is closest to the node responsible for a given key
     *
     * @param key key to be looked up
     * @return closest chord identifier to the supplied key
     */
    public ChordNodeIdentifier getClosestNode(ChordKey key)
    {
        if (key.getHashedIdentifier().equals(self.getHashedIdentifier()))
        {
            return self;
        }

        ChordNodeIdentifier ret = self;
        BigInteger compare = ret.numericIdentifier;
        BigInteger val = compare.subtract(key.numericIdentifier).abs();

        for (int i = 0; i < this.fingerTable.length; ++i)
        {
            BigInteger valToCompare = this.fingerTable[i].numericIdentifier;
            BigInteger diff = valToCompare.subtract((key.numericIdentifier)).abs();
            int comp = diff.compareTo(val);
            if (comp == 0)//is equal
            {
                ret = this.fingerTable[i];

            } else if (comp == 1)//is larger
            {
                continue;
            } else if (comp == -1)//is smaller
            {
                ret = this.fingerTable[i];
                val = diff;
            }
        }

        return ret;
    }

    public void update(ChordNodeIdentifier node)
    {
        for (int i = 0; i < this.expectedFingerTableIDs.length; ++i)
        {
            BigInteger expected = this.expectedFingerTableIDs[i];
            int comp = expected.compareTo(node.getNumericIdentifier());
            if (comp == 0) //equal
            {
                this.expectedFingerTableIDs[i] = node.getNumericIdentifier();
            } else if (comp == 1) //higher than
            {
                ChordNodeIdentifier current = this.fingerTable[i];

                BigInteger compare1 = current.getNumericIdentifier();
                BigInteger diff1 = compare1.subtract(expected).abs();
                BigInteger compare2 = node.getNumericIdentifier();
                BigInteger diff2 = compare2.subtract(expected).abs();

                comp = diff2.compareTo(diff1);
                if (comp == -1)
                {
                    this.expectedFingerTableIDs[i] = node.getNumericIdentifier();
                }
            }
        }
    }

    public void nodeLeft(ChordNodeIdentifier old, ChordNodeIdentifier newNode)
    {
        for (int i = 0; i < this.fingerTable.length; ++i)
        {
            if ((old.getNumericIdentifier().compareTo(this.fingerTable[i].getNumericIdentifier())) == 0)
            {
                this.fingerTable[i] = newNode;
            }
        }
    }

    public static short getmBits()
    {
        return mBits;
    }

    public ChordNodeIdentifier getPredecessor()
    {
        return predecessor;
    }

    public void setPredecessor(ChordNodeIdentifier predecessor)
    {
        this.predecessor = predecessor;
    }

    public ChordNodeIdentifier getSelf()
    {
        return self;
    }

    public void setSelf(ChordNodeIdentifier self)
    {
        this.self = self;
    }

    public ChordNodeIdentifier getSuccessor()
    {
        return successor;
    }

    public void setSuccessor(ChordNodeIdentifier successor)
    {
        this.successor = successor;
        this.fingerTable[0] = successor;
    }

    public ChordNodeIdentifier[] getFingerTable()
    {
        return fingerTable;
    }

    public void setFingerTable(ChordNodeIdentifier[] fingerTable)
    {
        this.fingerTable = fingerTable;
    }

    public BigInteger[] getExpectedFingerTableIDs()
    {
        return expectedFingerTableIDs;
    }

    public void setExpectedFingerTableIDs(BigInteger[] expectedFingerTableIDs)
    {
        this.expectedFingerTableIDs = expectedFingerTableIDs;
    }

    public HashMap<String, ChordKey> getKeyTable()
    {
        return keyTable;
    }

    public ArrayList<ChordKey> getChordTableKeysList()
    {
        return new ArrayList<>(keyTable.values());
    }

    public void setKeyTable(HashMap<String, ChordKey> keyTable)
    {
        this.keyTable = keyTable;
    }

    public void addToKeyTable(String identifier, ChordKey key)
    {
        this.keyTable.put(identifier, key);
    }

    public void removeFromKeyTable(String identifier, ChordKey key)
    {
        this.keyTable.remove(identifier, key);
    }

    public void removeKeysFromKeyTableByChunkId(List<String> chunkIds)
    {
        for(String chunkId : chunkIds)
        {
            this.keyTable.remove(chunkId);
        }
    }
}
