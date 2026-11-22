package com.example.core.common

import android.graphics.Bitmap
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

object KeystoreAndBackupCrypto {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "focusnest_local_master_key"
  private const val GCM_TAG_BITS = 128
  private const val PBKDF2_ITERATIONS = 60_000

  fun sha256Hex(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
  }

  /**
   * Standard AES-256-GCM passphrase-protected backup envelope for portable offline backup/restore.
   */
  fun encryptPortableBackup(plainJson: String, passphrase: String): String {
    require(passphrase.length >= 6) { "Passphrase must be at least 6 characters." }
    val random = SecureRandom()
    val salt = ByteArray(16).also { random.nextBytes(it) }
    val iv = ByteArray(12).also { random.nextBytes(it) }
    val key = deriveAesKey(passphrase.toCharArray(), salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    val ciphertext = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
    return JSONObject()
      .put("format", "focusnest-encrypted-backup")
      .put("version", 1)
      .put("kdf", "PBKDF2WithHmacSHA256")
      .put("iterations", PBKDF2_ITERATIONS)
      .put("salt", Base64.getEncoder().encodeToString(salt))
      .put("iv", Base64.getEncoder().encodeToString(iv))
      .put("payload", Base64.getEncoder().encodeToString(ciphertext))
      .toString(2)
  }

  fun decryptPortableBackup(envelopeJson: String, passphrase: String): String {
    val obj = JSONObject(envelopeJson)
    require(obj.optString("format") == "focusnest-encrypted-backup") {
      "Not a valid FocusNest encrypted backup file."
    }
    val salt = Base64.getDecoder().decode(obj.getString("salt"))
    val iv = Base64.getDecoder().decode(obj.getString("iv"))
    val payload = Base64.getDecoder().decode(obj.getString("payload"))
    val iterations = obj.optInt("iterations", PBKDF2_ITERATIONS)
    val key = deriveAesKey(passphrase.toCharArray(), salt, iterations)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    val plainBytes = cipher.doFinal(payload)
    return String(plainBytes, Charsets.UTF_8)
  }

  private fun deriveAesKey(
    passphrase: CharArray,
    salt: ByteArray,
    iterations: Int = PBKDF2_ITERATIONS,
  ): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(passphrase, salt, iterations, 256)
    val encoded = factory.generateSecret(spec).encoded
    return SecretKeySpec(encoded, "AES")
  }

  fun getOrCreateKeystoreSecret(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing
    val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
    keyGen.init(
      KeyGenParameterSpec.Builder(
          KEY_ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256)
        .build()
    )
    return keyGen.generateKey()
  }
}

object QrCodeGenerator {
  fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
    if (content.isBlank()) return null
    return try {
      val hints = mapOf(EncodeHintType.MARGIN to 2)
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val pixels = IntArray(sizePx * sizePx)
      for (y in 0 until sizePx) {
        val offset = y * sizePx
        for (x in 0 until sizePx) {
          pixels[offset + x] = if (matrix[x, y]) 0xFF0E2D27.toInt() else 0xFFFFFFFF.toInt()
        }
      }
      Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    } catch (_: Exception) {
      null
    }
  }
}

data class OemGuidance(
  val manufacturer: String,
  val androidVersionLabel: String,
  val hasAggressiveBackgroundKilling: Boolean,
  val steps: List<String>,
)

object OemCompatibilityHelper {
  fun getGuidance(): OemGuidance {
    val mfg = Build.MANUFACTURER.orEmpty().trim()
    val normalized = mfg.lowercase()
    val androidLabel = "Android ${Build.VERSION.RELEASE ?: "8+"} (API ${Build.VERSION.SDK_INT})"
    return when {
      normalized.contains("realme") || normalized.contains("oppo") || normalized.contains("oneplus") ->
        OemGuidance(
          manufacturer = mfg.ifEmpty { "Realme / ColorOS" },
          androidVersionLabel = androidLabel,
          hasAggressiveBackgroundKilling = true,
          steps =
            listOf(
              "Enable 'Allow background activity' in Settings > Apps > FocusNest > Battery usage.",
              "Enable 'Auto-launch' (if present on Realme UI / ColorOS) so scheduled blocks recover after reboot.",
              "Lock FocusNest in the Recent Apps switcher to prevent aggressive memory cleanup on Android 11+.",
            ),
        )
      normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("poco") ->
        OemGuidance(
          manufacturer = mfg,
          androidVersionLabel = androidLabel,
          hasAggressiveBackgroundKilling = true,
          steps =
            listOf(
              "Set Battery Saver to 'No restrictions' for FocusNest.",
              "Enable Autostart and 'Display pop-up windows while running in background' for reliable block screens.",
            ),
        )
      normalized.contains("samsung") ->
        OemGuidance(
          manufacturer = mfg,
          androidVersionLabel = androidLabel,
          hasAggressiveBackgroundKilling = true,
          steps =
            listOf(
              "Remove FocusNest from 'Sleeping apps' and 'Deep sleeping apps' in Device Care.",
              "Set FocusNest battery mode to 'Unrestricted'.",
            ),
        )
      else ->
        OemGuidance(
          manufacturer = mfg.ifEmpty { "Android" },
          androidVersionLabel = androidLabel,
          hasAggressiveBackgroundKilling = false,
          steps =
            listOf(
              "Disable Battery Optimization for FocusNest if focus timers or schedules pause in Doze mode.",
              "Grant Usage Access and optional Accessibility only when you enable those modules.",
            ),
        )
    }
  }
}
