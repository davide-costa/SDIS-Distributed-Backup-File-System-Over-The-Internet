package TCPMessengers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPServerMessenger extends TCPMessenger
{
    private ServerSocket serverSocket;

    public TCPServerMessenger(int port) throws IOException
    {
        serverSocket = new ServerSocket(port);
    }

    public TCPServerMessenger(int port, int timeoutInSeconds) throws IOException
    {
        this(port);
        serverSocket.setSoTimeout(timeoutInSeconds * 1000);
    }

    public Socket acceptConnection() throws IOException
    {
        socket = serverSocket.accept();
        //socket.startHandshake();
        return socket;
    }

    public void closeSocket()
    {
        super.closeSocket();

        try
        {
            serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
