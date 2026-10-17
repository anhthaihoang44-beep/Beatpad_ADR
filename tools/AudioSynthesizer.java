package tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

/**
 * Procedural Audio Synthesizer generating studio-quality CC0 drum & synth samples
 * for the 5 BeatPad sound packs.
 */
public class AudioSynthesizer {

    private static final int SAMPLE_RATE = 44100;
    private static final Random random = new Random(42);

    public static void main(String[] args) {
        String baseDir = "app/src/main/assets/samples";
        new File(baseDir).mkdirs();

        System.out.println("Synthesizing BeatPad sound samples...");

        // Metronome
        generateMetronomeClicks(baseDir);

        // 8 Genre Packs (96 Samples total)
        generateTrapPack(new File(baseDir, "trap"));
        generateDubstepPack(new File(baseDir, "dubstep"));
        generateEdmPack(new File(baseDir, "edm"));
        generateHousePack(new File(baseDir, "house"));
        generateDnbPack(new File(baseDir, "dnb"));
        generateHipHopPack(new File(baseDir, "hiphop"));
        generateElectroPack(new File(baseDir, "electro"));
        generateFutureBassPack(new File(baseDir, "futurebass"));

        System.out.println("All 98 audio samples generated successfully!");
    }

    private static void generateMetronomeClicks(String baseDir) {
        saveWav(new File(baseDir, "metronome_high.wav"), generateSineTone(1500, 0.04f, 0.9f));
        saveWav(new File(baseDir, "metronome_low.wav"), generateSineTone(800, 0.04f, 0.8f));
    }

    private static void generateTrapPack(File dir) {
        dir.mkdirs();
        // 0: Kick 808
        saveWav(new File(dir, "pad_0.wav"), generateKick(160, 42, 0.45f, 0.95f));
        // 1: Trap Snare
        saveWav(new File(dir, "pad_1.wav"), generateSnare(220, 0.22f, 0.85f));
        // 2: Closed Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.06f, false));
        // 3: Open Hat
        saveWav(new File(dir, "pad_3.wav"), generateHiHat(0.35f, true));
        // 4: 808 Sub Glide
        saveWav(new File(dir, "pad_4.wav"), generate808Bass(46.25f, 0.85f)); // F1
        // 5: Trap Clap
        saveWav(new File(dir, "pad_5.wav"), generateClap(0.24f));
        // 6: Brass Horn Stab
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{174.6f, 220.0f, 261.6f}, 0.4f, "saw"));
        // 7: Synth Pluck
        saveWav(new File(dir, "pad_7.wav"), generatePluck(349.2f, 0.35f)); // F4
        // 8: Laser FX
        saveWav(new File(dir, "pad_8.wav"), generatePitchSweep(1200, 150, 0.3f));
        // 9: Noise Riser
        saveWav(new File(dir, "pad_9.wav"), generateRiser(0.8f));
        // 10: Vocal 'Hey'
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(260, 800, 1200, 0.25f));
        // 11: Vocal Chop High
        saveWav(new File(dir, "pad_11.wav"), generateVocalChop(390, 950, 1800, 0.3f));
    }

    private static void generateHipHopPack(File dir) {
        dir.mkdirs();
        // 0: Boom Bap Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(130, 50, 0.32f, 0.9f));
        // 1: Vinyl Rimshot
        saveWav(new File(dir, "pad_1.wav"), generateRimshot(450, 0.12f));
        // 2: Dusty Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.08f, false));
        // 3: Lo-Fi Snare
        saveWav(new File(dir, "pad_3.wav"), generateSnare(180, 0.25f, 0.8f));
        // 4: Mellow Upright Bass
        saveWav(new File(dir, "pad_4.wav"), generateSineTone(65.4f, 0.6f, 0.85f)); // C2
        // 5: Rhodes Chord 1
        saveWav(new File(dir, "pad_5.wav"), generateChord(new float[]{261.6f, 329.6f, 392.0f, 493.9f}, 0.7f, "rhodes")); // Cmaj7
        // 6: Rhodes Chord 2
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{220.0f, 261.6f, 329.6f, 392.0f}, 0.7f, "rhodes")); // Am7
        // 7: Lo-Fi Guitar Lick
        saveWav(new File(dir, "pad_7.wav"), generatePluck(523.2f, 0.45f)); // C5
        // 8: Vinyl Crackle Texture
        saveWav(new File(dir, "pad_8.wav"), generateVinylCrackle(0.6f));
        // 9: Tape Stop FX
        saveWav(new File(dir, "pad_9.wav"), generatePitchSweep(600, 80, 0.45f));
        // 10: Smooth Vocal
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(220, 700, 1100, 0.4f));
        // 11: Lo-Fi Hum
        saveWav(new File(dir, "pad_11.wav"), generateSineTone(110.0f, 0.6f, 0.6f));
    }

    private static void generateEdmPack(File dir) {
        dir.mkdirs();
        // 0: Mainstage Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(190, 48, 0.35f, 1.0f));
        // 1: Big Room Snare
        saveWav(new File(dir, "pad_1.wav"), generateSnare(240, 0.28f, 0.95f));
        // 2: Bright Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.07f, false));
        // 3: Stadium Clap
        saveWav(new File(dir, "pad_3.wav"), generateClap(0.28f));
        // 4: Donk Bass
        saveWav(new File(dir, "pad_4.wav"), generateKick(220, 60, 0.22f, 0.9f));
        // 5: Supersaw Lead 1
        saveWav(new File(dir, "pad_5.wav"), generateChord(new float[]{392.0f, 466.2f, 587.3f}, 0.5f, "saw")); // Gm
        // 6: Supersaw Lead 2
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{349.2f, 440.0f, 523.2f}, 0.5f, "saw")); // F
        // 7: Drop Synth Grunt
        saveWav(new File(dir, "pad_7.wav"), generateChord(new float[]{98.0f, 196.0f}, 0.3f, "saw"));
        // 8: White Noise Sweep
        saveWav(new File(dir, "pad_8.wav"), generateRiser(0.7f));
        // 9: Downlifter
        saveWav(new File(dir, "pad_9.wav"), generatePitchSweep(2000, 100, 0.5f));
        // 10: Vocal 'Drop'
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(180, 600, 1000, 0.28f));
        // 11: Staccato Vocal Chop
        saveWav(new File(dir, "pad_11.wav"), generateVocalChop(350, 850, 1400, 0.22f));
    }

    private static void generateHousePack(File dir) {
        dir.mkdirs();
        // 0: Deep 909 Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(140, 44, 0.38f, 0.9f));
        // 1: House Clap
        saveWav(new File(dir, "pad_1.wav"), generateClap(0.22f));
        // 2: Open 909 Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.28f, true));
        // 3: Closed Shaker
        saveWav(new File(dir, "pad_3.wav"), generateHiHat(0.06f, false));
        // 4: Classic House Organ
        saveWav(new File(dir, "pad_4.wav"), generateChord(new float[]{220.0f, 261.6f, 329.6f, 440.0f}, 0.45f, "organ")); // Am
        // 5: Deep Bouncy Bassline
        saveWav(new File(dir, "pad_5.wav"), generateSineTone(55.0f, 0.4f, 0.9f)); // A1
        // 6: Piano Chord Stab
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{261.6f, 329.6f, 392.0f}, 0.5f, "rhodes")); // C
        // 7: Electric Analog Tom
        saveWav(new File(dir, "pad_7.wav"), generateKick(220, 110, 0.2f, 0.75f));
        // 8: Reverse Cymbal Swell
        saveWav(new File(dir, "pad_8.wav"), generateReverseCymbal(0.6f));
        // 9: Dub Siren Echo
        saveWav(new File(dir, "pad_9.wav"), generatePitchSweep(880, 440, 0.35f));
        // 10: Soul Vocal 'Yeah'
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(280, 850, 1350, 0.35f));
        // 11: House Groove Vocal
        saveWav(new File(dir, "pad_11.wav"), generateVocalChop(330, 900, 1600, 0.25f));
    }

    private static void generateFutureBassPack(File dir) {
        dir.mkdirs();
        // 0: Future Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(170, 48, 0.32f, 0.95f));
        // 1: Snap Snare
        saveWav(new File(dir, "pad_1.wav"), generateSnare(260, 0.22f, 0.9f));
        // 2: Trap Hi-Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.05f, false));
        // 3: Crash Cymbal
        saveWav(new File(dir, "pad_3.wav"), generateHiHat(0.5f, true));
        // 4: Sub Boom 808
        saveWav(new File(dir, "pad_4.wav"), generate808Bass(41.2f, 0.8f)); // E1
        // 5: Supersaw LFO Swell
        saveWav(new File(dir, "pad_5.wav"), generateFutureBassChord(new float[]{329.6f, 392.0f, 493.9f, 587.3f}, 0.7f)); // Em9
        // 6: Pitched Vocal Lead
        saveWav(new File(dir, "pad_6.wav"), generateVocalChop(440, 1000, 2000, 0.3f));
        // 7: Kawaii Arp Pluck
        saveWav(new File(dir, "pad_7.wav"), generatePluck(659.2f, 0.25f)); // E5
        // 8: Sub Impact FX
        saveWav(new File(dir, "pad_8.wav"), generateKick(120, 35, 0.8f, 1.0f));
        // 9: Future Riser
        saveWav(new File(dir, "pad_9.wav"), generateRiser(0.75f));
        // 10: Chibi Vocal 'Go!'
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(520, 1100, 2200, 0.22f));
        // 11: Glitch Stutter
        saveWav(new File(dir, "pad_11.wav"), generatePitchSweep(900, 200, 0.18f));
    }

    private static void generateDubstepPack(File dir) {
        dir.mkdirs();
        // 0: Dubstep Punch Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(200, 45, 0.38f, 1.0f));
        // 1: Metallic Snare
        saveWav(new File(dir, "pad_1.wav"), generateSnare(260, 0.28f, 0.95f));
        // 2: Dubstep Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.06f, false));
        // 3: Crash Splash
        saveWav(new File(dir, "pad_3.wav"), generateHiHat(0.45f, true));
        // 4: Heavy Wobble Bass
        saveWav(new File(dir, "pad_4.wav"), generateWobbleBass(41.2f, 4.0f, 0.75f)); // E1 wobble
        // 5: Sub Bass Drop
        saveWav(new File(dir, "pad_5.wav"), generate808Bass(38.8f, 0.85f));
        // 6: Screech Lead Synth
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{329.6f, 392.0f}, 0.4f, "saw"));
        // 7: Dark Arp Pluck
        saveWav(new File(dir, "pad_7.wav"), generatePluck(493.9f, 0.3f));
        // 8: Laser Impact FX
        saveWav(new File(dir, "pad_8.wav"), generatePitchSweep(1800, 80, 0.45f));
        // 9: Build-up Riser
        saveWav(new File(dir, "pad_9.wav"), generateRiser(0.8f));
        // 10: Vocal 'Drop It'
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(220, 750, 1300, 0.35f));
        // 11: Tape Glitch Stop
        saveWav(new File(dir, "pad_11.wav"), generatePitchSweep(700, 100, 0.22f));
    }

    private static void generateDnbPack(File dir) {
        dir.mkdirs();
        // 0: Fast DnB Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(190, 55, 0.22f, 0.95f));
        // 1: Tight 200Hz Snare
        saveWav(new File(dir, "pad_1.wav"), generateSnare(210, 0.18f, 0.9f));
        // 2: Rolling Amen Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.04f, false));
        // 3: Ghost Snare
        saveWav(new File(dir, "pad_3.wav"), generateSnare(240, 0.12f, 0.5f));
        // 4: Heavy Reese Bass
        saveWav(new File(dir, "pad_4.wav"), generateReeseBass(73.4f, 0.7f)); // D2 Reese
        // 5: Sub Drone 808
        saveWav(new File(dir, "pad_5.wav"), generate808Bass(36.7f, 0.8f)); // D1 sub
        // 6: Jump-Up Lead Stab
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{293.6f, 349.2f}, 0.3f, "saw"));
        // 7: Atmospheric Pad
        saveWav(new File(dir, "pad_7.wav"), generateChord(new float[]{146.8f, 174.6f, 220.0f}, 0.6f, "organ"));
        // 8: Uplifter Sweep
        saveWav(new File(dir, "pad_8.wav"), generateRiser(0.6f));
        // 9: Laser Downlifter
        saveWav(new File(dir, "pad_9.wav"), generatePitchSweep(1600, 120, 0.35f));
        // 10: Jungle Vocal Chop
        saveWav(new File(dir, "pad_10.wav"), generateVocalChop(370, 900, 1800, 0.25f));
        // 11: Breakbeat Roll
        saveWav(new File(dir, "pad_11.wav"), generateClap(0.18f));
    }

    private static void generateElectroPack(File dir) {
        dir.mkdirs();
        // 0: Electro Fat Kick
        saveWav(new File(dir, "pad_0.wav"), generateKick(160, 48, 0.32f, 0.95f));
        // 1: Electro Clap
        saveWav(new File(dir, "pad_1.wav"), generateClap(0.24f));
        // 2: Crispy Hat
        saveWav(new File(dir, "pad_2.wav"), generateHiHat(0.05f, false));
        // 3: Electro Open Hat
        saveWav(new File(dir, "pad_3.wav"), generateHiHat(0.3f, true));
        // 4: Acid Saw Bass 303
        saveWav(new File(dir, "pad_4.wav"), generateAcidBass(61.7f, 0.45f)); // B1 acid
        // 5: Buzz Bassline Loop
        saveWav(new File(dir, "pad_5.wav"), generateWobbleBass(61.7f, 8.0f, 0.6f));
        // 6: Square Lead Hook
        saveWav(new File(dir, "pad_6.wav"), generateChord(new float[]{246.9f, 311.1f, 369.9f}, 0.35f, "saw"));
        // 7: Disco Electro Pluck
        saveWav(new File(dir, "pad_7.wav"), generatePluck(493.9f, 0.22f));
        // 8: Siren Laser FX
        saveWav(new File(dir, "pad_8.wav"), generatePitchSweep(1100, 300, 0.28f));
        // 9: White Noise Sweep
        saveWav(new File(dir, "pad_9.wav"), generateRiser(0.65f));
        // 10: Robot Vocoder 'Beat'
        saveWav(new File(dir, "pad_10.wav"), generateVocoderVoice(160, 0.3f));
        // 11: Robot Vocoder 'Pad'
        saveWav(new File(dir, "pad_11.wav"), generateVocoderVoice(240, 0.3f));
    }

    // --- Audio Synthesis DSP Algorithms ---

    private static float[] generateKick(float startFreq, float endFreq, float durationSec, float volume) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float freq = startFreq * (float) Math.exp(-progress * 9.0) + endFreq;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 6.0);
            // Click transient at start
            float click = (i < 80) ? (random.nextFloat() * 2f - 1f) * 0.4f : 0f;
            buffer[i] = (float) Math.tanh(Math.sin(phase) * env + click) * volume;
        }
        return buffer;
    }

    private static float[] generate808Bass(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 2.5);
            // Gentle saturation for warm harmonics
            float sine = (float) Math.sin(phase);
            buffer[i] = (float) Math.tanh(sine * 1.5) * env * 0.9f;
        }
        return buffer;
    }

    private static float[] generateSnare(float toneFreq, float durationSec, float volume) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * toneFreq / SAMPLE_RATE;
            float toneEnv = (float) Math.exp(-progress * 15.0);
            float noiseEnv = (float) Math.exp(-progress * 9.0);
            float noise = (random.nextFloat() * 2f - 1f);
            float tone = (float) Math.sin(phase);
            buffer[i] = (tone * toneEnv * 0.4f + noise * noiseEnv * 0.6f) * volume;
        }
        return buffer;
    }

    private static float[] generateHiHat(float durationSec, boolean isOpen) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        float decayRate = isOpen ? 6.0f : 40.0f;
        float lastSample = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float env = (float) Math.exp(-progress * decayRate);
            // High-pass filtered noise
            float rawNoise = random.nextFloat() * 2f - 1f;
            float hpNoise = rawNoise - lastSample * 0.85f;
            lastSample = rawNoise;
            buffer[i] = hpNoise * env * 0.7f;
        }
        return buffer;
    }

    private static float[] generateClap(float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        // Claps have 3 micro-bursts before final release
        int burstInterval = (int) (SAMPLE_RATE * 0.012f);
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float noise = random.nextFloat() * 2f - 1f;
            float env;
            if (i < burstInterval * 3) {
                int inBurst = i % burstInterval;
                env = (float) Math.exp(-(inBurst / (float) burstInterval) * 4.0) * 0.7f;
            } else {
                env = (float) Math.exp(-progress * 10.0) * 0.85f;
            }
            buffer[i] = noise * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generateRimshot(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 30.0);
            float noise = (random.nextFloat() * 2f - 1f) * 0.3f;
            buffer[i] = ((float) Math.sin(phase) + noise) * env * 0.85f;
        }
        return buffer;
    }

    private static float[] generateChord(float[] freqs, float durationSec, String type) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double[] phases = new double[freqs.length];

        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float env = (float) Math.exp(-progress * 3.5);
            float sum = 0;

            for (int f = 0; f < freqs.length; f++) {
                phases[f] += 2.0 * Math.PI * freqs[f] / SAMPLE_RATE;
                float osc;
                if ("saw".equals(type)) {
                    osc = (float) (2.0 * (phases[f] / (2.0 * Math.PI) - Math.floor(phases[f] / (2.0 * Math.PI) + 0.5)));
                } else if ("organ".equals(type)) {
                    osc = (float) (Math.sin(phases[f]) + 0.5 * Math.sin(phases[f] * 2) + 0.25 * Math.sin(phases[f] * 3));
                } else {
                    // Rhodes / Electric Piano
                    osc = (float) (Math.sin(phases[f]) + 0.3 * Math.sin(phases[f] * 2) * Math.exp(-progress * 4.0));
                }
                sum += osc;
            }
            buffer[i] = (float) Math.tanh((sum / freqs.length) * 1.2) * env * 0.75f;
        }
        return buffer;
    }

    private static float[] generateFutureBassChord(float[] freqs, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double[] phases = new double[freqs.length];
        double lfoPhase = 0;

        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            // 4 Hz LFO modulation on cutoff/volume for signature future bass pump
            lfoPhase += 2.0 * Math.PI * 4.0 / SAMPLE_RATE;
            float lfo = (float) (0.5 + 0.5 * Math.sin(lfoPhase));
            float env = (float) Math.exp(-progress * 2.8) * (0.3f + 0.7f * lfo);

            float sum = 0;
            for (int f = 0; f < freqs.length; f++) {
                // Detuned saws
                phases[f] += 2.0 * Math.PI * (freqs[f] * 1.005f) / SAMPLE_RATE;
                float saw = (float) (2.0 * (phases[f] / (2.0 * Math.PI) - Math.floor(phases[f] / (2.0 * Math.PI) + 0.5)));
                sum += saw;
            }
            buffer[i] = (float) Math.tanh(sum * 0.4) * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generatePluck(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 12.0);
            // Bell / Pluck with harmonic overtone
            float tone = (float) (Math.sin(phase) + 0.4 * Math.sin(phase * 2.0) + 0.2 * Math.sin(phase * 3.0));
            buffer[i] = tone * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generatePitchSweep(float startFreq, float endFreq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float freq = startFreq + (endFreq - startFreq) * progress;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 3.0);
            buffer[i] = (float) Math.sin(phase) * env * 0.75f;
        }
        return buffer;
    }

    private static float[] generateRiser(float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            // Exponential volume rise
            float env = (float) Math.pow(progress, 2.0);
            float noise = random.nextFloat() * 2f - 1f;
            buffer[i] = noise * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generateReverseCymbal(float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            float env = (float) Math.pow(progress, 3.0);
            float noise = random.nextFloat() * 2f - 1f;
            buffer[i] = noise * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generateVinylCrackle(float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        for (int i = 0; i < length; i++) {
            float noise = (random.nextFloat() * 2f - 1f) * 0.05f;
            // Occasional pops
            if (random.nextFloat() < 0.0008f) {
                noise += (random.nextFloat() * 2f - 1f) * 0.4f;
            }
            buffer[i] = noise;
        }
        return buffer;
    }

    private static float[] generateVocalChop(float pitch, float f1, float f2, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double pPhase = 0;
        double f1Phase = 0;
        double f2Phase = 0;

        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            pPhase += 2.0 * Math.PI * pitch / SAMPLE_RATE;
            f1Phase += 2.0 * Math.PI * f1 / SAMPLE_RATE;
            f2Phase += 2.0 * Math.PI * f2 / SAMPLE_RATE;

            float env = (float) (Math.sin(Math.PI * Math.min(1.0, progress * 4.0)) * Math.exp(-progress * 3.5));
            // Formant resonance sum
            float formant = (float) (Math.sin(pPhase) * 0.4 + Math.sin(f1Phase) * 0.35 + Math.sin(f2Phase) * 0.25);
            buffer[i] = (float) Math.tanh(formant * 1.5) * env * 0.85f;
        }
        return buffer;
    }

    private static float[] generateSineTone(float freq, float durationSec, float volume) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float env = (float) Math.exp(-progress * 4.0);
            buffer[i] = (float) Math.sin(phase) * env * volume;
        }
        return buffer;
    }

    private static float[] generateWobbleBass(float freq, float lfoHz, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        double lfoPhase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            lfoPhase += 2.0 * Math.PI * lfoHz / SAMPLE_RATE;
            float lfo = (float) (0.5 + 0.5 * Math.sin(lfoPhase)); // 0..1 wobble
            float env = (float) Math.exp(-progress * 2.0);
            float saw = (float) (2.0 * (phase / (2.0 * Math.PI) - Math.floor(phase / (2.0 * Math.PI) + 0.5)));
            // Drive saturation
            buffer[i] = (float) Math.tanh(saw * (1.5 + 3.0 * lfo)) * env * 0.85f;
        }
        return buffer;
    }

    private static float[] generateReeseBass(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double p1 = 0, p2 = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            // Detuned saws create thick chorus beating
            p1 += 2.0 * Math.PI * (freq * 0.992) / SAMPLE_RATE;
            p2 += 2.0 * Math.PI * (freq * 1.008) / SAMPLE_RATE;
            float saw1 = (float) (2.0 * (p1 / (2.0 * Math.PI) - Math.floor(p1 / (2.0 * Math.PI) + 0.5)));
            float saw2 = (float) (2.0 * (p2 / (2.0 * Math.PI) - Math.floor(p2 / (2.0 * Math.PI) + 0.5)));
            float env = (float) Math.exp(-progress * 1.8);
            buffer[i] = (float) Math.tanh((saw1 + saw2) * 0.7) * env * 0.9f;
        }
        return buffer;
    }

    private static float[] generateAcidBass(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double phase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            // Fast resonant filter sweep
            float cutoff = (float) Math.exp(-progress * 8.0);
            phase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            float saw = (float) (2.0 * (phase / (2.0 * Math.PI) - Math.floor(phase / (2.0 * Math.PI) + 0.5)));
            float resonance = (float) Math.sin(phase * (1.0 + 4.0 * cutoff));
            float env = (float) Math.exp(-progress * 3.5);
            buffer[i] = (float) Math.tanh((saw * 0.6f + resonance * 0.4f) * 1.8) * env * 0.8f;
        }
        return buffer;
    }

    private static float[] generateVocoderVoice(float freq, float durationSec) {
        int length = (int) (SAMPLE_RATE * durationSec);
        float[] buffer = new float[length];
        double pulsePhase = 0;
        for (int i = 0; i < length; i++) {
            float progress = (float) i / length;
            pulsePhase += 2.0 * Math.PI * freq / SAMPLE_RATE;
            // Pulse waveform bandpassed
            float pulse = (float) (Math.sin(pulsePhase) > 0 ? 1.0 : -1.0);
            float formant = (float) (Math.sin(pulsePhase * 3.5) + Math.sin(pulsePhase * 6.0));
            float env = (float) (Math.sin(Math.PI * Math.min(1.0, progress * 6.0)) * Math.exp(-progress * 3.0));
            buffer[i] = (float) Math.tanh((pulse * 0.4f + formant * 0.6f) * 1.2) * env * 0.85f;
        }
        return buffer;
    }

    // --- WAV File Writer ---

    private static void saveWav(File file, float[] monoSamples) {
        try {
            int numSamples = monoSamples.length;
            int channels = 2; // Stereo
            int totalAudioLen = numSamples * channels * 2;
            int totalDataLen = totalAudioLen + 36;
            int byteRate = SAMPLE_RATE * channels * 2;

            byte[] header = new byte[44];
            header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
            header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
            header[20] = 1; header[21] = 0; // PCM
            header[22] = (byte) channels; header[23] = 0;
            header[24] = (byte) (SAMPLE_RATE & 0xff);
            header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
            header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
            header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (channels * 2); header[33] = 0;
            header[34] = 16; header[35] = 0; // 16 bits
            header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            ByteBuffer pcmBuffer = ByteBuffer.allocate(totalAudioLen).order(ByteOrder.LITTLE_ENDIAN);
            for (float s : monoSamples) {
                short val = (short) (Math.max(-1.0f, Math.min(1.0f, s)) * 32767);
                pcmBuffer.putShort(val); // Left
                pcmBuffer.putShort(val); // Right
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(header);
                fos.write(pcmBuffer.array());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
