package Protocols.Requesters.P2PRequesters;

import LoggingAndSettings.Logging;
import Messaging.P2PMessages.DeleteRequestP2PMessage;
import Messaging.P2PMessages.DeleteResponseP2PMessage;
import Protocols.Chunk;
import TCPMessengers.TCPClientMessenger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestDeleteChunk implements Runnable
{
    private Chunk chunk;

    public RequestDeleteChunk(Chunk chunk)
    {
        this.chunk = chunk;
    }

    @Override
    public void run()
    {
        DeleteRequestP2PMessage deleteRequestP2PMessage = new DeleteRequestP2PMessage(chunk.getId());
        ArrayList<InetSocketAddress> peersAddressesAndPorts = chunk.getBackupPeersAddressesAndPorts();
        ExecutorService executor = Executors.newFixedThreadPool(peersAddressesAndPorts.size());
        for(InetSocketAddress peerAddressAndPort : peersAddressesAndPorts)
        {
            InetAddress address = peerAddressAndPort.getAddress();
            int port = peerAddressAndPort.getPort();
            SendDeleteAndWaitForAck waitAckFromDelete = new SendDeleteAndWaitForAck(address, port, deleteRequestP2PMessage);
            executor.execute(waitAckFromDelete);
        }
        executor.shutdown();
    }

    public class SendDeleteAndWaitForAck implements Runnable
    {
        private TCPClientMessenger sslClientMessenger;
        private DeleteRequestP2PMessage deleteRequestP2PMessage;

        public SendDeleteAndWaitForAck(InetAddress address, int port, DeleteRequestP2PMessage deleteRequestP2PMessage)
        {
            try
            {
                sslClientMessenger = new TCPClientMessenger(address, port);
            }
            catch (IOException e)
            {
                Logging.LogError("Failed to open connection to " + address + ":" + port + ", to send delete ack.");
                return;
            }
            this.deleteRequestP2PMessage = deleteRequestP2PMessage;
        }

        @Override
        public void run()
        {
            //send delete message
            sslClientMessenger.writeObject(deleteRequestP2PMessage);

            //read response
            try
            {
                Object messageRead = sslClientMessenger.readObject();

                boolean instanceOfDeleteResponseP2PMessage = messageRead instanceof DeleteResponseP2PMessage;
                boolean succeeded = false;
                boolean chunkIdMatches = false;
                if(instanceOfDeleteResponseP2PMessage)
                {
                    DeleteResponseP2PMessage deleteResponseP2PMessage =  (DeleteResponseP2PMessage) messageRead;
                    chunkIdMatches = deleteRequestP2PMessage.getChunkId().getHashedIdentifier().equals(deleteResponseP2PMessage.getChunkId().getHashedIdentifier());
                    succeeded = deleteResponseP2PMessage.succeeded();
                }

                while(instanceOfDeleteResponseP2PMessage == false || succeeded == false || chunkIdMatches == false)
                    deleteRequestSendRetry();

                sslClientMessenger.closeSocket();
            }
            catch (IOException e)
            {
                Logging.LogError("IOException in readObject from socket in delete from chunk " + deleteRequestP2PMessage.getChunkId().getHashedIdentifier());
            }
            catch (ClassNotFoundException e)
            {
                Logging.LogError("ClassNotFoundException in readObject from socket in delete from chunk " + deleteRequestP2PMessage.getChunkId().getHashedIdentifier());
            }
        }

        private void deleteRequestSendRetry()
        {
            sslClientMessenger.writeObject(deleteRequestP2PMessage);
        }
    }


}
