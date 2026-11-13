package me.trossty.car.client.sound;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A looping engine drone that follows one car, so everybody nearby hears it and
 * not just the driver. Pitch and volume both rise with speed; an occupied but
 * stationary car idles quietly, an empty one is silent.
 *
 * <p>What it sounds like follows what is in the bay: a V8 burbles, a diesel or
 * biofuel engine clatters, an electric motor whines and all but goes quiet
 * at a standstill, and the fours and sixes share the plain drone, the sixes a
 * little lower. Swap the engine and the sound swaps with it.
 */
public class CarEngineSoundInstance extends AbstractTickableSoundInstance {

    private static final float IDLE_PITCH = 1.0F;
    private static final float MAX_PITCH = 2.0F;
    private static final float IDLE_VOLUME = 0.28F;
    private static final float MAX_VOLUME = 0.95F;
    /** Per-tick smoothing so the engine spools up instead of snapping. */
    private static final float SMOOTHING = 0.25F;
    /** How much quieter the engine goes at the start of a gear change. */
    private static final float SHIFT_DIP = 0.35F;

    /** How much higher a two-wheeler's engine sings than a car's. */
    private static final float MOTORCYCLE_PITCH = 1.45F;

    /** Ticks a parked, empty car keeps its channel before letting it go. */
    private static final int QUIET_TICKS = 20;
    /** A loop the sound engine has not ticked for this long was never started (the pool was full). */
    private static final int LOST_TICKS = 40;
    /** The loop each car has playing, so there is only ever one. */
    private static final java.util.Map<CarEntity, CarEngineSoundInstance> PLAYING = new java.util.WeakHashMap<>();
    /** The cars whose engines were running last tick, to hear them start. */
    private static final java.util.Set<CarEntity> RUNNING =
            java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    /** When each car's engine last started: the loop waits while the starter turns it over. */
    private static final java.util.Map<CarEntity, Long> STARTED = new java.util.WeakHashMap<>();
    /** How long the starter cranks before the engine catches, in ticks. */
    private static final int CRANK_TICKS = 15;
    /** A drop in revs this big in one tick is the throttle snapping shut. */
    private static final float LIFT_DROP = 0.12F;
    /** Revs, as a share of the redline, the engine has to be above for a lift to pop or blow off. */
    private static final float LIFT_FROM = 0.6F;

    private final CarEntity car;
    private final Voice voice;
    private int quiet;
    private long tickedAt;
    private float throttle;
    /** Last tick's revs, to hear the throttle shut. */
    private float lastRevs;
    private int popCooldown;
    /** Distance rolled since the last crunch from the road. */
    private float rolled;

    /**
     * A channel only for a car that makes a noise: running, or rolling on the
     * road. The sound engine has a few hundred channels in all, and a car park
     * of silent loops would take them from everything else in the world.
     * Called every client tick.
     */
    public static void listen(net.minecraft.client.multiplayer.ClientLevel level) {
        long now = level.getGameTime();
        PLAYING.values().removeIf(sound -> sound.isStopped());
        for (net.minecraft.world.entity.Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof CarEntity car) || car instanceof me.trossty.car.entity.TrailerEntity) {
                continue;
            }
            boolean running = running(car);
            if (running && RUNNING.add(car) && !car.isElectric()) {
                // Somebody got in and turned the key: crank, catch.
                STARTED.put(car, now);
                level.playLocalSound(car.getX(), car.getY() + 0.5, car.getZ(), ModSounds.ENGINE_START,
                        SoundSource.NEUTRAL, 0.9F, Voice.of(car) == Voice.DIESEL ? 0.85F : 1.0F, false);
            } else if (!running) {
                RUNNING.remove(car);
            }
            if (!audible(car)) {
                continue;
            }
            CarEngineSoundInstance playing = PLAYING.get(car);
            if (playing == null || now - playing.tickedAt > LOST_TICKS) {
                start(car, Voice.of(car), now);
            }
        }
    }

    private static void start(CarEntity car, Voice voice, long now) {
        CarEngineSoundInstance sound = new CarEngineSoundInstance(car, voice);
        sound.tickedAt = now;
        CarEngineSoundInstance old = PLAYING.put(car, sound);
        if (old != null) {
            old.stop();
        }
        net.minecraft.client.Minecraft.getInstance().getSoundManager().queueTickingSound(sound);
    }

    private static boolean running(CarEntity car) {
        return car.getControllingPassenger() != null && car.hasFuel();
    }

    private static boolean audible(CarEntity car) {
        return running(car) || car.onGround() && Math.abs(car.getEngineSpeed()) >= 0.02F;
    }

    private CarEngineSoundInstance(CarEntity car, Voice voice) {
        super(voice.sound, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.voice = voice;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.pitch = IDLE_PITCH;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !this.car.isSilent();
    }

    /**
     * Required, not cosmetic. We start at volume 0 because an empty car has to be
     * silent, but {@code SoundSystem.play} throws away any instance whose adjusted
     * volume is 0 ("Skipped playing sound {}, volume was zero") unless this returns
     * true. A discarded instance never enters {@code tickingSounds}, so it would
     * never tick, and the engine would stay silent even once somebody got in.
     *
     * <p>Vanilla's {@code EntityRidingSoundInstance} overrides this for the same
     * reason. Unlike that one we keep the default LINEAR attenuation, so the sound
     * is positional and bystanders hear it; vanilla's minecart drone uses
     * {@code AttenuationType.NONE} because it belongs to the rider alone.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.car.isRemoved() || PLAYING.get(this.car) != this) {
            this.stop();
            return;
        }
        this.tickedAt = this.car.level().getGameTime();
        // Parked and empty for a while: give the channel back.
        this.quiet = audible(this.car) ? 0 : this.quiet + 1;
        if (this.quiet > QUIET_TICKS) {
            this.stop();
            PLAYING.remove(this.car);
            return;
        }

        Voice now = Voice.of(this.car);
        if (now != this.voice) {
            // A different engine went in: hand over to a loop of the new one.
            start(this.car, now, this.tickedAt);
            return;
        }

        this.x = this.car.getX();
        this.y = this.car.getY() + 0.5;
        this.z = this.car.getZ();

        boolean running = running(this.car);
        // The note follows the revs, so it climbs through each gear and drops
        // at the change, and a car with a higher top speed simply revs on.
        float revs = running
                ? Mth.clamp(this.car.getEngineRpm() / this.car.getRedlineRpm(), 0.0F, 1.0F)
                : 0.0F;

        this.lift(revs);
        this.throttle = Mth.lerp(0.45F, this.throttle, revs);
        this.road();
        this.pitch = Mth.lerp(this.throttle, IDLE_PITCH * this.voice.idlePitch, MAX_PITCH * this.voice.maxPitch);
        if (this.car.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            // A small engine spinning fast: the same note, an octave-ish up.
            this.pitch *= MOTORCYCLE_PITCH;
        }
        this.volume = running
                ? Mth.lerp(this.throttle, IDLE_VOLUME * this.voice.idleVolume, MAX_VOLUME * this.voice.maxVolume)
                : 0.0F;
        // The box lifts off the drive for a gear change: the note drops and
        // eases for a moment before it pulls again.
        this.volume *= 1.0F - SHIFT_DIP * this.car.getShift();
        // Still on the starter: the loop comes in as the engine catches.
        Long started = STARTED.get(this.car);
        if (started != null) {
            long since = this.tickedAt - started;
            this.volume *= Mth.clamp((since - CRANK_TICKS + 4) / 4.0F, 0.0F, 1.0F);
        }
    }

    /**
     * The throttle snapping shut from high revs: a turbo blows its boost off,
     * and a big engine, or a tired one, pops through the exhaust.
     */
    private void lift(float revs) {
        if (this.popCooldown > 0) {
            this.popCooldown--;
        }
        boolean lifted = this.lastRevs > LIFT_FROM && this.lastRevs - revs > LIFT_DROP;
        this.lastRevs = revs;
        if (!lifted || this.popCooldown > 0 || this.car.isElectric()) {
            return;
        }
        this.popCooldown = 30;
        var random = this.car.getRandom();
        if (this.car.hasTurbo()) {
            this.car.level().playLocalSound(this.x, this.y, this.z, ModSounds.BLOWOFF, SoundSource.NEUTRAL,
                    0.7F, 0.95F + random.nextFloat() * 0.1F, false);
        }
        boolean popper = this.voice == Voice.V8
                || this.car.getConditionFraction(me.trossty.car.entity.CarPart.SPARK_PLUGS)
                        <= me.trossty.car.entity.FaultCodes.WARNING;
        if (popper && this.car.getShift() == 0.0F && random.nextFloat() < 0.6F) {
            this.car.level().playLocalSound(this.x, this.y, this.z, ModSounds.BACKFIRE, SoundSource.NEUTRAL,
                    0.6F, 0.9F + random.nextFloat() * 0.25F, false);
        }
    }

    /** Which loop a car's engine plays, and how far it is pitched and how loud. */
    enum Voice {
        FOUR(ModSounds.CAR_ENGINE, 1.0F, 1.0F, 1.0F, 1.0F),
        SIX(ModSounds.CAR_ENGINE, 0.88F, 0.95F, 1.0F, 1.0F),
        V8(ModSounds.ENGINE_V8, 1.0F, 1.05F, 1.15F, 1.05F),
        DIESEL(ModSounds.ENGINE_DIESEL, 1.0F, 0.85F, 1.1F, 1.0F),
        PROPELLER(ModSounds.ENGINE_PROPELLER, 0.9F, 1.0F, 1.2F, 1.15F),
        OUTBOARD(ModSounds.ENGINE_OUTBOARD, 0.9F, 0.95F, 1.0F, 1.0F),
        /** Near silent at rest; the whine climbs a long way with speed. */
        ELECTRIC(ModSounds.ENGINE_ELECTRIC, 0.55F, 1.05F, 0.25F, 0.7F);

        final net.minecraft.sounds.SoundEvent sound;
        final float idlePitch;
        final float maxPitch;
        final float idleVolume;
        final float maxVolume;

        Voice(net.minecraft.sounds.SoundEvent sound, float idlePitch, float maxPitch, float idleVolume, float maxVolume) {
            this.sound = sound;
            this.idlePitch = idlePitch;
            this.maxPitch = maxPitch;
            this.idleVolume = idleVolume;
            this.maxVolume = maxVolume;
        }

        static Voice of(CarEntity car) {
            if (car.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE) {
                return PROPELLER;
            }
            if (car.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT) {
                return OUTBOARD;
            }
            if (car.isElectric()) {
                return ELECTRIC;
            }
            if (!car.getEngineFuel().needsSpark()) {
                return DIESEL;
            }
            return switch (car.getLayout()) {
                case V8 -> V8;
                case INLINE6, V6 -> SIX;
                default -> FOUR;
            };
        }
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
}
