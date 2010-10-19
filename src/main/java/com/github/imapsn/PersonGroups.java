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

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PersonGroups implements ImapsnDataFile {

	public static String FILENAME = "/person-groups.json";
	JSONObject personGroups;
	ImapClientBase imap;

	public PersonGroups(JSONObject PersonGroups) {
		this.personGroups = PersonGroups;
	}

	public PersonGroups(ImapClientBase imap) throws ReadFailure {
		this.imap = imap;
		try {
			personGroups = imap.get(getFilename());
			if (personGroups == null) {
				personGroups = new JSONObject();
				personGroups.put("id", imap.getAccountOwner().newId());
			}
		} catch (MessagingException e) {
			throw new ReadFailure(e);
		} catch (IOException e) {
			throw new ReadFailure(e);
		} catch (JSONException e) {

		}
	}

	// properties

	public String getFilename() {
		return FILENAME;
	}

	public JSONObject getJson() {
		return personGroups;
	}

	// persisting back to IMAPSN

	public void save() throws MessagingException {
		imap.put(getFilename(), personGroups);
	}

	// mailing to a group

	public void sendMail(String groupName, String subject, String filename,
			JSONObject data) throws MessagingException {
		AccountOwner owner = imap.getAccountOwner();
		JSONArray group = getGroup(groupName);
		InternetAddress[] to = new InternetAddress[group.length()];
		PersonStatusMap personStatusMap = imap.getPersonStatusMap();
		for (int i = 0; i < group.length(); i++) {
			try {
				JSONObject ref = group.getJSONObject(i);
				String displayName = ref.getString("displayName");
				String email = ref.getString("email");
				String id = ref.getString("id");
				personStatusMap.noteSend(id);
				to[i] = new InternetAddress(displayName + "<" + email + ">");
			} catch (JSONException e) {
				// TODO: log this
			} catch (AddressException e) {
				// TODO: log this
			}
		}
		personStatusMap.save();
		owner.sendMail(to, subject, filename, data);
	}

	// group interface (modifies JSON only)

	public void appendToGroup(String group, String personId, String email,
			String displayName) throws AddressException {
		JSONArray groupArray;
		JSONObject person = new JSONObject();
		new InternetAddress(email);

		try {
			person.put("id", personId);
			person.put("email", email);
			person.put("displayName", displayName);
		} catch (JSONException e2) {
		}
		try {
			groupArray = personGroups.getJSONArray(group);
		} catch (JSONException e) {
			groupArray = new JSONArray();
			try {
				personGroups.put(group, groupArray);
			} catch (JSONException e1) {
			}
		}
		boolean exists = false;
		for (int i = 0; i < groupArray.length(); i++) {
			try {
				if (groupArray.getJSONObject(i).getString("id")
						.equals(personId)) {
					exists = true;
				}
			} catch (JSONException e) {
			}
		}
		if (!exists) {
			groupArray.put(person);
		}
	}

	public void removeFromGroup(String group, String personId) {
		JSONArray groupArray;
		try {
			groupArray = personGroups.getJSONArray(group);
			for (int i = 0; i < groupArray.length(); i++) {
				if (groupArray.getString(i).equals(personId)) {
					groupArray.remove(i);
					break;
				}
			}
		} catch (JSONException e) {
			return;
		}
	}

	public JSONArray getGroup(String name) {
		try {
			return personGroups.getJSONArray(name);
		} catch (JSONException e) {
			return null;
		}
	}

	// inner classes

	public class ReadFailure extends Exception {
		private static final long serialVersionUID = 1L;

		public ReadFailure(Exception e) {
			super(e);
		}
	}

}
