package me.trossty.car.sound;

import net.minecraft.util.Mth;

/**
 * The arithmetic of an engine's note, kept apart from the sound engine so it
 * can be checked.
 *
 * <p>A four-stroke fires each cylinder once every two turns, so the note's
 * fundamental, the firing frequency, is {@code rpm / 60 × cylinders / 2}: a
 * four at 3000 rpm thumps a hundred times a second, a V8 at the same revs
 * twice that. Over it ride the firing frequency's harmonics, and under it, in
 * an engine whose cylinders do not all sound alike, the orders of the whole
 * two-turn cycle: a V8's burble, a single's lope.
 *
 * <p>The sound engine will only play a sample between half and twice its own
 * pitch, an octave either way, so every kind of engine ({@link Voice}) comes
 * as {@link #LAYERS} loops made at firing frequencies an octave apart. The
 * two either side of the note play together, crossfaded at equal power, each
 * pitched to sound the note exactly. Each layer is made at its own
 * frequency, so the exhaust's resonances stay where they are as the revs
 * climb instead of sliding up with them.
 *
 * <p>Each layer comes in two {@link Band bands} split by firing order: the
 * firing band, the fundamental and its first harmonics with the cycle's
 * orders under them, which an engine makes whenever it turns; and the load
 * band, the upper harmonics and the combustion's rasp, which only come when
 * it is made to work. Lift off and the load band goes, leaving the hollow
 * note of an engine on the overrun.
 */
public final class EngineAcoustics {

    /** Layers per voice and band, an octave apart. */
    public static final int LAYERS = 4;

    /** The furthest the sound engine pitches a sample, either way. */
    public static final float MIN_PITCH = 0.5F;
    public static final float MAX_PITCH = 2.0F;

    /**
     * How full each breath the engine takes is with the throttle shut: a
     * fifth of an atmosphere in the manifold, against a whole one wide open.
     */
    private static final float CLOSED_BREATH = 0.2F;

    /** The load band's share at idle revs on full throttle; it rises to all of it at the redline. */
    private static final float LOAD_AT_IDLE = 0.4F;

    /** The turbo's whistle loop is made at what the compressor sings at a little over half speed. */
    private static final float WHISTLE_PITCH_FLAT_OUT = 1.6F;

    /** Boost, of the most the turbo makes, worth a blow-off when it is let go. */
    public static final float BLOW_OFF_BOOST = 0.25F;

    private EngineAcoustics() {
    }

    /** The firing frequency in hertz: each cylinder fires once every two turns. */
    public static float firingHz(float rpm, int cylinders) {
        return rpm / 60.0F * cylinders / 2.0F;
    }

    /** Where a firing frequency sits on a voice's layers, in octaves above its first. */
    public static float octave(Voice voice, float hz) {
        return (float) (Math.log(Math.max(hz, 0.001F) / voice.baseHz) / Math.log(2.0));
    }

    /**
     * How loud {@code layer} plays to sound {@code hz}: the two layers either
     * side share it at equal power, cos and sin of how far along between
     * them it is, so the sum of their squares, the loudness, stays level.
     * Below the first layer or above the last, that one plays alone.
     */
    public static float layerGain(Voice voice, float hz, int layer) {
        float x = octave(voice, hz);
        int low = Mth.clamp(Mth.floor(x), 0, LAYERS - 2);
        float along = Mth.clamp(x - low, 0.0F, 1.0F) * Mth.HALF_PI;
        if (layer == low) {
            return Mth.cos(along);
        }
        return layer == low + 1 ? Mth.sin(along) : 0.0F;
    }

    /** The pitch to play {@code layer} at so it sounds {@code hz}, held to what the sound engine allows. */
    public static float layerPitch(Voice voice, float hz, int layer) {
        return Mth.clamp(hz / layerHz(voice, layer), MIN_PITCH, MAX_PITCH);
    }

    /** The firing frequency {@code layer}'s loops were made at. */
    public static float layerHz(Voice voice, int layer) {
        return voice.baseHz * (1 << layer);
    }

    /**
     * How loud the engine is, 0 to 1 at the redline on full throttle. The
     * sound's power goes with the gas pumped through: the revs times how full
     * each breath is. The ear's amplitude is the square root of power.
     */
    public static float loudness(float rpm, float redlineRpm, float throttle) {
        float breath = CLOSED_BREATH + (1.0F - CLOSED_BREATH) * Mth.clamp(throttle, 0.0F, 1.0F);
        return Mth.sqrt(Mth.clamp(rpm / redlineRpm, 0.0F, 1.1F) * breath);
    }

    /**
     * How much of the load band plays against the firing band, 0 to 1: the
     * harder each cylinder is pushed the sharper its pressure rises, and the
     * further up the harmonics go; the faster they come, the more of them are
     * heard. Nothing on a closed throttle.
     */
    public static float loadBand(float rpm, float idleRpm, float redlineRpm, float throttle) {
        float revs = Mth.clamp((rpm - idleRpm) / (redlineRpm - idleRpm), 0.0F, 1.0F);
        return Mth.clamp(throttle, 0.0F, 1.0F) * Mth.lerp(revs, LOAD_AT_IDLE, 1.0F);
    }

    /** The whistle's pitch: the compressor wheel's blades pass at a rate going with the shaft's speed. */
    public static float whistlePitch(float turboSpeed) {
        return Mth.clamp(WHISTLE_PITCH_FLAT_OUT * turboSpeed, MIN_PITCH, MAX_PITCH);
    }

    /** The whistle's loudness: the air's noise off the wheel climbs steeply with its tip speed. */
    public static float whistleGain(float turboSpeed) {
        return turboSpeed * turboSpeed;
    }

    /**
     * The boost the blow-off valve just let go, or 0: there was boost last
     * tick, worth hearing, and the throttle has shut on it.
     */
    public static float ventedBoost(float boostBefore, float boostNow) {
        return boostBefore >= BLOW_OFF_BOOST && boostNow < BLOW_OFF_BOOST * 0.5F ? boostBefore : 0.0F;
    }

    /**
     * A kind of combustion engine as it sounds: the cylinders its loops were
     * made with, the firing frequency of its lowest layer and how loud it is.
     * An engine with another count of cylinders sounds like the nearest, at
     * its own firing frequency.
     */
    public enum Voice {
        /** One or two big pistons: a thump each, long gaps between, so its layers start an octave lower. */
        SINGLE("single", 1, 15.0F, 0.95F),
        FOUR("four", 4, 30.0F, 1.0F),
        /** Six, inline or V: so even that next to nothing comes between the firings. */
        SIX("six", 6, 30.0F, 1.0F),
        /** A cross-plane V8: each bank fires unevenly, and the two banks do not sound alike. */
        V8("v8", 8, 30.0F, 1.15F),
        /** Compression ignition: each firing a hard knock. */
        DIESEL("diesel", 4, 30.0F, 1.1F);

        private final String key;
        private final int cylinders;
        private final float baseHz;
        private final float level;

        Voice(String key, int cylinders, float baseHz, float level) {
            this.key = key;
            this.cylinders = cylinders;
            this.baseHz = baseHz;
            this.level = level;
        }

        public String key() {
            return this.key;
        }

        /** The cylinders the loops were made with. */
        public int cylinders() {
            return this.cylinders;
        }

        /** How loud this kind of engine is against a four. */
        public float level() {
            return this.level;
        }

        public static Voice of(int cylinders, boolean diesel) {
            if (diesel) {
                return DIESEL;
            }
            if (cylinders <= 2) {
                return SINGLE;
            }
            if (cylinders <= 4) {
                return FOUR;
            }
            return cylinders <= 6 ? SIX : V8;
        }
    }

    /** The halves of each loop, split by firing order. */
    public enum Band {
        /** The fundamental and its first harmonics, and the cycle's orders under them. */
        FIRING("firing"),
        /** The upper harmonics and the combustion's rasp, which come with load. */
        LOAD("load");

        private final String key;

        Band(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }
}
