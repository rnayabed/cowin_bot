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

public class Mail extends TimerTask
{
    private HashMap<String, ArrayList<Vaccine>> vaccineHashMap;

    private String from;
    private String pass;
    private String to;
    private String host;


    public Mail(HashMap<String, ArrayList<Vaccine>> vaccineHashMap)
    {
        this.vaccineHashMap = vaccineHashMap;

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
            stringBuilder.append("Date : ").append(date);

            stringBuilder.append("\n\n");

            ArrayList<Vaccine> vaccines = vaccineHashMap.get(date);

            for(int i = 0;i< vaccines.size();i++)
            {
                Vaccine vaccine = vaccines.get(i);

                stringBuilder.append((i+1)).append(".\n")
                        .append("Center Name: ").append(vaccine.getCenterName()).append("\n")
                        .append("Center Address: ").append(vaccine.getCenterAddress()).append("\n")
                        .append("Vaccine Name: ").append(vaccine.getVaccineName()).append("\n")
                        .append("Amount Left: ").append(vaccine.getAmountLeft()).append("\n")
                        .append("Age Limit: ").append(vaccine.getAgeLimit()).append("\n\n");
            }

            stringBuilder.append("\n\n\n");
        }

        stringBuilder.append("\n\ncowin_bot by rnayabed (Debayan Sutradhar)");

        return stringBuilder.toString();
    }


    @Override
    public void run()
    {
        // Get the default Session object.
        Session session = Session.getDefaultInstance(System.getProperties());

        try{
            // Create a default MimeMessage object.
            MimeMessage message = new MimeMessage(session);

            // Set From: header field of the header.
            message.setFrom(new InternetAddress(from));

            // Set To: header field of the header.
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));

            // Set Subject: header field
            message.setSubject("VACCINES AVAILABLE!");

            // Now set the actual message
            message.setText(getEmailBody(vaccineHashMap));

            // Send message
            Transport transport = session.getTransport("smtp");
            transport.connect(host, from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
            System.out.println("Sent message successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }

    }
}
