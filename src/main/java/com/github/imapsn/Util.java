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
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

public class Util {

	/**
	 * Return a JSON object to be used as the generator property of activities.
	 */
	public static JSONObject getServiceJson() {
		JSONObject ret = new JSONObject();
		try {
			ret.put("objectType", "service");
			ret.put("id", "imapsn-javalib-0.1");
		} catch (JSONException e) {
		}
		return ret;
	}

	public static JSONObject makePersonRef(JSONObject person) {
		JSONObject personRef = new JSONObject();
		try {
			personRef.put("id", person.getString("id"));
			personRef.put("objectType", "person");
			personRef.put("displayName", person.getString("displayName"));
			personRef.put("email", person.getJSONObject("email"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return personRef;
	}

	public static JSONObject openMagicEnvelope(ImapClientBase imap,
			MimeMessage mm, String filename) throws MessagingException,
			IOException {
		boolean good = false;
		// init keyMap
		JSONObject keyMap = imap.getKeyMap().getJson();
		JSONObject ret = null;
		try {
			JSONObject magicEnvelope = MimeUtil.getAttachedJson(mm, filename);

			// decode magic envelope
			ret = new JSONObject(new String(Base64.decodeBase64(magicEnvelope
					.getString("data")), "UTF8"));

			// check magicEnvelope
			Pkcs.checkMagicEnvelope(magicEnvelope, keyMap);
			good = true;

		} catch (InvalidKeySpecException e) {
			// TODO: log this
		} catch (Pkcs.InvalidSignature e) {
			// TODO log this
		} catch (Pkcs.UnknownProvenance e) {
			// TODO log this
		} catch (Pkcs.UnreadableMagicEnvelope e) {
			// TODO log this
			e.printStackTrace();
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (good) {
			return ret;
		} else {
			return null;
		}
	}

	// ISO8601 dates

	private static DateFormat m_ISO8601Local = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss");

	public static Date parseDateTime(String dateTime) throws ParseException {
		return m_ISO8601Local.parse(dateTime);
	}

	public static String formatDateTime() {
		return formatDateTime(new Date(System.currentTimeMillis()));
	}

	public static String formatDateTime(Date date) {
		if (date == null) {
			return formatDateTime(new Date(System.currentTimeMillis()));
		}

		String dateStr = m_ISO8601Local.format(date);

		return dateStr;
	}

}
