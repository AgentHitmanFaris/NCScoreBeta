package com.noobcompany.nc_scorebeta

import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
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
 * compress the result, and encrypt it using AES-256 for secure transmission in bug reports.
 */
object AppLogger {
    private val logs = CopyOnWriteArrayList<String>()
    private const val MAX_LOGS = 200
    
    // 32 bytes for AES-256
    private const val ENCRYPTION_KEY = "NCScoreBetaKey2024SecretKeyVer01" 
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"

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
     * 5. Encrypts the compressed binary using AES-256 (CBC mode).
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
     * Encrypts a byte array using AES-256 CBC.
     *
     * Generates a random Initialization Vector (IV) for each encryption operation.
     * The IV is prepended to the ciphertext to allow for decryption.
     *
     * @param data The data to encrypt.
     * @return A Base64 string of [IV + Ciphertext].
     */
    private fun encrypt(data: ByteArray): String {
        val secretKey = SecretKeySpec(ENCRYPTION_KEY.toByteArray(StandardCharsets.UTF_8), "AES")
        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        
        // Generate random IV (16 bytes)
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val encryptedBytes = cipher.doFinal(data)

        // Combine IV + Encrypted Data
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
}
