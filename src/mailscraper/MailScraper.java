/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailscraper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.search.AndTerm;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

/**
 *
 * @author chauhan
 */
public class MailScraper {

    public static final String RECEIVED_HEADER_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final String RECEIVED_HEADER_REGEXP = "^[^;]+;(.+)$";

    public static void check(String host, String port, String storeType, String user,
            String password) {
        try {
            //create properties field
            Properties properties = new Properties();

            properties.put("mail.imap.host", host);
            properties.put("mail.imap.port", port);
            properties.put("mail.store.protocol", "imaps");
            properties.put("mail.imap.starttls.enable", "true");
            Session emailSession = Session.getDefaultInstance(properties);

            //create the POP3 store object and connect with the pop server
            Store store = emailSession.getStore("imaps");

            store.connect(host, user, password);

            // Get All folders
            Folder[] f = store.getDefaultFolder().list();
            for (Folder fd : f) {
                System.out.println(">> " + fd.getName());
            }

            //create the folder object and open it
            Folder emailFolder = store.getFolder("PWA Errors");
            emailFolder.open(Folder.READ_ONLY);

            Date someFutureDate = new GregorianCalendar(2018, 10, 20).getTime();
            Date somePastDate = new GregorianCalendar(2018, 10, 19).getTime();

            SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, someFutureDate);
            SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, somePastDate);
            SearchTerm andTerm = new AndTerm(olderThan, newerThan);
            Message[] messages = emailFolder.search(andTerm);

            // retrieve the messages from the folder in an array and print it
            // Message[] messages = emailFolder.getMessages();
            System.out.println("messages.length---" + messages.length);

            for (int i = 0, n = messages.length; i < n; i++) {
                Message message = messages[i];
                System.out.println("---------------------------------");
                System.out.println("Email Number " + (i + 1));
                System.out.println("Subject: " + message.getSubject());
                System.out.println("From: " + message.getFrom()[0]);
                System.out.println("Text: " + message.getContent().toString());
                System.out.println("ReceiveDate: " + message.getReceivedDate());
                if (i > 10) {
                    break;
                }
            }
            //close the store and folder objects
            emailFolder.close(false);
            store.close();
        } catch (NoSuchProviderException | IOException e) {
            e.printStackTrace();
        } catch (MessagingException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String host = "imap.gmail.com";// change accordingly
        String port = "993";
        String mailStoreType = "imaps";
        String username = System.getenv("GMAIL_SCRAP_EMAIL");// change accordingly
        String password = System.getenv("GMAIL_SCRAP_PWD");// change accordingly
        check(host, port, mailStoreType, username, password);
    }

    public Date resolveReceivedDate(MimeMessage message) throws MessagingException {
        if (message.getReceivedDate() != null) {
            return message.getReceivedDate();
        }
        String[] receivedHeaders = message.getHeader("Received");
        if (receivedHeaders == null) {
            return (Calendar.getInstance().getTime());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(RECEIVED_HEADER_DATE_FORMAT);
        Date finalDate = Calendar.getInstance().getTime();
        finalDate.setTime(0l);
        boolean found = false;
        for (String receivedHeader : receivedHeaders) {
            Pattern pattern = Pattern.compile(RECEIVED_HEADER_REGEXP);
            Matcher matcher = pattern.matcher(receivedHeader);
            if (matcher.matches()) {
                String regexpMatch = matcher.group(1);
                if (regexpMatch != null) {
                    regexpMatch = regexpMatch.trim();
                    try {
                        Date parsedDate = sdf.parse(regexpMatch);
                        //LogMF.debug(log, "Parsed received date {0}", parsedDate);
                        if (parsedDate.after(finalDate)) {
                            //finding the first date mentioned in received header
                            finalDate = parsedDate;
                            found = true;
                        }
                    } catch (Exception e) {
                        //LogMF.warn(log, "Unable to parse date string {0}", regexpMatch);
                    }
                } else {
                    //LogMF.warn(log, "Unable to match received date in header string {0}", receivedHeader);
                }
            }
        }
        return found ? finalDate : Calendar.getInstance().getTime();
    }
}
