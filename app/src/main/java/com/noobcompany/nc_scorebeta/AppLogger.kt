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
 * Singleton object for handling application logging.
 *
 * It provides methods to log messages and errors, store them in an internal buffer,
 * and export encrypted logs for bug reporting.
 */
object AppLogger {
    private val logs = CopyOnWriteArrayList<String>()
    private const val MAX_LOGS = 200
    
    // 32 bytes for AES-256
    private const val ENCRYPTION_KEY = "NCScoreBetaKey2024SecretKeyVer01" 
    private const val CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding"

    /**
     * Logs a debug message.
     *
     * The message is printed to the standard Android Log and added to the internal log buffer.
     *
     * @param tag The tag identifying the source of the log message.
     * @param message The message to be logged.
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
     * Logs an error message and an optional exception.
     *
     * The error is printed to the standard Android Log and added to the internal log buffer.
     *
     * @param tag The tag identifying the source of the error.
     * @param message The error message to be logged.
     * @param throwable The exception or error associated with the log, if any.
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
     * Retrieves a copy of the current internal logs.
     *
     * @return A list of log strings.
     */
    fun getLogs(): List<String> {
        return logs.toList()
    }
    
    /**
     * Clears the internal log buffer.
     */
    fun clear() {
        logs.clear()
    }

    /**
     * Captures system logcat and internal logs, compresses them using GZIP, and encrypts them using AES.
     *
     * This method is useful for generating secure bug reports.
     *
     * @return A Base64 encoded string containing the encrypted and compressed logs.
     */
    fun getEncryptedSystemLogs(): String {
        try {
            val logBuilder = StringBuilder()

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
     * Compresses the given string data using GZIP.
     *
     * @param data The string data to compress.
     * @return A byte array containing the compressed data.
     */
    private fun compress(data: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(data.toByteArray(StandardCharsets.UTF_8))
        }
        return bos.toByteArray()
    }

    /**
     * Encrypts the given byte array using AES encryption.
     *
     * @param data The byte array to encrypt.
     * @return A Base64 encoded string containing the IV and the encrypted data.
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
