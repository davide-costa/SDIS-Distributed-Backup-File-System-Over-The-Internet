package ChannelListenners;

import LoggingAndSettings.Logging;
import TCPMessengers.TCPMessenger;
import TCPMessengers.TCPServerMessenger;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChannelListener implements Runnable
{
    protected TCPServerMessenger serverMessenger;
    ExecutorService executor;

    public ChannelListener(TCPServerMessenger serverMessenger)
    {
        this.serverMessenger = serverMessenger;
        executor = Executors.newFixedThreadPool(150);
    }

    @Override
    public void run()
    {
        while (true)
        {
            try
            {
                Logging.Log("Attempting to Accept Connection");
                Socket socket = serverMessenger.acceptConnection();
                Logging.Log("Accepted Connection");
                TCPMessenger TCPMessenger = new TCPMessenger(socket);
                ChannelDispatcher dispatcher = new ChannelDispatcher(TCPMessenger);
                Logging.Log("Sending Message To Dispatcher");
                executor.execute(dispatcher);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }
}
