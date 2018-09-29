package it.unibo.arces.wot.sepa.commons.security;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import it.unibo.arces.wot.sepa.commons.exceptions.SEPASecurityException;

/**
 * The Class Encryption.
 */
class Encryption {

	/** The Constant ALGO. */
	// AES 128 bits (16 bytes)
	private static final String ALGO = "AES";

	/** The key value. */
	private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm', 'i', '!', 'I', 'e', '2', '3',
			'7', 'A', 'N' };

	/** The key. */
	private static Key key = new SecretKeySpec(keyValue, ALGO);

	/**
	 * Inits the.
	 *
	 * @param secret
	 *            the secret
	 */
	public Encryption(byte[] secret) {
		if (secret != null && secret.length == 16)
			keyValue = secret;
		key = new SecretKeySpec(keyValue, ALGO);
	}

	public Encryption() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Encrypt.
	 *
	 * @param Data
	 *            the data
	 * @return the string
	 * @throws SEPASecurityException
	 *
	 */
	 String encrypt(String Data) throws SEPASecurityException {
		Cipher c;
		try {
			c = Cipher.getInstance(ALGO);
			c.init(Cipher.ENCRYPT_MODE, key);
			return new String(Base64.getEncoder().encode(c.doFinal(Data.getBytes("UTF-8"))));
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}

	}

	/**
	 * Decrypt.
	 *
	 * @param encryptedData
	 *            the encrypted data
	 * @return the string
	 * @throws SEPASecurityException
	 *
	 */
	 String decrypt(String encryptedData) throws SEPASecurityException {
		Cipher c;
		try {
			c = Cipher.getInstance(ALGO);
			c.init(Cipher.DECRYPT_MODE, key);
			return new String(c.doFinal(Base64.getDecoder().decode(encryptedData)));
		} catch (Exception e) {
			throw new SEPASecurityException(e);
		}

	}
}
