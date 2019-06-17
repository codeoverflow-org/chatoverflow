package org.codeoverflow.chatoverflow.configuration

import java.nio.charset.StandardCharsets
import java.security.{DigestException, MessageDigest, SecureRandom}
import java.util
import java.util.Base64

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}

/**
  * Provides methods to de- and encrypt using the AES-CBC algorithm.
  *
  * This code is based on https://gist.github.com/twuni/5668121.
  * The SSL compliant / crypto-js compliant code is based on
  * https://stackoverflow.com/questions/41432896/cryptojs-aes-encryption-and-java-aes-decryption
  */
object CryptoUtil {

  // Used for the run-unique auth key
  private val runSpecificRandom = generateIV

  /**
    * Generates a run-unique authentication key using a supplied password.
    *
    * @param password a password to communicate with the framework
    * @return an auth key based the password an an random array
    */
  def generateAuthKey(password: String): String = {
    val authBase = runSpecificRandom.mkString + password

    val digest = MessageDigest.getInstance("SHA-256")
    digest.digest(authBase.getBytes(StandardCharsets.UTF_8)).mkString
  }

  /**
    * Decrypts the ciphertext SSL compatible and thus also compatible to crypto-js.
    * Can be combined with: CryptoJS.AES.encrypt(msg, key).toString();
    *
    * @param secret     the secret, password, key, call it whatever you want
    * @param ciphertext the ciphertext. Don't forget the integrity check ("CHECK")
    * @return Some plaintext if the key was correct and the integrity check was happy
    */
  def decryptSSLcompliant(secret: String, ciphertext: String): Option[String] = {
    try {
      val cipherData = Base64.getDecoder.decode(ciphertext)
      val saltData = util.Arrays.copyOfRange(cipherData, 8, 16)

      val md5 = MessageDigest.getInstance("MD5")
      val keyAndIV = generateSSLcompliantKeyAndIV(32, 16, 1, saltData, secret.getBytes(StandardCharsets.UTF_8), md5)
      val key = new SecretKeySpec(keyAndIV(0), "AES")
      val iv = new IvParameterSpec(keyAndIV(1))


      val encrypted = util.Arrays.copyOfRange(cipherData, 16, cipherData.length)
      val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")
      aesCBC.init(Cipher.DECRYPT_MODE, key, iv)
      val decryptedData = aesCBC.doFinal(encrypted)
      val decrString = new String(decryptedData, StandardCharsets.UTF_8)

      // Same stupid integrity check
      if (!decrString.startsWith("CHECK")) {
        None
      } else {
        Some(decrString.substring(5))
      }
    } catch {
      case _: Exception => None
    }
  }

  /**
    * This helping function is based on the OpenSSL-Implementation.
    * Please see: https://github.com/openssl/openssl/blob/master/crypto/evp/evp_key.c
    */
  private def generateSSLcompliantKeyAndIV(keyLength: Int, ivLength: Int, iterations: Int, salt: Array[Byte], password: Array[Byte], md: MessageDigest): Array[Array[Byte]] = {
    val digestLength = md.getDigestLength
    val requiredLength = (keyLength + ivLength + digestLength - 1) / digestLength * digestLength
    val generatedData = new Array[Byte](requiredLength)
    var generatedLength = 0
    try {
      md.reset()

      // Repeat process until sufficient data has been generated
      while ( {
        generatedLength < keyLength + ivLength
      }) { // Digest data (last digest if available, password data, salt if available)
        if (generatedLength > 0) md.update(generatedData, generatedLength - digestLength, digestLength)
        md.update(password)
        if (salt != null) md.update(salt, 0, 8)
        md.digest(generatedData, generatedLength, digestLength)
        // additional rounds
        var i = 1
        while ( {
          i < iterations
        }) {
          md.update(generatedData, generatedLength, digestLength)
          md.digest(generatedData, generatedLength, digestLength)

          {
            i += 1
            i - 1
          }
        }
        generatedLength += digestLength
      }
      // Copy key and IV into separate byte arrays
      val result = new Array[Array[Byte]](2)
      result(0) = util.Arrays.copyOfRange(generatedData, 0, keyLength)
      if (ivLength > 0) result(1) = util.Arrays.copyOfRange(generatedData, keyLength, keyLength + ivLength)
      result
    } catch {
      case e: DigestException =>
        throw new RuntimeException(e)
    } finally {
      // Clean out temporary data
      util.Arrays.fill(generatedData, 0.toByte)
    }
  }

  /**
    * Encrypts the plaintext SSL compatible and thus also compatible to crypto-js.
    * Can be combined with: CryptoJS.AES.decrypt(msg, key).toString(CryptoJS.enc.Utf8);
    *
    * @param secret    the secret, password, key, call it whatever you want
    * @param plaintext the plaintext to encrypt. adds the integrity check ("CHECK") internally
    * @return an ciphertext ready to be send
    */
  def encryptSSLcompliant(secret: String, plaintext: String): String = {
    val saltData = generateSalt
    val md5 = MessageDigest.getInstance("MD5")
    val keyAndIV = generateSSLcompliantKeyAndIV(32, 16, 1, saltData, secret.getBytes(StandardCharsets.UTF_8), md5)
    val key = new SecretKeySpec(keyAndIV(0), "AES")
    val iv = new IvParameterSpec(keyAndIV(1))

    val decrypted = ("CHECK" + plaintext).getBytes(StandardCharsets.UTF_8)
    val aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding")
    aesCBC.init(Cipher.ENCRYPT_MODE, key, iv)
    val encrypted = aesCBC.doFinal(decrypted)

    val message = "Salted__".getBytes(StandardCharsets.UTF_8) ++ saltData ++ encrypted

    new String(Base64.getEncoder.encode(message), StandardCharsets.UTF_8)
  }

  /**
    * Encrypts the provided plaintext using AES.
    *
    * @param password  the password for the encryption
    * @param plaintext the raw text to encrypt
    * @return a encrypted value
    */
  def encrypt(password: Array[Char], plaintext: String): String = {

    // Generate a key from the password and salt and a init vector
    val iv = generateIV
    val salt = generateSalt
    val key = generateKeyFromString(password, salt)

    // Use the AES algorithm to encrypt the plaintext
    val decrypted = ("CHECK" + plaintext).getBytes // Stupid integrity test
    val cipher = Cipher.getInstance(key.getAlgorithm + "/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv))
    val encrypted = cipher.doFinal(decrypted)

    // Use a string builder and Base64 to save the init vector and encrypted data
    val ciphertext = new StringBuilder
    ciphertext.append(org.apache.commons.codec.binary.Base64.encodeBase64String(iv))
    ciphertext.append(":")
    ciphertext.append(org.apache.commons.codec.binary.Base64.encodeBase64String(salt))
    ciphertext.append(":")
    ciphertext.append(org.apache.commons.codec.binary.Base64.encodeBase64String(encrypted))
    ciphertext.toString
  }

  private def generateSalt: Array[Byte] = {
    val random = new SecureRandom()
    val salt = new Array[Byte](8)
    random.nextBytes(salt)
    salt
  }

  private def generateIV: Array[Byte] = {
    val random = new SecureRandom()
    val iv = new Array[Byte](16)
    random.nextBytes(iv)
    iv
  }

  private def generateKeyFromString(password: Array[Char], salt: Array[Byte]) = {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = new PBEKeySpec(password, salt, 10000, 128)
    val tmp = factory.generateSecret(spec)
    new SecretKeySpec(tmp.getEncoded, "AES")
  }

  /**
    * Decrypts the provided ciphertext using AES.
    *
    * @param password   the password used for the encryption process
    * @param ciphertext the encrypted ciphertext
    * @return some decrypted plaintext or none if the password was incorrect
    */
  def decrypt(password: Array[Char], ciphertext: String): Option[String] = {

    // Retrieve the used init vector from the ciphertext
    val parts = ciphertext.split(":")
    val iv = org.apache.commons.codec.binary.Base64.decodeBase64(parts(0))
    val salt = org.apache.commons.codec.binary.Base64.decodeBase64(parts(1))

    // Generate a key from the password
    val key = generateKeyFromString(password, salt)

    try {
      // Decrypt the ciphertext using AES
      val encrypted = org.apache.commons.codec.binary.Base64.decodeBase64(parts(2))
      val cipher = Cipher.getInstance(key.getAlgorithm + "/CBC/PKCS5Padding")
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv))
      val decrypted = cipher.doFinal(encrypted)
      val decrString = new String(decrypted)

      // Stupid integrity check
      if (!decrString.startsWith("CHECK")) {
        None
      } else {
        Some(decrString.substring(5))
      }
    } catch {
      case _: Exception => None
    }
  }

}
