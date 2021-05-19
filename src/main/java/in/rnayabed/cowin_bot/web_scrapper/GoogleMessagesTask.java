package in.rnayabed.cowin_bot.web_scrapper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class GoogleMessagesTask
{
    final WebDriver webDriver;
    final String authWebsite;
    final String convoWebsite;
    final Logger logger;
    final int otpIndex;

    final String phoneNumber;
    private final String[] senders;
    final int authTimeoutInSeconds;

    private String thisTabHandle;

    private int otpValidityMin;

    private WebDriverWait webDriverWait;

    public GoogleMessagesTask(WebDriver webDriver, WebDriverWait webDriverWait, String phoneNumber, int authTimeoutInSeconds,
                              String authWebsite, String convoWebsite, String[] senders, int otpIndex,
                              int otpValidityMin)
    {
        this.webDriverWait = webDriverWait;
        this.webDriver = webDriver;
        this.authWebsite = authWebsite;
        this.convoWebsite = convoWebsite;
        this.logger = Logger.getLogger("in.rnayabed");
        this.phoneNumber = phoneNumber;
        this.authTimeoutInSeconds = authTimeoutInSeconds;

        this.senders = senders;

        this.otpIndex = otpIndex;

        this.otpValidityMin = otpValidityMin;
    }

    public Logger getLogger()
    {
        return logger;
    }


    public String getOTP()
    {
        try
        {
            webDriver.switchTo().window(getThisTabHandle());

            WebElement convContainer = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("conv-container")));

            int s = 0;
            boolean isOTPFound = false;

            String otp = null;

            while(s<authTimeoutInSeconds)
            {
                try
                {

                    WebElement firstConvListItemElement = convContainer.findElements(By.tagName("mws-conversation-list-item")).get(0);

                    WebElement textContentElement = firstConvListItemElement.findElement(By.className("text-content"));

                    WebElement senderElement = textContentElement.findElement(By.tagName("h3"));
                    WebElement contentElement = textContentElement.findElement(By.className("snippet-text"));

                    WebElement timeElement = firstConvListItemElement.findElement(By.className("list-item-info"));

                    String sender = senderElement.getText().strip();

                    for(String reqSender : senders)
                    {
                        if(sender.equals(reqSender))
                        {
                            if(!timeElement.getText().equals("Now"))
                            {
                                String min = timeElement.getText().split(" ")[0];

                                if(Integer.parseInt(min.strip()) >= otpValidityMin)
                                    continue;
                            }

                            String text = contentElement.getText().split(" ")[otpIndex];

                            otp = text.substring(0, text.length()-1);

                            isOTPFound = true;
                            break;
                        }
                    }


                    if(isOTPFound)
                        break;

                    try
                    {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    s++;
                }
                catch (StaleElementReferenceException e)
                {
                    e.printStackTrace();
                    continue;
                }
            }

            if(isOTPFound)
                return otp;
            else
                getLogger().severe("No OTP was found among senders "+ Arrays.toString(senders)+" and index "+otpIndex +
                        "within "+authTimeoutInSeconds+" seconds.");

            return null;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            getLogger().severe("Unable to use google messages. probably unable to connect to phone!");

            return null;
        }

    }

    public String getThisTabHandle()
    {
        return thisTabHandle;
    }

    public void load()
    {
        webDriver.switchTo().newWindow(WindowType.TAB);
        thisTabHandle = webDriver.getWindowHandle();

        webDriver.get(authWebsite);

        waitForAuth();
    }

    private void waitForAuth()
    {
        getLogger().info("Waiting for user auth on Google Messages ...");

        WebDriverWait fiveSecondWait = new WebDriverWait(webDriver, Duration.ofSeconds(1));
        while(webDriver.getCurrentUrl().equals(authWebsite))
        {
            try
            {
                fiveSecondWait.until(ExpectedConditions.urlMatches(convoWebsite));
            }
            catch (TimeoutException e)
            {
                getLogger().info("Still waiting ...");
            }
        }

        getLogger().info("... Done !");
    }
}
