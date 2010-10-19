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

/**
 * The functions in this class are in want of a good review 
 * by someone experienced with security and PKCS.
 * 
 * One note to keep in mind is that the java APIs use BigIntegers 
 * and these can't be zeroe'd out.  
 * 
 * See <http://osdir.com/ml/encryption.bouncy-castle.devel/2005-08/msg00109.html>.
 * 
 * And because of this I don't go out of my way to zero out password strings.
 * 
 */

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Pkcs {

    /**
     * Generate public and private keys for use with RSA algorithm, returning an
     * array of two strings where first string is the public key encoded as
     * application/magic-key, and the second is the private key serialized as
     * PKCS#8, base64url encoded, and then encrypted using a key derived form
     * password and serialized to a string as
     * "<cipher_text>.<salt>.<initialization_vector>" where each of the three
     * components is base64url encoded.
     */
    public static String[] generateKeys(String password) {
        KeyPair keyPair;
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
            return Pkcs.encodeKeys(keyPair, password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup.", e);
        }
    }

    /**
     * Using a keyPair with RSA public and private keys, return array of two
     * strings where first string is the public key encoded as
     * application/magic-key, and the second is the private key serialized as
     * PKCS#8 and then base64url encoded.
     */
    public static String[] encodeKeys(KeyPair keyPair, String password) {

        if (null == Security.getProvider("bc")) {
            Security.addProvider(new BouncyCastleProvider());
        }
        String[] ret = new String[2];
        RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
        ret[0] = encodeMagicKey(pubKey);

        RSAPrivateKey privKey = (RSAPrivateKey) keyPair.getPrivate();
        ret[1] = encodePrivateKey(privKey, password);

        return ret;
    }

    public static String encodeMagicKey(RSAPublicKey pubKey) {
        if (null == Security.getProvider("bc")) {
            Security.addProvider(new BouncyCastleProvider());
        }
        // encode as application/magic-key
        byte[] mbytes = pubKey.getModulus().toByteArray();
        byte[] ebytes = pubKey.getPublicExponent().toByteArray();
        String magicKey = "RSA." + Base64.encodeBase64URLSafeString(mbytes) + "."
                + Base64.encodeBase64URLSafeString(ebytes);
        return magicKey;
    }

    public static String encodePrivateKey(RSAPrivateKey privKey, String password) {
        try {
            assert (privKey.getFormat().equalsIgnoreCase("PKCS8"));
            byte[] pkcs8encoding = privKey.getEncoded();
            String magicKeyPrivate = "PKCS8." + Base64.encodeBase64URLSafeString(pkcs8encoding);

            SecretKeyFactory factory;

            // see
            // <http://download.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html>
            factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            SecureRandom randGen = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[8];
            randGen.nextBytes(salt);

            KeySpec spec = new PBEKeySpec(toChars(password), salt, 1024, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "DES");

            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            byte[] ciphertext = cipher.doFinal(magicKeyPrivate.getBytes("UTF8"));
            return Base64.encodeBase64URLSafeString(ciphertext) + "."
                    + Base64.encodeBase64URLSafeString(salt) + "."
                    + Base64.encodeBase64URLSafeString(iv);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (InvalidParameterSpecException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("bug in key encode", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("bug in key encode", e);
        }
    }

    /**
     * Return a KeyPair decoded from an array of two strings where first string
     * is the public key encoded as application/magic-key, and the second is the
     * private key serialized as PKCS#8 and then base64url encoded.
     * 
     * @throws InvalidKeySpecException
     */
    public static KeyPair decodeKeys(String[] keys, String password) throws InvalidKeySpecException {
        RSAPublicKey pubKey = decodeMagicKey(keys[0]);
        RSAPrivateKey privKey = decodePrivateKey(keys[1], password);
        return new KeyPair(pubKey, privKey);
    }

    /**
     * Decode a public key encoded as application/magic-key and return an
     * RSAPublicKey.
     * 
     * @throws InvalidKeySpecException
     */
    public static RSAPublicKey decodeMagicKey(String magicKey) throws InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        try {
            Base64 b64 = new Base64(true);
            String[] pubParts = magicKey.split("\\.");
            BigInteger pubMod = new BigInteger(b64.decode(pubParts[1]));
            BigInteger pubExpt = new BigInteger(b64.decode(pubParts[2]));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(pubMod, pubExpt);
            // this throws InvalidKeySpecException if pubKeySpec isn't right
            return (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup", e);
        }
    }

    public static char[] toChars(String password) {
        char[] pwd = new char[password.length()];
        for (int i = 0; i < password.length(); i++) {
            pwd[i] = password.charAt(i);
        }
        return pwd;
    }

    /**
     * Decode a private key encoded as PKCS#8+base64url and return an
     * RSAPrivateKey.
     */
    public static RSAPrivateKey decodePrivateKey(String privateMagicKey, String password)
            throws InvalidKeySpecException {
        Security.addProvider(new BouncyCastleProvider());
        try {
            Base64 b64 = new Base64(true);

            String parts[] = privateMagicKey.split("\\.");
            byte[] ciphertext = b64.decode(parts[0].getBytes());
            byte[] salt = b64.decode(parts[1].getBytes());
            byte[] iv = b64.decode(parts[2].getBytes());

            // make the key
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(toChars(password), salt, 1024, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "DES");

            // decrypt
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
            parts = new String(cipher.doFinal(ciphertext), "UTF8").split("\\.");
         
            byte[] pkcs8encoding = b64.decode(parts[1]);

            // make the key
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8encoding);
            return (RSAPrivateKey) kf.generatePrivate(keySpec);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup", e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException("bug in key decode", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("bug in key decode", e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException("bug in key decode", e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("bug in key decode", e);
        } catch (BadPaddingException e) {
            throw new RuntimeException("bug in key decode", e);
        } catch (UnsupportedEncodingException e) {
        	throw new RuntimeException("bug in key decode", e);
		}
    }

    /**
     * Return a base64url encoded SHA-256 hash of an application/magic-key.
     */
    public static String magicKeyHash(String magicKey) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(magicKey.getBytes("UTF8"));
            return Base64.encodeBase64URLSafeString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup", e);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("RuntimeException in UTF8 encoding setup.", e);
        }
    }

    /**
     * Transform a string into a application/magic-env+json.
     * 
     * @param data
     *            gets encoded into the "data" field.
     * @param type
     *            for example, "application/json" if plaintext is a JSON string.
     * @param pkcs8key
     *            a private key encoded as PKCS#8+base64url
     * @param magicKeyHash
     *            a base64url encoded SHA-256 hash of an application/magic-key
     *            string
     * @return an application/magic-env+json
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     */
    public static JSONObject makeMagicEnvelope(String data, String type, RSAPrivateKey privateKey,
            String magicKeyHash) throws InvalidKeySpecException {
        try {
            {
                JSONObject ret = new JSONObject();
                String m;
                // initialize m
                {
                    String e_data = Base64.encodeBase64URLSafeString(data.getBytes("UTF8"));
                    ret.put("data", e_data);

                    ret.put("data_type", type);
                    String e_type = Base64.encodeBase64URLSafeString(type.getBytes("UTF8"));

                    String encoding = "base64url";
                    ret.put("encoding", encoding);
                    String e_encoding = Base64.encodeBase64URLSafeString(encoding.getBytes("UTF8"));

                    String alg = "RSA-SHA256";
                    ret.put("alg", alg);
                    String e_alg = Base64.encodeBase64URLSafeString(alg.getBytes("UTF8"));

                    m = e_data + "." + e_type + "." + e_encoding + "." + e_alg;
                }

                // initialize the signature instance
                Signature signer = Signature.getInstance("SHA256withRSA");
                // decodePKCS8Key throws InvalidKeySpecException if the pkcs8key
                // can't be parsed
                signer.initSign(privateKey);
                signer.update(m.getBytes());

                // sign, and save
                JSONObject sig = new JSONObject();
                sig.put("value", Base64.encodeBase64URLSafeString(signer.sign()));
                sig.put("keyhash", magicKeyHash);

                // return it all in the envelope
                JSONArray sigs = new JSONArray();
                sigs.put(sig);
                ret.put("sigs", sigs);
                return ret;
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("RuntimeException in UTF8 encoding setup.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup", e);
        } catch (JSONException e) {
            throw new RuntimeException("Unexpected problem while putting values in JSONObject", e);
        } catch (SignatureException e) {
            throw new RuntimeException("Unexpected problem with Signature state.", e);
        } catch (InvalidKeyException e) {
            // assume we'd get InvalidKeySpecException from
            // decodePKCS8Key first if there were problems with the key
            throw new RuntimeException("Unexected problem with RSA private key.", e);
        }
    }

    /**
     * Return the decoded data string from an application/magic-env+json.
     * 
     * @param magicEnv
     * @param magicKeyMap
     *            maps a base64url encoded SHA-256 hash of an
     *            application/magic-key to the magic-key.
     * @return
     * @throws InvalidSignature
     *             thrown when a signature from a public key found in
     *             magicKeyMap doesn't validate
     * @throws UnknownProvenance
     *             thrown when none of the signatures are found in magicKeyMap
     * @throws UnreadableMagicEnvelope
     *             thrown when there is an RuntimeException reading the magic
     *             envelope, for example, if a required property is missing.
     * @throws InvalidKeySpecException
     *             thrown when a magicKey read from magicKeyMap cannot be
     *             decoded.
     */
    public static void checkMagicEnvelope(JSONObject magicEnv, JSONObject magicKeyMap)
            throws InvalidSignature, UnknownProvenance, UnreadableMagicEnvelope,
            InvalidKeySpecException {

        try {

            // read the parts of the envelope
            String data, data_type, encoding, alg;
            JSONArray sigs;
            try {
                data = magicEnv.getString("data");
                data_type = magicEnv.getString("data_type");
                encoding = magicEnv.getString("encoding");
                alg = magicEnv.getString("alg");
                sigs = magicEnv.getJSONArray("sigs");
            } catch (JSONException e) {
                throw new UnreadableMagicEnvelope(
                        "A required property is missing from the envelope.", e);
            }          

            // construct the message, m
            String m = data + "." + Base64.encodeBase64URLSafeString(data_type.getBytes()) + "."
                    + Base64.encodeBase64URLSafeString(encoding.getBytes()) + "."
                    + Base64.encodeBase64URLSafeString(alg.getBytes());
            byte[] mbytes = null;
            mbytes = m.getBytes("UTF8");

            // check signatures
            Signature signer = Signature.getInstance("SHA256withRSA");
            int validSigs = 0;
            for (int i = 0; i < sigs.length(); i++) {

                // get the ith signature
                JSONObject sig;
                try {
                    sig = sigs.getJSONObject(i);
                } catch (JSONException e) {
                    throw new RuntimeException("Object #" + i
                            + " is unexpectedly missing in JSON array.", e);
                }

                // look up the magic key
                String keyhash;
                try {
                    keyhash = sig.getString("keyhash");
                } catch (JSONException e) {
                    throw new UnreadableMagicEnvelope(
                            "The required 'keyhash' property is missing for a signture.", e);
                }
                String magicKey = null;
                try {
                    magicKey = magicKeyMap.getString(keyhash);
                } catch (JSONException e) {
                    // if we don't have the magic key for this signature
                    // go on to the next signature
                    continue;
                }
                // throws InvalidKeySpecException if can't read magicKey
                RSAPublicKey rsaPubKey = decodeMagicKey(magicKey);

                // we have the key, so now it has to validate

                // get the signature
                String value;
                try {
                    value = sig.getString("value");
                } catch (JSONException e) {
                    throw new UnreadableMagicEnvelope(
                            "There is no 'value' property for signature from " + keyhash, e);
                }
                byte[] sigbytes = Base64.decodeBase64(value);

                // Verify!
                try {
                    signer.initVerify(rsaPubKey);
                    signer.update(mbytes);
                    if (!signer.verify(sigbytes)) {
                        throw new InvalidSignature("Signature for " + keyhash + "was not valid.");
                    }
                } catch (InvalidKeyException e) {
                    // catch InvalidKeyException b/c assuming we would have seen
                    // InvalidKeySpecException if the key were no good.
                    throw new RuntimeException(
                            "Problem with the rsa key when initializing the signer", e);
                } catch (SignatureException e) {
                    throw new UnreadableMagicEnvelope("Unable to process signature from "
                            + keyhash, e);
                }
                validSigs++;
            }

            // we didn't have a public key for any of the signatures
            if (validSigs == 0) {
                throw new UnknownProvenance();
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new RuntimeException("RuntimeException in UTF8 encoding setup.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RuntimeException in java security API setup", e);
        }
    }
    
    public static class UnknownProvenance extends Exception {

        private static final long serialVersionUID = 1L;

        public UnknownProvenance() {
        }

        public UnknownProvenance(String message) {
            super(message);
        }

        public UnknownProvenance(Throwable cause) {
            super(cause);
        }

        public UnknownProvenance(String message, Throwable cause) {
            super(message, cause);
        }

    }
    
    public static class InvalidSignature extends Exception {

    	private static final long serialVersionUID = 1L;

    	public InvalidSignature() {
        }

        public InvalidSignature(String message) {
            super(message);
        }

        public InvalidSignature(Throwable cause) {
            super(cause);
        }

        public InvalidSignature(String message, Throwable cause) {
            super(message, cause);
        }

    }
    
    public static class UnreadableMagicEnvelope extends Exception {

        private static final long serialVersionUID = 1L;

        public UnreadableMagicEnvelope() {
            // TODO Auto-generated constructor stub
        }

        public UnreadableMagicEnvelope(String message) {
            super(message);
            // TODO Auto-generated constructor stub
        }

        public UnreadableMagicEnvelope(Throwable cause) {
            super(cause);
            // TODO Auto-generated constructor stub
        }

        public UnreadableMagicEnvelope(String message, Throwable cause) {
            super(message, cause);
            // TODO Auto-generated constructor stub
        }

    }

}
