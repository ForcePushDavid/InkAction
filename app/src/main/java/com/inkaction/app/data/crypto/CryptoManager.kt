package com.inkaction.app.data.crypto

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inkaction.app.data.model.EncryptedCloudNote
import com.inkaction.app.data.model.SavedNote
import com.inkaction.app.ui.canvas.InkStroke
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedPayload(
    val ciphertextBase64: String,
    val ivBase64: String,
    val authTagBase64: String = ""
)

object CryptoManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val KEY_SIZE_BITS = 256
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128
    private const val PBKDF2_ITERATIONS = 100_000

    // Salt used for deriving AES key from user sync passkey (InkAction E2EE namespace)
    private val FIXED_APP_SALT = "InkAction_E2EE_Vault_Salt_v1".toByteArray(StandardCharsets.UTF_8)
    private val secureRandom = SecureRandom()
    private val gson = Gson()

    fun compressGzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    fun decompressGzip(data: ByteArray): ByteArray {
        if (data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()) {
            val bis = ByteArrayInputStream(data)
            return GZIPInputStream(bis).use { it.readBytes() }
        }
        return data
    }

    /**
     * Derives a 256-bit AES key from a user passphrase / PIN using PBKDF2 HMAC-SHA256.
     */
    fun deriveKey(passphrase: String): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), FIXED_APP_SALT, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val rawKey = factory.generateSecret(spec).encoded
        return SecretKeySpec(rawKey, "AES")
    }

    /**
     * Encrypts plaintext using AES-256-GCM with optional GZIP compression.
     */
    fun encrypt(plaintext: String, key: SecretKey, compress: Boolean = true): EncryptedPayload {
        val iv = ByteArray(IV_SIZE_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val rawBytes = plaintext.toByteArray(StandardCharsets.UTF_8)
        val dataToEncrypt = if (compress) compressGzip(rawBytes) else rawBytes
        val cipherBytes = cipher.doFinal(dataToEncrypt)

        // In standard Java AES/GCM, cipherBytes contains [encrypted text + 16-byte authentication tag]
        val ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        return EncryptedPayload(
            ciphertextBase64 = ciphertextBase64,
            ivBase64 = ivBase64
        )
    }

    /**
     * Decrypts an EncryptedPayload using AES-256-GCM and decompresses GZIP if present.
     * Throws an exception if passkey is wrong or authentication tag verification fails.
     */
    fun decrypt(payload: EncryptedPayload, key: SecretKey): String {
        val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(payload.ciphertextBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        val decryptedBytes = cipher.doFinal(cipherBytes)
        val decompressedBytes = decompressGzip(decryptedBytes)
        return String(decompressedBytes, StandardCharsets.UTF_8)
    }

    /**
     * Encrypts a SavedNote and optional InkStrokes into an EncryptedCloudNote.
     */
    fun encryptNote(
        note: SavedNote,
        strokes: List<InkStroke>?,
        passphrase: String,
        userId: String
    ): EncryptedCloudNote {
        val key = deriveKey(passphrase)
        val noteJson = gson.toJson(note)
        val notePayload = encrypt(noteJson, key)

        val (strokesCiphertext, strokesIv) = if (!strokes.isNullOrEmpty()) {
            val strokesJson = gson.toJson(strokes)
            val strokePayload = encrypt(strokesJson, key)
            strokePayload.ciphertextBase64 to strokePayload.ivBase64
        } else {
            "" to ""
        }

        return EncryptedCloudNote(
            id = note.id.toString(),
            userId = userId,
            updatedAt = System.currentTimeMillis(),
            iv = notePayload.ivBase64,
            authTag = "",
            ciphertext = notePayload.ciphertextBase64,
            hasStrokes = !strokes.isNullOrEmpty(),
            strokesCiphertext = strokesCiphertext,
            strokesIv = strokesIv,
            isArchived = note.isArchived
        )
    }

    /**
     * Decrypts an EncryptedCloudNote back into a SavedNote and optional InkStrokes.
     */
    fun decryptNote(
        cloudNote: EncryptedCloudNote,
        passphrase: String
    ): Pair<SavedNote, List<InkStroke>?> {
        val key = deriveKey(passphrase)
        val notePayload = EncryptedPayload(
            ciphertextBase64 = cloudNote.ciphertext,
            ivBase64 = cloudNote.iv
        )
        val noteJson = decrypt(notePayload, key)
        val note = gson.fromJson(noteJson, SavedNote::class.java)

        val strokes = if (cloudNote.hasStrokes && cloudNote.strokesCiphertext.isNotBlank()) {
            val strokesPayload = EncryptedPayload(
                ciphertextBase64 = cloudNote.strokesCiphertext,
                ivBase64 = cloudNote.strokesIv
            )
            val strokesJson = decrypt(strokesPayload, key)
            val type = object : TypeToken<List<InkStroke>>() {}.type
            gson.fromJson<List<InkStroke>>(strokesJson, type)
        } else {
            null
        }

        return note to strokes
    }
}
