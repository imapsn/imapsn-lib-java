/*
Copyright 2010 Jason Kantz. All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are
permitted provided that the following conditions are met:

   1. Redistributions of source code must retain the above copyright notice, this list of
      conditions and the following disclaimer.

   2. Redistributions in binary form must reproduce the above copyright notice, this list
      of conditions and the following disclaimer in the documentation and/or other materials
      provided with the distribution.

THIS SOFTWARE IS PROVIDED BY JASON KANTZ ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JASON KANTZ OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

The views and conclusions contained in the software and documentation are those of the
authors and should not be interpreted as representing official policies, either expressed
or implied, of Jason Kantz.
 */

package com.github.imapsn;

// http://java.sun.com/products/javamail/javadocs/index.html?com/sun/mail/imap/package-summary.html

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SubjectTerm;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.mail.imap.IMAPStore;

public class ImapClient implements ImapClientBase {

	ImapsnClient imapsnClient;
	Properties props;
	Session session;
	IMAPStore store;
	Folder inbox;
	Folder imapsn;
	AccountOwner owner;
	PersonStatusMap personStatusMap;
	PersonGroups personGroups;
	KeyMap keyMap;

	boolean isConnected = false;

	public ImapClient(ImapsnClient imapsn) {
		this.props = new Properties();
		this.imapsnClient = imapsn;
		if (imapsn.getImapEnableSsl()) {
			props.setProperty("mail.imap.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			// don't fallback to normal IMAP connections on failure.
			props.setProperty("mail.imap.socketFactory.fallback", "false");
			// use the simap port for imap/ssl connections.
			props.setProperty("mail.imap.socketFactory.port",
					imapsn.getImapPort("imap-port"));
		}

		// fill props with any information
		this.session = Session.getDefaultInstance(props, null);
		try {
			this.store = (IMAPStore) session.getStore("imap");
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
	}

	// properties

	public ImapsnClient getImapsnClient() {
		return imapsnClient;
	}

	public AccountOwner getAccountOwner() {
		assert (isConnected);
		return owner;
	}

	public PersonStatusMap getPersonStatusMap() {
		assert (isConnected);
		return personStatusMap;
	}

	public PersonGroups getPersonGroups() {
		assert (isConnected);
		return personGroups;
	}

	public KeyMap getKeyMap() {
		assert (isConnected);
		return keyMap;
	}

	public Session getSession() {
		return this.session;
	}

	public boolean isConnected() {
		return isConnected;
	}

	public Folder getImapsnFolder() {
		return imapsn;
	}

	// connections

	public void connect() throws Exception {
		this.store.connect(imapsnClient.getImapHost(),
				imapsnClient.getImapUser(), imapsnClient.getImapPassword());
		inbox = getFolder("INBOX");
		inbox.open(Folder.READ_WRITE);
		imapsn = openFolder(imapsnClient.getImapsnFolderName());
		owner = new AccountOwner(this);
		personStatusMap = new PersonStatusMap(this);
		personGroups = new PersonGroups(this);
		keyMap = new KeyMap(this);
		isConnected = true;
	}

	public void close() throws MessagingException {
		if (imapsn.isOpen()) {
			imapsn.close(true);
		}
		if (inbox.isOpen()) {
			inbox.close(true);
		}

		this.store.close();
		isConnected = false;
	}

	// messages and folders

	public MimeMessage getNewMessage() {
		return new MimeMessage(this.session);
	}

	public Folder getFolder(String name) throws MessagingException {
		return this.store.getFolder(name);
	}

	/**
	 * Open and return the folder, creating it if it doesn't exist.
	 */
	public Folder openFolder(String name) throws MessagingException {
		assert isConnected();
		Folder folder = inbox.getFolder(name);
		boolean success = false;
		if (!folder.exists()) {
			success = folder.create(Folder.HOLDS_MESSAGES);
		} else {
			success = true;
		}
		if (success) {
			folder.open(Folder.READ_WRITE);
		} else {
			throw new MessagingException("Cannot open the folder " + name);
		}
		return folder;
	}

	public Message[] getNewMessages() throws MessagingException {
		return getNewMessages(null);
	}
	public Message[] getNewMessages(String messageType) throws MessagingException {
		String folderName = owner.getNewMessageFolder();
		Folder folder;
		if (folderName.equals("INBOX")) {
			folder = inbox;
		} else {
			folder = openFolder(folderName);
		}
		String searchString;
		if (messageType != null) {
			searchString = "[IMAPSN] " + messageType + ":";
		} else {
			searchString = "[IMAPSN]";
		}
		return folder.search(new SubjectTerm(searchString));
	}

	// storage interface

	public void put(String path, JSONObject json, String text)
			throws MessagingException {
		String[] parts = path.split("/");
		String fileName = parts[parts.length - 1];
		MimeUtil.saveJsonData(this, imapsn, json, path, fileName);
	}

	public void put(String path, JSONObject json) throws MessagingException {
		put(path, json, "");
	}

	public JSONObject get(String path) throws MessagingException, IOException {
		JSONObject ret = null;
		MimeMessage mm = MimeUtil.getFolderMessage(imapsn, path);
		if (mm != null) {
			String[] parts = path.split("/");
			String fileName = parts[parts.length - 1];
			try {
				ret = MimeUtil.getAttachedJson(mm, fileName);
			} catch (JSONException e) {
			}
		}
		return ret;
	}
}
