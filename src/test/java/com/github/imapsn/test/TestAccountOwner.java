package com.github.imapsn.test;

import static org.junit.Assert.assertTrue;

import javax.mail.Folder;

import org.junit.Before;
import org.junit.Test;

import com.github.imapsn.AccountOwner;
import com.github.imapsn.EInvalidAccountConfig;
import com.github.imapsn.ImapClient;
import com.github.imapsn.ImapsnClient;

/**
 * Unit test for simple App.
 */
public class TestAccountOwner {

	@Before
	public void deleteImapsnFolder() throws Exception {
		ImapsnClient imapsn;
		try {
			imapsn = new ImapsnClient();
			ImapClient imap = imapsn.getImapClient();
			imap.connect();
			Folder imapsnFolder = imap.getImapsnFolder();
			imapsnFolder.close(true);
			if (imapsnFolder.exists()) {
				boolean success = imapsnFolder.delete(true);
				assertTrue(success);
			}
			imap.close();
		} catch (EInvalidAccountConfig e) {
			throw new RuntimeException("Something wrong with ~/.imapsn?");
		}
	}

	@Test
	public void testInitFolders() throws Throwable {
		ImapsnClient accountConfig = new ImapsnClient();
		ImapClient imap = accountConfig.getImapClient();

		// see if it's there, modify it
		imap.connect();
		AccountOwner owner = imap.getAccountOwner();
		assertTrue(owner.getEmail().equals("jason@kantz.com"));
		assertTrue(owner.getJson().getString("wall-group").equals("everybody"));
		owner.getJson().put("wall-group", "family");
		owner.save();
		imap.close();

		// see if changes were persisted
		imap.connect();
		owner = imap.getAccountOwner();
		assertTrue(owner.getJson().getString("wall-group").equals("family"));
		imap.close();

	}

}