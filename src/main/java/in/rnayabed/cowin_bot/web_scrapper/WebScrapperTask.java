package in.rnayabed.cowin_bot.web_scrapper;

import in.rnayabed.cowin_bot.email.Mail;
import in.rnayabed.cowin_bot.exception.BotException;
import in.rnayabed.cowin_bot.vaccine.SearchFilter;
import in.rnayabed.cowin_bot.vaccine.SearchType;
import in.rnayabed.cowin_bot.vaccine.Vaccine;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebScrapperTask extends TimerTask
{
    private String url;
    private String state;
    private String[] districts;
    private String[] pins;
    private String dateLimit;

    private String browserUserDataDirectory;
    private String browserProfile;

    private WebDriver webDriver;
    private WebDriverWait webDriverWait;

    private SearchType searchType;

    private boolean instantMail;

    private SearchFilter[] searchFilters = null;

    private boolean isLoggedIn = false;

    private String thisTabHandle;

    public String getThisTabHandle()
    {
        return thisTabHandle;
    }

    private GoogleMessagesTask googleMessagesTask;

    private String otp = null;

    private String authWebsite;
    private String phoneNumber;
    private int googleMessagesAuthTimeoutInSeconds;
    private String convoWebsite;

    private String[] senders;
    private int otpIndex;

    private int otpValidityMin;

    public WebScrapperTask()
    {
        logger = Logger.getLogger("in.rnayabed");

        instantMail = System.getProperty("instant.mail").equals("true");

        dateLimit = System.getProperty("search.date.limit").strip();

        this.authWebsite = System.getProperty("google.messages.auth.website.url");
        this.convoWebsite = System.getProperty("google.messages.convo.website.url");
        this.browserUserDataDirectory = System.getProperty("browser.user.data.dir");
        this.browserProfile = System.getProperty("browser.profile");

        this.phoneNumber = System.getProperty("otp.phone.number");
        this.googleMessagesAuthTimeoutInSeconds = Integer.parseInt(System.getProperty("google.messages.auth.timeout.seconds"));

        searchType = SearchType.valueOf(System.getProperty("search.type").strip());

        districtHashMap= new HashMap<>();

        url = System.getProperty("cowin.website.url");
        state = System.getProperty("search.state").strip();

        districts = System.getProperty("search.districts").split(",");

        for(int j = 0;j<districts.length;j++)
        {
            districts[j] = districts[j].strip();
        }


        pins = System.getProperty("search.pins").split(",");

        for(int j = 0;j<pins.length;j++)
        {
            pins[j] = pins[j].strip();
        }

        this.senders = System.getProperty("otp.senders").split(",");

        for(int i =0;i<senders.length;i++)
        {
            senders[i] = senders[i].strip();
        }

        this.otpIndex = Integer.parseInt(System.getProperty("otp.message.space.index"));

        this.otpValidityMin = Integer.parseInt(System.getProperty("otp.validity.min"));

        if(!System.getProperty("search.filters").isBlank())
        {
            String[] tmpFilters = System.getProperty("search.filters").split(",");

            searchFilters = new SearchFilter[tmpFilters.length];
            for(int j = 0;j<tmpFilters.length;j++)
            {
                searchFilters[j] = SearchFilter.valueOf(tmpFilters[j].strip());
            }
        }


        BrowserType browserType = BrowserType.valueOf(System.getProperty("browser.choice"));
        boolean runHeadless = System.getProperty("browser.run.headless").equalsIgnoreCase("true");

        String windowHeight = System.getProperty("browser.window.height").strip();
        String windowWidth = System.getProperty("browser.window.width").strip();

        if(browserType == BrowserType.CHROME || browserType == BrowserType.CHROMIUM)
        {
            getLogger().log(Level.INFO, "Setting up chrome driver ...");

            ChromeOptions chromeOptions = new ChromeOptions();

            if(runHeadless)
            {
                chromeOptions.addArguments("--headless","--start-maximized");
            }

            chromeOptions.addArguments("--window-size="+windowWidth+","+windowHeight,
                    "user-data-dir="+browserUserDataDirectory,
                    "profile-directory="+browserProfile);

            webDriver = new ChromeDriver(chromeOptions);
        }
        else if(browserType == BrowserType.EDGE)
        {
            getLogger().log(Level.INFO, "Setting up edge driver ...");

            EdgeOptions edgeOptions = new EdgeOptions();

            if(runHeadless)
            {
                edgeOptions.addArguments("--headless");
                edgeOptions.addArguments("--start-maximized");
            }

            edgeOptions.addArguments("--window-size="+windowWidth+","+windowHeight,
                    "user-data-dir="+browserUserDataDirectory,
                    "profile-directory="+browserProfile);

            webDriver = new EdgeDriver(edgeOptions);
        }
        else if (browserType == BrowserType.FIREFOX || browserType == BrowserType.GECKO)
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
            throw new IllegalArgumentException("browser.choice is invalid !");
        }


        webDriverWait = new WebDriverWait(webDriver, Duration.ofSeconds(Long.parseLong(System.getProperty("timeout.seconds"))));
        getLogger().log(Level.INFO, "... Done!");


        try
        {

            thisTabHandle = webDriver.getWindowHandle();

            googleMessagesTask = new GoogleMessagesTask(webDriver, webDriverWait, phoneNumber, googleMessagesAuthTimeoutInSeconds,
                    authWebsite, convoWebsite, senders, otpIndex, otpValidityMin);

            googleMessagesTask.load();

            loadCowinWebsite();
            sendOTP();

            sleep(3000);

            while(webDriver.getCurrentUrl().equals(url))
            {
                otp = googleMessagesTask.getOTP();


                if(otp != null)
                {
                    isLoggedIn = true;

                    getLogger().info("Successfully obtained OTP : "+otp);
                }
                else
                {
                    webDriver.close();
                    return;
                }


                sleep(3000);

                webDriver.switchTo().window(getThisTabHandle());
                WebElement otpInputBox = webDriver.findElement(By.tagName("input"));

                otpInputBox.sendKeys(otp);

                WebElement button = webDriver.findElement(By.tagName("ion-button"));

                JavascriptExecutor executor = (JavascriptExecutor)webDriver;
                executor.executeScript("arguments[0].click();", button);

                sleep(2000);
            }


            sleep(3000);
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("btnlist")))
                    .findElement(By.tagName("a"))
                    .click();

            sleep(5000);



            selectSearchByDistrictOrPin();


            if(searchType == SearchType.STATE_DISTRICT)
            {
                if(districts.length == 1)
                {
                    chooseStateDistrictAndType(state, districts[0]);
                }
            }
            else if(searchType == SearchType.PIN)
            {
                if(pins.length == 1)
                {
                    choosePin(pins[0]);
                }
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
        webDriver.switchTo().window(getThisTabHandle());

        getLogger().log(Level.INFO, "Loading cowin website ("+url+") ...");
        webDriver.get(url);
        getLogger().log(Level.INFO, "... Done!");
    }

    private void sendOTP()
    {
//        WebElement formElement = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("login-block")))
//                .findElement(By.tagName("ion-row"))
//                .findElements(By.tagName("ion-col")).get(1);
//
//        WebElement inputElement = formElement.findElement(By.tagName("ion-item"))
//                .findElement(By.tagName("mat-form-field"))
//                .findElement(By.tagName("div"))
//                .findElement(By.tagName("div"))
//                .findElement(By.tagName("div"))
//                .findElement(By.tagName("input"));
//
//
//        inputElement.sendKeys(phoneNumber);
//
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        WebElement buttonElement = formElement.findElement(By.className("covid-button-desktop"))
//                .findElement(By.tagName("ion-button"));
//
//        buttonElement.click();
//
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        sleep(3000);

        webDriver.switchTo().window(getThisTabHandle());
        WebElement otpInputBox = webDriver.findElement(By.tagName("input"));

        otpInputBox.sendKeys(phoneNumber);

        WebElement button = webDriver.findElement(By.tagName("ion-button"));
        button.click();

    }

    private void selectSearchByDistrictOrPin()
    {

        WebElement statusSwitchElementDiv = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("status-switch")));

        if(webDriver.findElements(By.className("mat-select-48")).size() == 0)
        {
            if(searchType == SearchType.STATE_DISTRICT)
            {
                getLogger().log(Level.INFO, "Selecting search by district ...");
                statusSwitchElementDiv.click();
            }
        }
        else
        {
            if(searchType == SearchType.PIN)
            {
                getLogger().log(Level.INFO, "Selecting search by PIN ...");
                statusSwitchElementDiv.click();
            }
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

    private void choosePinAndTypeAndFinallySearchAndGetAvailableVaccines(String[] pins) throws BotException
    {
        for(String pin: pins)
        {
            choosePin(pin);
            search();
            getAvailableVaccines(pin);
        }
    }

    private HashMap<String, Integer> districtHashMap;

    private void chooseStateDistrictAndType(String stateName, String districtName) throws BotException
    {
        if(stateSelectorBox == null)
        {
            List<WebElement> matSelects = webDriverWait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("mat-select")));

            for(WebElement element : matSelects)
            {
                if(element.getAttribute("formcontrolname").equals("state_id"))
                {
                    stateSelectorBox = element;
                }

                if(element.getAttribute("formcontrolname").equals("district_id"))
                {
                    districtSelectorBox = element;
                }
            }
        }

        String alreadyPresent = stateSelectorBox
               .findElement(By.tagName("div"))
               .findElement(By.tagName("div"))
               .findElement(By.tagName("span")).getText();


        if(!alreadyPresent.equalsIgnoreCase(stateName))
        {
            stateSelectorBox.click();

            int stateFound = selectOption(stateName, -1);

            if(stateFound == -1)
            {
                throw new BotException("Unable to find state : '"+stateName+"'");
            }
        }


        sleep(1000);

        districtSelectorBox.click();

        int oldVal = districtHashMap.getOrDefault(districtName, -1);

        int districtFound = selectOption(districtName, oldVal);

        if(districtFound == -1)
        {
            throw new BotException("Unable to find district : '"+districtName+"'");
        }

        districtHashMap.put(districtName, districtFound);
    }


    private WebElement pinSelectorBox = null;

    private void choosePin(String pinCode)
    {
        if(pinSelectorBox == null)
        {
            pinSelectorBox = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("pin-search")))
                    .findElement(By.tagName("input"));
        }


        pinSelectorBox.clear();
        pinSelectorBox.sendKeys(pinCode);
    }

    private WebElement stateSelectorBox = null;
    private WebElement districtSelectorBox = null;

    private WebElement age18PlusFilterWebElement = null;
    private WebElement age45PlusFilterWebElement = null;
    private WebElement covishieldFilterWebElement = null;
    private WebElement covaxinFilterWebElement = null;
    private WebElement sputnikVFilterWebElement = null;
    private WebElement freeFilterWebElement = null;
    private WebElement paidFilterWebElement = null;


    private WebElement searchButton = null;

    private void search()
    {
        if(searchButton == null)
        {
            searchButton = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("pin-search-btn")));

            JavascriptExecutor executor = (JavascriptExecutor)webDriver;
            executor.executeScript("arguments[0].click();", searchButton);
        }


        searchButton.click();




        if(age18PlusFilterWebElement == null)
        {
            WebElement filtersBlock = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("agefilterblock")));

            List<WebElement> filters = filtersBlock.findElements(By.className("form-check"));

            age18PlusFilterWebElement = filters.get(0);
            age45PlusFilterWebElement = filters.get(1);
            covishieldFilterWebElement = filters.get(2);
            covaxinFilterWebElement = filters.get(3);
            sputnikVFilterWebElement = filters.get(4);
            freeFilterWebElement = filters.get(5);
            paidFilterWebElement = filters.get(6);
        }


        if(searchFilters != null)
        {
            for(SearchFilter searchFilter : searchFilters)
            {
                if(searchFilter == SearchFilter.AGE_18_PLUS)
                    age18PlusFilterWebElement.click();
                else if(searchFilter == SearchFilter.AGE_45_PLUS)
                    age45PlusFilterWebElement.click();
                else if (searchFilter == SearchFilter.COVISHIELD)
                    covishieldFilterWebElement.click();
                else if(searchFilter == SearchFilter.COVAXIN)
                    covaxinFilterWebElement.click();
                else if(searchFilter == SearchFilter.SPUTNIKV)
                    sputnikVFilterWebElement.click();
                else if(searchFilter == SearchFilter.FREE)
                    freeFilterWebElement.click();
                else if(searchFilter == SearchFilter.PAID)
                    paidFilterWebElement.click();
            }
        }
    }

    private int selectOption(String option, int oldVal)
    {
        WebElement selectorPanel = webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("mat-select-panel")));

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
            if(districts.length > 1 || pins.length > 1)
            {
                if(searchType == SearchType.STATE_DISTRICT)
                    chooseStateDistrictAndTypeAndFinallySearchAndGetAvailableVaccines(state, districts);
                else if(searchType == SearchType.PIN)
                    choosePinAndTypeAndFinallySearchAndGetAvailableVaccines(pins);
            }
            else
            {
                search();

                if(searchType == SearchType.STATE_DISTRICT)
                    getAvailableVaccines(state, districts[0]);
                else
                    getAvailableVaccines(pins[0]);
            }



            getLogger().info("Repeating after "+System.getProperty("repeat.millis")+" millis ...");
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    int stepCount = 0;

    private void getAvailableVaccines(String pinCode)
    {
        getAvailableVaccines(pinCode, null, null);
    }

    private void getAvailableVaccines(String stateName, String districtName)
    {
        getAvailableVaccines(null, stateName, districtName);
    }



    private void getAvailableVaccines(String pinCode, String stateName, String districtName)
    {
        WebElement availabilityDateUl = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("availability-date-ul")));


        if(nextButton == null)
        {
            nextButton = availabilityDateUl.findElement(By.className("carousel-control-next"));

            prevButton = availabilityDateUl.findElement(By.className("carousel-control-prev"));
        }



        while(stepCount > 0)
        {

            prevButton.click();
            stepCount--;
        }


        boolean vaccineFound = false;


        ArrayList<String> dates = new ArrayList<>();
        HashMap<String, ArrayList<Vaccine>> vaccineHashMap = new HashMap<>();

        boolean dateReached = false;


        while(true)
        {

            WebElement centersBox = webDriverWait.until(ExpectedConditions.visibilityOfElementLocated(By.className("center-box")))
                    .findElement(By.tagName("div"));

            if(centersBox.findElements(By.tagName("ion-item")).size() == 1)
                break;

            WebElement sub = centersBox.findElement(By.tagName("div"));

            List<WebElement> centersBoxElements = sub.findElements(By.tagName("div"));

            if(centersBoxElements.isEmpty())
            {

                break;
            }



            List<WebElement> dateElements = availabilityDateUl
                    .findElement(By.className("carousel-inner"))
                    .findElements(By.tagName("slide"));



            for(WebElement eachDateElement : dateElements)
            {

                String date = eachDateElement.findElement(By.tagName("p")).getText();

                if(date.isEmpty())
                    break;

                if(!dateReached)
                {
                    dates.add(date);
                    vaccineHashMap.put(date, new ArrayList<>());
                }

                if(dateLimit.equals(date))
                {
                    dateReached = true;
                }


            }




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


                for(int i = 0;i< dates.size(); i++)
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

                    Vaccine vaccine = new Vaccine(centerName, centerAddress, amountLeft, vaccineName, ageLimit);

                    vaccineHashMap.get(date).add(vaccine);

                    if(instantMail)
                    {
                        instantMail = false;

                        if(searchType == SearchType.STATE_DISTRICT)
                        {
                            getLogger().info("INSTANT VACCINE AVAILABLE IN "+districtName+", "+stateName+" ...");
                            new Timer().schedule(new Mail(stateName, districtName, date, vaccine), 0);
                        }
                        else if(searchType == SearchType.PIN)
                        {
                            getLogger().info("INSTANT VACCINE AVAILABLE IN "+pinCode+" ...");
                            new Timer().schedule(new Mail(pinCode, date, vaccine), 0);
                        }
                    }

                    vaccineFound=true;
                }
            }

            if(dateReached)
                break;

            while(true)
            {
                try
                {
                    nextButton.click();
                    break;
                }
                catch (ElementClickInterceptedException e)
                {
                    sleep(500);
                }
            }

            stepCount++;
        }



        if(vaccineFound)
        {
            if(searchType == SearchType.STATE_DISTRICT)
            {
                getLogger().info("VACCINE AVAILABLE IN "+districtName+", "+stateName+" ...");
                new Timer().schedule(new Mail(stateName, districtName, vaccineHashMap), 0);
            }
            else if(searchType == SearchType.PIN)
            {
                getLogger().info("VACCINE AVAILABLE IN "+pinCode+" ...");
                new Timer().schedule(new Mail(pinCode, vaccineHashMap), 0);
            }
        }
        else
        {
            if(searchType == SearchType.STATE_DISTRICT)
            {
                getLogger().info("No Vaccine IN "+districtName+", "+stateName+" ...");
            }
            else if(searchType == SearchType.PIN)
            {
                getLogger().info("No Vaccine IN "+pinCode+" ...");
            }
        }
    }

    private final Logger logger;
    private Logger getLogger()
    {
        return logger;
    }

    private WebElement nextButton = null;
    private WebElement prevButton = null;


    private void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}
