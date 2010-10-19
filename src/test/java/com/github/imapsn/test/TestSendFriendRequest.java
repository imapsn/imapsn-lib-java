package com.github.imapsn.test;

/**
 * This isn't the world's most polished test. You have to set breakpoints 
 * in each test and look at watch inboxes to see that messages have arrived
 * before continuing at each step.  You'll need two config files for two accounts: 
 * ~/.imapsn and ~/.imapsn2
 */

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

import com.github.imapsn.Friend;
import com.github.imapsn.ImapClientBase;
import com.github.imapsn.ImapsnClient;
import com.github.imapsn.NewsItem;

public class TestSendFriendRequest {

	@Test
	public void testSendFriendRequest() throws MessagingException {
		ImapClientBase imap = null;
		try {
			ImapsnClient ac = new ImapsnClient();
			imap = (ImapClientBase) ac.getImapClient();
			imap.connect();
			Friend.sendFriendRequest(imap, new InternetAddress(
					"jasonkantz@gmail.com"));
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			if (imap != null) {
				imap.close();
			}
		}
	}

	@Test
	public void testProcessFriendRequest() throws Exception {
		ImapsnClient ac = new ImapsnClient(System.getProperty("user.home")
				+ "/.imapsn2");
		ImapClientBase imap = (ImapClientBase) ac.getImapClient();

		try {
			imap.connect();
			Message[] mm = imap.getNewMessages("friend-request");
			for (int i = 0; i < mm.length; i++) {
				Friend.acceptFriendRequest(imap, (MimeMessage) mm[i]);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			imap.close();
		}
	}

	@Test
	public void testProcessFriendResponse() throws Exception {
		ImapsnClient ac = new ImapsnClient();
		ImapClientBase imap = (ImapClientBase) ac.getImapClient();

		try {
			imap.connect();
			Message[] mm = imap.getNewMessages("friend-response");
			for (int i = 0; i < mm.length; i++) {
				Friend.processFriendResponse(imap, (MimeMessage) mm[i]);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			imap.close();
		}
	}

	@Test
	public void testProcessNewsItem() throws Exception {

		ImapsnClient ac = new ImapsnClient();
		ImapClientBase imap = (ImapClientBase) ac.getImapClient();
		try {
			imap.connect();
			Message[] mm = imap.getNewMessages("news-item");
			for (int i = 0; i < mm.length; i++) {
				NewsItem.processNewsItem(imap, (MimeMessage) mm[i]);
			}

		} finally {
			imap.close();
		}

		ac = new ImapsnClient(System.getProperty("user.home") + "/.imapsn2");
		imap = (ImapClientBase) ac.getImapClient();
		try {
			imap.connect();
			Message[] mm = imap.getNewMessages("news-item");
			for (int i = 0; i < mm.length; i++) {
				NewsItem.processNewsItem(imap, (MimeMessage) mm[i]);
			}

		} finally {
			imap.close();
		}
	}

}
