package com.example

import com.example.core.common.KeystoreAndBackupCrypto
import com.example.domain.model.FocusTimeUtils
import com.example.domain.model.RulePriorityResolver
import com.example.domain.model.UnlockChallengeEngine
import com.example.domain.model.UnlockChallengeType
import com.example.service.FocusNestVpnService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FocusNestDomainUnitTest {

  @Test
  fun unlockChallengeEngine_validatesMathSentenceIntentionAndLimits() {
    val math = UnlockChallengeEngine.generateMathChallenge(attemptCountToday = 4)
    assertEquals(2, math.difficultyLevel)

    assertTrue(UnlockChallengeEngine.validateSentence("Protect focus now.", "Protect focus now."))
    assertFalse(UnlockChallengeEngine.validateSentence("Protect focus now.", "protect focus"))

    assertTrue(UnlockChallengeEngine.validateIntention("Checking calendar schedule for meeting"))
    assertFalse(UnlockChallengeEngine.validateIntention("bored"))

    val (dupAllowed, dupMsg) =
      UnlockChallengeEngine.canGrantUnlock(
        unlocksUsedToday = 1,
        dailyUnlockLimit = 5,
        lastUnlockEpochMs = 100_000L,
        nowEpochMs = 103_000L,
      )
    assertFalse(dupAllowed)
    assertNotNull(dupMsg)

    val (capAllowed, _) =
      UnlockChallengeEngine.canGrantUnlock(
        unlocksUsedToday = 5,
        dailyUnlockLimit = 5,
        lastUnlockEpochMs = 10_000L,
        nowEpochMs = 100_000L,
      )
    assertFalse(capAllowed)
  }

  @Test
  fun rulePriorityResolver_respectsPredictableHierarchy() {
    val tempUnlocked =
      RulePriorityResolver.resolve(
        packageName = "com.app",
        tempUnlockExpiresAtEpochMs = 200_000L,
        nowEpochMs = 100_000L,
        activeFocusBlocksPackage = true,
        activeFocusProfileName = "Work",
        matchingScheduledProfileName = "Study",
        matchingScheduledProfileId = 1L,
        matchingScheduledPriority = 8,
        matchingChallenge = UnlockChallengeType.ADAPTIVE_MATH,
        isStrictMode = true,
        usedMinutesToday = 90,
        dailyLimitMinutes = 30,
      )
    assertFalse(tempUnlocked.isBlocked)

    val focusWinsOverDailyLimit =
      RulePriorityResolver.resolve(
        packageName = "com.app",
        tempUnlockExpiresAtEpochMs = null,
        nowEpochMs = 100_000L,
        activeFocusBlocksPackage = true,
        activeFocusProfileName = "Deep Work",
        matchingScheduledProfileName = null,
        matchingScheduledProfileId = null,
        matchingScheduledPriority = 0,
        matchingChallenge = UnlockChallengeType.INTENTION_ENTRY,
        isStrictMode = false,
        usedMinutesToday = 60,
        dailyLimitMinutes = 30,
      )
    assertTrue(focusWinsOverDailyLimit.isBlocked)
    assertEquals(90, focusWinsOverDailyLimit.priorityRank)
  }

  @Test
  fun vpnDomainMatcher_andDnsPacketParser_workLocally() {
    assertTrue(FocusNestVpnService.domainMatches("m.youtube.com", "youtube.com"))
    assertTrue(FocusNestVpnService.domainMatches("youtube.com", "youtube.com"))
    assertFalse(FocusNestVpnService.domainMatches("notyoutube.com", "youtube.com"))

    // Minimal IPv4 UDP port 53 DNS query for "a.com"
    val pkt =
      byteArrayOf(
        0x45, 0x00, 0x00, 0x25, 0x00, 0x01, 0x00, 0x00, 0x40, 0x11, 0x00, 0x00,
        10, 88, 0, 2, 10, 88, 0, 1,
        0x30, 0x39, 0x00, 0x35, 0x00, 0x11, 0x00, 0x00,
        0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        1, 'a'.code.toByte(), 3, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(), 0,
      )
    val parsed = FocusNestVpnService.parseDnsQueryDomain(pkt, pkt.size)
    assertEquals("a.com", parsed)
    val nx = FocusNestVpnService.buildDnsNxdomainResponse(pkt, pkt.size)
    assertNotNull(nx)
    assertEquals(0x80, nx!![30].toInt() and 0x80) // QR=1
    assertEquals(0x03, nx[31].toInt() and 0x0F) // RCODE=NXDOMAIN
  }

  @Test
  fun portableAesGcmEncryption_roundTripsAndFormatsTime() {
    val encrypted = KeystoreAndBackupCrypto.encryptPortableBackup("{\"hello\":\"focus\"}", "strongpass")
    val decrypted = KeystoreAndBackupCrypto.decryptPortableBackup(encrypted, "strongpass")
    assertEquals("{\"hello\":\"focus\"}", decrypted)
    assertEquals("1h 25m", FocusTimeUtils.formatDurationMinutes(85))
    assertEquals("02:05", FocusTimeUtils.formatTimerSeconds(125))
  }

  @Test
  fun unlockChallengeEngine_supports12ChallengesMathLevel3AndThresholdWarnings() {
    assertEquals(12, UnlockChallengeType.entries.size)
    assertTrue(UnlockChallengeType.BREATHING in UnlockChallengeType.entries)
    assertEquals(6, com.example.domain.model.AccentPalette.entries.size)

    val math3 = UnlockChallengeEngine.generateMathChallenge(attemptCountToday = 6)
    assertEquals(3, math3.difficultyLevel)
    assertTrue(math3.prompt.contains("÷"))

    assertEquals(
      com.example.domain.model.LimitThresholdLevel.NONE,
      UnlockChallengeEngine.evaluateUsageThreshold(usedMinutes = 15, limitMinutes = 30),
    )
    assertEquals(
      com.example.domain.model.LimitThresholdLevel.WARN_80,
      UnlockChallengeEngine.evaluateUsageThreshold(usedMinutes = 24, limitMinutes = 30),
    )
    assertEquals(
      com.example.domain.model.LimitThresholdLevel.WARN_90,
      UnlockChallengeEngine.evaluateUsageThreshold(usedMinutes = 27, limitMinutes = 30),
    )
    assertEquals(
      com.example.domain.model.LimitThresholdLevel.WARN_100,
      UnlockChallengeEngine.evaluateUsageThreshold(usedMinutes = 30, limitMinutes = 30),
    )
  }

  @Test
  fun rulePriorityResolver_supportsGracePeriodAndCategoryDailyLimits() {
    val inGrace =
      RulePriorityResolver.resolve(
        packageName = "com.app",
        tempUnlockExpiresAtEpochMs = null,
        nowEpochMs = 100_000L,
        activeFocusBlocksPackage = false,
        activeFocusProfileName = null,
        matchingScheduledProfileName = null,
        matchingScheduledProfileId = null,
        matchingScheduledPriority = 0,
        matchingChallenge = UnlockChallengeType.BREATHING,
        isStrictMode = false,
        usedMinutesToday = 32,
        dailyLimitMinutes = 30,
        gracePeriodMinutes = 5,
      )
    assertFalse(inGrace.isBlocked)
    assertTrue(inGrace.reason.contains("Grace period"))

    val afterGrace =
      RulePriorityResolver.resolve(
        packageName = "com.app",
        tempUnlockExpiresAtEpochMs = null,
        nowEpochMs = 100_000L,
        activeFocusBlocksPackage = false,
        activeFocusProfileName = null,
        matchingScheduledProfileName = null,
        matchingScheduledProfileId = null,
        matchingScheduledPriority = 0,
        matchingChallenge = UnlockChallengeType.BREATHING,
        isStrictMode = false,
        usedMinutesToday = 35,
        dailyLimitMinutes = 30,
        gracePeriodMinutes = 5,
      )
    assertTrue(afterGrace.isBlocked)
    assertEquals(UnlockChallengeType.BREATHING, afterGrace.challengeType)

    val categoryBlocked =
      RulePriorityResolver.resolve(
        packageName = "com.app",
        tempUnlockExpiresAtEpochMs = null,
        nowEpochMs = 100_000L,
        activeFocusBlocksPackage = false,
        activeFocusProfileName = null,
        matchingScheduledProfileName = null,
        matchingScheduledProfileId = null,
        matchingScheduledPriority = 0,
        matchingChallenge = UnlockChallengeType.REFLECTION_REMINDER,
        isStrictMode = false,
        usedMinutesToday = 15,
        dailyLimitMinutes = 45,
        categoryUsedMinutesToday = 65,
        categoryDailyLimitMinutes = 60,
        categoryName = "SOCIAL",
      )
    assertTrue(categoryBlocked.isBlocked)
    assertEquals(58, categoryBlocked.priorityRank)
  }

  @Test
  fun launchPauseCoordinator_andWellbeingConfigs_workDeterministically() {
    val pkg = "com.instagram.android"
    val paused = setOf(pkg)
    val now = 1_000_000L

    assertFalse(
      com.example.domain.model.LaunchPauseCoordinator.shouldTriggerPause(
        packageName = pkg,
        launchPauseEnabled = false,
        pausedPackages = paused,
        lastProceedEpochMs = null,
        nowEpochMs = now,
      )
    )
    assertFalse(
      com.example.domain.model.LaunchPauseCoordinator.shouldTriggerPause(
        packageName = "com.other.app",
        launchPauseEnabled = true,
        pausedPackages = paused,
        lastProceedEpochMs = null,
        nowEpochMs = now,
      )
    )
    assertTrue(
      com.example.domain.model.LaunchPauseCoordinator.shouldTriggerPause(
        packageName = pkg,
        launchPauseEnabled = true,
        pausedPackages = paused,
        lastProceedEpochMs = null,
        nowEpochMs = now,
      )
    )
    assertFalse(
      com.example.domain.model.LaunchPauseCoordinator.shouldTriggerPause(
        packageName = pkg,
        launchPauseEnabled = true,
        pausedPackages = paused,
        lastProceedEpochMs = now - 60_000L,
        nowEpochMs = now,
      )
    )
    assertTrue(
      com.example.domain.model.LaunchPauseCoordinator.shouldTriggerPause(
        packageName = pkg,
        launchPauseEnabled = true,
        pausedPackages = paused,
        lastProceedEpochMs = now - 6 * 60_000L,
        nowEpochMs = now,
      )
    )

    assertEquals(5, com.example.domain.model.AmbientSoundType.entries.size)
    assertTrue(com.example.domain.model.BedtimeChecklistDefaults.steps.size >= 4)
    assertEquals(
      "https://github.com/OWNER/FocusNest/releases/latest",
      com.example.domain.model.UpdateReleaseConfig.RELEASES_URL,
    )
  }
}
