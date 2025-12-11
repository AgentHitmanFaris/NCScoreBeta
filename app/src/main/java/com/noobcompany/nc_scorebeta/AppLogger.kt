package com.noobcompany.nc_scorebeta

import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Singleton utility responsible for comprehensive application logging and secure bug reporting.
 *
 * `AppLogger` intercepts and stores log messages in a thread-safe in-memory buffer.
 * It provides advanced functionality to aggregate these internal logs with the system's logcat,
 * compress the result, and encrypt it using a hybrid RSA+AES approach for secure transmission.
 */
object AppLogger {
    private val logs = CopyOnWriteArrayList<String>()
    private const val MAX_LOGS = 200
    
    // RSA Public Key (2048-bit)
    private const val PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvJL2pB0Uu28GrpXjReEV8HTMMRgJ127DZKiePVz5aiu8YNf51UaHEKm8TsMyvK0nEs7f6ba777Sax73dWrecLQ6A/rZWTG375+4fSoo7NO21Nn0CyzaFAO2q58FFoeN01pvOY3eePky63FH9NaRwhP0pF6Y6W5lznVXudgfTWIl3O63+iqIAYntyHgukLKtJCtShf1Nyzx0yBvzC5Zk8n6FYhskWafmu1xfj7E4PfBcynQ3G7dcAad+xdmWizthF92znb4IVoJr/WVgIO+u8N9li8mnxELRwJQPhjq1UR2nlrUEe3/7SIsxkGhqjtyU4FlgsyJfuscNUBzU0+dDJFwIDAQAB"
    private const val AES_ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

    /**
     * Records a standard debug message.
     *
     * The message is written to the Android Logcat with the debug priority and
     * appended to the internal rolling buffer for potential future export.
     *
     * @param tag A short string tag identifying the source component (e.g., class name).
     * @param message The debug message text to record.
     */
    fun log(tag: String, message: String) {
        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "$timestamp [$tag] $message"
        
        // Standard Android Log
        Log.d(tag, message)
        
        // Internal Buffer
        logs.add(logEntry)
        if (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }
    }

    /**
     * Records an error message and associated exception.
     *
     * This method captures the stack trace of the provided throwable and logs it
     * alongside the error message to both Logcat (error priority) and the internal buffer.
     *
     * @param tag A short string tag identifying the source component.
     * @param message The error description.
     * @param throwable The exception or error that caused this log entry (optional).
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val logEntry = "$timestamp [$tag] ERROR: $message \n ${throwable?.stackTraceToString() ?: ""}"
        
        Log.e(tag, message, throwable)
        
        logs.add(logEntry)
        if (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }
    }

    /**
     * Returns a snapshot of the current in-memory logs.
     *
     * @return An immutable [List] containing the recent log strings.
     */
    fun getLogs(): List<String> {
        return logs.toList()
    }
    
    /**
     * Clears the internal in-memory log buffer.
     */
    fun clear() {
        logs.clear()
    }

    /**
     * Compiles, compresses, and encrypts a full system log dump.
     *
     * This operation performs the following steps:
     * 1. Collects any user-provided comments.
     * 2. Appends the internal application logs.
     * 3. Executes the `logcat` command to retrieve recent system logs for this process.
     * 4. Compresses the combined text using GZIP.
     * 5. Encrypts the compressed binary using Hybrid Encryption (RSA + AES).
     *
     * @param userComment Additional context provided by the user for the bug report.
     * @return A Base64-encoded string containing the fully encrypted log package.
     */
    fun getEncryptedSystemLogs(userComment: String = ""): String {
        try {
            val logBuilder = StringBuilder()

            // 0. Add User Comment if present
            if (userComment.isNotEmpty()) {
                logBuilder.append("=== USER COMMENT ===\n")
                logBuilder.append(userComment).append("\n\n")
            }

            // 1. Add Internal Logs
            logBuilder.append("=== INTERNAL APP LOGS ===\n")
            logs.forEach { logBuilder.append(it).append("\n") }
            
            // 2. Add System Logcat (App process only)
            logBuilder.append("\n=== SYSTEM LOGCAT ===\n")
            val process = Runtime.getRuntime().exec("logcat -d -v threadtime")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                logBuilder.append(line).append("\n")
            }

            val rawLogs = logBuilder.toString()

            // 3. Compress & Encrypt
            val compressedBytes = compress(rawLogs)
            return encrypt(compressedBytes)

        } catch (e: Exception) {
            Log.e("AppLogger", "Failed to generate logs", e)
            return "Error generating logs: ${e.message}"
        }
    }

    /**
     * Compresses a string into a GZIP byte array.
     *
     * @param data The raw string data.
     * @return The compressed data as a [ByteArray].
     */
    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data.toByteArray(StandardCharsets.UTF_8))
        }
        return bos.toByteArray()
    }

    /**
     * Encrypts a byte array using Hybrid Encryption (RSA + AES).
     *
     * 1. Generates a random AES key (32 bytes).
     * 2. Encrypts the data using AES-256-CBC.
     * 3. Encrypts the AES key using the RSA Public Key.
     * 4. Combines [Encrypted AES Key (256)] + [IV (16)] + [AES Encrypted Data].
     *
     * @param data The data to encrypt.
     * @return A Base64 string of the combined blob.
     */
    private fun encrypt(data: ByteArray): String {
        // 1. Generate random AES Key (32 bytes)
        val aesKeyBytes = ByteArray(32)
        SecureRandom().nextBytes(aesKeyBytes)
        val secretKey = SecretKeySpec(aesKeyBytes, "AES")

        // 2. Encrypt Data with AES
        val cipherAes = Cipher.getInstance(AES_ALGORITHM)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipherAes.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encryptedData = cipherAes.doFinal(data)

        // 3. Encrypt AES Key with RSA Public Key
        val pubKeySpec = X509EncodedKeySpec(Base64.decode(PUBLIC_KEY, Base64.DEFAULT))
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(pubKeySpec)

        val cipherRsa = Cipher.getInstance(RSA_ALGORITHM)
        cipherRsa.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = cipherRsa.doFinal(aesKeyBytes) // 256 bytes for 2048-bit key

        // 4. Combine: [EncryptedKey (256)] + [IV (16)] + [EncryptedData]
        val combined = ByteArray(encryptedKey.size + iv.size + encryptedData.size)
        System.arraycopy(encryptedKey, 0, combined, 0, encryptedKey.size)
        System.arraycopy(iv, 0, combined, encryptedKey.size, iv.size)
        System.arraycopy(encryptedData, 0, combined, encryptedKey.size + iv.size, encryptedData.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
}
