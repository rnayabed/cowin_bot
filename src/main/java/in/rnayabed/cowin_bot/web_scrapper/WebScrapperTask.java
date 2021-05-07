package in.rnayabed.cowin_bot.web_scrapper;

import in.rnayabed.cowin_bot.email.Mail;
import in.rnayabed.cowin_bot.exception.BotException;
import in.rnayabed.cowin_bot.vaccine.Filter;
import in.rnayabed.cowin_bot.vaccine.Vaccine;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebScrapperTask extends TimerTask
{
    private String url;
    private String state;
    private String[] districts;

    private WebDriver webDriver;
    private WebDriverWait webDriverWait;

    private Filter[] filters;


    public WebScrapperTask()
    {
        logger = Logger.getLogger("in.rnayabed");

        districtHashMap= new HashMap<>();

        url = System.getProperty("cowin.website.url");
        state = System.getProperty("search.state").strip();

        districts = System.getProperty("search.districts").split(",");

        for(int j = 0;j<districts.length;j++)
        {
            districts[j] = districts[j].strip();
        }



        String[] tmpFilters = System.getProperty("search.filters").split(",");

        filters = new Filter[tmpFilters.length];
        for(int j = 0;j<tmpFilters.length;j++)
        {
            filters[j] = Filter.valueOf(tmpFilters[j].strip());
        }

        String browserChoice = System.getProperty("browser.choice").toLowerCase();
        boolean runHeadless = System.getProperty("browser.run.headless").equalsIgnoreCase("true");

        String windowHeight = System.getProperty("browser.window.height").strip();
        String windowWidth = System.getProperty("browser.window.width").strip();

        if(browserChoice.equals("chrome") || browserChoice.equals("chromium"))
        {
            getLogger().log(Level.INFO, "Setting up chrome driver ...");

            ChromeOptions chromeOptions = new ChromeOptions();

            if(runHeadless)
            {
                chromeOptions.addArguments("--headless");
                chromeOptions.addArguments("--start-maximized");
            }

            chromeOptions.addArguments("--window-size="+windowWidth+","+windowHeight);

            webDriver = new ChromeDriver(chromeOptions);
        }
        else if (browserChoice.equals("firefox") || browserChoice.equals("gecko"))
        {
            getLogger().log(Level.INFO, "Setting up firefox driver ...");

            FirefoxOptions firefoxOptions = new FirefoxOptions();

            firefoxOptions.addArguments("--width="+windowWidth);
            firefoxOptions.addArguments("--height="+windowHeight);

            if(runHeadless)
                firefoxOptions.addArguments("--headless");

            webDriver = new FirefoxDriver(firefoxOptions);
        }
        else
        {
            throw new IllegalArgumentException("browser.choice property is invalid ("+browserChoice+")");
        }


        webDriverWait = new WebDriverWait(webDriver, Long.parseLong(System.getProperty("timeout.seconds")));
        getLogger().log(Level.INFO, "... Done!");


        try
        {
            loadCowinWebsite();
            selectSearchByDistrict();

            if(districts.length == 1)
            {
                chooseStateDistrictAndType(state, districts[0]);
            }
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void loadCowinWebsite()
    {
        getLogger().log(Level.INFO, "Loading cowin website ("+url+") ...");
        webDriver.get(url);
        getLogger().log(Level.INFO, "... Done!");
    }

    private void selectSearchByDistrict()
    {
        getLogger().log(Level.INFO, "Selecting search by district ...");

        WebElement statusSwitchElementDiv = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("status-switch")));

        if(webDriver.findElements(By.className("mat-select-48")).size() == 0)
        {
            statusSwitchElementDiv.click();
        }
    }

    private void chooseStateDistrictAndTypeAndFinallySearchAndGetAvailableVaccines(String stateName, String[] districts) throws BotException
    {
        for(String district : districts)
        {
            chooseStateDistrictAndType(stateName, district);
            search();
            getAvailableVaccines(stateName, district);
        }
    }

    private HashMap<String, Integer> districtHashMap;

    private void chooseStateDistrictAndType(String stateName, String districtName) throws BotException
    {
        WebElement searchdistwrperDiv = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("searchdistwrper")));

        List<WebElement> selectorDivs = searchdistwrperDiv.findElements(By.className("pullleft"));

        WebElement stateSelectorBox = selectorDivs.get(0).findElement(By.tagName("mat-select"));

        String alreadyPresent = stateSelectorBox
               .findElement(By.tagName("div"))
               .findElement(By.tagName("div"))
               .findElement(By.tagName("span")).getText();

       if(!alreadyPresent.equalsIgnoreCase(stateName))
       {
           stateSelectorBox.click();

           int stateFound = selectOption(stateName,"mat-select-0-panel", -1);

           if(stateFound == -1)
           {
               throw new BotException("Unable to find state : '"+stateName+"'");
           }
       }


       WebElement districtSelectorBox = selectorDivs.get(1).findElement(By.tagName("mat-select"));
       districtSelectorBox.click();

       int oldVal = districtHashMap.getOrDefault(districtName, -1);

       int districtFound = selectOption(districtName,"mat-select-2-panel", oldVal);

        if(districtFound == -1)
        {
            throw new BotException("Unable to find district : '"+districtName+"'");
        }

        districtHashMap.put(districtName, districtFound);


    }

    WebElement age18PlusFilterWebElement = null;
    WebElement age45PlusFilterWebElement = null;
    WebElement covishieldFilterWebElement = null;
    WebElement covaxinFilterWebElement = null;
    WebElement freeFilterWebElement = null;
    WebElement paidFilterWebElement = null;

    private void search()
    {
        webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("searchdistwrper")))
                .findElement(By.className("pinbtncover"))
                .findElement(By.tagName("button"))
                .click();




        if(age18PlusFilterWebElement == null)
        {
            WebElement filtersBlock = webDriver.findElement(By.className("agefilterblock"));

            List<WebElement> filters = filtersBlock.findElements(By.className("form-check-label"));

            logger.info("Not required ...");

            age18PlusFilterWebElement = filters.get(0);
            age45PlusFilterWebElement = filters.get(1);
            covishieldFilterWebElement = filters.get(2);
            covaxinFilterWebElement = filters.get(3);
            freeFilterWebElement = filters.get(4);
            paidFilterWebElement = filters.get(5);
        }


        for(Filter filter : filters)
        {
            if(filter == Filter.AGE_18_PLUS)
                age18PlusFilterWebElement.click();
            else if(filter == Filter.AGE_45_PLUS)
                age45PlusFilterWebElement.click();
            else if (filter == Filter.COVISHIELD)
                covishieldFilterWebElement.click();
            else if(filter == Filter.COVAXIN)
                covaxinFilterWebElement.click();
            else if(filter == Filter.FREE)
                freeFilterWebElement.click();
            else if(filter == Filter.PAID)
                paidFilterWebElement.click();
        }
    }

    private int selectOption(String option, String selectorIdName, int oldVal)
    {
        WebElement selectorPanel = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.id(selectorIdName)));

        List<WebElement> options = selectorPanel.findElements(By.tagName("mat-option"));

        if(oldVal > -1)
        {
            options.get(oldVal).click();
            return oldVal;
        }

        int index = -1;
        for(int i = 0;i<options.size();i++)
        {
            WebElement eachOption = options.get(i);

            WebElement span = eachOption.findElement(By.className("mat-option-text"));

            if(span.getText().equalsIgnoreCase(option))
            {
                eachOption.click();
                index = i;
                break;
            }
        }

        return index;
    }



    @Override
    public void run()
    {
        try
        {
            if(districts.length > 1)
            {
                chooseStateDistrictAndTypeAndFinallySearchAndGetAvailableVaccines(state, districts);
            }
            else
            {
                search();
                getAvailableVaccines(state, districts[0]);
            }
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void getAvailableVaccines(String stateName, String districtName)
    {
        List<WebElement> dateElements = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("availability-date-ul")))
                .findElement(By.className("carousel-inner"))
                .findElements(By.tagName("slide"));


        HashMap<String, ArrayList<Vaccine>> vaccineHashMap = new HashMap<>();

        ArrayList<String> dates = new ArrayList<>();

        for(WebElement eachDateElement : dateElements)
        {
            String date = eachDateElement.findElement(By.tagName("p")).getText();

            if(date.isEmpty())
                break;

            dates.add(date);

            vaccineHashMap.put(date, new ArrayList<>());
        }






        WebElement centersBox = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("center-box")))
                .findElement(By.tagName("div"))
                .findElement(By.tagName("div"));

        List<WebElement> centersBoxElements = centersBox.findElements(By.tagName("div"));

        if(centersBoxElements.isEmpty())
        {
            getLogger().info("No Vaccine IN "+districtName+", "+stateName+"!");
            return;
        }



        boolean vaccineFound = false;

        for(WebElement eachCenterElement : centersBoxElements)
        {
            List<WebElement> insideElement = eachCenterElement
                    .findElements(By.tagName("div"));


            if(insideElement.isEmpty())
                continue;

            List<WebElement> centerBoxes = insideElement.get(0).findElements(By.tagName("div"));

            if(centerBoxes.isEmpty())
                continue;

            WebElement centerBox = centerBoxes.get(0);


            List<WebElement> centerNameTitles = centerBox.findElements(By.className("center-name-title"));

            if(centerNameTitles.isEmpty())
                continue;

            String centerName = centerNameTitles.get(0).getText();
            String centerAddress = centerBox.findElement(By.className("center-name-text")).getText();


            List<WebElement> slotAvWrap = insideElement.get(1).findElements(By.className("slot-available-wrap"));

            if(slotAvWrap.isEmpty())
            {
                continue;
            }

            List<WebElement> vaccineBoxes = slotAvWrap.get(0).findElements(By.className("vaccine-box"));


            for(int i = 0;i< vaccineBoxes.size(); i++)
            {
                WebElement eachVaccineBox = vaccineBoxes.get(i);

                WebElement aElement = eachVaccineBox.findElement(By.tagName("a"));

                String amountLeft = aElement.getText();

                if(amountLeft.equals("Booked") || amountLeft.equals("NA"))
                    continue;

                WebElement vaccineNameElement = eachVaccineBox.findElement(By.className("vaccine-cnt"));

                String vaccineName = vaccineNameElement.getText();


                WebElement ageLimitElement = eachVaccineBox.findElement(By.className("age-limit"));

                String ageLimit = ageLimitElement.getText();


                String date = dates.get(i);

                vaccineHashMap.get(date).add(new Vaccine(centerName, centerAddress, amountLeft, vaccineName, ageLimit));
                vaccineFound = true;
            }
        }


        if(vaccineFound)
        {
            getLogger().info("VACCINE AVAILABLE IN "+districtName+", "+stateName+" ...");
            new Timer().schedule(new Mail(stateName, districtName, vaccineHashMap), 0);
        }
        else
        {
            getLogger().info("No Vaccine IN "+districtName+", "+stateName+"!");
        }

        getLogger().info("Repeating after "+System.getProperty("repeat.millis")+" millis ...");

    }

    private final Logger logger;
    private Logger getLogger()
    {
        return logger;
    }

}
