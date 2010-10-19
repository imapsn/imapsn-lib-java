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

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Message.RecipientType;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class AccountOwner implements ImapsnDataFile {
	private static String FILENAME = "/account-owner.json";
	ImapClientBase imap;
	JSONObject ownerJson;

	public AccountOwner(ImapClientBase imap) throws EInvalidConfigData,
			EAccountOwnerReadError, EAccountOwnerSaveError, MessagingException {
		this.imap = imap;

		try {
			ownerJson = imap.get(getFilename());
		} catch (IOException e) {
			throw new EAccountOwnerReadError(e);
		}
		if (ownerJson == null) {
			ownerJson = defaultOwnerJson();
			save();
		}
		validateOwnerJson(ownerJson);
	}

	// properties

	public String getFilename() {
		return FILENAME;
	}

	public JSONObject getJson() {
		return this.ownerJson;
	}

	// persisting back to IMAPSN

	public void save() throws MessagingException {
		imap.put(getFilename(), ownerJson);
	}

	public JSONObject defaultOwnerJson() {
		try {
			ImapsnClient imapsnClient = imap.getImapsnClient();
			JSONObject data = new JSONObject();

			// fill in defaults
			data.put("id", newId());
			data.put("new-message-folder", "INBOX");
			data.put("wall-group", "everybody");

			// construct account-owner

			String emailId = imapsnClient.getEmailId();
			JSONObject email = new JSONObject();
			email.put("type", "imapsn");
			email.put("value", emailId);

			JSONObject person = new JSONObject();
			person.put("displayName", imapsnClient.getKey("displayName"));
			person.put("id", "acct:" + emailId + "#0");
			person.put("objectType", "person");
			person.put("email", email);

			data.put("account-owner", person);

			// generate the public and private keys

			String[] keys = Pkcs.generateKeys(imapsnClient
					.getPrivateKeyPassword());
			person.put("publicKey", keys[0]);
			person.put("keyhash", Pkcs.magicKeyHash(keys[0]));

			data.put("privateKey", keys[1]);

			return data;

		} catch (JSONException e) {
			throw new RuntimeException("bug: invalid imapsn account config", e);
		}
	}

	// magic envelope interface

	public JSONObject makeMagicEnvelope(JSONObject json, String password) {
		JSONObject ret = null;
		try {
			RSAPrivateKey pk = Pkcs.decodePrivateKey(getPrivateKey(), password);
			ret = Pkcs.makeMagicEnvelope(json.toString(), "application/json",
					pk, getPublicKeyHash());
		} catch (InvalidKeySpecException e) {

		}
		return ret;
	}

	public void sendMail(InternetAddress to, String subject, String filename,
			JSONObject data) throws MessagingException {
		InternetAddress[] recipients = new InternetAddress[1];
		recipients[0] = to;
		sendMail(recipients, subject, filename, data);
	}

	public void sendMail(InternetAddress[] recipients, String subject,
			String filename, JSONObject data) throws MessagingException {
		JSONObject magicEnvelope = makeMagicEnvelope(data, imap
				.getImapsnClient().getPrivateKeyPassword());
		SmtpClientBase smtp = imap.getImapsnClient().getSmtpAccount();
		MimeMessage mm = (MimeMessage) smtp.getNewMessage();
		MimeUtil.attachJsonObject(mm, subject, filename, magicEnvelope);
		mm.setRecipients(RecipientType.TO, recipients);
		mm.setFrom(new InternetAddress(getEmail()));
		mm.setSubject(subject);
		smtp.sendMessage(mm);
	}

	// interface to the json data

	public JSONObject getAccountOwnerPerson() {
		JSONObject ret = null;
		try {
			ret = ownerJson.getJSONObject("account-owner");
		} catch (JSONException e) {
			throw new RuntimeException("bug: no account-owner", e);
		}
		return ret;
	}

	public String getDisplayName() {
		try {
			return getAccountOwnerPerson().getString("displayName");
		} catch (JSONException e) {
			throw new RuntimeException("bug: no email", e);
		}
	}

	public String getEmail() {
		String email = null;
		try {
			if (ownerJson != null) {
				email = getAccountOwnerPerson().getJSONObject("email")
						.getString("value");
			} else {
				email = imap.getImapsnClient().getEmailId();
			}
		} catch (JSONException e) {
			throw new RuntimeException("bug: no email", e);
		}
		return email;
	}

	public String getPrivateKey() {
		String ret = null;
		try {
			ret = ownerJson.getString("privateKey");
		} catch (JSONException e) {
			throw new RuntimeException("bug: no privateKey", e);
		}
		return ret;
	}

	public String getPublicKeyHash() {
		String ret = null;
		try {
			ret = getAccountOwnerPerson().getString("keyhash");
		} catch (JSONException e) {
			throw new RuntimeException("bug: account-owner.keyhash", e);
		}
		return ret;
	}

	public String getWallGroup() {
		try {
			return ownerJson.getString("wall-group");
		} catch (JSONException e) {
			throw new RuntimeException("bug: account-owner.wall-group", e);
		}
	}

	public String getNewMessageFolder() {
		try {
			return ownerJson.getString("new-message-folder");
		} catch (JSONException e) {
			throw new RuntimeException("bug: account-owner.new-message-folder",
					e);
		}
	}

	// helpers

	public String newId() {
		return "acct:" + getEmail() + "#" + UUID.randomUUID().toString();
	}

	public static void validateOwnerJson(JSONObject configData)
			throws EInvalidConfigData {
		String prop = null;
		try {
			String[] props = { "new-message-folder", "wall-group",
					"privateKey", "account-owner" };
			for (String x : props) {
				prop = x;
				configData.get(prop);
			}
			JSONObject owner = configData.getJSONObject("account-owner");

			prop = "account-owner.id";
			owner.getString("id");

			prop = "account-owner.email.value";
			String email = owner.getJSONObject("email").getString("value");
			new InternetAddress(email);

			prop = "account-owner.publicKey"; // TODO: validate this
			owner.getString("publicKey");

			prop = "account-owner.keyhash"; // TODO: validate this
			owner.getString("keyhash");
		} catch (JSONException e) {
			throw new EInvalidConfigData(prop, configData);
		} catch (AddressException e) {
			throw new EInvalidConfigData(prop, configData);
		}
	}

	// inner classes

	public class EAccountOwnerReadError extends Exception {

		private static final long serialVersionUID = 1L;

		public EAccountOwnerReadError() {
		}

		public EAccountOwnerReadError(String message) {
			super(message);
		}

		public EAccountOwnerReadError(Throwable cause) {
			super(cause);
		}

		public EAccountOwnerReadError(String message, Throwable cause) {
			super(message, cause);
		}

	}

	public class EAccountOwnerSaveError extends Exception {

		private static final long serialVersionUID = 1L;

		public EAccountOwnerSaveError() {
		}

		public EAccountOwnerSaveError(String message) {
			super(message);
		}

		public EAccountOwnerSaveError(Throwable cause) {
			super(cause);
		}

		public EAccountOwnerSaveError(String message, Throwable cause) {
			super(message, cause);
		}

	}

	public static class EInvalidConfigData extends Exception {
		private static final long serialVersionUID = 1L;
		public String missingProperty = null;
		public JSONObject data;

		public EInvalidConfigData() {
		}

		public EInvalidConfigData(String missing, JSONObject data) {
			super(missing);
			missingProperty = missing;
			this.data = data;
		}

		public EInvalidConfigData(Throwable cause) {
			super(cause);
		}

		public EInvalidConfigData(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
