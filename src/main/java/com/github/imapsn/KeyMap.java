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

import org.json.JSONException;
import org.json.JSONObject;

public class KeyMap implements ImapsnDataFile {
    
    static String FILENAME = "/key-map.json";
    JSONObject keyMap;
    ImapClientBase imap;

    public KeyMap(ImapClientBase imap) throws ReadFailure {
    	this.imap = imap;
        try {
			keyMap = imap.get(getFilename());
			if (keyMap ==  null) {
				keyMap = new JSONObject();
				keyMap.put("id", imap.getAccountOwner().newId());
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
		// TODO Auto-generated method stub
		return keyMap;
	}

	// persisting back to IMAPSN
	
    public void save() throws MessagingException {
    	imap.put(getFilename(), keyMap);
    }
	
	// set and lookup keyhash
    
    public boolean hasPublicKey(String keyhash) {
        return keyMap.has(keyhash);
    }
    
    public String getPublicKey(String keyhash) {
        try {
            return keyMap.getString(keyhash);
        } catch (JSONException e) {
            return null;
        }
    }
    
    public void putPublicKey(String keyhash, String publicKey) {
    	try {
			keyMap.put(keyhash, publicKey);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
    }

	// inner classes
	
    public static class ReadFailure extends Exception {
        private static final long serialVersionUID = 1L;
        public ReadFailure(Exception e) {
            super(e);
        }
    }
	
}
