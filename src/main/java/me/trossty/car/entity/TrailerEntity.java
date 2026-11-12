package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A box on one axle that goes where the vehicle in front of it goes.
 *
 * <p>No engine, no seats, no fuel: a trailer is a chest that can be pulled.
 * Its only parts are its running gear ({@link #hasPart}), so the lift, the
 * service window and the tablet offer nothing else on it.
 * Click it with an empty hand next to the back of a car and it hitches on;
 * click it again and it lets go. Sneak-click and the load opens, the same
 * chest screen as the pickup's bed.
 *
 * <h2>How it follows</h2>
 * Not with physics. Every tick the trailer stands its coupler on the towing
 * vehicle's tow ball and points itself at it, the way a real one-axle trailer
 * does: it swings wide on a turn and straightens out behind you. Both sides
 * work this out, so the driver sees it locked on to their own car rather
 * than a step behind what the server last heard.
 *
 * <p>A load slows whatever pulls it: {@link #EMPTY_TOW_FACTOR} of the
 * tractor's top speed with nothing aboard, {@link #LADEN_TOW_FACTOR} full.
 */
public class TrailerEntity extends CarEntity {

    public static final int LOAD_SLOTS = 27;

    public static final float EMPTY_TOW_FACTOR = 0.92F;
    public static final float LADEN_TOW_FACTOR = 0.78F;

    /** How close the coupler has to be to a car's tow ball to hitch on, in blocks. */
    private static final double HITCH_REACH = 2.5;

    /** Further than this from the tow ball and the coupler has come off. */
    private static final double BREAK_DISTANCE = 6.0;

    /** The tractor's entity id, so a client can find it; -1 when free. */
    private static final EntityDataAccessor<Integer> TRACTOR =
            SynchedEntityData.defineId(TrailerEntity.class, EntityDataSerializers.INT);

    private final SimpleContainer load = new SimpleContainer(LOAD_SLOTS);

    /** Kept for the save file: entity ids do not survive a reload. */
    private @Nullable UUID tractorUuid;

    public TrailerEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TRACTOR, -1);
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

    /** What the tractor's top speed is multiplied by while it pulls this. */
    public float towFactor() {
        return Mth.lerp(this.getLoadFraction(), EMPTY_TOW_FACTOR, LADEN_TOW_FACTOR);
    }

    public @Nullable CarEntity getTractor() {
        int id = this.entityData.get(TRACTOR);
        if (id < 0) {
            return null;
        }
        return this.level().getEntity(id) instanceof CarEntity car && !car.isRemoved()
                && !(car instanceof TrailerEntity) ? car : null;
    }

    /** Unhitched, the nose drops onto the jockey wheel. */
    @Override
    protected float restPoseTarget() {
        return this.isHitched() ? 0.0F : 1.0F;
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
        super.tick();
        if (this.isRemoved()) {
            return;
        }
        if (!this.level().isClientSide()) {
            if (this.tractorUuid != null && !this.isHitched()
                    && this.level() instanceof ServerLevel server
                    && server.getEntity(this.tractorUuid) instanceof CarEntity found
                    && !(found instanceof TrailerEntity)) {
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
            tractor.towedBy(this);
            this.follow(tractor);
        }
    }

    /**
     * Stands the coupler on the tractor's tow ball and points the trailer at
     * it. Called from both ends — the trailer's own tick and the tractor's —
     * so whichever of the two ticks last this tick, the trailer ends up where
     * the tractor finished, never a tick behind it.
     */
    void follow(CarEntity tractor) {
        Vec3 ball = tractor.hitchPoint();
        double dx = ball.x - this.getX();
        double dz = ball.z - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > BREAK_DISTANCE + this.couplerReach()) {
            if (!this.level().isClientSide()) {
                this.hitchTo(null);
                this.playSound(SoundEvents.CHAIN_BREAK, 1.0F, 1.0F);
            }
            return;
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
        double y = Mth.lerp(0.5, this.getY(), tractor.getY());
        // Moved, not placed, so a trailer dragged into a wall stops at it; if
        // it is held back far enough the coupling breaks, as above.
        this.move(net.minecraft.world.entity.MoverType.SELF, new Vec3(
                ball.x - dirX * reach - this.getX(), y - this.getY(), ball.z - dirZ * reach - this.getZ()));
        this.setYRot((float) Math.toDegrees(Math.atan2(-dirX, dirZ)));
        this.setDeltaMovement(Vec3.ZERO);
    }

    /** From the middle of the trailer forward to the tip of its drawbar, in blocks. */
    private double couplerReach() {
        return this.frontReach();
    }

    // ---- clicking: hitch, unhitch, or open the load ----

    @Override
    protected InteractionResult interactUnlocked(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            if (this.level().isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            player.openMenu(new SimpleMenuProvider(
                    (syncId, playerInventory, ignored) ->
                            ChestMenu.threeRows(syncId, playerInventory, this.load),
                    Component.translatable("container.car.trailer")));
            return InteractionResult.CONSUME;
        }
        if (!player.getItemInHand(hand).isEmpty()) {
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
        CarEntity tractor = this.nearestTractor();
        if (tractor == null) {
            player.displayClientMessage(Component.translatable("message.car.trailer_no_vehicle"), true);
            return InteractionResult.CONSUME;
        }
        this.hitchTo(tractor);
        this.follow(tractor);
        this.playSound(SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
        this.playSound(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.6F, 1.4F);
        player.displayClientMessage(Component.translatable("message.car.trailer_hitched", tractor.getDisplayName()), true);
        return InteractionResult.CONSUME;
    }

    /** The vehicle whose tow ball is nearest this coupler, if one is close enough and free. */
    private @Nullable CarEntity nearestTractor() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 coupler = this.position().add(
                -Mth.sin(yaw) * this.couplerReach(), 0.0, Mth.cos(yaw) * this.couplerReach());
        List<CarEntity> near = this.level().getEntitiesOfClass(CarEntity.class,
                this.getBoundingBox().inflate(8.0), car -> car != this && !(car instanceof TrailerEntity)
                        && !car.isRemoved() && !car.isTowing(this));
        CarEntity best = null;
        double bestDistance = HITCH_REACH * HITCH_REACH;
        for (CarEntity car : near) {
            double distance = car.hitchPoint().distanceToSqr(coupler.x, car.hitchPoint().y, coupler.z);
            if (distance < bestDistance) {
                best = car;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---- the load goes where the trailer goes ----

    @Override
    protected void spillCargo(ServerLevel world) {
        Containers.dropContents(world, (Entity) this, this.load);
        this.load.clearContent();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        ContainerHelper.loadAllItems(view, this.load.getItems());
        this.tractorUuid = view.read("Tractor", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        ContainerHelper.saveAllItems(view, this.load.getItems());
        if (this.tractorUuid != null && this.isHitched()) {
            view.store("Tractor", UUIDUtil.CODEC, this.tractorUuid);
        }
    }
}
