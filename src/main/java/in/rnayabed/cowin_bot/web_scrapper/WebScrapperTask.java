package in.rnayabed.cowin_bot.web_scrapper;

import in.rnayabed.cowin_bot.email.Mail;
import in.rnayabed.cowin_bot.exception.BotException;
import in.rnayabed.cowin_bot.vaccine.Vaccine;
import in.rnayabed.cowin_bot.vaccine.VaccineType;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
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
    private VaccineType vaccineType;

    private WebDriver webDriver;
    private WebDriverWait webDriverWait;


    public WebScrapperTask()
    {
        logger = Logger.getLogger("in.rnayabed");

        url = System.getProperty("cowin.website.url");
        state = System.getProperty("search.state").strip();
        districts = System.getProperty("search.districts").split(",");
        vaccineType = VaccineType.valueOf(System.getProperty("search.vaccine.type"));

        getLogger().log(Level.INFO, "Setting up firefox driver ...");
        webDriver = new FirefoxDriver();
        webDriverWait = new WebDriverWait(webDriver, 10);
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

        WebElement statusSwitchElementDiv = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("status-switch")));

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


    private void chooseStateDistrictAndType(String stateName, String districtName) throws BotException
    {
       WebElement searchdistwrperDiv = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("searchdistwrper")));

       List<WebElement> selectorDivs = searchdistwrperDiv.findElements(By.className("pullleft"));

       WebElement stateSelectorBox = selectorDivs.get(0).findElement(By.tagName("mat-select"));
        stateSelectorBox.click();

       boolean stateFound = selectOption(stateName,"mat-select-0-panel");

       if(!stateFound)
       {
           throw new BotException("Unable to find state : '"+stateName+"'");
       }




       WebElement districtSelectorBox = selectorDivs.get(1).findElement(By.tagName("mat-select"));
       districtSelectorBox.click();

       boolean districtFound = selectOption(districtName.strip(),"mat-select-2-panel");

       if(!districtFound)
       {
           throw new BotException("Unable to find district : '"+districtName+"'");
       }




       WebElement ageFilterBlock = searchdistwrperDiv.findElement(By.className("agefilterblock"));

       List<WebElement> choices = ageFilterBlock.findElements(By.className("form-check"));

       if(vaccineType == VaccineType.V_ALL)
           choices.get(1).click();
       else if(vaccineType == VaccineType.V_18_TO_44)
           choices.get(2).click();
       else if(vaccineType == VaccineType.V_45_PLUS)
           choices.get(3).click();



    }

    private void search()
    {
        webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("searchdistwrper")))
                .findElement(By.className("pinbtncover"))
                .findElement(By.tagName("button"))
                .click();
    }

    private boolean selectOption(String option, String selectorIdName)
    {
        WebElement selectorPanel = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.id(selectorIdName)));

        List<WebElement> options = selectorPanel.findElements(By.tagName("mat-option"));

        boolean optionFound = false;
        for(WebElement eachOption : options)
        {
            WebElement span = eachOption.findElement(By.className("mat-option-text"));

            if(span.getText().equalsIgnoreCase(option))
            {
                eachOption.click();
                optionFound = true;
                break;
            }
        }

        return optionFound;
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
            e.printStackTrace();
        }


    }

    private void getAvailableVaccines(String stateName, String districtName)
    {
        // first get dates


        List<WebElement> dateElements = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("availability-date-ul")))
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






        WebElement centersBox = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("center-box")))
                .findElement(By.tagName("div"))
                .findElement(By.tagName("div"));

        List<WebElement> centersBoxElements = centersBox.findElements(By.tagName("div"));

        if(centersBoxElements.isEmpty())
        {
            getLogger().info("No Vaccine available!");
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
            logger.info("VACCINE AVAILABLE IN "+districtName+", "+stateName+" ...");
            new Timer().schedule(new Mail(stateName, districtName, vaccineHashMap), 0);
        }
        else
        {
            logger.info("No Vaccine IN "+districtName+", "+stateName+"!");
        }

        logger.info("Repeating after "+System.getProperty("repeat.millis")+" milis ...");

    }

    private final Logger logger;
    private Logger getLogger()
    {
        return logger;
    }

}
