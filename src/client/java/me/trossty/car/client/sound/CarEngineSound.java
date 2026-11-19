package me.trossty.car.client.sound;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.Gearbox;
import me.trossty.car.sound.EngineAcoustics;
import me.trossty.car.sound.ModSounds;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * One vehicle's engine as everybody nearby hears it, mixed from loops that
 * follow it about.
 *
 * <p>A combustion engine sounds at its firing frequency, read off the revs
 * the {@link Gearbox} runs and the cylinders in the bay: two layers of its
 * voice either side of the note, each in a firing band and a load band, the
 * load band swelling with the throttle. So the note climbs through a gear,
 * dips and goes hollow as the throttle is cut for the next, flares on a
 * change down's blip, and a big engine's burble, a single's thump and a
 * diesel's knock stay their own at any revs. See {@link EngineAcoustics}.
 *
 * <p>On top: a turbo's whistle with its shaft, its blow-off as the throttle
 * shuts on boost, pops from a big or tired engine lifting off, the starter
 * cranking, and the road under the tyres. An electric motor, a propeller and
 * an outboard each play a loop of their own, pitched with the revs.
 */
public final class CarEngineSound {

    /** A starved engine idles quietly but is still heard; the mix keeps it off the floor. */
    private static final float FLOOR = 0.22F;
    private static final float MAX_VOLUME = 0.95F;
    /** The whistle at full shaft speed, against the engine. */
    private static final float WHISTLE_VOLUME = 0.32F;
    /** How much of the gap to its new loudness a loop closes each tick, so layers swap without a click. */
    private static final float GAIN_EASE = 0.5F;
    /** A loop this quiet that is no longer wanted gives its channel back. */
    private static final float GONE = 0.004F;

    /** Ticks a parked, empty car keeps its channels before letting them go. */
    private static final int QUIET_TICKS = 20;
    /** A loop the sound engine has not ticked for this long was never started (the pool was full). */
    private static final int LOST_TICKS = 40;
    /** How long the starter cranks before the engine catches, in ticks. */
    private static final int CRANK_TICKS = 15;
    /** Revs, as a share of the redline, the engine has to be above for a lift to pop. */
    private static final float POP_FROM = 0.6F;

    /** The engine each car has playing, so there is only ever one. */
    private static final Map<CarEntity, CarEngineSound> ENGINES = new WeakHashMap<>();
    /** The cars whose engines were running last tick, to hear them start. */
    private static final Set<CarEntity> RUNNING = Collections.newSetFromMap(new WeakHashMap<>());
    /** When each car's engine last started: the loops wait while the starter turns it over. */
    private static final Map<CarEntity, Long> STARTED = new WeakHashMap<>();

    private final CarEntity car;
    private final Map<SoundEvent, Loop> loops = new HashMap<>();
    private Sound sound;
    private int quiet;
    private float lastThrottle;
    private float lastBoost;
    private int popCooldown;
    /** Distance rolled since the last crunch from the road. */
    private float rolled;

    /**
     * Channels only for a car that makes a noise: running, or rolling on the
     * road. The sound engine has a few hundred channels in all, and a car park
     * of silent loops would take them from everything else in the world.
     * Called every client tick.
     */
    public static void listen(ClientLevel level) {
        long now = level.getGameTime();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof CarEntity car) || car instanceof me.trossty.car.entity.TrailerEntity) {
                continue;
            }
            boolean running = running(car);
            if (running && RUNNING.add(car) && !car.isElectric()) {
                // Somebody got in and turned the key: crank, catch.
                STARTED.put(car, now);
                level.playLocalSound(car.getX(), car.getY() + 0.5, car.getZ(), ModSounds.ENGINE_START,
                        SoundSource.NEUTRAL, 0.9F, car.getEngineFuel().needsSpark() ? 1.0F : 0.85F, false);
            } else if (!running) {
                RUNNING.remove(car);
            }
            if (audible(car)) {
                ENGINES.computeIfAbsent(car, CarEngineSound::new);
            }
        }
        ENGINES.values().removeIf(engine -> !engine.tick(now));
    }

    private static boolean running(CarEntity car) {
        return car.getControllingPassenger() != null && car.hasFuel();
    }

    private static boolean audible(CarEntity car) {
        return running(car) || car.onGround() && Math.abs(car.getEngineSpeed()) >= 0.02F;
    }

    private CarEngineSound(CarEntity car) {
        this.car = car;
        this.sound = Sound.of(car);
    }

    /** One tick of the mix; false once the car is gone or quiet, and every loop let go. */
    private boolean tick(long now) {
        this.quiet = audible(this.car) ? 0 : this.quiet + 1;
        if (this.car.isRemoved() || this.quiet > QUIET_TICKS) {
            this.loops.values().forEach(Loop::end);
            this.loops.clear();
            return false;
        }
        Sound sound = Sound.of(this.car);
        if (sound != this.sound) {
            // A different engine went in: its loops take over at once.
            this.loops.values().forEach(Loop::end);
            this.loops.clear();
            this.sound = sound;
        }

        boolean running = running(this.car);
        Gearbox box = this.car.gearbox();
        float rpm = running ? box.getEngineRpm() : 0.0F;
        float throttle = running ? box.getThrottle() : 0.0F;
        float level = running ? this.level(rpm, throttle) : 0.0F;
        // A blowing silencer lets the whole bang out.
        level *= this.car.upkeep().exhaustNoise();
        // Still on the starter: the engine comes in as it catches.
        Long started = STARTED.get(this.car);
        if (started != null) {
            level *= Mth.clamp((now - started - CRANK_TICKS + 4) / 4.0F, 0.0F, 1.0F);
        }

        Map<SoundEvent, float[]> wanted = new HashMap<>();
        if (this.sound.voice != null) {
            this.combustion(wanted, rpm, throttle, level);
        } else {
            float revs = Mth.clamp(rpm / box.getRedlineRpm(), 0.0F, 1.0F);
            wanted.put(this.sound.loop, new float[] {
                    level * Mth.lerp(revs, this.sound.idleVolume, this.sound.maxVolume),
                    Mth.lerp(revs, this.sound.idlePitch, this.sound.maxPitch)});
        }
        float shaft = box.getTurboSpeed();
        if (shaft > 0.01F) {
            wanted.put(ModSounds.TURBO_WHISTLE, new float[] {
                    WHISTLE_VOLUME * EngineAcoustics.whistleGain(shaft), EngineAcoustics.whistlePitch(shaft)});
        }
        this.mix(wanted, now);

        this.lift(rpm / box.getRedlineRpm(), throttle, running ? box.getBoost() : 0.0F);
        this.road();
        return true;
    }

    /** How loud the engine runs, from what it is pumping, squeezed for the game's mix. */
    private float level(float rpm, float throttle) {
        float loudness = EngineAcoustics.loudness(rpm, this.car.gearbox().getRedlineRpm(), throttle);
        float level = MAX_VOLUME * (FLOOR + (1.0F - FLOOR) * loudness);
        return this.sound.voice != null ? level * this.sound.voice.level() : level;
    }

    /** Both layers either side of the firing note, in both bands. */
    private void combustion(Map<SoundEvent, float[]> wanted, float rpm, float throttle, float level) {
        Gearbox box = this.car.gearbox();
        EngineAcoustics.Voice voice = this.sound.voice;
        float hz = EngineAcoustics.firingHz(Math.max(rpm, 1.0F), box.getEngineSpec().cylinders());
        float load = EngineAcoustics.loadBand(rpm, box.getIdleRpm(), box.getRedlineRpm(), throttle);
        for (int layer = 0; layer < EngineAcoustics.LAYERS; layer++) {
            float gain = EngineAcoustics.layerGain(voice, hz, layer);
            if (gain <= 0.0F) {
                continue;
            }
            float pitch = EngineAcoustics.layerPitch(voice, hz, layer);
            wanted.put(ModSounds.engineLoop(voice, EngineAcoustics.Band.FIRING, layer),
                    new float[] {level * gain, pitch});
            wanted.put(ModSounds.engineLoop(voice, EngineAcoustics.Band.LOAD, layer),
                    new float[] {level * gain * load, pitch});
        }
    }

    /**
     * Brings each loop towards what is wanted of it: starts the ones that
     * are not playing, eases each one's loudness, and lets go of the ones no
     * longer wanted once they are silent.
     */
    private void mix(Map<SoundEvent, float[]> wanted, long now) {
        for (Map.Entry<SoundEvent, float[]> want : wanted.entrySet()) {
            Loop loop = this.loops.get(want.getKey());
            if (loop != null && now - loop.tickedAt > LOST_TICKS) {
                loop.end();
                loop = null;
            }
            if (loop == null && want.getValue()[0] > GONE) {
                loop = new Loop(want.getKey(), this.car, now);
                this.loops.put(want.getKey(), loop);
                Minecraft.getInstance().getSoundManager().queueTickingSound(loop);
            }
        }
        for (Iterator<Map.Entry<SoundEvent, Loop>> it = this.loops.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<SoundEvent, Loop> entry = it.next();
            Loop loop = entry.getValue();
            float[] want = wanted.get(entry.getKey());
            float target = want == null ? 0.0F : want[0];
            loop.gain += (target - loop.gain) * GAIN_EASE;
            if (want != null) {
                loop.targetPitch = want[1];
            }
            if (want == null && loop.gain < GONE || loop.isStopped()) {
                loop.end();
                it.remove();
            }
        }
    }

    /**
     * The throttle snapping shut. With boost in the plenum the blow-off
     * valve lets it go, between gears as well, as it does; off a pedal at
     * high revs a big engine, or a tired one, pops through the exhaust.
     */
    private void lift(float revs, float throttle, float boost) {
        if (this.popCooldown > 0) {
            this.popCooldown--;
        }
        var random = this.car.getRandom();
        float vented = EngineAcoustics.ventedBoost(this.lastBoost, boost);
        if (vented > 0.0F) {
            this.play(ModSounds.BLOWOFF, 0.3F + 0.6F * vented, 0.9F + 0.2F * vented + random.nextFloat() * 0.06F);
        }
        boolean shut = this.lastThrottle > 0.5F && throttle == 0.0F;
        this.lastThrottle = throttle;
        this.lastBoost = boost;
        if (!shut || revs < POP_FROM || this.popCooldown > 0 || this.car.isElectric()
                || this.car.gearbox().getShift() > 0.0F) {
            return;
        }
        boolean popper = this.sound.voice == EngineAcoustics.Voice.V8
                || this.car.parts().getConditionFraction(me.trossty.car.entity.CarPart.SPARK_PLUGS)
                        <= me.trossty.car.entity.FaultCodes.WARNING;
        if (popper && random.nextFloat() < 0.6F) {
            this.popCooldown = 30;
            this.play(ModSounds.BACKFIRE, 0.6F, 0.9F + random.nextFloat() * 0.25F);
        }
    }

    private void play(SoundEvent event, float volume, float pitch) {
        this.car.level().playLocalSound(this.car.getX(), this.car.getY() + 0.5, this.car.getZ(), event,
                SoundSource.NEUTRAL, volume, pitch, false);
    }

    /**
     * The road under the tyres: the block's own footstep sound, played as the
     * wheels roll over it, loud on gravel and dirt and a whisper on stone.
     */
    private void road() {
        float speed = Math.abs(this.car.getEngineSpeed());
        if (!this.car.onGround() || speed < 0.02F) {
            this.rolled = 0.0F;
            return;
        }
        this.rolled += speed;
        BlockState under = RoadSurface.under(this.car);
        float roughness = RoadSurface.roughness(under);
        // Rough ground is a stream of crunches; smooth road an occasional hiss.
        float every = Mth.lerp(roughness, 1.6F, 0.55F);
        if (this.rolled < every || under.isAir()) {
            return;
        }
        this.rolled = 0.0F;
        float speedFraction = Mth.clamp(speed / CarEntity.MAX_SPEED, 0.0F, 1.0F);
        var group = under.getSoundType();
        float volume = group.getVolume() * (0.08F + 0.42F * roughness) * (0.4F + 0.6F * speedFraction);
        float pitch = group.getPitch() * (0.75F + 0.3F * speedFraction)
                + (this.car.getRandom().nextFloat() - 0.5F) * 0.15F;
        this.car.level().playLocalSound(this.car.getX(), this.car.getY(), this.car.getZ(),
                group.getStepSound(), SoundSource.NEUTRAL, volume, pitch, false);
    }

    /**
     * What a vehicle's engine sounds like: a combustion engine's voice, from
     * its cylinders and fuel, or one loop of its own for an electric motor, a
     * propeller or an outboard, pitched and swelled between idle and redline.
     * Swap the engine and the sound swaps with it.
     */
    private record Sound(EngineAcoustics.Voice voice, SoundEvent loop,
                         float idlePitch, float maxPitch, float idleVolume, float maxVolume) {

        /** Near silent at rest; the whine climbs a long way with speed. */
        static final Sound ELECTRIC = new Sound(null, ModSounds.ENGINE_ELECTRIC, 0.55F, 2.1F, 0.25F, 0.7F);
        static final Sound PROPELLER = new Sound(null, ModSounds.ENGINE_PROPELLER, 0.9F, 2.0F, 1.2F, 1.15F);
        static final Sound OUTBOARD = new Sound(null, ModSounds.ENGINE_OUTBOARD, 0.9F, 1.9F, 1.0F, 1.0F);
        private static final Map<EngineAcoustics.Voice, Sound> COMBUSTION = new java.util.EnumMap<>(EngineAcoustics.Voice.class);

        static Sound of(CarEntity car) {
            VehicleDefinition.Chassis chassis = car.chassis();
            if (chassis == VehicleDefinition.Chassis.PLANE || chassis == VehicleDefinition.Chassis.HELICOPTER) {
                return PROPELLER;
            }
            if (chassis == VehicleDefinition.Chassis.BOAT) {
                return OUTBOARD;
            }
            if (car.isElectric()) {
                return ELECTRIC;
            }
            EngineAcoustics.Voice voice = EngineAcoustics.Voice.of(
                    car.gearbox().getEngineSpec().cylinders(), !car.getEngineFuel().needsSpark());
            return COMBUSTION.computeIfAbsent(voice, v -> new Sound(v, null, 1.0F, 1.0F, 1.0F, 1.0F));
        }
    }

    /** One loop the mix keeps playing at the car, at the loudness and pitch it is given. */
    private static final class Loop extends AbstractTickableSoundInstance {

        private final CarEntity car;
        private float gain;
        private float targetPitch = 1.0F;
        private long tickedAt;

        Loop(SoundEvent event, CarEntity car, long now) {
            super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
            this.car = car;
            this.tickedAt = now;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
            this.follow();
        }

        @Override
        public boolean canPlaySound() {
            return !this.car.isSilent();
        }

        /**
         * Required, not cosmetic. Each loop starts at volume 0 and fades in,
         * but {@code SoundSystem.play} throws away any instance whose adjusted
         * volume is 0 ("Skipped playing sound {}, volume was zero") unless
         * this returns true. A discarded instance never enters
         * {@code tickingSounds}, so it would never tick, and the engine would
         * stay silent. Vanilla's {@code EntityRidingSoundInstance} overrides
         * this for the same reason; unlike that one these keep the default
         * linear attenuation, so they are positional and bystanders hear them.
         */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (this.car.isRemoved()) {
                this.stop();
                return;
            }
            this.tickedAt = this.car.level().getGameTime();
            this.volume = Mth.clamp(this.gain, 0.0F, 1.0F);
            this.pitch = this.targetPitch;
            this.follow();
        }

        private void follow() {
            this.x = this.car.getX();
            this.y = this.car.getY() + 0.5;
            this.z = this.car.getZ();
        }

        void end() {
            this.stop();
        }
    }
}
