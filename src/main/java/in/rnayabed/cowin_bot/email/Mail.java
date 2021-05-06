package in.rnayabed.cowin_bot.email;

import in.rnayabed.cowin_bot.vaccine.Vaccine;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Mail extends TimerTask
{
    private HashMap<String, ArrayList<Vaccine>> vaccineHashMap;

    private String from;
    private String pass;
    private String to;
    private String host;

    private String stateName, districtName;

    private String body;

    public Mail(String stateName, String districtName, HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        this.vaccineHashMap = vaccineHashMap;
        this.stateName = stateName;
        this.districtName = districtName;
        this.body = getEmailBody(vaccineHashMap);

        logger = Logger.getLogger("in.rnayabed");

        from = System.getProperty("mail.smtp.user");
        pass = System.getProperty("mail.smtp.user.pass");

        to = System.getProperty("mail.smtp.to");

        host = System.getProperty("mail.smtp.host");
    }

    public String getEmailBody(HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        StringBuilder stringBuilder = new StringBuilder("Vaccines are available!\n\n\n");


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
                        .append("Limit: ").append(vaccine.getAgeLimit()).append("\n\n");
            }

            stringBuilder.append("\n\n\n");
        }

        stringBuilder.append("\n\ncowin_bot by rnayabed (Debayan Sutradhar)\n")
                .append("Version: ").append(System.getProperty("bot.version"))
                .append("\nSource: ").append(System.getProperty("bot.repo"));

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


            message.setSubject("VACCINES AVAILABLE IN "+districtName+", "+stateName);


            message.setText(getEmailBody(vaccineHashMap));


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
