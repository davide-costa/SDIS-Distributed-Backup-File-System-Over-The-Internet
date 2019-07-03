package RMITestApp;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp
{

    private static ServerlessDistributedBackupServiceInterfaceRMI stub;
    private static String[] arguments;

    public static void main(String[] args)
    {
        arguments = args;
        if (arguments.length < 2)
        {
            System.out.println("Usage: TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>");
            return;
        }

        try
        {
            // <peer_ap> on format //host/name
            String peerAcessPoint = arguments[0];
            int hostNameSlash = peerAcessPoint.lastIndexOf("/");
            if (hostNameSlash == -1)
            {
                System.out.println("<peer_ap> on format //host/name");
                return;
            }
            String host = peerAcessPoint.substring(2, hostNameSlash);
            String name = peerAcessPoint.substring(hostNameSlash + 1);

            Registry registry = LocateRegistry.getRegistry(host);
            stub = (ServerlessDistributedBackupServiceInterfaceRMI) registry.lookup(name);

            String operation = TestApp.arguments[1];
            switch (operation)
            {
                case "BACKUP":
                    backupProtocol();
                    break;
                case "RESTORE":
                    restoreProtocol();
                    break;
                case "DELETE":
                    deleteProtocol();
                    break;
                case "CLOSE":
                    closeProtocol();
                    break;
                case "STATE":
                    stateProtocol();
                    break;
                default:
                    System.err.println("Unrecognized protocol. Supported protocols are: BACKUP, RESTORE, DELETE, RECLAIM, STATE");
                    System.exit(-1);
                    break;
            }
        } catch (Exception e)
        {
            System.err.println("Error in RMI call!");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static String backupProtocol()
    {
        if (arguments.length < 4)
        {
            String messageError = "Usage: TestApp <peer_ap> BACKUP <filepath> <replication_degree>";
            System.err.println(messageError);
            System.exit(-1);
        }

        String filepath = arguments[2];
        int replicationDegree = Integer.parseInt(arguments[3]);
        if (replicationDegree > 9)
        {
            System.err.println("Replication degree must be 9 ou lower!");
            System.exit(-1);
        }

        try
        {
            return stub.backup(filepath, replicationDegree);
        } catch (RemoteException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }

    private static void restoreProtocol()
    {
        if (arguments.length < 3)
        {
            System.err.println("Usage: TestApp <peer_ap> RESTORE <filepath>");
            System.exit(-1);
        }

        String filepath = arguments[2];
        try
        {
            stub.restore(filepath);
        } catch (RemoteException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void deleteProtocol()
    {
        String filepath = arguments[2];
        if (arguments.length < 3)
        {
            System.err.println("Usage: TestApp <peer_ap> DELETE <filepath>");
            System.exit(-1);
        }

        try
        {
            System.out.println(stub.delete(filepath));
        } catch (RemoteException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void closeProtocol()
    {
        try
        {
            stub.close();
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void stateProtocol()
    {
        try
        {
            System.out.println(stub.state());
        } catch (RemoteException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
