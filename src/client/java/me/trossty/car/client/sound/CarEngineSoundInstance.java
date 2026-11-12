package me.trossty.car.client.sound;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.sound.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;

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
public class CarEngineSoundInstance extends MovingSoundInstance {

    private static final float IDLE_PITCH = 1.0F;
    private static final float MAX_PITCH = 2.0F;
    private static final float IDLE_VOLUME = 0.28F;
    private static final float MAX_VOLUME = 0.95F;
    /** Per-tick smoothing so the engine spools up instead of snapping. */
    private static final float SMOOTHING = 0.25F;

    /** How much higher a two-wheeler's engine sings than a car's. */
    private static final float MOTORCYCLE_PITCH = 1.45F;

    private final CarEntity car;
    private final Voice voice;
    private float throttle;
    /** Distance rolled since the last crunch from the road. */
    private float rolled;

    public CarEngineSoundInstance(CarEntity car) {
        this(car, Voice.of(car));
    }

    private CarEngineSoundInstance(CarEntity car, Voice voice) {
        super(voice.sound, SoundCategory.NEUTRAL, SoundInstance.createRandom());
        this.car = car;
        this.voice = voice;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.0F;
        this.pitch = IDLE_PITCH;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canPlay() {
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
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public void tick() {
        if (this.car.isRemoved()) {
            this.setDone();
            return;
        }

        Voice now = Voice.of(this.car);
        if (now != this.voice) {
            // A different engine went in: hand over to a loop of the new one.
            this.setDone();
            net.minecraft.client.MinecraftClient.getInstance().getSoundManager()
                    .playNextTick(new CarEngineSoundInstance(this.car, now));
            return;
        }

        this.x = this.car.getX();
        this.y = this.car.getY() + 0.5;
        this.z = this.car.getZ();

        boolean running = this.car.getControllingPassenger() != null && this.car.hasFuel();
        // The note follows the revs, so it climbs through each gear and drops
        // at the change, and a car with a higher top speed simply revs on.
        float revs = running
                ? MathHelper.clamp(this.car.getEngineRpm() / this.car.getRedlineRpm(), 0.0F, 1.0F)
                : 0.0F;

        this.throttle = MathHelper.lerp(0.45F, this.throttle, revs);
        this.road();
        this.pitch = MathHelper.lerp(this.throttle, IDLE_PITCH * this.voice.idlePitch, MAX_PITCH * this.voice.maxPitch);
        if (this.car.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            // A small engine spinning fast: the same note, an octave-ish up.
            this.pitch *= MOTORCYCLE_PITCH;
        }
        this.volume = running
                ? MathHelper.lerp(this.throttle, IDLE_VOLUME * this.voice.idleVolume, MAX_VOLUME * this.voice.maxVolume)
                : 0.0F;
    }

    /** Which loop a car's engine plays, and how far it is pitched and how loud. */
    enum Voice {
        FOUR(ModSounds.CAR_ENGINE, 1.0F, 1.0F, 1.0F, 1.0F),
        SIX(ModSounds.CAR_ENGINE, 0.88F, 0.95F, 1.0F, 1.0F),
        V8(ModSounds.ENGINE_V8, 1.0F, 1.05F, 1.15F, 1.05F),
        DIESEL(ModSounds.ENGINE_DIESEL, 1.0F, 0.85F, 1.1F, 1.0F),
        /** Near silent at rest; the whine climbs a long way with speed. */
        ELECTRIC(ModSounds.ENGINE_ELECTRIC, 0.55F, 1.05F, 0.25F, 0.7F);

        final net.minecraft.sound.SoundEvent sound;
        final float idlePitch;
        final float maxPitch;
        final float idleVolume;
        final float maxVolume;

        Voice(net.minecraft.sound.SoundEvent sound, float idlePitch, float maxPitch, float idleVolume, float maxVolume) {
            this.sound = sound;
            this.idlePitch = idlePitch;
            this.maxPitch = maxPitch;
            this.idleVolume = idleVolume;
            this.maxVolume = maxVolume;
        }

        static Voice of(CarEntity car) {
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
        if (!this.car.isOnGround() || speed < 0.02F) {
            this.rolled = 0.0F;
            return;
        }
        this.rolled += speed;
        BlockState under = RoadSurface.under(this.car);
        float roughness = RoadSurface.roughness(under);
        // Rough ground is a stream of crunches; smooth road an occasional hiss.
        float every = MathHelper.lerp(roughness, 1.6F, 0.55F);
        if (this.rolled < every || under.isAir()) {
            return;
        }
        this.rolled = 0.0F;
        float speedFraction = MathHelper.clamp(speed / CarEntity.MAX_SPEED, 0.0F, 1.0F);
        var group = under.getSoundGroup();
        float volume = group.getVolume() * (0.08F + 0.42F * roughness) * (0.4F + 0.6F * speedFraction);
        float pitch = group.getPitch() * (0.75F + 0.3F * speedFraction)
                + (this.car.getRandom().nextFloat() - 0.5F) * 0.15F;
        this.car.getEntityWorld().playSoundClient(this.car.getX(), this.car.getY(), this.car.getZ(),
                group.getStepSound(), SoundCategory.NEUTRAL, volume, pitch, false);
    }
}
