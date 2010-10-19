package com.github.imapsn.test;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.junit.Test;

import com.github.imapsn.Pkcs;
import static org.junit.Assert.*;

public class TestMagicEnvelope {
    @Test
    public void testMagicEnvelope() throws Exception {
        JSONObject orig = new JSONObject("{'foo': 'bar', 'baz': 'buz', 'fried': 'potatoes'}");
        String plaintext = orig.toString();
        String keys[] = Pkcs.generateKeys("foobarbazpass");
        String magicKeyHash = Pkcs.magicKeyHash(keys[0]);

        JSONObject magicEnv = Pkcs.makeMagicEnvelope(plaintext, "application/json",
                Pkcs.decodePrivateKey(keys[1], "foobarbazpass"), magicKeyHash);
        // String mestr = magicEnv.toString(3);
        JSONObject magicKeyMap = new JSONObject();
        magicKeyMap.put(magicKeyHash, keys[0]);
        Pkcs.checkMagicEnvelope(magicEnv, magicKeyMap);
        String plaintext2 = new String(Base64.decodeBase64(magicEnv.getString("data").getBytes("UTF8")));
        JSONObject recov = new JSONObject(plaintext2);
        assertTrue(orig.get("foo").equals(recov.get("foo")));
        assertTrue(orig.get("baz").equals(recov.get("baz")));
        assertTrue(orig.get("fried").equals(recov.get("fried")));
    }

}
