package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Vibrator
import android.content.Context
import android.os.VibrationEffect
import android.os.Build

object LudoAudio {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playTone(toneType: Int, duration: Int) {
        try {
            toneGenerator?.startTone(toneType, duration)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sequence play using a safe background thread to avoid blocking UI
    private fun playSequence(vararg tones: Pair<Int, Int>) {
        Thread {
            try {
                for ((tone, delayMs) in tones) {
                    playTone(tone, delayMs)
                    Thread.sleep((delayMs + 10).toLong())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun playDiceRoll(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Ascending rapid rolling sound effect
        playSequence(
            ToneGenerator.TONE_PROP_BEEP to 40,
            ToneGenerator.TONE_PROP_BEEP2 to 40,
            ToneGenerator.TONE_CDMA_PIP to 50
        )
        vibratePattern(context, longArrayOf(0, 30, 40, 30))
    }

    fun playPieceMove(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Upward sliding step sound
        playSequence(
            ToneGenerator.TONE_CDMA_PIP to 35,
            ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE to 40
        )
        vibrate(context, 15)
    }

    fun playCapture(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Downward aggressive crash sound
        playSequence(
            ToneGenerator.TONE_SUP_ERROR to 150,
            ToneGenerator.TONE_SUP_CONGESTION to 150,
            ToneGenerator.TONE_SUP_ERROR to 200
        )
        vibratePattern(context, longArrayOf(0, 100, 50, 150))
    }

    fun playVictory(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Ascending tri-tone champion fanfare
        playSequence(
            ToneGenerator.TONE_PROP_ACK to 120,
            ToneGenerator.TONE_SUP_PIP to 120,
            ToneGenerator.TONE_PROP_ACK to 120,
            ToneGenerator.TONE_SUP_PIP to 300
        )
        vibratePattern(context, longArrayOf(0, 150, 80, 150, 80, 300))
    }

    fun playCheat(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Magical sweep beep
        playSequence(
            ToneGenerator.TONE_PROP_BEEP to 60,
            ToneGenerator.TONE_PROP_ACK to 80,
            ToneGenerator.TONE_SUP_PIP to 120
        )
        vibratePattern(context, longArrayOf(0, 60, 40, 120))
    }

    fun playMenuClick(context: Context, enabled: Boolean) {
        if (!enabled) return
        playTone(ToneGenerator.TONE_PROP_BEEP, 35)
        vibrate(context, 10)
    }

    fun playError(context: Context, enabled: Boolean) {
        if (!enabled) return
        playSequence(
            ToneGenerator.TONE_SUP_CONGESTION to 100,
            ToneGenerator.TONE_SUP_CONGESTION to 150
        )
        vibratePattern(context, longArrayOf(0, 80, 50, 80))
    }

    fun playReleaseYard(context: Context, enabled: Boolean) {
        if (!enabled) return
        // Teleport/warp sweep
        playSequence(
            ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE to 80,
            ToneGenerator.TONE_PROP_ACK to 150
        )
        vibratePattern(context, longArrayOf(0, 40, 40, 80))
    }

    fun playReachedHome(context: Context, enabled: Boolean) {
        if (!enabled) return
        // High-pitched magical sparkle
        playSequence(
            ToneGenerator.TONE_SUP_PIP to 80,
            ToneGenerator.TONE_SUP_PIP to 80,
            ToneGenerator.TONE_PROP_ACK to 180
        )
        vibratePattern(context, longArrayOf(0, 50, 50, 50, 50, 100))
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.createAttributionContext("ludo_game")
            } else {
                context
            }
            val vibrator = attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            durationMs,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePattern(context: Context, pattern: LongArray) {
        try {
            val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.createAttributionContext("ludo_game")
            } else {
                context
            }
            val vibrator = attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
