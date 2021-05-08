package in.rnayabed.cowin_bot.email;

import in.rnayabed.cowin_bot.vaccine.SearchType;
import in.rnayabed.cowin_bot.vaccine.Vaccine;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Mail extends TimerTask
{

    private String from;
    private String pass;
    private String to;
    private String host;

    private String stateName, districtName;

    private String body;
    private String pinCode;

    private SearchType searchType;

    public Mail(String stateName, String districtName, HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        this();

        this.stateName = stateName;
        this.districtName = districtName;

        this.body = getEmailBody(vaccineHashMap);

        this.searchType = SearchType.STATE_DISTRICT;
    }

    public Mail(String pinCode, HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        this();

        this.pinCode = pinCode;

        this.body = getEmailBody(vaccineHashMap);

        this.searchType = SearchType.PIN;
    }

    public Mail(String stateName, String districtName, String date, Vaccine vaccine)
    {
        this();

        this.stateName = stateName;
        this.districtName = districtName;

        this.body = getEmailBody(date, vaccine);

        this.searchType = SearchType.STATE_DISTRICT;
    }

    public Mail(String pinCode, String date, Vaccine vaccine)
    {
        this();

        this.pinCode = pinCode;

        this.body = getEmailBody(date, vaccine);

        this.searchType = SearchType.STATE_DISTRICT;
    }


    private Mail()
    {
        logger = Logger.getLogger("in.rnayabed");

        from = System.getProperty("mail.smtp.user");
        pass = System.getProperty("mail.smtp.user.pass");

        to = System.getProperty("mail.smtp.to");

        host = System.getProperty("mail.smtp.host");
    }




    public String getEmailBody(HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        StringBuilder stringBuilder = new StringBuilder();


        for(String date : vaccineHashMap.keySet())
        {
            ArrayList<Vaccine> vaccines = vaccineHashMap.get(date);

            if(vaccines.size() == 0)
                continue;

            stringBuilder.append("Date : ").append(date);

            stringBuilder.append("\n\n");

            for(int i = 0;i< vaccines.size();i++)
            {
                Vaccine vaccine = vaccines.get(i);

                stringBuilder.append((i+1)).append(".\n")
                        .append("Center Name: ").append(vaccine.getCenterName()).append("\n")
                        .append("Center Address: ").append(vaccine.getCenterAddress()).append("\n")
                        .append("Vaccine Name: ").append(vaccine.getVaccineName()).append("\n")
                        .append("Amount Left: ").append(vaccine.getAmountLeft()).append("\n")
                        .append("Limit: ").append(vaccine.getAgeLimit());
            }

            stringBuilder.append("\n\n\n");
        }

        stringBuilder.append(getBodyFooter());



        return stringBuilder.toString();
    }

    public String getEmailBody(String date, Vaccine vaccine)
    {
        return "Instant Vaccine notifier. Larger mail with all available vaccines coming soon." +
                "\nDate : " + date +
                "\nCenter Name: " + vaccine.getCenterName() +
                "\nCenter Address: " + vaccine.getCenterAddress() +
                "\nVaccine Name: " + vaccine.getVaccineName() +
                "\nAmount Left: " + vaccine.getAmountLeft() +
                "\nLimit: " + vaccine.getAgeLimit() +
                getBodyFooter();
    }

    public String getBodyFooter()
    {
        StringBuilder stringBuilder= new StringBuilder();

        stringBuilder.append("\n\ncowin_bot by rnayabed (Debayan Sutradhar)")
                .append("\nVersion: ").append(System.getProperty("bot.version"))
                .append("\nSource: ").append(System.getProperty("bot.repo"));

        try
        {
            stringBuilder.append("\nHostname : ").append(InetAddress.getLocalHost().getHostName());
        }
        catch (Exception e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            getLogger().info("Failed to get system name !");
        }

        return stringBuilder.toString();
    }

    private Logger logger;
    private Logger getLogger()
    {
        return logger;
    }

    @Override
    public void run()
    {
        getLogger().info("Sending mail ...");

        Session session = Session.getDefaultInstance(System.getProperties());

        try
        {

            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(from));


            String[] messages = to.split(",");

            InternetAddress[] internetAddresses = new InternetAddress[messages.length];
            for (int i = 0;i<messages.length;i++)
            {
                internetAddresses[i] = new InternetAddress(messages[i].strip());
            }


            message.addRecipients(Message.RecipientType.TO,
                    internetAddresses);

            if(searchType == SearchType.STATE_DISTRICT)
                message.setSubject("VACCINE AVAILABLE IN "+districtName+", "+stateName);
            else if(searchType == SearchType.PIN)
                message.setSubject("VACCINE AVAILABLE IN "+pinCode);

            message.setText(body);


            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            getLogger().info("... Done!");
        }
        catch (MessagingException e)
        {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
            getLogger().info("... Failed to send mail!");
        }

    }
}
