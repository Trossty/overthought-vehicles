import javax.sound.sampled.*;
import java.io.*;

/**
 * Synthesises the mod's sounds as mono 44.1kHz WAVs: the engine loops (one
 * per kind of engine), the horn, the tyre squeal and the machines' noises.
 * Run from this folder, then encode each with {@code oggenc -q 4}.
 */
public class GenSounds {
    static final float RATE = 44100f;

    static void writeWav(String path, double[] samples) throws Exception {
        byte[] pcm = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            double v = Math.max(-1.0, Math.min(1.0, samples[i]));
            short s = (short) Math.round(v * 32767.0);
            pcm[i * 2] = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(
                new ByteArrayInputStream(pcm), fmt, samples.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, new File(path));
        }
        System.out.println("wrote " + path + " (" + samples.length + " samples)");
    }

    /** Engine loop: integer cycle counts keep the loop seamless. */
    static double[] engine(double seconds, double fundamental) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        // amplitudes for harmonics 1..8 - uneven weighting gives a combustion-ish timbre
        double[] amp = {1.00, 0.62, 0.45, 0.30, 0.22, 0.14, 0.09, 0.06};
        double[] phase = {0.0, 0.7, 1.9, 2.6, 0.4, 1.2, 2.9, 0.9};
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            for (int h = 0; h < amp.length; h++) {
                double f = fundamental * (h + 1);
                v += amp[h] * Math.sin(2 * Math.PI * f * t + phase[h]);
            }
            // slow throb at 2 cycles per loop keeps it seamless and less synthetic
            double throb = 1.0 + 0.18 * Math.sin(2 * Math.PI * (2.0 / seconds) * t);
            out[i] = v * throb;
        }
        normalise(out, 0.82);
        return out;
    }

    /** Two-tone horn, a major third apart, with a sawtooth edge. */
    static double[] horn(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        double[] tones = {440.0, 554.37};
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            for (double f : tones) {
                for (int h = 1; h <= 6; h++) {
                    v += (1.0 / h) * Math.sin(2 * Math.PI * f * h * t);
                }
            }
            double attack = Math.min(1.0, t / 0.015);
            double release = Math.min(1.0, (seconds - t) / 0.05);
            out[i] = v * attack * Math.max(0.0, release);
        }
        normalise(out, 0.85);
        return out;
    }

    /**
     * Tyre squeal: a wavering ~950 Hz whine with a little rubbery buzz and
     * hiss under it, faded in and out so overlapping plays blend.
     */
    static double[] skid(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(7);
        double phase = 0;
        double noise = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double f = 950.0 + 70.0 * Math.sin(2 * Math.PI * 23.0 * t) + 40.0 * Math.sin(2 * Math.PI * 7.0 * t);
            phase += 2 * Math.PI * f / RATE;
            double v = Math.sin(phase) + 0.35 * Math.sin(2 * phase) + 0.15 * Math.sin(3 * phase);
            // Rough rubber: amplitude flutter.
            v *= 0.75 + 0.25 * Math.sin(2 * Math.PI * 61.0 * t);
            noise = noise * 0.6 + (random.nextDouble() * 2 - 1) * 0.4;
            v += 0.35 * noise;
            double attack = Math.min(1.0, t / 0.06);
            double release = Math.min(1.0, (seconds - t) / 0.12);
            out[i] = v * attack * Math.max(0.0, release);
        }
        normalise(out, 0.7);
        return out;
    }

    /**
     * A cross-plane V8's burble: a low firing note whose eight pulses per two
     * turns come unevenly, so it lopes. Everything repeats a whole number of
     * times a second, so the one-second loop is seamless.
     */
    static double[] engineV8(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        double fundamental = 46.0;
        double[] amp = {1.00, 0.85, 0.40, 0.55, 0.20, 0.28, 0.10, 0.12, 0.05, 0.06};
        double[] phase = {0.0, 1.1, 2.3, 0.6, 1.7, 2.8, 0.2, 1.4, 2.0, 0.9};
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            for (int h = 0; h < amp.length; h++) {
                v += amp[h] * Math.sin(2 * Math.PI * fundamental * (h + 1) * t + phase[h]);
            }
            // The lope: half the firing rate, a deep beat in loudness.
            double lope = 1.0 + 0.35 * Math.sin(2 * Math.PI * (fundamental / 2.0) * t)
                    + 0.12 * Math.sin(2 * Math.PI * 3.0 * t);
            // A sub-octave rumble under it, the exhaust.
            v += 0.45 * Math.sin(2 * Math.PI * (fundamental / 2.0) * t + 0.3);
            out[i] = Math.tanh(1.3 * v * lope);
        }
        normalise(out, 0.82);
        return out;
    }

    /**
     * A diesel's clatter: a dull drone with a sharp knock of noise at every
     * firing. The knocks decay well before the loop's end, so it joins cleanly.
     */
    static double[] engineDiesel(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(11);
        double fundamental = 52.0;
        double firing = 26.0;
        double noise = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            double[] amp = {1.0, 0.5, 0.45, 0.25, 0.18, 0.12};
            for (int h = 0; h < amp.length; h++) {
                v += amp[h] * Math.sin(2 * Math.PI * fundamental * (h + 1) * t + h * 0.8);
            }
            double sinceKnock = (t * firing) % 1.0 / firing;
            double knock = Math.exp(-sinceKnock / 0.006);
            noise = noise * 0.35 + (random.nextDouble() * 2 - 1) * 0.65;
            v += 1.6 * knock * noise;
            out[i] = v;
        }
        normalise(out, 0.8);
        return out;
    }

    /**
     * An electric motor's whine: the inverter's clean tone and its overtones,
     * a faint mains-like hum under them, and no firing at all.
     */
    static double[] engineElectric(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = Math.sin(2 * Math.PI * 330.0 * t)
                    + 0.35 * Math.sin(2 * Math.PI * 660.0 * t + 0.4)
                    + 0.12 * Math.sin(2 * Math.PI * 990.0 * t + 1.1)
                    + 0.08 * Math.sin(2 * Math.PI * 2640.0 * t)
                    + 0.30 * Math.sin(2 * Math.PI * 110.0 * t);
            v *= 1.0 + 0.05 * Math.sin(2 * Math.PI * 4.0 * t);
            out[i] = v;
        }
        normalise(out, 0.6);
        return out;
    }

    /** A generator set running: a busy drone with a little rattle, faded at both ends. */
    static double[] generatorHum(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(3);
        double noise = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = Math.sin(2 * Math.PI * 100.0 * t) + 0.6 * Math.sin(2 * Math.PI * 200.0 * t + 0.5)
                    + 0.3 * Math.sin(2 * Math.PI * 300.0 * t + 1.3) + 0.2 * Math.sin(2 * Math.PI * 50.0 * t);
            noise = noise * 0.8 + (random.nextDouble() * 2 - 1) * 0.2;
            v += 0.5 * noise * (1.0 + Math.sin(2 * Math.PI * 25.0 * t));
            out[i] = v * fade(t, seconds, 0.25, 0.35);
        }
        normalise(out, 0.6);
        return out;
    }

    /** The walking beam's stroke: a dull thump, then the bearing's long creak. */
    static double[] pumpjackCreak(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(5);
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double thump = Math.sin(2 * Math.PI * 55.0 * t) * Math.exp(-t / 0.08);
            double creakT = t - 0.25;
            double creak = 0;
            if (creakT > 0) {
                double f = 380.0 + 220.0 * Math.sin(Math.PI * creakT / (seconds - 0.25));
                phase += 2 * Math.PI * f / RATE;
                // Stick-slip: the tone stutters on and off.
                double stick = 0.5 + 0.5 * Math.signum(Math.sin(2 * Math.PI * 31.0 * creakT + random.nextDouble() * 0.3));
                creak = (Math.sin(phase) + 0.4 * Math.sin(3 * phase)) * stick * 0.35
                        * Math.sin(Math.PI * creakT / (seconds - 0.25));
            }
            out[i] = thump + creak;
        }
        normalise(out, 0.7);
        return out;
    }

    /** Crude boiling in the column: a run of small bubbles popping. */
    static double[] refineryBubble(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(9);
        for (int b = 0; b < 14; b++) {
            double start = random.nextDouble() * (seconds - 0.12);
            double f0 = 180.0 + random.nextDouble() * 380.0;
            double size = 0.4 + random.nextDouble() * 0.6;
            int from = (int) (start * RATE);
            for (int i = from; i < Math.min(n, from + (int) (0.1 * RATE)); i++) {
                double t = (i - from) / RATE;
                // A bubble rises in pitch as it pops.
                double f = f0 * (1.0 + 2.5 * t);
                out[i] += size * Math.sin(2 * Math.PI * f * t) * Math.exp(-t / 0.025);
            }
        }
        for (int i = 0; i < n; i++) {
            out[i] *= fade(i / RATE, seconds, 0.05, 0.1);
        }
        normalise(out, 0.6);
        return out;
    }

    /** A charging post drawing: a transformer buzz with a soft rising tone. */
    static double[] chargerHum(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double buzz = 0;
            for (int h = 1; h <= 9; h += 2) {
                buzz += Math.sin(2 * Math.PI * 100.0 * h * t) / h;
            }
            double tone = 0.25 * Math.sin(2 * Math.PI * (880.0 + 60.0 * t) * t);
            out[i] = (0.7 * buzz + tone) * fade(t, seconds, 0.15, 0.3);
        }
        normalise(out, 0.5);
        return out;
    }

    /**
     * The starter: a geared whir that labours at every compression, about
     * five a second, then the engine catches and roars up before settling.
     */
    static double[] engineStart(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(11);
        double crank = 0.75;
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v;
            if (t < crank) {
                // A whine at ~180 Hz, chopped by the compressions.
                double labour = 0.55 + 0.45 * Math.pow(Math.abs(Math.sin(Math.PI * 5.5 * t)), 0.6);
                v = labour * (Math.sin(2 * Math.PI * 180 * t) + 0.5 * Math.sin(2 * Math.PI * 360 * t)
                        + 0.25 * (random.nextDouble() - 0.5));
                v *= 0.55 * Math.min(1.0, t / 0.03);
            } else {
                // It fires: the note jumps up, flares and falls back to idle.
                double since = t - crank;
                double flare = 1.0 + 1.1 * Math.exp(-since * 5.0) * Math.min(1.0, since / 0.06);
                double f = 70.0 * flare;
                phase += 2 * Math.PI * f / RATE;
                v = 0;
                double[] amp = {1.00, 0.62, 0.45, 0.30, 0.22, 0.14};
                for (int h = 0; h < amp.length; h++) {
                    v += amp[h] * Math.sin(phase * (h + 1) + h);
                }
                v *= Math.min(1.0, since / 0.02) * Math.max(0.0, Math.min(1.0, (seconds - t) / 0.25));
            }
            out[i] = v;
        }
        normalise(out, 0.8);
        return out;
    }

    /** A pop out of the tailpipe: a thump with a crack of noise on top, gone in a third of a second. */
    static double[] backfire(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(3);
        double low = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double noise = random.nextDouble() * 2 - 1;
            low += (noise - low) * 0.08;
            double thump = Math.sin(2 * Math.PI * 55 * t * (1.0 - t)) * Math.exp(-t * 18);
            out[i] = 1.4 * thump + 2.2 * low * Math.exp(-t * 26) + 0.35 * noise * Math.exp(-t * 60);
        }
        normalise(out, 0.9);
        return out;
    }

    /** A turbo's blow-off valve: a hiss that falls in pitch as the pressure goes. */
    static double[] blowoff(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(5);
        double band = 0;
        double band2 = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double noise = random.nextDouble() * 2 - 1;
            // A two-pole low-pass whose corner slides down: "pssshh".
            double k = 0.55 * Math.exp(-t * 3.0) + 0.05;
            band += (noise - band) * k;
            band2 += (band - band2) * k;
            double whistle = 0.25 * Math.sin(2 * Math.PI * (2400 - 1600 * t / seconds) * t) * Math.exp(-t * 8);
            out[i] = ((noise - band2) * 0.6 + whistle) * fade(t, seconds, 0.01, seconds * 0.8);
        }
        normalise(out, 0.7);
        return out;
    }

    /**
     * A light aircraft's propeller: an engine drone with the blades' chop on
     * top, a two-blade prop at twice the crank, in a seamless one-second loop.
     */
    static double[] propeller(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(9);
        double air = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            double[] amp = {1.0, 0.7, 0.5, 0.35, 0.2, 0.12};
            for (int h = 0; h < amp.length; h++) {
                v += amp[h] * Math.sin(2 * Math.PI * 40.0 * (h + 1) * t + h * 0.9);
            }
            double noise = random.nextDouble() * 2 - 1;
            air += (noise - air) * 0.2;
            double chop = 0.6 + 0.4 * Math.abs(Math.sin(2 * Math.PI * 40.0 * t));
            out[i] = (v * 0.7 + air * 1.6) * chop;
        }
        normalise(out, 0.8);
        return out;
    }

    /** An outboard motor: a busy two-stroke buzz with the water burbling under it. */
    static double[] outboard(double seconds) {
        int n = (int) (seconds * RATE);
        double[] out = new double[n];
        java.util.Random random = new java.util.Random(13);
        double water = 0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double v = 0;
            for (int h = 1; h <= 10; h++) {
                v += (1.0 / h) * Math.sin(2 * Math.PI * 95.0 * h * t + h * 1.3);
            }
            double noise = random.nextDouble() * 2 - 1;
            water += (noise - water) * 0.03;
            double burble = 1.0 + 0.3 * Math.sin(2 * Math.PI * 6.0 * t);
            out[i] = v * 0.8 + water * 3.0 * burble;
        }
        normalise(out, 0.8);
        return out;
    }

    static double fade(double t, double seconds, double in, double out) {
        return Math.min(1.0, t / in) * Math.max(0.0, Math.min(1.0, (seconds - t) / out));
    }

    static void normalise(double[] a, double peak) {
        double max = 0;
        for (double v : a) max = Math.max(max, Math.abs(v));
        if (max == 0) return;
        double k = peak / max;
        for (int i = 0; i < a.length; i++) a[i] *= k;
    }

    public static void main(String[] args) throws Exception {
        writeWav("engine.wav", engine(1.0, 70.0));
        writeWav("horn.wav", horn(0.65));
        writeWav("skid.wav", skid(0.55));
        writeWav("engine_v8.wav", engineV8(1.0));
        writeWav("engine_diesel.wav", engineDiesel(1.0));
        writeWav("engine_electric.wav", engineElectric(1.0));
        writeWav("generator_hum.wav", generatorHum(1.4));
        writeWav("pumpjack_creak.wav", pumpjackCreak(1.6));
        writeWav("refinery_bubble.wav", refineryBubble(1.5));
        writeWav("charger_hum.wav", chargerHum(1.0));
        writeWav("engine_start.wav", engineStart(1.3));
        writeWav("backfire.wav", backfire(0.35));
        writeWav("blowoff.wav", blowoff(0.55));
        writeWav("engine_propeller.wav", propeller(1.0));
        writeWav("engine_outboard.wav", outboard(1.0));
    }
}
