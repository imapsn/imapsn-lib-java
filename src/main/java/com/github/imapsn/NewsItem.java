package com.github.imapsn;

import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;

public class NewsItem {

	public static void sendNewsItem(ImapClientBase imap, String group,
			String subject, JSONObject activity) throws MessagingException {
		String subject1 = "[IMAPSN] news-item: " + subject;
		imap.getPersonGroups().sendMail(group, subject1, "news-item.json",
				activity);
	}

	public static void postStatus(ImapClientBase imap, String group,
			String statusString) throws MessagingException {
		String subject1 = "[IMAPSN] news-item: status from "
				+ imap.getAccountOwner().getDisplayName();
		JSONObject activity = new JSONObject();
		try {
			AccountOwner owner = imap.getAccountOwner();
			activity.put("id", owner.newId());
			activity.put("objectType", "activity");
			activity.put("verb", "post");

			activity.put("actor",
					Util.makePersonRef(owner.getAccountOwnerPerson()));

			JSONObject status = new JSONObject();
			status.put("id", owner.newId());
			status.put("objectType", "note");
			status.put("content", statusString);
			activity.put("object", status);

			imap.getPersonGroups().sendMail(group, subject1, "news-item.json",
					activity);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void processNewsItem(ImapClientBase imap, MimeMessage mm) {
		try {
			JSONObject news = Util
					.openMagicEnvelope(imap, mm, "news-item.json");
			
			if (news != null) {
				String path = "/news/" + news.getString("id");
				imap.put(path, news);
				PersonStatusMap statusMap = imap.getPersonStatusMap();
				statusMap.noteReceived(news.getJSONObject("actor").getString(
						"id"));
				statusMap.save();
			}
			// TODO: probably shouldn't just delete this if news is null ...
			// delete the message
			mm.setFlag(Flag.DELETED, true);

		} catch (MessagingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
