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

// http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
// http://java.sun.com/products/javamail/SSLNOTES.txt

import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.mail.util.MailSSLSocketFactory;

public class SmtpClient implements SmtpClientBase {
	
	Properties props = System.getProperties();
	Session session;
	String password;
	String connectionTimeout = "5000"; // 5s timeout
	String socketTimeout = "5000"; // 5s timeout
	
	public SmtpClient(JSONObject obj) throws JSONException {
		this(obj.getString("smtp-host"),
				obj.getString("smtp-user"), obj.getString("smtp-password"),
				obj.getString("smtp-port"), obj.getBoolean("smtp-enable-ttls"));
	}
	
	public SmtpClient(String host, String user, String password,
			 String port, boolean useTLS) {
		super();
		this.props.put("mail.smtp.host", host);
		this.props.put("mail.smtp.user", user);
		this.props.put("mail.smtp.port", port);
		this.props.put("mail.smtp.connectiontimeout", connectionTimeout);
		this.props.put("mail.smtp.timeout", socketTimeout);
		this.password = password;
		if (useTLS) {
			props.put("mail.smtp.ssl.enable", "true");
			MailSSLSocketFactory sf;
			try {
				sf = new MailSSLSocketFactory();
			} catch (GeneralSecurityException e) {
				sf = null;
			}
			// trust everybody
			if (sf != null) {
				sf.setTrustAllHosts(true);
				props.put("mail.smtp.ssl.socketFactory", sf);
				props.put("mail.smtp.socketFactory.fallback", "false");
			}			
		}
		session = Session.getInstance(props, null);
	}

	@Override
	public MimeMessage getNewMessage() {
		return new MimeMessage(this.session);
	}

	@Override
	public void sendMessage(Message message) throws MessagingException {
		Transport transport = session.getTransport("smtp");
		transport.connect(props.getProperty("mail.smtp.host"),props.getProperty("mail.smtp.user"),this.password);
		transport.sendMessage(message, message.getAllRecipients());
		transport.close();
	}

}
