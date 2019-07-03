package Protocols;

import AES.AES;
import ChannelListenners.ChannelListener;
import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Configurations;
import LoggingAndSettings.Logging;
import Protocols.Requesters.ChordRequesters.RequestPredecessorEntryNotification;
import Protocols.Requesters.ChordRequesters.RequestSuccessorEntry;
import Protocols.Requesters.ChordRequesters.RequestTransferOwnership;
import Protocols.Requesters.ChordRequesters.RequestTransferSuccessor;
import Protocols.Requesters.P2PRequesters.RequestBackupChunk;
import Protocols.Requesters.P2PRequesters.RequestDeleteChunk;
import Protocols.Requesters.P2PRequesters.RequestRestoreChunk;
import RMITestApp.ServerlessDistributedBackupServiceInterfaceRMI;
import TCPMessengers.TCPServerMessenger;
import Utils.CommonUtils;
import Utils.FileUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class ServerlessDistributedBackupService implements ServerlessDistributedBackupServiceInterfaceRMI
{

    public static final int CHUNK_MAX_SIZE = 1024 * 1024;
    public static final int NUM_MAX_THREADS = 300;

    //this set contains the available ports to be listen on for TCP protocol for file restoring protocol
    //thus, the initial capacity of the container indicates the maximum number of threads accepting TCP connections at the same time (but not the amount of restores being made at any given time)
    public static BlockingQueue<Integer> availablePorts = new LinkedBlockingQueue<Integer>();
    public static int firstTCPPort = 5560;
    public static int lastTCPPort = 5760;

    public static ArrayList<ChordNodeIdentifier> wellKnownedNodes = new ArrayList<>();

    public static SystemManagement systemData;

    public static InetAddress address;
    public static int messagePort;

    public static String privateKey;

    public static String systemDataFilePath = "ServerlessDistributedBackupService/systemData";
    public static String chunksPath = "ServerlessDistributedBackupService/backedUpChunks/";
    public static String restoredChunksPath = "ServerlessDistributedBackupService/restoredChunks/";
    public static String restoredFilesPath = "ServerlessDistributedBackupService/restoredFiles/";


    public static void main(String[] args) throws IOException
    {
        if (args.length < 3)
        {
            System.out.println("Usage: ServerlessDistributedBackupService <server_access_point> <address:messagePort> <private_key>");
            return;
        }

        //parse arguments
        String serverAcessPoint = args[0];
        address = getAdressFromString(args[1]);
        messagePort = getPortFromString(args[1]);
        privateKey = args[2];

        //start listening on tcp channel
        startListeningOnChannel(messagePort);
        
        addTCPPortsToAvailablePortsQueue();
        
        initiateProtocolRMI(serverAcessPoint);

        //create folders if not existents
        createProgramFolders();

        //system data management creation and initiate periodic saver that stores the information at disk at a given period
        if (!readSystemDataIfExists())
            systemData = new SystemManagement();
        systemData.initiatePeriodicSaver(2);

        new Configurations();
        wellKnownedNodes = Configurations.wellKnownNodes;

        if(startChordAlgorithm() == false)
        {
            Logging.FatalErrorLog("Error starting chord algorithm");
            System.exit(-1);
        }
    }

    private static boolean startChordAlgorithm()
    {
        ChordNodeIdentifier selfChordNodeIdentifier = new ChordNodeIdentifier(address, messagePort);
        System.out.println(selfChordNodeIdentifier.getNumericIdentifier());
        ServerlessDistributedBackupService.systemData.setChordNode(new ChordNode(selfChordNodeIdentifier, selfChordNodeIdentifier, selfChordNodeIdentifier));

        if(wellKnownedNodes.contains(selfChordNodeIdentifier))
        {
            return true;
        }

        for(ChordNodeIdentifier wellKnownNode : wellKnownedNodes)
        {
            ChordKey selfKey = new ChordKey(selfChordNodeIdentifier.getNumericIdentifier(), selfChordNodeIdentifier.getHashedIdentifier());
            RequestSuccessorEntry requestSuccessorEntry = new RequestSuccessorEntry(selfKey, wellKnownNode);
            if(requestSuccessorEntry.sendRequest() == false)
                continue;

            ChordNodeIdentifier predecessor = requestSuccessorEntry.getPredecessor();
            ServerlessDistributedBackupService.systemData.getChordNode().setPredecessor(predecessor);
            RequestPredecessorEntryNotification requestPredecessorEntryNotification =
                    new RequestPredecessorEntryNotification(ServerlessDistributedBackupService.systemData.getChordNode().getSelf(), predecessor);
            requestPredecessorEntryNotification.sendRequest();
            return true;
        }

        return false;

    }

    private static void leaveChord()
    {
        ChordNodeIdentifier predecessor = ServerlessDistributedBackupService.systemData.getChordNode().getPredecessor();
        ChordNodeIdentifier successor = ServerlessDistributedBackupService.systemData.getChordNode().getSuccessor();

        RequestTransferSuccessor requestTransferSuccessor = new RequestTransferSuccessor(predecessor, successor);
        requestTransferSuccessor.run();
        RequestTransferOwnership requestTransferOwnership = new RequestTransferOwnership(predecessor,successor);
        requestTransferOwnership.run();
    }

    private static void createProgramFolders()
    {
        File dir = new File("ServerlessDistributedBackupService");
        dir.mkdir();
        dir = new File("ServerlessDistributedBackupService/backedUpChunks");
        dir.mkdir();
        dir = new File("ServerlessDistributedBackupService/restoredChunks");
        dir.mkdir();
        dir = new File("ServerlessDistributedBackupService/restoredFiles");
        dir.mkdir();
    }

    private static InetAddress getExternalIp()
    {
        try
        {
            URL whatIsMyIpRequest = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatIsMyIpRequest.openStream()));
            return InetAddress.getByName(in.readLine());
        }
        catch (IOException e)
        {
            Logging.LogError("Error getting external ip");
            return null;
        }
    }

    private static void addTCPPortsToAvailablePortsQueue()
    {
        try
        {
            for (int i = firstTCPPort; i <= lastTCPPort; i++)
                availablePorts.put(i);
        }
        catch (InterruptedException e)
        {
            Logging.FatalErrorLog("InterruptedException on adding TCP ports to available ports queue");
            System.exit(-1);
        }
    }

    private static void startListeningOnChannel(int port) throws IOException
    {
        TCPServerMessenger sslServerMessenger = new TCPServerMessenger(port);
        ChannelListener channel = new ChannelListener(sslServerMessenger);
        Thread thread = new Thread(channel);
        thread.start();
    }

    private static boolean readSystemDataIfExists()
    {
        Object object = FileUtils.readObjectFromDisk(systemDataFilePath);
        if(object == null)
            return false;

        systemData = (SystemManagement) object;
        return true;
    }

    private static void initiateProtocolRMI(String peerAcessPoint)
    {
        try
        {
            ServerlessDistributedBackupService serverlessDistributedBackupService = new ServerlessDistributedBackupService();
            ServerlessDistributedBackupServiceInterfaceRMI stub
                    = (ServerlessDistributedBackupServiceInterfaceRMI) UnicastRemoteObject.exportObject(serverlessDistributedBackupService, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(peerAcessPoint, stub);
        } catch (Exception e)
        {
            System.err.println("ServerlessDistributedBackupService RMI exception: " + e.toString());
            e.printStackTrace();

        }
    }

    @Override
    public String backup(String filepath, int replicationDegree) throws RemoteException
    {
        if (replicationDegree < 1 || replicationDegree > 255)
            return "Replication degree must be between 1 and 255.";

        if (systemData.ownerBackedUpFilePathsContainsFilePath(filepath))
        {
            Logging.Log("Trying to back up file already backed up, request ignored");
            return "File has already been backed up!";
        }

        ArrayList<Chunk> chunks = new ArrayList<>();
        try
        {
            File f = new File(filepath);
            if (!f.exists())
                return "File '" + filepath + "' does not exist!";


            int numChunks = (int) Math.ceil((double)f.length() / CHUNK_MAX_SIZE);
            ExecutorService executor = Executors.newFixedThreadPool(getNumThreadsBasedOnNumChunks(numChunks));
            byte[] buffer = new byte[CHUNK_MAX_SIZE];
            FileInputStream inputStream = new FileInputStream(filepath);
            int chunkNumber = 0;
            int bytesRead;

            boolean endOfFile = false;
            while (!endOfFile)
            {
                bytesRead = inputStream.read(buffer);

                if (bytesRead != CHUNK_MAX_SIZE)
                {
                    if (bytesRead == -1)
                        bytesRead = 0;

                    byte[] dataRead = Arrays.copyOfRange(buffer, 0, bytesRead);
                    buffer = dataRead;
                    endOfFile = true;
                }

                buffer = AES.encryptBytesWithAES(privateKey, buffer); //encrypt chunk data
                byte[] filepathAndChunkNumberByteArray = (filepath + chunkNumber).getBytes();
                byte[] chunkIdentifierForHash = new byte[buffer.length + filepathAndChunkNumberByteArray.length];
                System.arraycopy(buffer, 0, chunkIdentifierForHash, 0, buffer.length);
                System.arraycopy((filepath + chunkNumber).getBytes(), 0, filepathAndChunkNumberByteArray,
                        0, filepathAndChunkNumberByteArray.length);
                ChordKey chunkId = new ChordKey(CommonUtils.getByteArraySHA256FromByteArray(chunkIdentifierForHash));
                ArrayList<ChordKey> backupPeersIds = getBackupPeersIdsOfChunk(chunkId.getHashedIdentifier(), replicationDegree);
                Chunk chunk = new Chunk(chunkId, buffer.length, replicationDegree, backupPeersIds);
                chunks.add(chunk);
                RequestBackupChunk handleBackupChunk = new RequestBackupChunk(chunk, buffer);
                executor.execute(handleBackupChunk);
                chunkNumber++;
                buffer = new byte[CHUNK_MAX_SIZE];
            }

            inputStream.close();
            executor.shutdown(); // terminates the executor after all running threads
        } catch (FileNotFoundException ex)
        {
            System.err.println("Unable to open file '" + filepath + "'");
        } catch (IOException ex)
        {
            System.err.println("Error reading file '" + filepath + "'");
        }

        systemData.addToOwnerBackedUpFilepathToChunks(filepath, chunks);
        systemData.addToOwnerBackedUpFilePaths(filepath);
        return "File is being backed up!";
    }

    private ArrayList<ChordKey> getBackupPeersIdsOfChunk(byte[] chunkIdByteArray, int numBackupPeers)
    {
        ArrayList<ChordKey> backupPeersIds = new ArrayList<>();
        for(Integer i = 0; i < numBackupPeers; i++)
        {
            byte[] chunkIdBytes = Arrays.copyOf(chunkIdByteArray, chunkIdByteArray.length + 1);
            chunkIdBytes[chunkIdBytes.length - 1] = i.byteValue();

            byte[] chordKeyBytes = CommonUtils.getByteArraySHA256FromByteArray(chunkIdBytes);
            ChordKey chordKey = new ChordKey(chordKeyBytes);
            backupPeersIds.add(chordKey);
        }

        return backupPeersIds;
    }

    private ArrayList<ChordKey> getBackupPeersIdsOfChunk(String chunkId, int numBackupPeers)
    {
        ArrayList<ChordKey> backupPeersIds = new ArrayList<>();
        for(Integer i = 0; i < numBackupPeers; i++)
        {
            String chunkIdString = chunkId + i;
            byte[] chordKeyBytes = CommonUtils.getByteArraySHA256FromString(chunkIdString);
            ChordKey chordKey = new ChordKey(chordKeyBytes);
            backupPeersIds.add(chordKey);
        }

        return backupPeersIds;
    }

    private int getNumThreadsBasedOnNumChunks(int numChunks)
    {
        if (numChunks < NUM_MAX_THREADS)
            return numChunks;
        int numThreads = (int) Math.ceil(numChunks * 0.3);
        if(numThreads > NUM_MAX_THREADS)
            return NUM_MAX_THREADS;

        return numThreads;
    }

    @Override
    public void restore(String filepath) throws RemoteException
    {
        List<Chunk> chunks = ServerlessDistributedBackupService.systemData.getOwnerBackedUpChunksOfFile(filepath);
        if (chunks.size() == 0)
            return;

        ExecutorService executor = Executors.newFixedThreadPool(getNumThreadsBasedOnNumChunks(chunks.size()));
        int i = 0;
        for (Chunk chunk : chunks)
        {
            RequestRestoreChunk requestRestoreChunk = new RequestRestoreChunk(chunk, i);
            executor.execute(requestRestoreChunk);
            i++;
        }
        executor.shutdown(); // terminates the executor after all running tasks

        if (timeoutInCaseChunksNeverComes(executor, chunks))
            return;

        String filename = Paths.get(filepath).getFileName().toString();
        getReceivedChunksAndReassembleToOriginalFile(chunks, filename);
    }

    private boolean timeoutInCaseChunksNeverComes(ExecutorService executor, List<Chunk> chunks)
    {
        int maxTimeToWaitMilliseconds = chunks.size() * 3000;

        boolean timeout;
        try
        {
            timeout = !executor.awaitTermination(maxTimeToWaitMilliseconds, TimeUnit.MILLISECONDS);
            executor.shutdownNow();
        } catch (InterruptedException e)
        {
            executor.shutdownNow();
            e.printStackTrace();
            return true;
        }

        return timeout;
    }

    private void getReceivedChunksAndReassembleToOriginalFile(List<Chunk> chunks, String filename)
    {
        List<String> chunksPaths = new ArrayList<>();
        String chunksBasePath = ServerlessDistributedBackupService.restoredChunksPath;
        int i = 0;
        for (Chunk chunk : chunks)
        {
            chunksPaths.add(chunksBasePath + chunk.getId().getHashedIdentifier() + "-" + i);
            i++;
        }
        String filePath = ServerlessDistributedBackupService.restoredFilesPath + filename;
        chunksPaths.sort((string1, string2) ->
        {
            //chunk number string1
            int dashIndex = string1.indexOf("-");
            int chunkNumberString1 = Integer.parseInt(string1.substring(dashIndex + 1));

            //chunk number string2
            dashIndex = string2.indexOf("-");
            int chunkNumberString2 = Integer.parseInt(string2.substring(dashIndex + 1));

            if (chunkNumberString1 < chunkNumberString2)
                return -1;
            else if (chunkNumberString1 > chunkNumberString2)
                return 1;
            else
                return 0;
        });

        int ret = FileUtils.reassembleChunksToFile(chunksPaths, filePath);
        switch (ret)
        {
            case 1:
                System.out.println("Error restoring file: at least one of the chunks doesn't exist, thus the file cannot be reassembled.");
                break;
            case 2:
                System.out.println("Error restoring file: exception occurred when reading from chunks or writing to reassembled file.");
                break;
            case 3:
                System.out.println("Warning restoring file: The output stream to the restored file couldn't be closed. This may cause problems editing the file externally. Consider restarting this program to free the file.");
                break;
            default:
                System.out.println("File " + filePath + " restored successfully.");
                break;
        }
    }

    @Override
    public String delete(String filepath)
    {
        if(systemData.ownerBackedUpFilePathsContainsFilePath(filepath) == false)
            return "File have not been backed up";

        ArrayList<Chunk> chunks = systemData.getOwnerBackedUpChunkByFileId(filepath);
        ExecutorService executor = Executors.newFixedThreadPool(getNumThreadsBasedOnNumChunks(chunks.size()));
        for(Chunk chunk : chunks)
        {
            RequestDeleteChunk deleteRequester = new RequestDeleteChunk(chunk);
            executor.execute(deleteRequester);
        }
        executor.shutdown(); // terminates the executor after all running threads

        //remove this chunks from system data
        ServerlessDistributedBackupService.systemData.removeOwnerBackedUpChunksOfFile(filepath);

        return "File is being deleted from the network";
    }

    @Override
    public void close() throws RemoteException
    {
        leaveChord();
        systemData.saveSystemInfo();
        System.exit(0);
    }

    @Override
    public String state() throws RemoteException
    {
        return systemData.getSystemData();
    }

    private void removeChunksFromDisk(ArrayList<Chunk> chunksToRemove)
    {
        for (Chunk chunk : chunksToRemove)
        {
            String filepath = ServerlessDistributedBackupService.chunksPath + chunk.getId();
            FileUtils.deleteFileFromDisk(filepath);
        }
    }

    private ArrayList<Chunk> removeChunksBasedOnReplicationDegree()
    {
        ArrayList<Chunk> chunksRemoved = new ArrayList<Chunk>();
        List<Chunk> communityStoredChunks = ServerlessDistributedBackupService.systemData.getCommunityBackedUpChunks();
        long maxSpaceToChunks = ServerlessDistributedBackupService.systemData.getMaxAmountDiskSpaceForBackup();

        //first remove chunks that curr replication degree is bigger than requested
        for (Iterator<Chunk> it = communityStoredChunks.iterator(); it.hasNext();)
        {
            if (ServerlessDistributedBackupService.systemData.getCurrCommunityBackedUpSpace() > maxSpaceToChunks)
            {
                Chunk chunk = it.next();
                if (chunk.getCurrReplicationDegree() > chunk.getExpectedReplicationDegree())
                {
                    chunksRemoved.add(chunk);
                    ServerlessDistributedBackupService.systemData.removeChunkFromCommunityBackedUpChunksByIterator(it, chunk);
                }
            } else
                return chunksRemoved;
        }

        //if last step not enough, remove randomly chunks until get in the max occupied space range
        ArrayList<Integer> chunksToRemoveIdx = new ArrayList<Integer>();
        while (ServerlessDistributedBackupService.systemData.getCurrCommunityBackedUpSpace() > maxSpaceToChunks)
        {
            int randomIndex = ThreadLocalRandom.current().nextInt(0, communityStoredChunks.size());
            chunksRemoved.add(communityStoredChunks.get(randomIndex));
            chunksToRemoveIdx.add(randomIndex);
            ServerlessDistributedBackupService.systemData.removeChunkFromCommunityBackedUpChunksByIndex(randomIndex);
        }

        return chunksRemoved;
    }

    private static InetAddress getAdressFromString(String string)
    {
        int indexOfDoblePoints = string.indexOf(":");
        try
        {
            return InetAddress.getByName(string.substring(0, indexOfDoblePoints));
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private static int getPortFromString(String string)
    {
        int indexOfDoblePoints = string.indexOf(":");
        return Integer.parseInt(string.substring(indexOfDoblePoints + 1));
    }

}
