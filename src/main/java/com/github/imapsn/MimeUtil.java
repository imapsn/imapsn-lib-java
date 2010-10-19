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
import java.io.InputStreamReader;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SubjectTerm;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class MimeUtil {
    /**
     * Serialize JSON object into an attachment of a message with subject.
     * Overwrites any existing message having the same subject.
     */
    public static MimeMessage saveJsonData(ImapClientBase imap, Folder folder,
            JSONObject data, String subject, String filename) throws MessagingException {

        assert folder.isOpen();

        // delete existing message
        MimeMessage mm = getFolderMessage(folder, subject);
        if (mm != null) {
            mm.setFlag(Flag.DELETED, true);
            folder.expunge();
        }

        // create replacement/new message
        mm = (MimeMessage) imap.getNewMessage();
        mm.setFlag(Flag.SEEN, true);
        try {
			mm.setHeader("X-IMAPSN-Id", data.getString("id"));
		} catch (JSONException e) {
			throw new RuntimeException("all IMAPSN JSON objects must have an id.", e);
		}

        // attach the object
        attachJsonObject(mm, subject, filename, data);

        MimeMessage[] msgs = new MimeMessage[1];
        msgs[0] = mm;
        folder.appendMessages(msgs);

        mm.saveChanges();

        return mm;
    }

    public static MimeMessage saveJsonData(ImapClientBase imap, Folder folder,
            JSONObject data, String filename) throws MessagingException {
        return saveJsonData(imap, folder, data, filename, filename);
    }

    /**
     * Search for a message with <code>subject</code> in <code>folder</code> and
     * return null if not found.
     */
    public static MimeMessage getFolderMessage(Folder folder, String subject)
            throws MessagingException {
        MimeMessage mm = null;
        SubjectTerm st = new SubjectTerm(subject);
        Message[] messages = folder.search(st);
        for (int i = 0; i < messages.length; i++) {
            if (!messages[i].getFlags().contains(Flag.DELETED)) {
                mm = (MimeMessage) messages[i];
                break;
            }
        }
        return mm;
    }
    
    /**
     * Attach a JSON object to the MimeMessage using subject as the message
     * subject and as the attached file name.
     */
    public static void attachJsonObject(MimeMessage mm, String filename, JSONObject jo)
            throws MessagingException {
        attachJsonObject(mm, filename, filename, jo);
    }

    public static void attachJsonObject(MimeMessage mm, String subject, String filename, JSONObject jo)
            throws MessagingException {
        mm.setSubject(subject);
        // add parts
        MimeMultipart mp = new MimeMultipart("mixed");
        BodyPart bp = new MimeBodyPart();
        DataSource ds = new JsonDataSource(jo);
        bp.setDataHandler(new DataHandler(ds));
        bp.setFileName(filename);
        mp.addBodyPart(bp);
        mm.setContent(mp);
    }

    /**
     * Return a JSONObject read from an application/json file named
     * <code>filename</code> attached to MimeMessage mm. Return null if not
     * found.
     */
    public static JSONObject getAttachedJson(MimeMessage mm, String filename)
            throws MessagingException, IOException, JSONException {

        JSONObject ret = null;
        Multipart mp = (Multipart) mm.getContent();

        for (int i = 0, n = mp.getCount(); i < n; i++) {

            Part part = mp.getBodyPart(i);
            String disp = part.getDisposition();
            String curfilename = part.getFileName();

            if ((disp != null)
                    && (disp.equalsIgnoreCase(Part.ATTACHMENT) || disp
                            .equalsIgnoreCase(Part.INLINE)) && curfilename.compareTo(filename) == 0) {
                InputStreamReader isr = new InputStreamReader(part.getInputStream(), "UTF8");
                JSONTokener jtok = new JSONTokener(isr);
                ret = new JSONObject(jtok);
                break;
            }
        }
        return ret;
    }
    


}
