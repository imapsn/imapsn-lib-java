package com.github.imapsn;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ImapsnClient {

	JSONObject accountConfig;

	public ImapsnClient() throws EInvalidAccountConfig {
		this(System.getProperty("user.home") + "/.imapsn");
	}

	/**
	 * Read account configuration data from <code>filename</code>.
	 * 
	 * Expected format is:
	 * 
	 * <pre>
	 * {
	 *   "email-id": "foo@bar.com",
	 *   "account-name": "test",
	 *   "displayName": "John Doe",
	 *   "imap-host": "bar.com",
	 *   "imap-user": "foo",
	 *   "imap-password": "secret",
	 *   "smtp-host":  "smtp.bar.com",
	 *   "smtp-user": "foo@bar.com",
	 *   "smtp-password": "secret",
	 *   "smtp-port": "465",
	 *   "smtp-enable-ttls": true,
	 *   "private-key-password": "secret",
	 *   "imapsn-folder": "IMAPSN",
	 *   "imap-enable-ssl": true
	 * }
	 * </pre>
	 */
	public ImapsnClient(String filename) throws EInvalidAccountConfig {
		String key = "";
		try {
			FileInputStream fis = null;
			InputStreamReader isr = null;
			JSONTokener jt = null;
			fis = new FileInputStream(filename);
			isr = new InputStreamReader(fis, "UTF8");
			jt = new JSONTokener(isr);
			accountConfig = new JSONObject(jt);

			// validate
			String[] keys = { "account-name", "displayName", "email-id",
					"imap-host", "imap-user", "imap-password", "imap-port",
					"smtp-host", "smtp-password", "smtp-port",
					"smtp-enable-ttls", "imap-enable-ssl",
					"private-key-password" };
			for (int i = 0; i < keys.length; i++) {
				key = keys[i];
				accountConfig.get(key);
			}
			// default imapsn-folder if it's not there
			String imapsnFolder = accountConfig.optString("imapsn-folder",
					"IMAPSN");
			accountConfig.put("imapsn-folder", imapsnFolder);
		} catch (JSONException e) {
			throw new EInvalidAccountConfig("Missing config key, " + key, e);
		} catch (FileNotFoundException e) {
			throw new EInvalidAccountConfig(e);
		} catch (UnsupportedEncodingException e) {
			throw new EInvalidAccountConfig(e);
		}
	}

	protected String getKey(String key) {
		try {
			return accountConfig.getString(key);
		} catch (JSONException e) {
			throw new RuntimeException("bug in AccountConfig validation", e);
		}
	}

	public String getAccountName() {
		return getKey("account-name");
	}

	public String getDisplayName() {
		return getKey("displayName");
	}

	public String getEmailId() {
		return getKey("email-id");
	}

	// imap stuff

	public ImapClient getImapClient() {
		return new ImapClient(this);
	}

	public String getImapHost() {
		return getKey("imap-host");
	}

	public String getImapUser() {
		return getKey("imap-user");
	}

	public String getImapPassword() {
		return getKey("imap-password");
	}

	// smtp stuff

	public SmtpClientBase getSmtpAccount() {
		try {
			return new SmtpClient(accountConfig);
		} catch (JSONException e) {
			throw new RuntimeException("bug in account config", e);
		}
	}

	public String getSmtpHost() {
		return getKey("smtp-host");
	}

	public String getSmtpPassword() {
		return getKey("smtp-password");
	}

	public String getSmtpPort() {
		return getKey("smtp-port");
	}

	public String getSmtpEnableTtls() {
		return getKey("smtp-enable-ttls");
	}

	public boolean getImapEnableSsl() {
		return getKey("imap-enable-ssl").equalsIgnoreCase("true");
	}

	// private key

	public String getPrivateKeyPassword() {
		return getKey("private-key-password");
	}

	// imapsn folder name

	public String getImapsnFolderName() {
		return getKey("imapsn-folder");
	}

	public String getImapPort(String string) {
		return getKey("imap-port");
	}

}
