package com.github.imapsn.test;


import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.github.imapsn.ImapsnClient;
import com.github.imapsn.EInvalidAccountConfig;
import com.github.imapsn.SmtpClientBase;
import junit.framework.TestCase;
/**
 * Unit test for simple App.
 */
public class TestSmtpAccount 
    extends TestCase
{
    public void testSend() throws EInvalidAccountConfig
    {
    	ImapsnClient ac = new ImapsnClient();
    	SmtpClientBase account = ac.getSmtpAccount();
    	// -- set up the message
    	boolean success = false;
    	MimeMessage msg = (MimeMessage) account.getNewMessage();
		try {
			msg.setFrom(new InternetAddress("user@foo.com"));
			msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("user@foo.com", false));
			msg.setSubject("The subject");
			msg.setText("the body");
			// -- Set some other header information --
			msg.setHeader("X-Mailer", "IMAPSN");
			msg.setSentDate(new Date());
			account.sendMessage(msg);
			success = true;
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}

        assertTrue( success );
    }
}
