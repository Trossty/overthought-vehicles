package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A box on one axle that goes where the vehicle in front of it goes.
 *
 * <p>No engine, no seats, no fuel: a trailer is a chest that can be pulled.
 * Its only parts are its running gear ({@link #hasPart}), so the lift, the
 * service window and the tablet offer nothing else on it.
 *
 * <p>Hitching is done by hand: click it with an empty hand and you take
 * the drawbar and walk it about; walk it up behind a vehicle and click that
 * vehicle to drop the coupler on its tow ball. Click a hitched trailer to let
 * it go again. Sneak-click a box trailer and its load opens, the same chest
 * screen as the pickup's bed.
 *
 * <p>A flat bed ({@link VehicleDefinition#carriesVehicles}) carries one
 * vehicle or one engine instead. Push a car, a bike, a boat or a plane up
 * behind it and sneak-click the bed: the winch pulls it on. Sneak-click again
 * and it rolls off the back. The vehicle is kept whole while it rides, with
 * its parts, fuel and key, exactly as it went on.
 *
 * <h2>How it follows</h2>
 * Not with physics. Every tick the trailer stands its coupler on the towing
 * vehicle's tow ball and points itself at it, the way a real one-axle trailer
 * does: it swings wide on a turn and straightens out behind you. Both sides
 * work this out, so the driver sees it locked on to their own car rather
 * than a step behind what the server last heard.
 *
 * <p>A load slows whatever pulls it by its weight: see {@link #towedMass}
 * and {@link CarEntity#towRatio}.
 */
public class TrailerEntity extends CarEntity {

    public static final int LOAD_SLOTS = 27;

    /** The game tick this last ticked, and the one the tractor last pulled it before that. */
    private long tickedAt = Long.MIN_VALUE;
    private long pulledAt = Long.MIN_VALUE;
    private boolean ticking;
    private double pulledFromX;
    private double pulledFromY;
    private double pulledFromZ;
    private float pulledFromYRot;

    /** What a box trailer's load weighs with every slot taken, in kilograms. */
    private static final float FULL_CHEST_KG = 600.0F;
    /** What a bare engine on a flat bed weighs, in kilograms. */
    private static final float ENGINE_KG = 180.0F;
    /** Kept in a carried vehicle's saved data: what it weighs. */
    private static final String CARGO_MASS = "car:cargo_mass";
    /** How close behind the bed a vehicle's nearer end has to be for the winch to reach it, in blocks. */
    private static final double WINCH_REACH = 4.0;
    /** Walked further than this from the drawbar, a hand lets go of it. */
    private static final double HOLD_REACH = 10.0;

    /** How close the coupler has to be to a car's tow ball to hitch on, in blocks. */
    private static final double HITCH_REACH = 2.5;

    /** Further than this from the tow ball and the coupler has come off. */
    private static final double BREAK_DISTANCE = 6.0;

    /** The tractor's entity id, so a client can find it; -1 when free. */
    private static final EntityDataAccessor<Integer> TRACTOR =
            SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);

    /** The player walking this by its drawbar, by entity id; -1 when nobody is. */
    private static final EntityDataAccessor<Integer> HOLDER =
            SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);

    /** The vehicle on a flat bed, as it saves itself; empty when there is none. */
    private static final EntityDataAccessor<CompoundTag> CARGO =
            SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** An engine on a flat bed. */
    private static final EntityDataAccessor<ItemStack> CARGO_ITEM =
            SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.ITEM_STACK);

    private final SimpleContainer load = this.carriedChest(LOAD_SLOTS);

    /** A client's stand-in for the carried vehicle, made from {@link #CARGO}, to draw it. */
    private @Nullable Entity cargoView;
    private @Nullable CompoundTag cargoViewOf;

    /** Kept for the save file: entity ids do not survive a reload. */
    private @Nullable UUID tractorUuid;

    public TrailerEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRACTOR, -1);
        builder.define(HOLDER, -1);
        builder.define(CARGO, new CompoundTag());
        builder.define(CARGO_ITEM, ItemStack.EMPTY);
    }

    public Container getLoad() {
        return this.load;
    }

    /** 0 empty, 1 with every slot taken. */
    public float getLoadFraction() {
        int used = 0;
        for (int slot = 0; slot < this.load.getContainerSize(); slot++) {
            if (!this.load.getItem(slot).isEmpty()) {
                used++;
            }
        }
        return (float) used / LOAD_SLOTS;
    }

    /** All a tractor pulls with this on its hitch, in kilograms: the trailer and what it carries. */
    public float towedMass() {
        float mass = this.massKg() + this.getLoadFraction() * FULL_CHEST_KG;
        CompoundTag cargo = this.entityData.get(CARGO);
        if (!cargo.isEmpty()) {
            mass += cargo.getFloat(CARGO_MASS);
        }
        if (!this.entityData.get(CARGO_ITEM).isEmpty()) {
            mass += ENGINE_KG;
        }
        return mass;
    }

    // ---- a flat bed and what rides on it ----

    /** Whether this is a flat bed that carries a vehicle or an engine, not a box. */
    public boolean isFlatbed() {
        VehicleDefinition defined = this.getDefinition();
        return defined != null && defined.carriesVehicles();
    }

    /** Whether this is a tow dolly: a car's front wheels ride on it, the back ones on the road. */
    public boolean isDolly() {
        VehicleDefinition defined = this.getDefinition();
        return defined != null && defined.carriesVehicles() && defined.dolly();
    }

    /** Whether this kind of vehicle may go on: what the definition lists, or anything but a trailer. */
    private boolean takes(CarEntity car) {
        VehicleDefinition defined = this.getDefinition();
        return defined == null || defined.takes().isEmpty() || defined.takes().contains(car.chassis());
    }

    /** Only aircraft go on this: it is the tow dolly for a plane's nose wheel. */
    private boolean forPlanes() {
        VehicleDefinition defined = this.getDefinition();
        return defined != null && defined.takes().equals(List.of(VehicleDefinition.Chassis.PLANE));
    }

    /** How high the bed is, in blocks. */
    public float deckHeight() {
        VehicleDefinition defined = this.getDefinition();
        return defined == null ? 0.0F : defined.deck() / 16.0F;
    }

    public boolean hasCargo() {
        return !this.entityData.get(CARGO).isEmpty() || !this.entityData.get(CARGO_ITEM).isEmpty();
    }

    /** The engine on the bed, or empty. */
    public ItemStack getCargoItem() {
        return this.entityData.get(CARGO_ITEM);
    }

    /**
     * The vehicle on the bed as a client draws it: a copy made from what the
     * server sent, never in the world. Null when the bed carries none.
     */
    public @Nullable Entity cargoView() {
        CompoundTag cargo = this.entityData.get(CARGO);
        if (cargo.isEmpty()) {
            this.cargoView = null;
            this.cargoViewOf = null;
            return null;
        }
        if (cargo != this.cargoViewOf) {
            this.cargoViewOf = cargo;
            this.cargoView = EntityType.create(cargo, this.level()).orElse(null);
        }
        return this.cargoView;
    }

    private InteractionResult loadEngine(Player player, ItemStack held) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.hasCargo()) {
            player.displayClientMessage(Component.translatable("message.car.flatbed_full"), true);
            return InteractionResult.CONSUME;
        }
        this.entityData.set(CARGO_ITEM, held.copyWithCount(1));
        held.consume(1, player);
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 0.8F);
        return InteractionResult.CONSUME;
    }

    /** The winch: pulls the nearest vehicle on, or lets what is on roll off. */
    private InteractionResult winch(Player player) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack engine = this.entityData.get(CARGO_ITEM);
        if (!engine.isEmpty()) {
            this.entityData.set(CARGO_ITEM, ItemStack.EMPTY);
            if (!player.addItem(engine.copy())) {
                player.drop(engine.copy(), false);
            }
            return InteractionResult.CONSUME;
        }
        if (!this.entityData.get(CARGO).isEmpty()) {
            Entity off = this.unload(false);
            if (off != null) {
                this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
                player.displayClientMessage(Component.translatable("message.car.flatbed_unloaded",
                        off.getDisplayName()), true);
            } else {
                player.displayClientMessage(Component.translatable("message.car.flatbed_blocked"), true);
            }
            return InteractionResult.CONSUME;
        }
        CarEntity car = this.nearestCargo();
        if (car == null) {
            player.displayClientMessage(Component.translatable(
                    this.forPlanes() ? "message.car.plane_dolly_nothing"
                            : this.isDolly() ? "message.car.dolly_nothing" : "message.car.flatbed_nothing"), true);
            return InteractionResult.CONSUME;
        }
        if (car.isVehicle()) {
            player.displayClientMessage(Component.translatable("message.car.flatbed_occupied"), true);
            return InteractionResult.CONSUME;
        }
        CompoundTag saved = new CompoundTag();
        if (!car.saveAsPassenger(saved)) {
            return InteractionResult.CONSUME;
        }
        saved.putFloat(CARGO_MASS, car.massKg());
        Component name = car.getDisplayName();
        // Taken out of the world whole, not destroyed: what a pickup carries
        // stays in its bed rather than spilling where it stood.
        car.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        this.entityData.set(CARGO, saved);
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
        this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6F, 0.8F);
        player.displayClientMessage(Component.translatable("message.car.flatbed_loaded", name), true);
        return InteractionResult.CONSUME;
    }

    /**
     * The vehicle nearest the back of the bed that could go on it — not a
     * trailer, not the one pulling this — if it has been pushed up within
     * the winch's reach.
     */
    private @Nullable CarEntity nearestCargo() {
        CarEntity tractor = this.getTractor();
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 tail = this.position().add(Mth.sin(yaw) * this.rearReach(), 0.0, -Mth.cos(yaw) * this.rearReach());
        CarEntity best = null;
        double bestGap = WINCH_REACH;
        for (CarEntity car : this.level().getEntitiesOfClass(CarEntity.class,
                this.getBoundingBox().inflate(WINCH_REACH + 8.0), car -> car != this && car != tractor
                        && !(car instanceof TrailerEntity) && !car.isRemoved() && !car.isTowing(null)
                        && this.takes(car))) {
            double gap = Math.sqrt(car.distanceToSqr(tail)) - Math.max(car.frontReach(), car.rearReach());
            if (gap < bestGap) {
                best = car;
                bestGap = gap;
            }
        }
        return best;
    }

    /**
     * Puts the carried vehicle back in the world on the ground behind the bed,
     * or on the cradle or jack it was backed up to.
     */
    private @Nullable Entity unload(boolean force) {
        CompoundTag saved0 = this.entityData.get(CARGO);
        CompoundTag saved = saved0.copy();
        this.entityData.set(CARGO, new CompoundTag());
        saved.remove(CARGO_MASS);
        Entity off = EntityType.create(saved, this.level()).orElse(null);
        if (off == null) {
            return null;
        }
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        double back = this.rearReach() + (off instanceof CarEntity car ? car.frontReach() : 2.0) + 0.5;
        Vec3 spot = new Vec3(this.getX() + Mth.sin(yaw) * back, this.getY(), this.getZ() - Mth.cos(yaw) * back);
        // Backed up to a boat's cradle or a plane's jack, it is let down on to that.
        Vec3 stand = off instanceof CarEntity car
                ? me.trossty.car.block.ServiceStandBlock.near(this.level(), spot, this.getYRot(), car) : null;
        if (stand != null) {
            spot = stand;
        }
        off.moveTo(spot.x, spot.y, spot.z, this.getYRot(), 0.0F);
        if (!force && !this.level().noCollision(off, off.getBoundingBox().deflate(0.05))) {
            // No room behind: it stays on the bed rather than going into a wall.
            this.entityData.set(CARGO, saved0);
            return null;
        }
        off.setDeltaMovement(Vec3.ZERO);
        this.level().addFreshEntity(off);
        return off;
    }

    // ---- walking it by the drawbar ----

    /** The player holding the drawbar, if one is. */
    public @Nullable Player getHolder() {
        int id = this.entityData.get(HOLDER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player && player.isAlive()
                && !player.isSpectator() ? player : null;
    }

    /** The trailer this player has by the drawbar, if any. */
    public static @Nullable TrailerEntity heldBy(Player player) {
        List<TrailerEntity> near = player.level().getEntitiesOfClass(TrailerEntity.class,
                player.getBoundingBox().inflate(HOLD_REACH + 8.0),
                trailer -> trailer.entityData.get(HOLDER) == player.getId());
        return near.isEmpty() ? null : near.get(0);
    }

    private void letGo() {
        this.entityData.set(HOLDER, -1);
    }

    /** Where a holding hand keeps the coupler: a step in front of the player, at their feet. */
    private static Vec3 hand(Player player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(player.getX() - Mth.sin(yaw) * 0.9, player.getY(), player.getZ() + Mth.cos(yaw) * 0.9);
    }

    /**
     * The player walking this clicked a vehicle: the coupler goes on its tow
     * ball if it is close enough to reach it.
     */
    InteractionResult hitchOnto(CarEntity tractor, Player player) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Vec3 ball = tractor.hitchPoint();
        Vec3 coupler = this.coupler();
        String refused = !tractor.canTow() ? "message.car.trailer_cannot_tow"
                : tractor.isTowing(this) ? "message.car.trailer_hitch_taken"
                : ball.distanceToSqr(coupler.x, ball.y, coupler.z) > HITCH_REACH * HITCH_REACH
                ? "message.car.trailer_too_far" : null;
        if (refused != null) {
            player.displayClientMessage(Component.translatable(refused), true);
            return InteractionResult.CONSUME;
        }
        this.letGo();
        this.hitchTo(tractor);
        this.follow(tractor);
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6F, 1.4F);
        player.displayClientMessage(Component.translatable("message.car.trailer_hitched", tractor.getDisplayName()), true);
        return InteractionResult.CONSUME;
    }

    /** Where the tip of the drawbar is now. */
    private Vec3 coupler() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return this.position().add(-Mth.sin(yaw) * this.couplerReach(), 0.0, Mth.cos(yaw) * this.couplerReach());
    }

    public @Nullable CarEntity getTractor() {
        int id = this.entityData.get(TRACTOR);
        if (id < 0) {
            return null;
        }
        return this.level().getEntity(id) instanceof CarEntity car && !car.isRemoved()
                && car.canTow() ? car : null;
    }

    /** Unhitched, the nose drops onto the jockey wheel. */
    @Override
    protected float restPoseTarget() {
        return this.isHitched() || this.entityData.get(HOLDER) >= 0 ? 0.0F : 1.0F;
    }

    public boolean isHitched() {
        return this.entityData.get(TRACTOR) >= 0;
    }

    private void hitchTo(@Nullable CarEntity tractor) {
        this.entityData.set(TRACTOR, tractor == null ? -1 : tractor.getId());
        this.tractorUuid = tractor == null ? null : tractor.getUUID();
    }

    /** Hitches straight on, wherever the two stand: for commands and tests. */
    public void hitch(@Nullable CarEntity tractor) {
        this.hitchTo(tractor);
        if (tractor != null) {
            this.follow(tractor);
        }
    }

    @Override
    protected boolean hitchedWith(Entity other) {
        return other != null && this.entityData.get(TRACTOR) == other.getId();
    }

    // ---- it is not something you sit in or start ----

    @Override
    protected int plainSeatCount() {
        return 0;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    /** Wheels, springs and brakes: a trailer has nothing else to service. */
    @Override
    public boolean hasPart(CarPart part) {
        return part == CarPart.WHEELS || part == CarPart.SUSPENSION
                || part == CarPart.BRAKE_PADS || part == CarPart.BRAKE_DISCS;
    }

    @Override
    public boolean canRun() {
        return false;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.TRAILER;
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.TRAILER;
    }

    // ---- following ----

    @Override
    public void tick() {
        long now = this.level().getGameTime();
        if (this.pulledAt == now) {
            // The tractor ticked first this tick and already dragged us along,
            // after the game had noted where we started. Put that start back,
            // or the step it took is never drawn and the trailer judders.
            this.xo = this.xOld = this.pulledFromX;
            this.yo = this.yOld = this.pulledFromY;
            this.zo = this.zOld = this.pulledFromZ;
            this.yRotO = this.pulledFromYRot;
        }
        this.tickedAt = now;
        this.ticking = true;
        try {
            super.tick();
        } finally {
            this.ticking = false;
        }
        if (this.isRemoved()) {
            return;
        }
        if (!this.level().isClientSide()) {
            if (this.tractorUuid != null && !this.isHitched()
                    && this.level() instanceof ServerLevel server
                    && server.getEntity(this.tractorUuid) instanceof CarEntity found
                    && found.canTow()) {
                // Loaded back in: the tractor has a new entity id now.
                this.hitchTo(found);
            }
            if (this.isHitched() && this.getTractor() == null && this.tractorUuid != null
                    && this.level() instanceof ServerLevel server
                    && server.getEntity(this.tractorUuid) == null && this.tickCount > 100) {
                // Wrecked, cleared away or unloaded without us: let go.
                this.hitchTo(null);
            }
        }
        CarEntity tractor = this.getTractor();
        if (tractor != null) {
            this.letGo();
            tractor.towedBy(this);
            this.follow(tractor);
            return;
        }
        if (this.entityData.get(HOLDER) < 0) {
            return;
        }
        Player holder = this.getHolder();
        if (holder == null || holder.distanceToSqr(this.coupler()) > HOLD_REACH * HOLD_REACH) {
            if (!this.level().isClientSide()) {
                this.letGo();
            }
            return;
        }
        this.pullTowards(hand(holder), holder.getY());
    }

    /**
     * Stands the coupler on the tractor's tow ball and points the trailer at
     * it. Called from both ends — the trailer's own tick and the tractor's —
     * so whichever of the two ticks last this tick, the trailer ends up where
     * the tractor finished, never a tick behind it.
     */
    void follow(CarEntity tractor) {
        long now = this.level().getGameTime();
        if (!this.ticking && this.tickedAt != now && this.pulledAt != now) {
            // Moved before our own tick this tick: remember where from.
            this.pulledAt = now;
            this.pulledFromX = this.getX();
            this.pulledFromY = this.getY();
            this.pulledFromZ = this.getZ();
            this.pulledFromYRot = this.getYRot();
        }
        if (this.pullTowards(tractor.hitchPoint(), tractor.getY()) && !this.level().isClientSide()) {
            this.hitchTo(null);
            this.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
        }
    }

    /**
     * Brings the tip of the drawbar to {@code ball} and points the trailer at
     * it, as one axle does behind whatever pulls it. True when the ball is
     * further than the coupling holds, and nothing was moved.
     */
    private boolean pullTowards(Vec3 ball, double groundY) {
        double dx = ball.x - this.getX();
        double dz = ball.z - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > BREAK_DISTANCE + this.couplerReach()) {
            return true;
        }
        double dirX;
        double dirZ;
        if (length < 1.0E-4) {
            float yaw = this.getYRot() * Mth.DEG_TO_RAD;
            dirX = -Mth.sin(yaw);
            dirZ = Mth.cos(yaw);
        } else {
            dirX = dx / length;
            dirZ = dz / length;
        }
        double reach = this.couplerReach();
        double y = Mth.lerp(0.5, this.getY(), groundY);
        // Moved, not placed, so a trailer dragged into a wall stops at it; if
        // it is held back far enough the coupling breaks, as above.
        this.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(
                ball.x - dirX * reach - this.getX(), y - this.getY(), ball.z - dirZ * reach - this.getZ()));
        this.setYRot((float) Math.toDegrees(Math.atan2(-dirX, dirZ)));
        this.setDeltaMovement(Vec3.ZERO);
        return false;
    }

    /**
     * Hitched on, a client places the trailer itself, behind its own view of
     * the tractor; the server's word on where it is would only drag it a step
     * behind that, and the two fighting is a judder. Unhitched, it listens.
     */
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (this.getTractor() == null && this.getHolder() == null) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    /** From the middle of the trailer forward to the tip of its drawbar, in blocks. */
    private double couplerReach() {
        return this.frontReach();
    }

    // ---- clicking: hitch, unhitch, or open the load ----

    @Override
    protected InteractionResult interactUnlocked(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (this.isFlatbed()) {
            if (held.is(ModItems.ENGINE) && !this.isDolly()) {
                return this.loadEngine(player, held);
            }
            if (player.isSecondaryUseActive() && held.isEmpty()) {
                return this.winch(player);
            }
        } else if (player.isSecondaryUseActive()) {
            if (this.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            player.openMenu(new SimpleMenuProvider(
                    (syncId, playerInventory, ignored) ->
                            ChestMenu.threeRows(syncId, playerInventory, this.load),
                    Component.translatable("container.car.trailer")));
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.isHitched()) {
            this.hitchTo(null);
            this.playSound(SoundEvents.CHAIN_HIT, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.car.trailer_unhitched"), true);
            return InteractionResult.CONSUME;
        }
        if (this.entityData.get(HOLDER) == player.getId()) {
            this.letGo();
            player.displayClientMessage(Component.translatable("message.car.trailer_let_go"), true);
            return InteractionResult.CONSUME;
        }
        TrailerEntity other = heldBy(player);
        if (other != null) {
            other.letGo();
        }
        this.entityData.set(HOLDER, player.getId());
        this.playSound(SoundEvents.CHAIN_HIT, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.car.trailer_grabbed"), true);
        return InteractionResult.CONSUME;
    }

    // ---- the load goes where the trailer goes ----

    @Override
    protected void spillCargo(ServerLevel world) {
        Containers.dropContents(world, (Entity) this, this.load);
        this.load.clearContent();
        ItemStack engine = this.entityData.get(CARGO_ITEM);
        if (!engine.isEmpty()) {
            Containers.dropItemStack(world, this.getX(), this.getY() + this.deckHeight(), this.getZ(), engine.copy());
            this.entityData.set(CARGO_ITEM, ItemStack.EMPTY);
        }
        if (!this.entityData.get(CARGO).isEmpty()) {
            // Wrecked: it goes down wherever it can, rather than vanish with the bed.
            this.unload(true);
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueInput view = ValueInput.of(tag, this.registryAccess());
        super.readAdditionalSaveData(tag);
        ContainerHelper.loadAllItems(view.tag(), this.load.getItems(), view.registries());
        this.showLoad(this.load);
        this.tractorUuid = view.read("Tractor", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(CARGO, tag.getCompound("Cargo"));
        this.entityData.set(CARGO_ITEM, ItemStack.parseOptional(this.registryAccess(), tag.getCompound("CargoItem")));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput view = ValueOutput.of(tag, this.registryAccess());
        super.addAdditionalSaveData(tag);
        ContainerHelper.saveAllItems(view.tag(), this.load.getItems(), view.registries());
        if (!this.entityData.get(CARGO).isEmpty()) {
            tag.put("Cargo", this.entityData.get(CARGO).copy());
        }
        if (!this.entityData.get(CARGO_ITEM).isEmpty()) {
            tag.put("CargoItem", this.entityData.get(CARGO_ITEM).save(this.registryAccess()));
        }
        if (this.tractorUuid != null && this.isHitched()) {
            view.store("Tractor", UUIDUtil.CODEC, this.tractorUuid);
        }
    }
}
