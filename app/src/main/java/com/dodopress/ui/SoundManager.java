package com.dodopress.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

public class SoundManager {

    private static final String PREF_NAME = "dodopress_sound_prefs";
    private static final String KEY_SOUND_ENABLED = "sound_effects_enabled";

    private static boolean soundEnabled = true;
    private static boolean initialized = false;

    public static synchronized void init(Context context) {
        if (!initialized) {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            soundEnabled = sp.getBoolean(KEY_SOUND_ENABLED, true);
            initialized = true;
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        soundEnabled = enabled;
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }

    /**
     * Plays a subtle tactile mechanical switch click (7ms impulse).
     */
    public static void playClick() {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                int sampleRate = 22050;
                int numSamples = (int) (sampleRate * 0.008); // 8ms
                short[] buffer = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / sampleRate;
                    double env = Math.exp(-t * 900.0);
                    double freq = 2600.0 - (t * 120000.0);
                    short sample = (short) (Math.sin(2 * Math.PI * freq * t) * 8000.0 * env);
                    buffer[i] = sample;
                }
                playPcm(buffer, sampleRate);
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Plays the authentic Windows XP print completion chime (F#5 + C#6 bell chime).
     */
    public static void playXpPrintChime() {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                int sampleRate = 22050;
                int durationMs = 1200;
                int numSamples = (int) (sampleRate * (durationMs / 1000.0));
                short[] buffer = new short[numSamples];

                double f1 = 739.99; // F#5
                double f2 = 1108.73; // C#6
                double f3 = 1479.98; // F#6 (overtone)

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / sampleRate;
                    // Bell envelope
                    double env = Math.exp(-t * 3.5);
                    double s1 = Math.sin(2 * Math.PI * f1 * t) * 0.55;
                    double s2 = Math.sin(2 * Math.PI * f2 * t) * 0.35;
                    double s3 = Math.sin(2 * Math.PI * f3 * t) * 0.15;

                    short sample = (short) ((s1 + s2 + s3) * 16000.0 * env);
                    buffer[i] = sample;
                }
                playPcm(buffer, sampleRate);
            } catch (Exception ignored) {}
        }).start();
    }

    /**
     * Plays warning alert for printer jam / hardware notice.
     */
    public static void playAlert() {
        if (!soundEnabled) return;
        new Thread(() -> {
            try {
                int sampleRate = 22050;
                int numSamples = (int) (sampleRate * 0.28);
                short[] buffer = new short[numSamples];

                for (int i = 0; i < numSamples; i++) {
                    double t = (double) i / sampleRate;
                    double freq = (t < 0.14) ? 440.0 : 330.0;
                    double env = 1.0 - (t / 0.28);
                    short sample = (short) (Math.sin(2 * Math.PI * freq * t) * 12000.0 * env);
                    buffer[i] = sample;
                }
                playPcm(buffer, sampleRate);
            } catch (Exception ignored) {}
        }).start();
    }

    private static void playPcm(short[] pcm, int sampleRate) {
        AudioTrack track = null;
        try {
            int bufSize = pcm.length * 2;
            if (Build.VERSION.SDK_INT >= 21) {
                track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                    .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                    .setBufferSizeInBytes(bufSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            } else {
                track = new AudioTrack(
                    AudioManager.STREAM_NOTIFICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize,
                    AudioTrack.MODE_STATIC
                );
            }

            track.write(pcm, 0, pcm.length);
            track.play();

            Thread.sleep(pcm.length * 1000L / sampleRate + 50);
            track.stop();
            track.release();
        } catch (Exception e) {
            if (track != null) {
                try { track.release(); } catch (Exception ignored) {}
            }
        }
    }
}
