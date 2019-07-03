package TCPMessengers;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class TCPMessenger
{
    protected Socket socket;

    public TCPMessenger() {}

    public TCPMessenger(Socket socket)
    {
        this.socket = socket;
    }

    public byte[] readMessage(int numBytesToRead)
    {
        byte[] messageBytes = new byte[numBytesToRead];
        try
        {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            int currIndex = 0;
            int sizeRead;

            while ((sizeRead = in.read(messageBytes, currIndex, numBytesToRead - currIndex)) != -1 && numBytesToRead != currIndex)
                currIndex += sizeRead;

            if (currIndex != numBytesToRead)
            {
                byte[] data = Arrays.copyOfRange(messageBytes, 0, currIndex);
                messageBytes = data;
            }

            return messageBytes;
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public InetAddress getAddress()
    {
        return socket.getInetAddress();
    }

    public int getPort()
    {
        return socket.getPort();
    }

    public Object readObject() throws IOException, ClassNotFoundException
    {
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        return in.readObject();
    }

    public boolean writeMessage(byte[] messageBytes) throws IOException
    {
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.write(messageBytes);
        out.flush();
        return true;
}

    public boolean writeStringMessage(String message)
    {
        try
        {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.write(message.getBytes());
            out.flush();
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public boolean writeObject(Object object)
    {
        try
        {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(object);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public void closeSocket()
    {
        try
        {
            socket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
