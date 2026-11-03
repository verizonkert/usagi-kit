package tsuki.util

import tsuki.InternalParsersApi
import tsuki.MangaLoaderContext
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val HASH_CIPHER = "AES/CBC/PKCS7PADDING"
private const val AES = "AES"
private const val KDF_DIGEST = "MD5"

/**
 * AES decryption utility conforming with the JavaScript `CryptoJS.AES` standard method.
 *
 * Employs OpenSSL's `EVP_BytesToKey` key derivation function (KDF) to decrypt salted payloads
 * commonly encountered on manga websites protecting encrypted chapter image URLs.
 *
 * @property context The loader context providing Base64 decoding services.
 */
@InternalParsersApi
public class CryptoAES(
	private val context: MangaLoaderContext,
) {

	/**
	 * Decrypts a CryptoJS-compatible base64 ciphertext using a passphrase string.
	 *
	 * Extracts the 8-byte salt from the payload, derives key and IV via MD5-based KDF,
	 * and decrypts the payload with AES-CBC.
	 *
	 * @param cipherText Base64-encoded encrypted payload with standard "Salted__" header.
	 * @param password Passphrase string used during encryption.
	 * @return Decrypted UTF-8 plain text string.
	 * @throws IllegalArgumentException if the ciphertext payload is smaller than 16 bytes.
	 * @throws Exception if cryptographic decryption fails.
	 */
	@Throws(Exception::class)
	public fun decrypt(cipherText: String, password: String): String {
		val ctBytes = context.decodeBase64(cipherText)
		if (ctBytes.size < 16) {
			throw IllegalArgumentException("Invalid CryptoJS payload: length is less than 16 bytes")
		}
		val saltBytes = ctBytes.copyOfRange(8, 16)
		val cipherTextBytes = ctBytes.copyOfRange(16, ctBytes.size)
		val md5: MessageDigest = MessageDigest.getInstance(KDF_DIGEST)
		val keyAndIV = generateKeyAndIV(32, 16, 1, saltBytes, password.toByteArray(Charsets.UTF_8), md5)
		return decryptAES(
			cipherTextBytes,
			keyAndIV.getOrNull(0) ?: ByteArray(32),
			keyAndIV.getOrNull(1) ?: ByteArray(16),
		)
	}

	/**
	 * Decrypts a base64-encoded ciphertext using raw [keyBytes] and [ivBytes].
	 *
	 * @param cipherText Base64-encoded encrypted ciphertext without salt header.
	 * @param keyBytes Raw AES secret key bytes.
	 * @param ivBytes Initialization Vector bytes.
	 * @return Decrypted UTF-8 plain text string.
	 * @throws Exception if cryptographic decryption fails.
	 */
	@Throws(Exception::class)
	public fun decrypt(cipherText: String, keyBytes: ByteArray, ivBytes: ByteArray): String {
		val cipherTextBytes = context.decodeBase64(cipherText)
		return decryptAES(cipherTextBytes, keyBytes, ivBytes)
	}

	/**
	 * Internal cipher execution routine for AES-CBC with PKCS7/PKCS5 padding.
	 */
	@Throws(Exception::class)
	private fun decryptAES(cipherTextBytes: ByteArray, keyBytes: ByteArray, ivBytes: ByteArray): String {
		val cipher = try {
			Cipher.getInstance(HASH_CIPHER)
		} catch (_: Exception) {
			Cipher.getInstance("AES/CBC/PKCS5PADDING")
		}
		val keyS = SecretKeySpec(keyBytes, AES)
		cipher.init(Cipher.DECRYPT_MODE, keyS, IvParameterSpec(ivBytes))
		return cipher.doFinal(cipherTextBytes).toString(Charsets.UTF_8)
	}

	/**
	 * Generates a key and an initialization vector (IV) from the given salt and password.
	 *
	 * Reference: https://stackoverflow.com/a/41434590
	 * This method is equivalent to OpenSSL's EVP_BytesToKey function
	 * (see https://github.com/openssl/openssl/blob/master/crypto/evp/evp_key.c).
	 * By default, OpenSSL uses a single iteration, MD5 as the algorithm and UTF-8 encoded password data.
	 *
	 * @param keyLength Desired key length in bytes (e.g. 32 for AES-256).
	 * @param ivLength Desired IV length in bytes (16 for standard AES block size).
	 * @param iterations Number of digest hash iterations.
	 * @param salt Optional 8-byte salt.
	 * @param password Password byte array.
	 * @param md MessageDigest algorithm instance (MD5).
	 * @return 2-element array containing `[keyBytes, ivBytes]`.
	 */
	@Suppress("SameParameterValue")
	@Throws(Exception::class)
	private fun generateKeyAndIV(
		keyLength: Int,
		ivLength: Int,
		iterations: Int,
		salt: ByteArray?,
		password: ByteArray,
		md: MessageDigest,
	): Array<ByteArray?> {
		val digestLength = md.digestLength
		val requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength
		val generatedData = ByteArray(requiredLength)
		var generatedLength = 0
		md.reset()

		// Repeat process until sufficient data has been generated
		while (generatedLength < keyLength + ivLength) {

			// Digest data (last digest if available, password data, salt if available)
			if (generatedLength > 0) md.update(generatedData, generatedLength - digestLength, digestLength)
			md.update(password)
			if (salt != null) md.update(salt, 0, 8)
			md.digest(generatedData, generatedLength, digestLength)

			// additional rounds
			for (i in 1 until iterations) {
				md.update(generatedData, generatedLength, digestLength)
				md.digest(generatedData, generatedLength, digestLength)
			}
			generatedLength += digestLength
		}

		// Copy key and IV into separate byte arrays
		val result = arrayOfNulls<ByteArray>(2)
		result[0] = generatedData.copyOfRange(0, keyLength)
		if (ivLength > 0) result[1] = generatedData.copyOfRange(keyLength, keyLength + ivLength)
		return result
	}
}
