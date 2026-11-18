package me.trossty.car.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What is inside the engine, which the engine's one condition is the average
 * of: the rings round each cylinder's piston, each cylinder's valves, the head
 * gasket and the bearings the crank turns in. Each wears for its own reasons
 * and shows its own fault, so two engines at the same condition can be tired
 * in quite different ways:
 * <ul>
 *   <li>the rings wear with the miles, faster on dirty oil and dirty fuel, and
 *   the worst of them lets oil past to be burnt;</li>
 *   <li>the valves coke up on dirty fuel, and a snapped belt bends the ones the
 *   pistons catch, which then hold next to nothing;</li>
 *   <li>the gasket cooks when the engine boils, and once blown it lets the two
 *   cylinders either side of it into each other and the coolant in;</li>
 *   <li>the bearings live on the oil: run them short of it and they knock.</li>
 * </ul>
 * Setting the engine's condition as a whole sets every one of them to it, so
 * an engine from before any of this, or one fresh from the bench, is alike all
 * through. On the server each is kept to the decimal; a client reads the
 * packed copies the car tracks.
 */
public final class EngineInternals {

    /** The shares of the engine's wear: rings take most, valves least, and they average 1. */
    private static final float RING_SHARE = 1.4F;
    private static final float VALVE_SHARE = 0.6F;
    private static final float GASKET_SHARE = 0.8F;
    private static final float BEARING_SHARE = 1.2F;
    /** How unlike each other the cylinders wear. */
    private static final float CYLINDER_SPREAD = 0.15F;
    /** Keeps a cylinder's valves from wearing in step with its rings, and which ones a belt bends from either. */
    private static final int VALVE_SALT = PartPositions.MAX;
    private static final int BEND_SALT = 2 * PartPositions.MAX;

    private final CarEntity car;
    private final float[] rings = PartPositions.filled(CarPart.MAX_CONDITION);
    private final float[] valves = PartPositions.filled(CarPart.MAX_CONDITION);
    private float gasket = CarPart.MAX_CONDITION;
    private float bearings = CarPart.MAX_CONDITION;

    EngineInternals(CarEntity car) {
        this.car = car;
    }

    public int cylinders() {
        return Math.min(PartPositions.MAX, this.car.getLayout().cylinders());
    }

    public float ring(int cylinder) {
        return this.car.level().isClientSide()
                ? PartPositions.unpack(this.car.getEntityData().get(CarEntity.ENGINE_RINGS), cylinder)
                : this.rings[cylinder];
    }

    public float valve(int cylinder) {
        return this.car.level().isClientSide()
                ? PartPositions.unpack(this.car.getEntityData().get(CarEntity.ENGINE_VALVES), cylinder)
                : this.valves[cylinder];
    }

    public float gasket() {
        return this.car.level().isClientSide()
                ? PartPositions.unpack(this.car.getEntityData().get(CarEntity.ENGINE_HEAD), 0) : this.gasket;
    }

    public float bearings() {
        return this.car.level().isClientSide()
                ? PartPositions.unpack(this.car.getEntityData().get(CarEntity.ENGINE_HEAD), 1) : this.bearings;
    }

    /** Bent, not worn: wear stops at {@link CarPart#WORN_OUT}, and only a belt takes a valve below it. */
    public boolean isBent(int cylinder) {
        return this.valve(cylinder) < CarPart.WORN_OUT;
    }

    /** The most worn set of rings: the one that lets the oil past. */
    public float worstRing() {
        float worst = CarPart.MAX_CONDITION;
        for (int cylinder = 0; cylinder < this.cylinders(); cylinder++) {
            worst = Math.min(worst, this.ring(cylinder));
        }
        return worst;
    }

    /** The engine as a whole: rings, valves, gasket and bearings, a quarter each. */
    float condition() {
        int count = this.cylinders();
        float rings = 0.0F;
        float valves = 0.0F;
        for (int cylinder = 0; cylinder < count; cylinder++) {
            rings += this.ring(cylinder);
            valves += this.valve(cylinder);
        }
        return (rings / count + valves / count + this.gasket() + this.bearings()) / 4.0F;
    }

    /** Every one of them at the same condition, as the engine as a whole was just set to. */
    void fill(float condition) {
        java.util.Arrays.fill(this.rings, condition);
        java.util.Arrays.fill(this.valves, condition);
        this.gasket = condition;
        this.bearings = condition;
        this.sync();
    }

    /**
     * Wears each by its own amount, already scaled for the engine's grade, and
     * faster the further gone it is, the way all wear goes. The rings and
     * valves wear cylinder by cylinder, no two quite alike.
     */
    void wear(float rings, float valves, float gasket, float bearings) {
        float[] ringShares = this.shares(0);
        float[] valveShares = this.shares(VALVE_SALT);
        for (int cylinder = 0; cylinder < this.cylinders(); cylinder++) {
            this.rings[cylinder] = this.worn(this.rings[cylinder], rings * RING_SHARE * ringShares[cylinder]);
            this.valves[cylinder] = this.worn(this.valves[cylinder], valves * VALVE_SHARE * valveShares[cylinder]);
        }
        this.gasket = this.worn(this.gasket, gasket * GASKET_SHARE);
        this.bearings = this.worn(this.bearings, bearings * BEARING_SHARE);
        this.settle();
    }

    /** One of them worn by this much; a bent valve stays bent rather than being worn up to the floor. */
    private float worn(float condition, float amount) {
        return Math.min(condition, Math.max(CarPart.WORN_OUT,
                condition - amount * this.car.parts().ageFactor(condition / CarPart.MAX_CONDITION)));
    }

    /** Each cylinder's share of the wear, averaging 1, the same for the same car. */
    private float[] shares(int salt) {
        int count = this.cylinders();
        float[] shares = new float[count];
        float total = 0.0F;
        for (int cylinder = 0; cylinder < count; cylinder++) {
            shares[cylinder] = 1.0F + CYLINDER_SPREAD * PartPositions.bias(this.car.getUUID(), CarPart.ENGINE, cylinder + salt);
            total += shares[cylinder];
        }
        for (int cylinder = 0; cylinder < count; cylinder++) {
            shares[cylinder] *= count / total;
        }
        return shares;
    }

    /**
     * A snapped belt: the camshaft stops and the pistons run on into the valves
     * it left open. Always the first cylinder's and about half the rest, the
     * same ones on the same car. A new belt does not straighten them; only new
     * valves on the stand do.
     */
    void bendValves() {
        for (int cylinder = 0; cylinder < this.cylinders(); cylinder++) {
            if (cylinder == 0 || PartPositions.bias(this.car.getUUID(), CarPart.ENGINE, cylinder + BEND_SALT) < 0.0F) {
                this.valves[cylinder] = 0.0F;
            }
        }
        this.settle();
    }

    /** What is inside now, to go with the engine when it comes out. */
    public Snapshot snapshot() {
        int count = this.cylinders();
        List<Integer> rings = new ArrayList<>(count);
        List<Integer> valves = new ArrayList<>(count);
        for (int cylinder = 0; cylinder < count; cylinder++) {
            rings.add(Snapshot.percent(this.ring(cylinder)));
            valves.add(Snapshot.percent(this.valve(cylinder)));
        }
        return new Snapshot(rings, valves, Snapshot.percent(this.gasket()), Snapshot.percent(this.bearings()));
    }

    /** These insides in, as the engine they came in goes in. */
    public void set(Snapshot inside) {
        Snapshot fits = inside.resized(this.cylinders());
        for (int cylinder = 0; cylinder < this.cylinders(); cylinder++) {
            this.rings[cylinder] = fits.rings().get(cylinder);
            this.valves[cylinder] = fits.valves().get(cylinder);
        }
        this.gasket = fits.gasket();
        this.bearings = fits.bearings();
        this.settle();
    }

    /** The packed copies a client reads, and the engine's condition as a whole while it is in. */
    private void settle() {
        this.sync();
        if (this.car.parts().isFitted(CarPart.ENGINE)) {
            this.car.getEntityData().set(CarEntity.PART_CONDITION.get(CarPart.ENGINE.ordinal()),
                    Math.max(CarPart.WORN_OUT, this.condition()));
        }
    }

    private void sync() {
        this.car.getEntityData().set(CarEntity.ENGINE_RINGS, PartPositions.pack(this.rings, PartPositions.MAX));
        this.car.getEntityData().set(CarEntity.ENGINE_VALVES, PartPositions.pack(this.valves, PartPositions.MAX));
        this.car.getEntityData().set(CarEntity.ENGINE_HEAD, PartPositions.pack(new float[] {this.gasket, this.bearings}, 2));
    }

    /** The packed gasket and bearings of a new engine. */
    static long newHead() {
        return PartPositions.pack(new float[] {CarPart.MAX_CONDITION, CarPart.MAX_CONDITION}, 2);
    }

    void save(ValueOutput view) {
        view.tag().putIntArray("EngineRings", bits(this.rings));
        view.tag().putIntArray("EngineValves", bits(this.valves));
        view.putFloat("EngineGasket", this.gasket);
        view.putFloat("EngineBearings", this.bearings);
    }

    /**
     * Reads back what {@link #save} wrote, after the engine's condition as a
     * whole: a car saved before the engine had insides keeps them alike, at
     * that condition. The condition itself stays as saved; the layout that
     * says how many cylinders count may not be read yet.
     */
    void load(ValueInput view) {
        int[] rings = view.tag().getIntArray("EngineRings");
        int[] valves = view.tag().getIntArray("EngineValves");
        if (rings.length == 0 || valves.length == 0) {
            return;
        }
        for (int cylinder = 0; cylinder < Math.min(PartPositions.MAX, Math.min(rings.length, valves.length)); cylinder++) {
            this.rings[cylinder] = Mth.clamp(Float.intBitsToFloat(rings[cylinder]), 0.0F, CarPart.MAX_CONDITION);
            this.valves[cylinder] = Mth.clamp(Float.intBitsToFloat(valves[cylinder]), 0.0F, CarPart.MAX_CONDITION);
        }
        this.gasket = Mth.clamp(view.getFloatOr("EngineGasket", this.gasket), 0.0F, CarPart.MAX_CONDITION);
        this.bearings = Mth.clamp(view.getFloatOr("EngineBearings", this.bearings), 0.0F, CarPart.MAX_CONDITION);
        this.sync();
    }

    private static int[] bits(float[] conditions) {
        int[] bits = new int[conditions.length];
        for (int i = 0; i < conditions.length; i++) {
            bits[i] = Float.floatToIntBits(conditions[i]);
        }
        return bits;
    }

    /**
     * What is inside an engine off the car, in whole per cents: its rings and
     * valves cylinder by cylinder, its gasket and its bearings. An engine that
     * does not carry one is alike all through, at its own condition.
     */
    public record Snapshot(List<Integer> rings, List<Integer> valves, int gasket, int bearings) {

        private static final Codec<List<Integer>> PERCENTS = Codec.intRange(0, 100).listOf();
        private static final StreamCodec<ByteBuf, List<Integer>> PERCENTS_STREAM =
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list());

        public static final Codec<Snapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PERCENTS.fieldOf("rings").forGetter(Snapshot::rings),
                PERCENTS.fieldOf("valves").forGetter(Snapshot::valves),
                Codec.intRange(0, 100).fieldOf("gasket").forGetter(Snapshot::gasket),
                Codec.intRange(0, 100).fieldOf("bearings").forGetter(Snapshot::bearings)
        ).apply(instance, Snapshot::new));

        public static final StreamCodec<ByteBuf, Snapshot> STREAM_CODEC = StreamCodec.composite(
                PERCENTS_STREAM, Snapshot::rings,
                PERCENTS_STREAM, Snapshot::valves,
                ByteBufCodecs.VAR_INT, Snapshot::gasket,
                ByteBufCodecs.VAR_INT, Snapshot::bearings,
                Snapshot::new);

        public Snapshot {
            rings = List.copyOf(rings);
            valves = List.copyOf(valves);
        }

        /** A condition as a whole per cent; anything left of a part that is not bent stays at least 1. */
        static int percent(float condition) {
            return condition <= 0.0F ? 0 : Math.max(1, Math.round(condition));
        }

        public static Snapshot alike(int cylinders, int condition) {
            List<Integer> each = Collections.nCopies(cylinders, condition);
            return new Snapshot(each, each, condition, condition);
        }

        /** What is in this engine: what it carries, or alike all through at its condition. */
        public static Snapshot of(ItemStack engine) {
            int cylinders = CarSetup.layoutOf(CarEntity.variantOf(engine)).cylinders();
            Snapshot kept = engine.get(ModComponents.ENGINE_INTERNALS);
            return kept != null ? kept.resized(cylinders)
                    : alike(cylinders, Math.round(Workshop.conditionOf(engine)));
        }

        /** The engine as a whole, worked out the way the car does it. */
        public float condition() {
            return (mean(this.rings) + mean(this.valves) + this.gasket + this.bearings) / 4.0F;
        }

        public boolean isAlike() {
            int first = this.gasket;
            return this.bearings == first && this.rings.stream().allMatch(c -> c == first)
                    && this.valves.stream().allMatch(c -> c == first);
        }

        /** Puts this on an engine off the car; an engine alike all through needs nothing on it. */
        public void stamp(ItemStack engine) {
            if (this.isAlike()) {
                engine.remove(ModComponents.ENGINE_INTERNALS);
            } else {
                engine.set(ModComponents.ENGINE_INTERNALS, this);
            }
        }

        /** The same insides for this many cylinders: a set that does not fit goes in at its average. */
        public Snapshot resized(int cylinders) {
            return new Snapshot(resize(this.rings, cylinders), resize(this.valves, cylinders), this.gasket, this.bearings);
        }

        /** One set, cylinder by cylinder, for this many cylinders: as it is, or its average all through. */
        public static List<Integer> resize(List<Integer> set, int cylinders) {
            return set.size() == cylinders ? set : Collections.nCopies(cylinders, Math.round(mean(set)));
        }

        public static float mean(List<Integer> set) {
            return set.isEmpty() ? 0.0F : (float) set.stream().mapToInt(Integer::intValue).sum() / set.size();
        }
    }
}
