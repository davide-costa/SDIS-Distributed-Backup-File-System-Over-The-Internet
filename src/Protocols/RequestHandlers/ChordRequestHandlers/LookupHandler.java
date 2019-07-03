package Protocols.RequestHandlers.ChordRequestHandlers;

import Chord.ChordKey;
import Chord.ChordNode;
import Chord.ChordNodeIdentifier;
import LoggingAndSettings.Logging;
import Messaging.ChordMessages.LookupChordMessage;
import Messaging.ChordMessages.LookupResponseChordMessage;
import Protocols.ServerlessDistributedBackupService;
import TCPMessengers.TCPClientMessenger;
import TCPMessengers.TCPMessenger;

import java.io.IOException;

public class LookupHandler {
    private LookupChordMessage message;
    private TCPMessenger messenger;

    public LookupHandler(LookupChordMessage message, TCPMessenger messenger)
    {
        this.message = message;
        this.messenger = messenger;
    }

    public void handleRequest()
    {
        Logging.Log("Initiating Lookup Request on thread " + Thread.currentThread().getId());

        ChordNode self = ServerlessDistributedBackupService.systemData.getChordNode();
        ChordKey key = new ChordKey(message.getNumericIdentifier(), message.getHashedIdentifier());
        Logging.Log("Looking Up Key In The Local Finger Table, thread:" + Thread.currentThread().getId());
        ChordNodeIdentifier id = self.getClosestNode(key);

        Logging.DebugLog("Self:"+self.getSelf().getMessageChannelAddress());
        Logging.DebugLog("Id:"+id.getMessageChannelAddress());

        LookupResponseChordMessage response;
        if (id.getHashedIdentifier().equals(self.getSelf().getHashedIdentifier())) {

            Logging.LogSuccess("Key sucessor located is self, located:"+id.getMessageChannelAddress().getHostAddress()+"thread:"+Thread.currentThread().getId());
            response = new LookupResponseChordMessage(self.getSelf().getHashedIdentifier(), self.getSelf().getNumericIdentifier(),
                    self.getSelf().getMessageChannelAddress(), self.getSelf().getMessageChannelPort(), self.getSelf());
        } else {
            Logging.Log("Key In Local Finger Table, routing to closest successor "+id.getMessageChannelAddress()+", thread:"+Thread.currentThread().getId());
            TCPClientMessenger clientMessenger = null;
            try
            {
                clientMessenger = new TCPClientMessenger(id.getMessageChannelAddress(), id.getMessageChannelPort());
            } catch (IOException e)
            {
                Logging.LogError("Error on connecting to lookup peer");
                e.printStackTrace();
            }

            Logging.Log("Sending rerouted lookup request, thread:"+Thread.currentThread().getId());
            clientMessenger.writeObject(message);

            try {
                response = (LookupResponseChordMessage) clientMessenger.readObject();
                clientMessenger.closeSocket();
            } catch (IOException e) {
                Logging.FatalErrorLog("IOException on read LookupResponseChordMessage.");
                return;
            } catch (ClassNotFoundException e) {
                Logging.FatalErrorLog("ClassNotFoundException on read LookupResponseChordMessage.");
                return;
            }
        }

        Logging.LogSuccess("Response Received return response to dispatcher and updating local information, thread:"+Thread.currentThread().getId());
        ChordNodeIdentifier source = message.getSource();
        self.update(source);

        messenger.writeObject(response);
        messenger.closeSocket();
    }
}
