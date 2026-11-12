package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
    private static final TrackedData<Integer> TRACTOR =
            DataTracker.registerData(TrailerEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private final SimpleInventory load = new SimpleInventory(LOAD_SLOTS);

    /** Kept for the save file: entity ids do not survive a reload. */
    private @Nullable UUID tractorUuid;

    public TrailerEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(TRACTOR, -1);
    }

    public Inventory getLoad() {
        return this.load;
    }

    /** 0 empty, 1 with every slot taken. */
    public float getLoadFraction() {
        int used = 0;
        for (int slot = 0; slot < this.load.size(); slot++) {
            if (!this.load.getStack(slot).isEmpty()) {
                used++;
            }
        }
        return (float) used / LOAD_SLOTS;
    }

    /** What the tractor's top speed is multiplied by while it pulls this. */
    public float towFactor() {
        return MathHelper.lerp(this.getLoadFraction(), EMPTY_TOW_FACTOR, LADEN_TOW_FACTOR);
    }

    public @Nullable CarEntity getTractor() {
        int id = this.dataTracker.get(TRACTOR);
        if (id < 0) {
            return null;
        }
        return this.getEntityWorld().getEntityById(id) instanceof CarEntity car && !car.isRemoved()
                && !(car instanceof TrailerEntity) ? car : null;
    }

    /** Unhitched, the nose drops onto the jockey wheel. */
    @Override
    protected float restPoseTarget() {
        return this.isHitched() ? 0.0F : 1.0F;
    }

    public boolean isHitched() {
        return this.dataTracker.get(TRACTOR) >= 0;
    }

    private void hitchTo(@Nullable CarEntity tractor) {
        this.dataTracker.set(TRACTOR, tractor == null ? -1 : tractor.getId());
        this.tractorUuid = tractor == null ? null : tractor.getUuid();
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
        return other != null && this.dataTracker.get(TRACTOR) == other.getId();
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
    protected Item asItem() {
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
        if (!this.getEntityWorld().isClient()) {
            if (this.tractorUuid != null && !this.isHitched()
                    && this.getEntityWorld() instanceof ServerWorld server
                    && server.getEntity(this.tractorUuid) instanceof CarEntity found
                    && !(found instanceof TrailerEntity)) {
                // Loaded back in: the tractor has a new entity id now.
                this.hitchTo(found);
            }
            if (this.isHitched() && this.getTractor() == null && this.tractorUuid != null
                    && this.getEntityWorld() instanceof ServerWorld server
                    && server.getEntity(this.tractorUuid) == null && this.age > 100) {
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
        Vec3d ball = tractor.hitchPoint();
        double dx = ball.x - this.getX();
        double dz = ball.z - this.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length > BREAK_DISTANCE + this.couplerReach()) {
            if (!this.getEntityWorld().isClient()) {
                this.hitchTo(null);
                this.playSound(SoundEvents.BLOCK_CHAIN_BREAK, 1.0F, 1.0F);
            }
            return;
        }
        double dirX;
        double dirZ;
        if (length < 1.0E-4) {
            float yaw = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
            dirX = -MathHelper.sin(yaw);
            dirZ = MathHelper.cos(yaw);
        } else {
            dirX = dx / length;
            dirZ = dz / length;
        }
        double reach = this.couplerReach();
        double y = MathHelper.lerp(0.5, this.getY(), tractor.getY());
        // Moved, not placed, so a trailer dragged into a wall stops at it; if
        // it is held back far enough the coupling breaks, as above.
        this.move(net.minecraft.entity.MovementType.SELF, new Vec3d(
                ball.x - dirX * reach - this.getX(), y - this.getY(), ball.z - dirZ * reach - this.getZ()));
        this.setYaw((float) Math.toDegrees(Math.atan2(-dirX, dirZ)));
        this.setVelocity(Vec3d.ZERO);
    }

    /** From the middle of the trailer forward to the tip of its drawbar, in blocks. */
    private double couplerReach() {
        return this.frontReach();
    }

    // ---- clicking: hitch, unhitch, or open the load ----

    @Override
    protected ActionResult interactUnlocked(PlayerEntity player, Hand hand) {
        if (player.shouldCancelInteraction()) {
            if (this.getEntityWorld().isClient()) {
                return ActionResult.SUCCESS;
            }
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, ignored) ->
                            GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this.load),
                    Text.translatable("container.car.trailer")));
            return ActionResult.CONSUME;
        }
        if (!player.getStackInHand(hand).isEmpty()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.getEntityWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        if (this.isHitched()) {
            this.hitchTo(null);
            this.playSound(SoundEvents.BLOCK_CHAIN_HIT, 1.0F, 0.8F);
            player.sendMessage(Text.translatable("message.car.trailer_unhitched"), true);
            return ActionResult.CONSUME;
        }
        CarEntity tractor = this.nearestTractor();
        if (tractor == null) {
            player.sendMessage(Text.translatable("message.car.trailer_no_vehicle"), true);
            return ActionResult.CONSUME;
        }
        this.hitchTo(tractor);
        this.follow(tractor);
        this.playSound(SoundEvents.BLOCK_CHAIN_PLACE, 1.0F, 1.0F);
        this.playSound(SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 0.6F, 1.4F);
        player.sendMessage(Text.translatable("message.car.trailer_hitched", tractor.getDisplayName()), true);
        return ActionResult.CONSUME;
    }

    /** The vehicle whose tow ball is nearest this coupler, if one is close enough and free. */
    private @Nullable CarEntity nearestTractor() {
        float yaw = this.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        Vec3d coupler = this.getEntityPos().add(
                -MathHelper.sin(yaw) * this.couplerReach(), 0.0, MathHelper.cos(yaw) * this.couplerReach());
        List<CarEntity> near = this.getEntityWorld().getEntitiesByClass(CarEntity.class,
                this.getBoundingBox().expand(8.0), car -> car != this && !(car instanceof TrailerEntity)
                        && !car.isRemoved() && !car.isTowing(this));
        CarEntity best = null;
        double bestDistance = HITCH_REACH * HITCH_REACH;
        for (CarEntity car : near) {
            double distance = car.hitchPoint().squaredDistanceTo(coupler.x, car.hitchPoint().y, coupler.z);
            if (distance < bestDistance) {
                best = car;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---- the load goes where the trailer goes ----

    @Override
    protected void spillCargo(ServerWorld world) {
        ItemScatterer.spawn(world, (Entity) this, this.load);
        this.load.clear();
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        Inventories.readData(view, this.load.getHeldStacks());
        this.tractorUuid = view.read("Tractor", Uuids.INT_STREAM_CODEC).orElse(null);
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        Inventories.writeData(view, this.load.getHeldStacks());
        if (this.tractorUuid != null && this.isHitched()) {
            view.put("Tractor", Uuids.INT_STREAM_CODEC, this.tractorUuid);
        }
    }
}
