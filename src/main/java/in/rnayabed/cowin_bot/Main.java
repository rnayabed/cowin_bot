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

            Logger.getLogger("in.rnayabed").info("cowin_bot "+System.getProperty("bot.version")+"\n By rnayabed (Debayan Sutradhar)" +
                    "\nSource : "+System.getProperty("bot.source"));

            new Timer().scheduleAtFixedRate(new WebScrapperTask(), 0,Long.parseLong(System.getProperty("repeat.millis")));
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
