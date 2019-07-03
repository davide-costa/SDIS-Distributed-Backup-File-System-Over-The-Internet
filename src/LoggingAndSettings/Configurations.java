package LoggingAndSettings;

import Chord.ChordNodeIdentifier;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class Configurations
{
    public static boolean LOG = true;
    public static boolean DEBUG = true;
    public static ArrayList<ChordNodeIdentifier> wellKnownNodes;
    private String configFilePath = "config.ini";

    /**
     * Writhes the current configuration values to the config file
     */
    private void createConfigFile()
    {
        //create the configuration file
        FileOutputStream file = null;
        try
        {
            file = new FileOutputStream(this.configFilePath);
        } catch (FileNotFoundException e)
        {
            Logging.FatalErrorLog("Failed To Create Config File Exiting");
            System.exit(-1);
        }
        String configVal = "";

        //write the predefined values of the various system configurations
        configVal += "LOG=" + LOG + "\n";
        configVal += "DEBUG=" + DEBUG + "\n";
        //end of the predefined configurations values

        //write the values to the file
        try
        {
            file.write(configVal.getBytes());
        } catch (IOException e)
        {
            Logging.FatalErrorLog("Failed To Write Configuration To Config File");
            System.exit(-1);
        }

        //close the config file FD
        try
        {
            file.close();
        } catch (IOException e)
        {
            Logging.FatalErrorLog("Failed To Close Config File");
            System.exit(-1);
        }
    }

    /**
     * Parse A line of the config file and sets the correct values
     *
     * @param configValue
     */
    private void parseLine(String configValue)
    {
        String[] values = configValue.split("=");
        switch (values[0])
        {
            case "LOG":
            {
                if (values[1].equals("TRUE") || values[1].equals("True") || values[1].equals("true") || values[1].equals("1"))
                {
                    Configurations.LOG = true;
                } else
                {
                    Configurations.LOG = false;
                }
                break;
            }

            case "DEBUG":
            {
                if (values[1].equals("TRUE") || values[1].equals("True") || values[1].equals("true") || values[1].equals("1"))
                {
                    Configurations.DEBUG = true;
                } else
                {
                    Configurations.DEBUG = DEBUG = false;
                }
                break;
            }

            case "NODE":
            {
                String[] args = values[1].split(":");
                try {
                    this.wellKnownNodes.add(new ChordNodeIdentifier(InetAddress.getByName(args[0]),Integer.parseInt(args[1])));
                } catch (UnknownHostException e) {
                    Logging.FatalErrorLog("Failed To Obtain Well Known Node Address");
                    e.printStackTrace();
                }
                break;
            }

            default:
            {
                LOG = true;
                DEBUG = true;
                break;
            }
        }
    }

    /**
     * Reads the configs value and sets the correct values
     */
    public void readConfigFile()
    {
        //Open the config file
        FileInputStream configFile = null;
        BufferedReader reader = null;
        try
        {
            configFile = new FileInputStream(this.configFilePath);
            reader = new BufferedReader(new InputStreamReader(configFile, "UTF-8"));
        } catch (FileNotFoundException e)
        {
            Logging.FatalErrorLog("Failed To Open Config File");
            System.exit(-1);
        } catch (UnsupportedEncodingException e)
        {
            Logging.FatalErrorLog("UTF-8 Encoding Is Not Supported");
            System.exit(-1);
        }

        //read config line by line
        try
        {
            String value = "";
            while ((value = reader.readLine()) != null)
            {
                this.parseLine(value);
            }
        } catch (IOException e)
        {
            Logging.FatalErrorLog("Failed To Read Config File");
            System.exit(-1);
        }

        //close the BufferedReader and The File
        try
        {
            reader.close();
        } catch (IOException e)
        {
            Logging.FatalErrorLog("Failed To Close Config File Buffered Reader");
            System.exit(-1);
        }
        try
        {
            configFile.close();
        } catch (IOException e)
        {
            Logging.FatalErrorLog("Failed To Close Config File");
            System.exit(-1);
        }
    }

    /**
     * Checks if the configuration file exits and creates it if it does not
     * exist
     *
     * @return whether or not the configuration file exits
     */
    private boolean configFileInitializtion()
    {
        File configFile = new File(this.configFilePath);
        if (!configFile.exists())
        {
            this.createConfigFile();
            return false;
        }
        return true;
    }

    public Configurations()
    {
        this.wellKnownNodes = new ArrayList<>();
        boolean inited = this.configFileInitializtion();
        if (inited)
        {
            this.readConfigFile();
        }
    }

}
