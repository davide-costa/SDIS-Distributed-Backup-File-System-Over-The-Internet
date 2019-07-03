package RMITestApp;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerlessDistributedBackupServiceInterfaceRMI extends Remote
{
    String backup(String filepath, int replicationDegree) throws RemoteException;

    void restore(String filepath) throws RemoteException;

    String delete(String filepath) throws RemoteException;

    void close() throws RemoteException;

    String state() throws RemoteException;
}
