package in.rnayabed.cowin_bot.web_scrapper;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebScrapperTask extends TimerTask
{
    private String url;

    private WebDriver webDriver;
    private WebDriverWait webDriverWait;

    public WebScrapperTask(String url)
    {
        logger = Logger.getLogger("");

        getLogger().log(Level.INFO, "Setting up firefox driver ...");
        webDriver = new FirefoxDriver();
        webDriverWait = new WebDriverWait(webDriver, 10);
        getLogger().log(Level.INFO, "... Done!");
    }



    @Override
    public void run()
    {


    }

    private final Logger logger;
    private Logger getLogger()
    {
        return logger;
    }

}
