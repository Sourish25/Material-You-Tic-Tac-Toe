package com.example.tiktac

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

// Simple synthesizer for SFX
object SoundManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun playSound(event: String) {
        scope.launch {
            when (event) {
                "move" -> playTone(440.0, 0.1) // A4, short
                "win" -> playWinArpeggio()
                "lose" -> playLoseSlide()
                "bomb" -> playExplosion()
                "pop" -> playTone(600.0, 0.05)
                else -> {}
            }
        }
    }

    private fun playTone(freq: Double, durationSec: Double, amplitude: Double = 0.8) {
        val sampleRate = 44100
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate / freq)
            // Apply simple envelope (fade out)
            val envelope = 1.0 - (i.toDouble() / numSamples)
            val sample = (sin(angle) * amplitude * envelope * Short.MAX_VALUE).toInt().toShort()
            buffer[2 * i] = (sample.toInt() and 0x00ff).toByte()
            buffer[2 * i + 1] = ((sample.toInt() and 0xff00) shr 8).toByte()
        }

        playSoundBuffer(buffer, sampleRate)
    }

    private fun playExplosion() {
        // White noise with low pass filter approximation (just random)
        val sampleRate = 44100
        val durationSec = 0.5
        val numSamples = (durationSec * sampleRate).toInt()
        val buffer = ByteArray(2 * numSamples)
        
        for (i in 0 until numSamples) {
             val envelope = (1.0 - (i.toDouble() / numSamples)).let { it * it } // Quadratic decay
             val noise = Random.nextDouble(-1.0, 1.0)
             val sample = (noise * 0.9 * envelope * Short.MAX_VALUE).toInt().toShort()
             buffer[2 * i] = (sample.toInt() and 0x00ff).toByte()
             buffer[2 * i + 1] = ((sample.toInt() and 0xff00) shr 8).toByte()
        }
        playSoundBuffer(buffer, sampleRate)
    }
    
    private fun playWinArpeggio() {
        playTone(523.25, 0.1) // C5
        Thread.sleep(100)
        playTone(659.25, 0.1) // E5
        Thread.sleep(100)
        playTone(783.99, 0.2) // G5
    }

    private fun playLoseSlide() {
        playTone(300.0, 0.3)
        Thread.sleep(300)
        playTone(200.0, 0.4)
    }

    private fun playSoundBuffer(buffer: ByteArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size.coerceAtLeast(minBufferSize))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            
        track.write(buffer, 0, buffer.size)
        track.play()
        // Fire and forget, handled by garbage collector eventually? AudioTrack static mode might need release.
        // For production, reuse track. Here, simple invocation.
        // Actually static mode requires write then play.
    }
}
