package com.github.imapsn.test;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import static org.junit.Assert.*;

import org.junit.Test;

import com.github.imapsn.Pkcs;

public class TestEncodeKeys {
	@Test
	public void testEncodeKeys() throws NoSuchAlgorithmException,
			InvalidKeySpecException, UnsupportedEncodingException,
			NoSuchPaddingException, InvalidKeyException,
			InvalidParameterSpecException, IllegalBlockSizeException,
			BadPaddingException {
		KeyPair keyPair1 = KeyPairGenerator.getInstance("RSA")
				.generateKeyPair();
		String password = "foobar"; 
		String[] keys = Pkcs.encodeKeys(keyPair1, password);
		KeyPair keyPair2 = Pkcs.decodeKeys(keys, password);
		byte[] bytes1 = keyPair1.getPublic().getEncoded();
		byte[] bytes2 = keyPair2.getPublic().getEncoded();
		assertTrue(Arrays.equals(bytes1, bytes2));
		RSAPrivateKey privkey1 = (RSAPrivateKey)keyPair1.getPrivate(); 
		RSAPrivateKey privkey2 = (RSAPrivateKey)keyPair2.getPrivate();
		bytes1 = privkey1.getEncoded();
		bytes2 = privkey2.getEncoded();
		assertTrue(Arrays.equals(bytes1, bytes2));

		assertTrue(true);
	}

}
