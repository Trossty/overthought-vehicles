package me.trossty.car.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;

/**
 * What a car needs looking after besides its parts: oil that burns and goes
 * black, coolant that boils off, brake fluid, a timing belt with a life, a
 * catalyst that clogs, tyres that lose their air, wheels knocked out of true
 * and the dirt of the road; and what topping each one up or putting it right
 * takes.
 */
public final class Upkeep {

    private final CarEntity car;

    Upkeep(CarEntity car) {
        this.car = car;
    }

    /** A sump's worth of oil burns away in about two hours of driving flat out. */
    private static final float OIL_BURN = 1.0F / 144000.0F;

    /** Coolant boils off slower, and faster the hotter the engine runs. */
    private static final float COOLANT_LOSS = 1.0F / 216000.0F;

    /** How many times faster than that a water pump with its seal gone weeps it away: a few minutes from full to low. */
    private static final float PUMP_WEEP_LOSS = 30.0F;

    /** Below this a level is low: the dash says so. */
    public static final float FLUID_LOW = 0.2F;

    /** Below this much oil the bearings start to run dry. */
    private static final float OIL_STARVED = 0.35F;

    /** How many times faster a dry engine wears than one with its oil. */
    private static final float DRY_ENGINE_WEAR = 6.0F;

    /** Below this much coolant the radiator has less and less to cool with. */
    private static final float COOLANT_SHORT = 0.4F;

    /** What a bottle of motor oil puts in the sump. */
    public static final float OIL_PER_BOTTLE = 0.5F;

    /** What a bottle of water puts in the coolant; a bucket fills it. */
    public static final float WATER_PER_BOTTLE = 0.34F;

    /** Whether the engine runs on oil at all: a motor has none. */
    public boolean hasOil() {
        return this.car.hasPart(CarPart.OIL_FILTER);
    }

    public float getOilLevel() {
        return this.car.getEntityData().get(CarEntity.OIL_LEVEL);
    }

    public void setOilLevel(float level) {
        this.car.getEntityData().set(CarEntity.OIL_LEVEL, Mth.clamp(level, 0.0F, 1.0F));
    }

    public float getCoolantLevel() {
        return this.car.getEntityData().get(CarEntity.COOLANT_LEVEL);
    }

    public void setCoolantLevel(float level) {
        this.car.getEntityData().set(CarEntity.COOLANT_LEVEL, Mth.clamp(level, 0.0F, 1.0F));
    }

    /** A timing belt lasts about ten hours of running before it lets go. */
    private static final float BELT_AGEING = 1.0F / 720000.0F;

    /** Past this much of its life a belt is due, and the scanner says so. */
    public static final float BELT_DUE = 0.8F;

    /** A clean-running engine takes about fifteen hours to choke its converter. */
    private static final float CAT_CLOGGING = 1.0F / 1080000.0F;

    /** Past this a converter holds the exhaust back and the engine feels it. */
    public static final float CAT_CHOKED = 0.5F;

    /** What a fully blocked converter leaves of the engine's power. */
    private static final float BLOCKED_CAT_POWER = 0.55F;

    /** Tyres lose about a quarter of their air in five hours of driving. */
    private static final float PRESSURE_LOSS = 1.0F / 1200000.0F;

    /** Below this the tyres are soft: they squirm, wear and drink. */
    public static final float SOFT_TYRES = 0.75F;

    /** What one stroke of the pump puts back. */
    public static final float PUMP_STROKE = 0.25F;

    /** A sump of fresh oil is black sludge after about five hours of driving. */
    private static final float OIL_AGEING = 1.0F / 360000.0F;

    /** How many times faster than normal a burning or leaking engine loses its oil. */
    private static final float OIL_LOSS_FAULT = 6.0F;

    /** How many times faster than normal a blown head gasket loses its coolant. */
    private static final float GASKET_COOLANT_LOSS = 8.0F;

    /** How much worse black oil is for the engine than fresh, at its blackest. */
    private static final float SLUDGE_WEAR = 2.0F;

    /** What a hard stop costs the brake fluid: a reservoir lasts many pad sets. */
    static final float BRAKE_FLUID_USE = 0.00035F;

    /** Below this much brake fluid the pedal goes soft. */
    private static final float BRAKE_FLUID_SHORT = 0.4F;

    /** What a bottle of brake fluid puts in the reservoir. */
    public static final float BRAKE_FLUID_PER_BOTTLE = 0.5F;

    /** How much a knock of one point of damage puts the wheels out of true. */
    static final float ALIGNMENT_PER_DAMAGE = 0.06F;

    /**
     * A running engine burns a little oil and boils off a little coolant, and
     * the oil it keeps goes blacker. A worn engine burns oil faster, a leak
     * drips it out, a blown head gasket boils the coolant away.
     */
    void tickFluids() {
        if (this.hasOil() && this.isOilLeaking()) {
            // A leak drips whether or not the engine runs.
            this.setOilLevel(this.getOilLevel() - OIL_BURN * OIL_LOSS_FAULT);
        }
        float load = Math.min(1.0F, Math.abs(this.car.getEngineSpeed()) / CarEntity.MAX_SPEED);
        if (load > 0.0F && this.car.hasPart(CarPart.WHEELS) && this.car.parts().isFitted(CarPart.WHEELS) && !this.car.isFlying()) {
            // Air seeps out of a rolling tyre, faster through a perished one.
            this.setTyrePressure(this.getTyrePressure() - PRESSURE_LOSS * (1.0F + load)
                    * (this.car.parts().getConditionFraction(CarPart.WHEELS) <= FaultCodes.CRITICAL ? 4.0F : 1.0F));
        }
        if (!this.car.isEngineRunning()) {
            return;
        }
        float running = CarEntity.IDLE_WEAR_SHARE + (1.0F - CarEntity.IDLE_WEAR_SHARE) * load;
        if (this.hasOil()) {
            this.setBeltWear(this.getBeltWear() + BELT_AGEING * running);
            if (this.getBeltWear() >= 1.0F) {
                this.snapBelt();
                return;
            }
        }
        if (this.hasCat()) {
            // Unburnt fuel and burnt oil both end up in the honeycomb.
            this.setCatClog(this.getCatClog() + CAT_CLOGGING * running
                    * (this.isRunningRich() ? 6.0F : 1.0F) * (this.isBurningOil() ? 10.0F : 1.0F));
        }
        if (this.hasOil()) {
            this.setOilLevel(this.getOilLevel() - OIL_BURN * running * (this.isBurningOil() ? OIL_LOSS_FAULT : 1.0F));
            if (this.getOilLevel() > 0.0F) {
                this.setOilAge(this.getOilAge() + OIL_AGEING * running
                        * this.car.parts().partFactor(CarPart.OIL_FILTER, 2.0F, 4.0F));
            }
        }
        if (this.car.hasPart(CarPart.RADIATOR)) {
            this.setCoolantLevel(this.getCoolantLevel()
                    - COOLANT_LOSS * running * (1.0F + 2.0F * this.car.getEngineTemp())
                    * (this.isGasketBlown() ? GASKET_COOLANT_LOSS : 1.0F)
                    // A tired water pump weeps out of its seal while it is under pressure.
                    * (1.0F + PUMP_WEEP_LOSS * this.car.parts().pumpWeep()));
        }
    }

    /** The belt lets go: the engine stops dead and the valves it caught open are bent. */
    public void snapBelt() {
        this.setBeltWear(1.0F);
        if (this.car.parts().isFitted(CarPart.ENGINE)) {
            this.car.parts().engine().bendValves();
        }
        this.car.flyDist *= 0.5F;
        this.car.playSound(SoundEvents.ITEM_BREAK, 1.2F, 0.6F);
        this.car.playSound(SoundEvents.ANVIL_LAND, 0.5F, 1.4F);
    }

    /** A burning engine with an exhaust has a converter in it; a motor has neither. */
    public boolean hasCat() {
        return this.hasOil() && this.car.hasPart(CarPart.EXHAUST);
    }

    public boolean isBeltSnapped() {
        return this.hasOil() && this.getBeltWear() >= 1.0F;
    }

    public float getBeltWear() {
        return this.car.getEntityData().get(CarEntity.BELT_WEAR);
    }

    public void setBeltWear(float wear) {
        this.car.getEntityData().set(CarEntity.BELT_WEAR, Mth.clamp(wear, 0.0F, 1.0F));
    }

    public float getCatClog() {
        return this.car.getEntityData().get(CarEntity.CAT_CLOG);
    }

    public void setCatClog(float clog) {
        this.car.getEntityData().set(CarEntity.CAT_CLOG, Mth.clamp(clog, 0.0F, 1.0F));
    }

    public float getTyrePressure() {
        return this.car.getEntityData().get(CarEntity.TYRE_PRESSURE);
    }

    public void setTyrePressure(float pressure) {
        this.car.getEntityData().set(CarEntity.TYRE_PRESSURE, Mth.clamp(pressure, 0.0F, 1.0F));
    }

    public float getDirt() {
        return this.car.getEntityData().get(CarEntity.DIRT);
    }

    public void setDirt(float dirt) {
        this.car.getEntityData().set(CarEntity.DIRT, Mth.clamp(dirt, 0.0F, 1.0F));
    }

    /** Off the road this many blocks to go from clean to caked. */
    private static final float DIRT_PER_BLOCK = 1.0F / 600.0F;

    /** Rain on it rinses it clean in about four minutes; driving through water in seconds. */
    private static final float RAIN_RINSE = 1.0F / 4800.0F;

    private static final float WATER_RINSE = 1.0F / 60.0F;

    /** How much one wet sponge wipes off. */
    private static final float SPONGE_WIPE = 0.5F;

    /**
     * Picks up mud off soft ground the way it picks up miles, and loses it to
     * the rain and to water. Server side, where it is kept.
     */
    void weather(float travelled) {
        float dirt = this.getDirt();
        if (this.car.isInWater()) {
            dirt -= WATER_RINSE;
        } else if (this.car.level().isRainingAt(this.car.blockPosition().above())) {
            dirt -= RAIN_RINSE;
        }
        if (this.car.onGround() && Math.abs(travelled) > 0.0F
                && CarEntity.isSoftGround(this.car.level().getBlockState(this.car.getBlockPosBelowThatAffectsMyMovement()))) {
            dirt += Math.abs(travelled) * DIRT_PER_BLOCK;
        }
        if (dirt != this.getDirt()) {
            this.setDirt(dirt);
        }
    }

    /** A wet sponge wiped over the bodywork: half the dirt off, and the sponge wrung dry. */
    InteractionResult wash(Player player, ItemStack held) {
        if (!held.is(Items.WET_SPONGE) || this.getDirt() <= 0.0F) {
            return InteractionResult.PASS;
        }
        if (this.car.level() instanceof ServerLevel world) {
            this.setDirt(this.getDirt() - SPONGE_WIPE);
            world.sendParticles(ParticleTypes.SPLASH, this.car.getX(), this.car.getY() + 1.0, this.car.getZ(),
                    24, this.car.getBbWidth() * 0.4, 0.4, this.car.getBbWidth() * 0.4, 0.1);
            world.sendParticles(ParticleTypes.BUBBLE_POP, this.car.getX(), this.car.getY() + 1.0, this.car.getZ(),
                    10, this.car.getBbWidth() * 0.4, 0.3, this.car.getBbWidth() * 0.4, 0.02);
            world.playSound(null, this.car.getX(), this.car.getY() + 0.5, this.car.getZ(),
                    SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.6F, 1.3F);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                if (!player.getInventory().add(new ItemStack(Items.SPONGE))) {
                    player.drop(new ItemStack(Items.SPONGE), false);
                }
            }
        }
        return InteractionResult.sidedSuccess(this.car.level().isClientSide());
    }

    /** How soft the tyres are, 0 at the right pressure or above SOFT_TYRES, 1 on the rims. */
    public float tyreSoftness() {
        if (!this.car.hasPart(CarPart.WHEELS)) {
            return 0.0F;
        }
        return Math.max(0.0F, (SOFT_TYRES - this.getTyrePressure()) / SOFT_TYRES);
    }

    float softTyreGrip() {
        return 1.0F - 0.3F * this.tyreSoftness();
    }

    /** What a choked converter leaves of the engine's breath. */
    float catFactor() {
        if (!this.hasCat() || this.getCatClog() <= CAT_CHOKED) {
            return 1.0F;
        }
        return Mth.lerp((this.getCatClog() - CAT_CHOKED) / (1.0F - CAT_CHOKED), 1.0F, BLOCKED_CAT_POWER);
    }

    /**
     * How much louder than it should be the engine sounds. The silencer is what
     * keeps it down: a tired one lets more of the bang out the more it rots, and
     * with none, or no pipe to hang it on, it is a racket. A holed pipe only
     * blows a little.
     */
    public float exhaustNoise() {
        if (!this.car.hasPart(CarPart.EXHAUST) || this.car.isElectric()) {
            return 1.0F;
        }
        if (!this.car.parts().isFitted(CarPart.EXHAUST)
                || this.car.hasPart(CarPart.MUFFLER) && !this.car.parts().isFitted(CarPart.MUFFLER)) {
            return 1.8F;
        }
        float noise = 1.0F + 0.2F * rot(this.car.parts().getConditionFraction(CarPart.EXHAUST));
        if (this.car.hasPart(CarPart.MUFFLER)) {
            noise += 0.7F * rot(this.car.parts().getConditionFraction(CarPart.MUFFLER));
        }
        return noise;
    }

    /** 0 for a part still above half, rising to 1 as it rots to nothing. */
    private static float rot(float left) {
        return left >= FaultCodes.ADVISORY ? 0.0F : (FaultCodes.ADVISORY - left) / FaultCodes.ADVISORY;
    }

    /** Whether the car stands on a rolling road: its wheels turn it nowhere. */
    public boolean isOnDyno() {
        BlockPos at = this.car.blockPosition();
        return this.car.level().getBlockState(at).is(me.trossty.car.registry.ModBlocks.DYNO)
                || this.car.level().getBlockState(at.below()).is(me.trossty.car.registry.ModBlocks.DYNO);
    }

    /** Worn rings in any one cylinder let the oil past, and it burns: blue smoke. */
    public boolean isBurningOil() {
        return this.engineIn()
                && this.car.parts().engine().worstRing() / CarPart.MAX_CONDITION <= FaultCodes.WARNING;
    }

    /** A cooked head gasket has blown: coolant in the cylinders, white smoke. */
    public boolean isGasketBlown() {
        return this.engineIn()
                && this.car.parts().engine().gasket() / CarPart.MAX_CONDITION <= FaultCodes.CRITICAL;
    }

    /** Worn crank bearings rattle in their shells, oil or no oil. */
    public boolean areBearingsWorn() {
        return this.engineIn()
                && this.car.parts().engine().bearings() / CarPart.MAX_CONDITION <= FaultCodes.CRITICAL;
    }

    private boolean engineIn() {
        return this.car.hasPart(CarPart.ENGINE) && this.car.parts().isFitted(CarPart.ENGINE);
    }

    /** Oil dripping out: no filter screwed on, or one rusted through. */
    public boolean isOilLeaking() {
        return this.hasOil() && this.getOilLevel() > 0.0F && (!this.car.parts().isFitted(CarPart.OIL_FILTER)
                || this.car.parts().getConditionFraction(CarPart.OIL_FILTER) <= FaultCodes.CRITICAL);
    }

    /** Too much fuel for the air: a choked filter, tired plugs or leaking injectors, black smoke. */
    public boolean isRunningRich() {
        return this.car.hasPart(CarPart.AIR_FILTER) && (!this.car.parts().isFitted(CarPart.AIR_FILTER)
                || this.car.parts().getConditionFraction(CarPart.AIR_FILTER) <= FaultCodes.WARNING)
                || this.car.hasPart(CarPart.SPARK_PLUGS) && this.car.parts().isFitted(CarPart.SPARK_PLUGS)
                && this.car.parts().getConditionFraction(CarPart.SPARK_PLUGS) <= FaultCodes.WARNING
                || this.car.hasPart(CarPart.INJECTORS) && this.car.parts().isFitted(CarPart.INJECTORS)
                && this.car.parts().worstFraction(CarPart.INJECTORS) <= FaultCodes.WARNING;
    }

    /** An engine short of oil knocks, and so does one whose bearings are gone. */
    public boolean isKnocking() {
        return this.hasOil() && this.getOilLevel() < OIL_STARVED || this.areBearingsWorn();
    }

    public float getOilAge() {
        return this.car.getEntityData().get(CarEntity.OIL_AGE);
    }

    public void setOilAge(float age) {
        this.car.getEntityData().set(CarEntity.OIL_AGE, Mth.clamp(age, 0.0F, 1.0F));
    }

    public float getBrakeFluid() {
        return this.car.getEntityData().get(CarEntity.BRAKE_FLUID);
    }

    public void setBrakeFluid(float level) {
        this.car.getEntityData().set(CarEntity.BRAKE_FLUID, Mth.clamp(level, 0.0F, 1.0F));
    }

    public float getAlignment() {
        return this.car.getEntityData().get(CarEntity.ALIGNMENT);
    }

    public void setAlignment(float alignment) {
        this.car.getEntityData().set(CarEntity.ALIGNMENT, Mth.clamp(alignment, -1.0F, 1.0F));
    }

    /**
     * Lets the whole sump out into a pan underneath: the level goes to
     * nothing and so does the black, ready for fresh oil from the top.
     * Returns how much came out, in sumps.
     */
    public float drainOil() {
        float out = this.hasOil() ? this.getOilLevel() : 0.0F;
        this.setOilLevel(0.0F);
        this.setOilAge(0.0F);
        return out;
    }

    /** How much of its bite the brakes have with the fluid there is. */
    float brakeFluidFactor() {
        return 0.3F + 0.7F * Math.min(1.0F, this.getBrakeFluid() / BRAKE_FLUID_SHORT);
    }

    /** A bottle of brake fluid into the reservoir under the bonnet. */
    private InteractionResult topUpBrakes(Player player, ItemStack held) {
        if (!this.car.hasPart(CarPart.BRAKE_PADS)) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.car.hasLid(CarEntity.BONNET_BIT) && !this.car.isLidOpen(CarEntity.BONNET_BIT)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.open_bonnet"), true);
            return InteractionResult.CONSUME;
        }
        if (this.getBrakeFluid() >= 0.99F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.brake_fluid_full"), true);
            return InteractionResult.CONSUME;
        }
        this.setBrakeFluid(this.getBrakeFluid() + BRAKE_FLUID_PER_BOTTLE);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
            if (held.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, empty);
            } else if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        this.car.playSound(SoundEvents.BOTTLE_EMPTY, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.brake_fluid_topped",
                Math.round(this.getBrakeFluid() * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    /** A wrench on the wheels of a car up on the lift: the tracking set straight. */
    /** A new timing belt goes on under the bonnet; the old one comes off in bits. */
    InteractionResult fitBelt(Player player, ItemStack belt) {
        if (!this.hasOil() || !this.car.parts().isFitted(CarPart.ENGINE)) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.car.hasLid(CarEntity.BONNET_BIT) && !this.car.isLidOpen(CarEntity.BONNET_BIT)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.open_bonnet"), true);
            return InteractionResult.CONSUME;
        }
        if (this.getBeltWear() < 0.05F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.belt_fresh"), true);
            return InteractionResult.CONSUME;
        }
        boolean snapped = this.isBeltSnapped();
        this.setBeltWear(0.0F);
        belt.consume(1, player);
        this.car.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable(snapped ? "message.overthought_vehicles.belt_fitted_snapped"
                : "message.overthought_vehicles.belt_fitted"), true);
        return InteractionResult.CONSUME;
    }

    /** The converter hangs under the floor: it is changed with the car up. */
    InteractionResult fitCat(Player player, ItemStack cat) {
        if (!this.hasCat()) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!VehicleLiftBlock.isServicing(this.car.level(), this.car)) {
            player.displayClientMessage(VehicleLiftBlock.needsLift(this.car), true);
            return InteractionResult.CONSUME;
        }
        if (this.getCatClog() < 0.05F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.cat_fresh"), true);
            return InteractionResult.CONSUME;
        }
        this.setCatClog(0.0F);
        cat.consume(1, player);
        this.car.playSound(SoundEvents.ANVIL_USE, 0.6F, 1.3F);
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.cat_fitted"), true);
        return InteractionResult.CONSUME;
    }

    /** One stroke of the foot pump: a quarter of the air back in every tyre. */
    InteractionResult pumpTyres(Player player) {
        if (!this.car.hasPart(CarPart.WHEELS) || !this.car.parts().isFitted(CarPart.WHEELS)) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.getTyrePressure() >= 0.99F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.tyres_firm"), true);
            return InteractionResult.CONSUME;
        }
        this.setTyrePressure(this.getTyrePressure() + PUMP_STROKE);
        this.car.playSound(SoundEvents.PISTON_EXTEND, 0.5F, 1.7F);
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.tyres_pumped",
                Math.round(this.getTyrePressure() * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    InteractionResult alignWheels(Player player) {
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!VehicleLiftBlock.isServicing(this.car.level(), this.car)) {
            player.displayClientMessage(VehicleLiftBlock.needsLift(this.car), true);
            return InteractionResult.CONSUME;
        }
        if (Math.abs(this.getAlignment()) < 0.01F) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.aligned_already"), true);
            return InteractionResult.CONSUME;
        }
        this.setAlignment(0.0F);
        this.car.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.aligned"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Jump leads: the first car clicked gives, the second takes. The giver has
     * to have a charged battery, both bonnets have to be up, and the two have to
     * be close enough for the leads to reach.
     */
    InteractionResult jumpWith(Player player, ItemStack leads) {
        if (this.car.isElectric() || !this.car.hasPart(CarPart.BATTERY)) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.car.hasLid(CarEntity.BONNET_BIT) && !this.car.isLidOpen(CarEntity.BONNET_BIT)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.open_bonnet"), true);
            return InteractionResult.CONSUME;
        }
        java.util.UUID donorId = leads.get(me.trossty.car.registry.ModComponents.OBD_LINK);
        Entity donor = donorId == null || !(this.car.level() instanceof ServerLevel server) ? null : server.getEntity(donorId);
        if (!(donor instanceof CarEntity giver) || giver == this.car || giver.distanceTo(this.car) > JUMP_REACH
                || giver.hasLid(CarEntity.BONNET_BIT) && !giver.isLidOpen(CarEntity.BONNET_BIT)) {
            if (!this.car.parts().isFitted(CarPart.BATTERY) || this.car.parts().getConditionFraction(CarPart.BATTERY) < JUMP_DONOR_MIN) {
                leads.remove(me.trossty.car.registry.ModComponents.OBD_LINK);
                player.displayClientMessage(Component.translatable("message.overthought_vehicles.jump_donor_flat"), true);
                return InteractionResult.CONSUME;
            }
            leads.set(me.trossty.car.registry.ModComponents.OBD_LINK, this.car.getUUID());
            this.car.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.4F);
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.jump_clipped"), true);
            return InteractionResult.CONSUME;
        }
        leads.remove(me.trossty.car.registry.ModComponents.OBD_LINK);
        if (!this.car.parts().isFitted(CarPart.BATTERY)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.jump_no_battery"), true);
            return InteractionResult.CONSUME;
        }
        float give = Math.min(JUMP_CHARGE, giver.parts().getCondition(CarPart.BATTERY) - JUMP_DONOR_MIN * CarPart.MAX_CONDITION);
        if (give <= 0.0F || giver.parts().getConditionFraction(CarPart.BATTERY) < JUMP_DONOR_MIN) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.jump_donor_flat"), true);
            return InteractionResult.CONSUME;
        }
        float before = this.car.parts().getCondition(CarPart.BATTERY);
        float after = Math.max(before, Math.min(CarPart.MAX_CONDITION, before + give));
        this.car.parts().setCondition(CarPart.BATTERY, after);
        giver.parts().setCondition(CarPart.BATTERY, giver.parts().getCondition(CarPart.BATTERY) - (after - before) * 0.5F);
        this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.COPPER_BULB_TURN_ON, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (this.car.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.car.getX(), this.car.getY() + 1.0, this.car.getZ(),
                    8, 0.3, 0.2, 0.3, 0.05);
        }
        player.displayClientMessage(Component.translatable("message.overthought_vehicles.jumped",
                Math.round(this.car.parts().getConditionFraction(CarPart.BATTERY) * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    /** How far apart two cars can stand for the leads to reach. */
    private static final float JUMP_REACH = 5.0F;

    /** A battery below this share of its charge has nothing to give. */
    private static final float JUMP_DONOR_MIN = 0.35F;

    /** How much charge one jump puts into a flat battery, out of 100. */
    private static final float JUMP_CHARGE = 30.0F;

    /** How many times faster the engine wears for want of oil: 1 with enough in it. */
    float lowOilWear() {
        if (!this.hasOil()) {
            return 1.0F;
        }
        if (this.getOilLevel() >= OIL_STARVED) {
            return 1.0F + SLUDGE_WEAR * this.getOilAge();
        }
        return Mth.lerp(this.getOilLevel() / OIL_STARVED, DRY_ENGINE_WEAR, 1.0F)
                * (1.0F + SLUDGE_WEAR * this.getOilAge());
    }

    /** How much of its cooling the radiator has with the coolant there is. */
    float coolantCooling() {
        if (!this.car.hasPart(CarPart.RADIATOR)) {
            return 1.0F;
        }
        return 0.35F + 0.65F * Math.min(1.0F, this.getCoolantLevel() / COOLANT_SHORT);
    }

    /**
     * Oil or water poured in under the bonnet: a bottle of motor oil into
     * the sump, a bottle or a bucket of water into the coolant. The bonnet
     * has to be up; PASS for anything else, so the click goes on.
     */
    InteractionResult topUp(Player player, ItemStack held) {
        if (held.is(ModItems.BRAKE_FLUID)) {
            return this.topUpBrakes(player, held);
        }
        boolean oil = held.is(ModItems.MOTOR_OIL);
        boolean bucket = held.is(Items.WATER_BUCKET);
        boolean bottle = held.is(Items.POTION) && held.getOrDefault(
                net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                net.minecraft.world.item.alchemy.PotionContents.EMPTY).is(net.minecraft.world.item.alchemy.Potions.WATER);
        if (!oil && !bucket && !bottle || oil && !this.hasOil() || !oil && !this.car.hasPart(CarPart.RADIATOR)) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Under the bonnet where there is one; a bike's filler is out in the open.
        if (this.car.hasLid(CarEntity.BONNET_BIT) && !this.car.isLidOpen(CarEntity.BONNET_BIT)) {
            player.displayClientMessage(Component.translatable("message.overthought_vehicles.open_bonnet"), true);
            return InteractionResult.CONSUME;
        }
        float level = oil ? this.getOilLevel() : this.getCoolantLevel();
        if (level >= 0.99F) {
            player.displayClientMessage(Component.translatable(oil ? "message.overthought_vehicles.oil_full" : "message.overthought_vehicles.coolant_full"), true);
            return InteractionResult.CONSUME;
        }
        float now = Math.min(1.0F, level + (oil ? OIL_PER_BOTTLE : bucket ? 1.0F : WATER_PER_BOTTLE));
        if (oil) {
            // Fresh oil on old thins the black by as much as it adds.
            this.setOilAge(now <= 0.0F ? 0.0F : this.getOilAge() * level / now);
            this.setOilLevel(now);
        } else {
            this.setCoolantLevel(now);
        }
        if (!player.getAbilities().instabuild) {
            ItemStack empty = new ItemStack(bucket ? Items.BUCKET : Items.GLASS_BOTTLE);
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, empty);
            } else if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        this.car.playSound(bucket ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(oil ? "message.overthought_vehicles.oil_topped" : "message.overthought_vehicles.coolant_topped",
                Math.round(now * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    /** Reads back what {@link #save} wrote; whatever is missing is as it left the factory. */
    void load(ValueInput view) {
        this.setOilLevel(view.getFloatOr("Oil", 1.0F));
        this.setCoolantLevel(view.getFloatOr("Coolant", 1.0F));
        this.setOilAge(view.getFloatOr("OilAge", 0.0F));
        this.setBrakeFluid(view.getFloatOr("BrakeFluid", 1.0F));
        this.setAlignment(view.getFloatOr("Alignment", 0.0F));
        this.setBeltWear(view.getFloatOr("BeltWear", 0.0F));
        this.setCatClog(view.getFloatOr("CatClog", 0.0F));
        this.setTyrePressure(view.getFloatOr("TyrePressure", 1.0F));
        this.setDirt(view.getFloatOr("Dirt", 0.0F));
    }

    /** Writes down only what has moved from factory fresh, so a new car saves small. */
    void save(ValueOutput view) {
        if (this.getOilLevel() < 1.0F) {
            view.putFloat("Oil", this.getOilLevel());
        }
        if (this.getCoolantLevel() < 1.0F) {
            view.putFloat("Coolant", this.getCoolantLevel());
        }
        if (this.getOilAge() > 0.0F) {
            view.putFloat("OilAge", this.getOilAge());
        }
        if (this.getBrakeFluid() < 1.0F) {
            view.putFloat("BrakeFluid", this.getBrakeFluid());
        }
        if (this.getAlignment() != 0.0F) {
            view.putFloat("Alignment", this.getAlignment());
        }
        if (this.getBeltWear() > 0.0F) {
            view.putFloat("BeltWear", this.getBeltWear());
        }
        if (this.getCatClog() > 0.0F) {
            view.putFloat("CatClog", this.getCatClog());
        }
        if (this.getTyrePressure() < 1.0F) {
            view.putFloat("TyrePressure", this.getTyrePressure());
        }
        if (this.getDirt() > 0.0F) {
            view.putFloat("Dirt", this.getDirt());
        }
    }
}
