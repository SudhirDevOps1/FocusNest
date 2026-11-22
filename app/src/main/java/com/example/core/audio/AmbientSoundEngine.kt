package com.example.core.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.model.AmbientSoundType
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 100% offline procedural PCM soundscape synthesizer for Focus Sanctuary.
 * Generates soothing brown noise, soft rain, warm harmonic drone, or ambient murmur
 * without bundled media bloat or internet access.
 */
class AmbientSoundEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  private var playbackJob: Job? = null

  @Volatile private var activeType: AmbientSoundType = AmbientSoundType.OFF
  @Volatile private var volumeLevel: Float = 0.45f

  @Synchronized
  fun startOrUpdate(type: AmbientSoundType, volume: Float) {
    volumeLevel = volume.coerceIn(0f, 1f)
    if (type == AmbientSoundType.OFF || volumeLevel <= 0.01f) {
      stop()
      return
    }
    if (activeType == type && playbackJob?.isActive == true) {
      return
    }
    stop()
    activeType = type
    playbackJob =
      scope.launch {
        runSynthLoop(type)
      }
  }

  @Synchronized
  fun stop() {
    activeType = AmbientSoundType.OFF
    playbackJob?.cancel()
    playbackJob = null
  }

  fun isPlaying(): Boolean = activeType != AmbientSoundType.OFF && playbackJob?.isActive == true

  private fun runSynthLoop(type: AmbientSoundType) {
    val sampleRate = 22050
    val minBufferBytes =
      runCatching {
          AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
          )
        }
        .getOrDefault(4096)
        .coerceAtLeast(2048)

    val track =
      runCatching {
          AudioTrack.Builder()
            .setAudioAttributes(
              AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            )
            .setAudioFormat(
              AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            )
            .setBufferSizeInBytes(minBufferBytes * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        }
        .getOrNull() ?: return

    val chunkSize = 1024
    val buffer = ShortArray(chunkSize)
    var brownState = 0.0
    var rainLp = 0.0
    var murmurLp1 = 0.0
    var murmurLp2 = 0.0
    var phaseA = 0.0
    var phaseB = 0.0
    var phaseC = 0.0
    var lfoPhase = 0.0
    val rng = Random(System.currentTimeMillis())

    try {
      track.play()
      while (scope.isActive && activeType == type) {
        val gain = volumeLevel.coerceIn(0f, 1f) * 0.45
        for (i in 0 until chunkSize) {
          val sampleNorm =
            when (type) {
              AmbientSoundType.BROWN_NOISE -> {
                val white = rng.nextDouble() * 2.0 - 1.0
                brownState = ((brownState + 0.025 * white) / 1.025).coerceIn(-1.0, 1.0)
                brownState * 2.4
              }
              AmbientSoundType.SOFT_RAIN -> {
                val white = rng.nextDouble() * 2.0 - 1.0
                rainLp = rainLp * 0.86 + white * 0.14
                val drop = if (rng.nextInt(140) == 0) (rng.nextDouble() - 0.5) * 0.6 else 0.0
                lfoPhase += (2.0 * PI * 0.12) / sampleRate
                val swell = 0.72 + 0.28 * sin(lfoPhase)
                (rainLp * 1.9 + drop) * swell
              }
              AmbientSoundType.WARM_DRONE -> {
                lfoPhase += (2.0 * PI * 0.08) / sampleRate
                phaseA += (2.0 * PI * 110.0) / sampleRate
                phaseB += (2.0 * PI * 164.81) / sampleRate
                phaseC += (2.0 * PI * 220.0) / sampleRate
                val shimmer = 0.8 + 0.2 * sin(lfoPhase)
                ((sin(phaseA) * 0.52) + (sin(phaseB) * 0.30) + (sin(phaseC) * 0.18)) * shimmer
              }
              AmbientSoundType.STUDY_MURMUR -> {
                val white = rng.nextDouble() * 2.0 - 1.0
                murmurLp1 = murmurLp1 * 0.92 + white * 0.08
                murmurLp2 = murmurLp2 * 0.75 + murmurLp1 * 0.25
                lfoPhase += (2.0 * PI * 0.22) / sampleRate
                val modulation = 0.65 + 0.35 * sin(lfoPhase)
                murmurLp2 * 2.8 * modulation
              }
              AmbientSoundType.OFF -> 0.0
            }
          val pcm = (sampleNorm.coerceIn(-1.0, 1.0) * gain * Short.MAX_VALUE).toInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
          buffer[i] = pcm
        }
        track.write(buffer, 0, chunkSize)
      }
    } catch (_: Exception) {
      // Fail gracefully when audio hardware is unavailable or stopped mid-stream
    } finally {
      runCatching {
        track.pause()
        track.flush()
        track.release()
      }
    }
  }
}
