package in.rnayabed.cowin_bot.logger;

import java.util.logging.FileHandler;

public class Handler extends FileHandler {

    public Handler(String logFilePath) throws Exception
    {
        super(logFilePath, false);

        setFormatter(new Formatter());
    }

    public void closeHandler()
    {
        close();
    }

}