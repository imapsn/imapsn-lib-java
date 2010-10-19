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
import java.io.UnsupportedEncodingException;
import java.security.spec.InvalidKeySpecException;

import javax.mail.Flags.Flag;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;

public class Friend {

	public static void sendFriendRequest(ImapClientBase imap, InternetAddress to)
			throws MessagingException {

		AccountOwner owner = imap.getAccountOwner();
		JSONObject activity = new JSONObject();
		JSONObject friend = new JSONObject();
		try {
			// make the activity
			activity.put("id", owner.newId());
			activity.put("objectType", "activity");
			activity.put("verb", "make-friend");
			activity.put("actor", owner.getAccountOwnerPerson());

			friend.put("objectType", "person");
			JSONObject friendemail = new JSONObject();
			friendemail.put("type", "imapsn");
			friendemail.put("value", to);
			friend.put("email", friendemail);

			activity.put("object", friend);
			activity.put("generator", Util.getServiceJson());

			// make entry in person-status-map
			PersonStatusMap personStatusMap = imap.getPersonStatusMap();
			personStatusMap.setEntry(activity.getString("id"), "pending",
					Util.formatDateTime(), null);
			personStatusMap.save();

			// send the message
			owner.sendMail(to,
					"[IMAPSN] friend-request: " + owner.getDisplayName(),
					"friend-request.json", activity);

		} catch (JSONException e) {
			throw new RuntimeException("bug while putting values", e);
		}
	}

	public static void acceptFriendRequest(ImapClientBase imap, MimeMessage mm)
			throws MessagingException {
		try {

			// decode magic envelope
			JSONObject magicEnvelope = MimeUtil.getAttachedJson(mm,
					"friend-request.json");
			JSONObject activity = new JSONObject(new String(
					Base64.decodeBase64(magicEnvelope.getString("data")),
					"UTF8"));
			JSONObject friend = activity.getJSONObject("actor");

			// check magicEnvelope
			boolean good = false;
			JSONObject tkm = new JSONObject();
			tkm.put(friend.getString("keyhash"), friend.getString("publicKey"));
			try {
				Pkcs.checkMagicEnvelope(magicEnvelope, tkm);
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
			}
			if (good) {

				saveFriend(imap, friend);

				// make person-status-map entry
				PersonStatusMap personStatusMap = imap.getPersonStatusMap();
				String now = Util.formatDateTime();
				personStatusMap.setEntry(friend.getString("id"), "active", now,
						now);
				personStatusMap.save();

				// send friend-response
				Friend.sendFriendResponse(imap, activity);

				// delete the message
				mm.setFlag(Flag.DELETED, true);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void sendFriendResponse(ImapClientBase imap,
			JSONObject friendRequestActivity) throws MessagingException {

		JSONObject activity = new JSONObject();

		try {
			AccountOwner owner = imap.getAccountOwner();

			// make-friend response activity

			activity.put("id", owner.newId());
			activity.put("objectType", "activity");

			activity.put("inReplyTo", friendRequestActivity.getString("id"));
			activity.put("verb", "make-friend");
			activity.put("generator", Util.getServiceJson());

			activity.put("actor", owner.getAccountOwnerPerson());

			JSONObject friend = new JSONObject();
			String friendEmail = friendRequestActivity.getJSONObject("actor")
					.getJSONObject("email").getString("value");
			JSONObject friendEmailObject = new JSONObject();
			String friendId = friendRequestActivity.getJSONObject("actor")
					.getString("id");
			friend.put("objectType", "person");
			friend.put("id", friendId);

			friendEmailObject.put("type", "imapsn");
			friendEmailObject.put("value", friendEmail);
			friend.put("email", friendEmailObject);

			activity.put("object", friend);

			// Send the response

			owner.sendMail(new InternetAddress(friendEmail),
					"[IMAPSN] friend-response: " + owner.getDisplayName(),
					"friend-response.json", activity);

			// construct news item (reuse activity)

			Friend.sendFriendshipNews(imap, activity);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void processFriendResponse(ImapClientBase imap, MimeMessage mm)
			throws MessagingException {
		try {

			// decode magic envelope
			JSONObject magicEnvelope = MimeUtil.getAttachedJson(mm,
					"friend-response.json");
			JSONObject activity = new JSONObject(new String(
					Base64.decodeBase64(magicEnvelope.getString("data")),
					"UTF8"));

			PersonStatusMap personStatusMap = imap.getPersonStatusMap();
			if (personStatusMap.getJson().has(activity.getString("inReplyTo"))) {

				JSONObject friend = activity.getJSONObject("actor");

				// check magicEnvelope
				boolean good = false;
				JSONObject tkm = new JSONObject();
				tkm.put(friend.getString("keyhash"),
						friend.getString("publicKey"));
				try {
					Pkcs.checkMagicEnvelope(magicEnvelope, tkm);
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
				}
				if (good) {

					saveFriend(imap, friend);

					// make person-status-map entry
					personStatusMap
							.deleteEntry(activity.getString("inReplyTo"));
					String now = Util.formatDateTime();
					personStatusMap.setEntry(friend.getString("id"), "active",
							now, now);
					personStatusMap.save();

					// send news-item
					sendFriendshipNews(imap, activity);
				}
				
				// delete the message
				// TODO: what to do with the message when sig fails?
				mm.setFlag(Flag.DELETED, true);
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// helpers

	protected static String recipientId(InternetAddress to) {
		return "acct:" + to.getAddress() + "#0";
	}

	protected static void saveFriend(ImapClientBase imap, JSONObject friend)
			throws JSONException, MessagingException {
		// save keyhash
		KeyMap km = imap.getKeyMap();
		km.putPublicKey(friend.getString("keyhash"),
				friend.getString("publicKey"));
		km.save();

		// save in contacts
		String path = "/contacts/" + friend.getString("id");
		imap.put(path, friend);

		// make entry in person-groups.js
		PersonGroups pg = imap.getPersonGroups();
		pg.appendToGroup("everybody", friend.getString("id"), friend
				.getJSONObject("email").getString("value"), friend
				.getString("displayName"));
		pg.save();
	}

	protected static void sendFriendshipNews(ImapClientBase imap,
			JSONObject activity) throws MessagingException {
		try {
			AccountOwner owner = imap.getAccountOwner();
			JSONObject activity2 = new JSONObject();
			activity2.put("id", owner.newId());
			activity2.put("objectType", "activity");
			activity2.put("verb", "make-friend");

			JSONObject ownerRef = Util.makePersonRef(owner
					.getAccountOwnerPerson());
			activity2.put("actor", ownerRef);

			JSONObject friend = activity.getJSONObject("actor");
			activity2.put("object", Util.makePersonRef(friend));

			String subject = ownerRef.get("displayName")
					+ " is now friends with " + friend.get("displayName");
			NewsItem.sendNewsItem(imap, owner.getWallGroup(), subject,
					activity2);

		} catch (JSONException e) {

		}
	}

}
