package in.rnayabed.cowin_bot;


import in.rnayabed.cowin_bot.exception.BotException;
import in.rnayabed.cowin_bot.logger.Handler;
import in.rnayabed.cowin_bot.web_scrapper.WebScrapperTask;

import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main
{
    public static void main(String[] args) 
    {
        try
        {
            initLogger();

            new Timer().schedule(new WebScrapperTask(), 0);
        }
        catch (BotException e)
        {
            e.printStackTrace();
            System.out.println("Quitting ...");
        }
    }

    private static void initLogger() throws BotException
    {
        try
        {
            Logger logger = Logger.getLogger("in.rnayabed");
            logger.setLevel(Level.ALL);
            logger.addHandler(new Handler("config"));
        }
        catch (Exception e)
        {
            throw new BotException("Unable to start logger");
        }
    }
}
