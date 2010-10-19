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

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.json.JSONObject;

public interface ImapClientBase {
	public Folder getFolder(String string) throws MessagingException;

	public Session getSession();

	public AccountOwner getAccountOwner();

	public PersonStatusMap getPersonStatusMap();

	public PersonGroups getPersonGroups();

	public KeyMap getKeyMap();

	public Message getNewMessage();

	public Message[] getNewMessages(String messageType)
			throws MessagingException;

	public Message[] getNewMessages() throws MessagingException;

	public void connect() throws Exception;

	public void close() throws MessagingException;

	public boolean isConnected();

	public ImapsnClient getImapsnClient();

	public Folder openFolder(String name) throws MessagingException;

	public JSONObject get(String path) throws MessagingException, IOException;

	public void put(String path, JSONObject json, String text)
			throws MessagingException;

	public void put(String path, JSONObject json) throws MessagingException;

}
