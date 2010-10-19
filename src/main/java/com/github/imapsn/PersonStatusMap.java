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
import java.text.ParseException;
import java.util.Date;

import javax.mail.MessagingException;

import org.json.JSONException;
import org.json.JSONObject;

public class PersonStatusMap implements ImapsnDataFile {

	protected static String FILENAME = "/person-status-map.json";
	protected JSONObject statusMap = null;
	protected ImapClientBase imap;

	public PersonStatusMap(ImapClientBase imap)
			throws EPersonStatusMapReadError {
		this.imap = imap;
		try {
			statusMap = imap.get(getFilename());
			if (statusMap == null) {
				statusMap = new JSONObject();
				statusMap.put("id", imap.getAccountOwner().newId());
			}

		} catch (MessagingException e) {
			throw new EPersonStatusMapReadError(e);
		} catch (IOException e) {
			throw new EPersonStatusMapReadError(e);
		} catch (JSONException e) {
		}
	}

	// properties

	public String getFilename() {
		return FILENAME;
	}

	public JSONObject getJson() {
		return statusMap;
	}

	// persisting back to IMAPSN

	public void save() throws MessagingException {
		imap.put(getFilename(), statusMap);
	}

	// status interface

	public void setEntry(String personId, String statusval, String lastSent,
			Object lastReceived) {
		JSONObject status = new JSONObject();
		try {
			status.put("status", statusval);
			status.put("last-sent", lastSent);
			if (lastReceived != null) {
				status.put("last-received", lastReceived);
			}
			statusMap.put(personId, status);
		} catch (JSONException e) {
		}
	}

	public JSONObject getStatus(String personId) {
		try {
			return statusMap.getJSONObject(personId);
		} catch (JSONException e) {
			JSONObject status = new JSONObject();
			try {
				status.put("status", "unknown");
				return status;
			} catch (JSONException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	public void deleteEntry(String personId) {
		statusMap.remove(personId);
	}

	// inner classes

	public class EPersonStatusMapSaveFailure extends Exception {
		private static final long serialVersionUID = 1L;

		public EPersonStatusMapSaveFailure(Exception e) {
			super(e);
		}
	}

	public class EPersonStatusMapReadError extends Exception {
		private static final long serialVersionUID = 1L;

		public EPersonStatusMapReadError() {
			super();
		}

		public EPersonStatusMapReadError(String message) {
			super(message);
		}

		public EPersonStatusMapReadError(Throwable cause) {
			super(cause);
		}

		public EPersonStatusMapReadError(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public void noteSend(String id) {
		try {
			String lastSentString = Util.formatDateTime();
			Date lastSent = Util.parseDateTime(lastSentString);
			String lastReceivedString = getStatus(id).getString("last-received");
			Date lastReceived = Util.parseDateTime(lastReceivedString);
			String statusString;
			final long threeDays = 3 * 1000 * 60 * 60 * 24;
			if ((lastSent.getTime() - lastReceived.getTime()) > threeDays) {
				statusString = "asleep";
			} else {
				statusString = "active";
			}
			setEntry(id, statusString, lastSentString, lastReceivedString);
		} catch (ParseException e) {
			throw new RuntimeException("person status map is corrupted", e);
		} catch (JSONException e) {
			throw new RuntimeException("person status map is corrupted", e);
		}
	}

	public void noteReceived(String id) {
		try {
			String lastReceivedString = Util.formatDateTime();
			Date lastReceived = Util.parseDateTime(lastReceivedString);
			String lastSentString = getStatus(id).getString("last-sent");
			Date lastSent = Util.parseDateTime(lastSentString);
			String statusString;
			final long threeDays = 3 * 1000 * 60 * 60 * 24;
			if ((lastReceived.getTime() - lastSent.getTime()) > threeDays) {
				statusString = "neglected";
			} else {
				statusString = "active";
			}
			setEntry(id, statusString, lastSentString, lastReceivedString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
