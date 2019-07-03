package LoggingAndSettings;

public class Logging
{
    public static void LogError(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_RED +"[Error]: "+log + TerminalColors.ANSI_RESET);
        }
    }

    public static void LogSuccess(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_GREEN +"[Success]: "+ log + TerminalColors.ANSI_RESET);
        }
    }

    public static void Log(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_YELLOW +"[Log]: "+ log + TerminalColors.ANSI_RESET);
        }
    }

    public static void FatalErrorLog(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_RED_BACKGROUND +"[Fatal Error]: "+log + TerminalColors.ANSI_RESET);
        }
    }

    public static void FatalSuccessLog(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_GREEN_BACKGROUND +"[Fatal Success]: "+ log + TerminalColors.ANSI_RESET);
        }
    }

    public static void MessageSentLog(String log)
    {
        if (Configurations.LOG)
        {
            System.out.println(TerminalColors.ANSI_CYAN+"[Message Log]: "+log + TerminalColors.ANSI_RESET);
        }
    }

    public static void MessageSentDebug(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_CYAN+"[Debug Message Log]:"+log+TerminalColors.ANSI_RESET);
        }
    }

    public static void FatalSuccessDebug(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_GREEN_BACKGROUND+"[Debug Fatal Success]:"+log+TerminalColors.ANSI_RESET);
        }
    }

    public static void FatalErrorDebug(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_RED_BACKGROUND+"[Debug Fatal Error]:"+log+TerminalColors.ANSI_RESET);
        }
    }

    public static void DebugLog(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_YELLOW+"[Debug Log]:"+log+TerminalColors.ANSI_RESET);
        }
    }

    public static void ErrorDebug(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_RED+"[Debug Error]:"+log+TerminalColors.ANSI_RESET);
        }
    }

    public static void SuccessDebug(String log)
    {
        if(Configurations.DEBUG)
        {
            System.out.println(TerminalColors.ANSI_GREEN+"[Debug Success]:"+log+TerminalColors.ANSI_RESET);
        }
    }
}
