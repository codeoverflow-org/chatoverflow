package org.codeoverflow.chatoverflow.configuration

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}

import javax.crypto.spec.{IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, SecretKeyFactory}
import org.apache.commons.codec.binary.Base64

/**
  * Provides methods to de- and encrypt using the AES-CBC algorithm.
  * This code is based on https://gist.github.com/twuni/5668121.
  */
object CryptoUtil {

  // TODO: This code should be reviewed by an expert to find potential security issues.

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
    ciphertext.append(Base64.encodeBase64String(iv))
    ciphertext.append(":")
    ciphertext.append(Base64.encodeBase64String(salt))
    ciphertext.append(":")
    ciphertext.append(Base64.encodeBase64String(encrypted))
    ciphertext.toString
  }

  private def generateIV: Array[Byte] = {
    val random = new SecureRandom()
    val iv = new Array[Byte](16)
    random.nextBytes(iv)
    iv
  }

  private def generateSalt: Array[Byte] = {
    val random = new SecureRandom()
    val salt = new Array[Byte](8)
    random.nextBytes(salt)
    salt
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
    val iv = Base64.decodeBase64(parts(0))
    val salt = Base64.decodeBase64(parts(1))

    // Generate a key from the password
    val key = generateKeyFromString(password, salt)

    try {
      // Decrypt the ciphertext using AES
      val encrypted = Base64.decodeBase64(parts(2))
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
