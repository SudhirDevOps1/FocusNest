package com.example

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Selects Robolectric Android SDK 36 when running under JDK 21+ (matching targetSdk = 36),
 * and falls back cleanly to Android SDK 34 when executed under older JDK 17 CI runners
 * because Robolectric API 36 ART instrumentation requires Java 21+.
 */
class AdaptiveRobolectricRunner(testClass: Class<*>) : RobolectricTestRunner(testClass) {
  @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
  override fun buildGlobalConfig(): Config {
    val spec = System.getProperty("java.specification.version") ?: "17"
    val majorJava = spec.removePrefix("1.").substringBefore('.').toIntOrNull() ?: 17
    val targetSdk = if (majorJava >= 21) 36 else 34
    return Config.Builder(super.buildGlobalConfig()).setSdk(targetSdk).build()
  }
}
