import javax.sound.sampled.*;
import java.io.*;

/**
 * Synthesises the mod's sounds as mono 44.1kHz WAVs: the engine loops (each
 * kind of combustion engine in two bands at four layers, the electric motor,
 * the propeller, the outboard), the turbo, the horn, the tyre squeal and the
 * machines' noises. Run from this folder, then encode each with
 * {@code oggenc -q 4}.
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

    // ---- the combustion engines (see EngineAcoustics in the mod) ----

    /**
     * Every engine loop is this long: a whole number of every voice's
     * two-turn cycles at every layer, so each partial and each firing comes
     * round exactly and the loop is seamless.
     */
    static final double LOOP_SECONDS = 0.8;

    /** Layers per voice, an octave apart, as EngineAcoustics.LAYERS. */
    static final int LAYERS = 4;

    /**
     * A kind of engine as it sounds, by the same keys, cylinder counts and
     * lowest-layer firing frequencies as EngineAcoustics.Voice.
     *
     * @param strength  each cylinder's pulse, in firing order: how alike they sound
     * @param timing    how far each fires off the even beat, as a share of the firing interval
     * @param firingTop the firing band reaches this many times the firing frequency
     * @param ringHz    the exhaust's resonance, fixed in hertz, so it stays put as the revs climb
     * @param ringQ     how sharp that resonance is
     * @param formantHz where the load band's rasp peaks: intake and exhaust honk, valve train
     * @param tilt      how fast the load band's harmonics fall away: lower is brighter
     * @param knock     how much combustion noise each firing adds to the load band
     * @param knockMs   how quickly that noise dies, in milliseconds
     */
    record Voice(String key, int cylinders, double baseHz, double[] strength, double[] timing,
                 double firingTop, double ringHz, double ringQ, double formantHz, double tilt,
                 double knock, double knockMs) {
    }

    static final Voice[] VOICES = {
            // One big piston: a thump each firing and nothing between, so
            // the firing band reaches further up.
            new Voice("single", 1, 15.0, new double[] {1.0}, new double[] {0.0},
                    6.0, 95.0, 2.5, 480.0, 1.3, 0.25, 1.6),
            new Voice("four", 4, 30.0, new double[] {1.0, 0.93, 1.05, 0.97}, new double[] {0.0, 0.02, -0.015, 0.01},
                    4.0, 160.0, 2.0, 560.0, 1.35, 0.15, 1.2),
            // Six even firings and hardly a difference between them: smooth,
            // and bright higher up.
            new Voice("six", 6, 30.0, new double[] {1.0, 0.99, 1.01, 0.995, 1.005, 0.99}, new double[6],
                    4.0, 210.0, 1.6, 700.0, 1.25, 0.12, 1.0),
            // Cross-plane, firing 1-8-4-3-6-5-7-2: left bank 1 3 5 7, right
            // 2 4 6 8. Each bank fires unevenly and the right one is heard
            // softer and a touch late down its longer pipe: the burble.
            new Voice("v8", 8, 30.0, new double[] {1.0, 0.72, 0.72, 1.0, 0.72, 1.0, 1.0, 0.72},
                    new double[] {0.0, 0.08, 0.08, 0.0, 0.08, 0.0, 0.0, 0.08},
                    4.0, 85.0, 2.2, 380.0, 1.4, 0.14, 1.4),
            // Compression ignition: every firing a hard, short knock.
            new Voice("diesel", 4, 30.0, new double[] {1.0, 0.9, 1.08, 0.95}, new double[] {0.0, 0.03, -0.02, 0.015},
                    4.0, 120.0, 1.5, 850.0, 1.2, 0.5, 0.7),
    };

    /** The rasp is not drawn past this; the knock's noise carries on above it. */
    static final double TOP_HZ = 4000.0;

    /** Where the silencer starts taking the top off the note. */
    static final double MUFFLER_HZ = 600.0;

    /**
     * One engine loop: {@code voice} firing at {@code firingHz}, its firing
     * band or its load band.
     *
     * <p>Built from the whole two-turn cycle, at fc = firing / cylinders.
     * Each cylinder's exhaust pulse lands at its slot in the cycle, with its
     * own strength; summing them over the cycle gives every harmonic of fc a
     * weight. Where the cylinders are alike only every cylinders-th harmonic
     * survives, the firing harmonics; what they do differently leaves the
     * orders between, a V8's burble. The firing band takes the harmonics up
     * to {@code firingTop} times the firing frequency over the exhaust's
     * resonance, with a little exhaust rumble; the load band the ones above,
     * falling away more slowly the brighter the voice, peaked at its formant,
     * with each firing's combustion noise.
     */
    static double[] engineLoop(Voice voice, boolean load, double firingHz) {
        int n = (int) Math.round(LOOP_SECONDS * RATE);
        double[] out = new double[n];
        int cylinders = voice.cylinders();
        double cycleHz = firingHz / cylinders;
        double top = load ? TOP_HZ : voice.firingTop() * firingHz;
        int first = load ? (int) Math.floor(voice.firingTop() * cylinders) + 1 : 1;
        for (int h = first; h * cycleHz <= top; h++) {
            // The cylinders' pulses summed at this harmonic of the cycle.
            double re = 0;
            double im = 0;
            for (int c = 0; c < cylinders; c++) {
                double at = 2 * Math.PI * h * (c + voice.timing()[c]) / cylinders;
                re += voice.strength()[c] * Math.cos(at);
                im -= voice.strength()[c] * Math.sin(at);
            }
            double weight = Math.hypot(re, im) / cylinders;
            if (weight < 1e-4) {
                continue;
            }
            double f = h * cycleHz;
            double order = f / firingHz;
            double amp = load
                    ? weight * Math.pow(order, -voice.tilt()) * (1.0 + 1.5 * peak(f, voice.formantHz(), 2.0))
                    : weight * Math.pow(order, -0.9) * (1.0 + 1.5 * peak(f, voice.ringHz(), voice.ringQ()));
            // The silencer: what leaves the tailpipe loses its top, the
            // exhaust's note more than the engine bay's rasp.
            amp /= 1.0 + Math.pow(f / (load ? MUFFLER_HZ * 1.5 : MUFFLER_HZ), 2);
            // Rotate a phasor rather than call sin per sample: hundreds of partials.
            double phase = Math.atan2(im, re) + 0.37 * h;
            double step = 2 * Math.PI * f / RATE;
            double cs = Math.cos(step);
            double sn = Math.sin(step);
            double x = Math.cos(phase);
            double y = Math.sin(phase);
            for (int i = 0; i < n; i++) {
                out[i] += amp * y;
                double nx = x * cs - y * sn;
                y = x * sn + y * cs;
                x = nx;
            }
        }
        // No two firings burn alike: each one's push wavers a little, and the
        // wavering, smoothed round the loop, spreads every harmonic into the
        // rumble a real engine makes rather than a buzzer's clean line.
        int firingsInLoop = (int) Math.round(firingHz * LOOP_SECONDS);
        java.util.Random burn = new java.util.Random(17L * voice.key().hashCode() + (long) firingHz + (load ? 3 : 0));
        double[] push = new double[firingsInLoop];
        for (int k = 0; k < firingsInLoop; k++) {
            push[k] = burn.nextGaussian();
        }
        double[] waver = new double[n];
        for (int i = 0; i < n; i++) {
            waver[i] = push[(int) (i / RATE * firingHz) % firingsInLoop];
        }
        waver = lowPass(lowPass(waver, 1.5 * firingHz), 1.5 * firingHz);
        double spread = rms(waver);
        for (int i = 0; i < n; i++) {
            out[i] *= 1.0 + (load ? 0.3 : 0.18) * waver[i] / Math.max(spread, 1e-9);
        }
        double tone = rms(out);
        // Noise with each firing: the exhaust's rumble low down, the
        // combustion's knock up in the load band. Filtered round the loop so
        // it joins up.
        double[] noise = periodicNoise(n, 31L * voice.key().hashCode() + (load ? 7 : 0) + (long) firingHz);
        double[] shaped = load
                ? lowPass(bandPass(noise, voice.formantHz(), 0.8), 2.0 * voice.formantHz())
                : lowPass(lowPass(noise, Math.min(2.0 * firingHz, 600.0)), Math.min(2.0 * firingHz, 600.0));
        double decay = load ? voice.knockMs() / 1000.0 : 1.0 / (2.0 * firingHz);
        // Pressure rises fast but not in no time: a fifth of a millisecond.
        double attack = 0.0002;
        double[] burst = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double firings = t * firingHz;
            int cylinder = (int) Math.floor(firings) % cylinders;
            double since = (firings - Math.floor(firings)) / firingHz;
            burst[i] = shaped[i] * voice.strength()[cylinder]
                    * (1.0 - Math.exp(-since / attack)) * Math.exp(-since / decay);
        }
        double share = load ? voice.knock() : 0.08;
        double noiseRms = rms(burst);
        double scale = noiseRms == 0 || tone == 0 ? 0 : share * tone / noiseRms;
        for (int i = 0; i < n; i++) {
            out[i] += burst[i] * scale;
        }
        level(out, load ? 0.11 : 0.22, 0.95);
        return out;
    }

    /**
     * The turbo's whine: the compressor's tone and its octave, wobbling a
     * little with the shaft, over the rush of air through it.
     */
    static double[] turboWhistle() {
        int n = (int) Math.round(LOOP_SECONDS * RATE);
        double[] out = new double[n];
        double[] air = bandPass(periodicNoise(n, 97L), 3200.0, 1.2);
        double tone = 2500.0;
        double wobble = 5.0;
        for (int i = 0; i < n; i++) {
            double t = i / RATE;
            double phase = 2 * Math.PI * tone * t + (tone * 0.012 / wobble) * Math.sin(2 * Math.PI * wobble * t);
            out[i] = Math.sin(phase) + 0.3 * Math.sin(2 * phase + 0.5) + 2.2 * air[i];
        }
        level(out, 0.2, 0.9);
        return out;
    }

    /** A resonance's response at {@code f}: 1 at {@code centre}, falling away either side by {@code q}. */
    static double peak(double f, double centre, double q) {
        double off = (f / centre - centre / f) * q;
        return 1.0 / (1.0 + off * off);
    }

    static double rms(double[] a) {
        double sum = 0;
        for (double v : a) sum += v * v;
        return Math.sqrt(sum / a.length);
    }

    /** Scales to an RMS level, but never so far that a peak passes {@code peak}. */
    static void level(double[] a, double rms, double peak) {
        double r = rms(a);
        double max = 0;
        for (double v : a) max = Math.max(max, Math.abs(v));
        if (r == 0) return;
        double k = Math.min(rms / r, peak / max);
        for (int i = 0; i < a.length; i++) a[i] *= k;
    }

    static double[] periodicNoise(int n, long seed) {
        java.util.Random random = new java.util.Random(seed);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = random.nextDouble() * 2 - 1;
        return out;
    }

    /** One-pole low-pass, run round the loop twice so it starts where it ends. */
    static double[] lowPass(double[] in, double cutoff) {
        double k = 1 - Math.exp(-2 * Math.PI * cutoff / RATE);
        double[] out = new double[in.length];
        double y = 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < in.length; i++) {
                y += (in[i] - y) * k;
                out[i] = y;
            }
        }
        return out;
    }

    /** A two-pole band-pass (constant peak gain), run round the loop twice so it starts where it ends. */
    static double[] bandPass(double[] in, double centre, double q) {
        double w = 2 * Math.PI * centre / RATE;
        double alpha = Math.sin(w) / (2 * q);
        double a0 = 1 + alpha;
        double b0 = alpha / a0;
        double b2 = -alpha / a0;
        double a1 = -2 * Math.cos(w) / a0;
        double a2 = (1 - alpha) / a0;
        double[] out = new double[in.length];
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < in.length; i++) {
                double y = b0 * in[i] + b2 * x2 - a1 * y1 - a2 * y2;
                x2 = x1;
                x1 = in[i];
                y2 = y1;
                y1 = y;
                out[i] = y;
            }
        }
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
        for (Voice voice : VOICES) {
            for (int layer = 0; layer < LAYERS; layer++) {
                double hz = voice.baseHz() * (1 << layer);
                writeWav("engine_" + voice.key() + "_firing_" + layer + ".wav", engineLoop(voice, false, hz));
                writeWav("engine_" + voice.key() + "_load_" + layer + ".wav", engineLoop(voice, true, hz));
            }
        }
        writeWav("turbo_whistle.wav", turboWhistle());
        writeWav("horn.wav", horn(0.65));
        writeWav("skid.wav", skid(0.55));
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
