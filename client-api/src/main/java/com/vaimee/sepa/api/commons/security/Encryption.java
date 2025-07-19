/* Utility class to encrypt/decrypt sensible data 
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.vaimee.sepa.api.commons.security;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.vaimee.sepa.api.commons.exceptions.SEPASecurityException;
import com.vaimee.sepa.logging.Logging;

/**
 * The Class Encryption.
 */
public class Encryption {
	/** The Constant ALGO. */
	// AES 128 bits (16 bytes)
	private static final String ALGO = "AES";

	/** The key value. */
	private static byte[] keyValue = new byte[] { '0', '1', 'R', 'a', 'v', 'a', 'm', 'i', '!', 'I', 'e', '2', '3', '7',
			'A', 'N' };

	/** The key. */
	private static Key key = new SecretKeySpec(keyValue, ALGO);

	/**
	 * Init the secret value.
	 *
	 * @param secret the secret
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
	 * @param Data the data
	 * @return the string
	 * @throws SEPASecurityException
	 *
	 */
	public String encrypt(String Data) throws SEPASecurityException {
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
	 * @param encryptedData the encrypted data
	 * @return the string
	 * @throws SEPASecurityException
	 *
	 */
	public String decrypt(String encryptedData) throws SEPASecurityException {
		Cipher c;

		try {
			c = Cipher.getInstance(ALGO);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			if (Logging.getLogger().isTraceEnabled())
				e.printStackTrace();
			throw new SEPASecurityException(e.getMessage());
		}

		try {
			c.init(Cipher.DECRYPT_MODE, key);
		} catch (InvalidKeyException e) {
			if (Logging.getLogger().isTraceEnabled())
				e.printStackTrace();
			throw new SEPASecurityException(e.getMessage());
		}
		
		byte[] decoded;
		try {
			decoded = Base64.getDecoder().decode(encryptedData);
		}
		catch (IllegalArgumentException e) {
			if (Logging.getLogger().isTraceEnabled())
				e.printStackTrace();
			throw new SEPASecurityException(e.getMessage());
		}
		
		byte[] decrypted;		
		try {
			decrypted = c.doFinal(decoded);
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			if (Logging.getLogger().isTraceEnabled())
				e.printStackTrace();
			throw new SEPASecurityException(e.getMessage());
		}

		return new String(decrypted);

	}
}
