package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.List;

/**
 * The pickup: a car with its back half cut off and a bed put there instead.
 *
 * <p>The tanker answered "how do I move fuel across a world". This answers the
 * other half of the same question — how do I move <em>everything else</em>.
 * {@link #BED_SLOTS} slots, a chest's worth, in a box you can drive.
 *
 * <h2>The bed is a chest</h2>
 * Sneak-click it and the ordinary double-less chest screen opens, because a
 * mod that invents its own grid for twenty-seven slots has invented nothing.
 * A plain click is still the vehicle: get in, pour a can in, paint it, mend it.
 *
 * <h2>It sweeps up what it drives over</h2>
 * Loose items the pickup touches go into the bed. That is the whole reason to
 * drive one back from a mining trip rather than walk: you do not stop, you do
 * not open anything, the drops simply end up in the back.
 *
 * <p>Nothing is lost when the bed is full — a sweep that cannot fit stays on
 * the ground, and a full pickup drives past its own spoils.
 *
 * <h2>A load slows it down</h2>
 * Empty it is nearly a car. Full it is {@link #LADEN_SPEED_FACTOR} of one, and
 * everything between is a straight line, so you feel the load without having to
 * read a number anywhere.
 *
 * <p>When the pickup is wrecked or cleared away the bed empties onto the ground,
 * the way a broken chest does. The load is in the bed, not in the item.
 */
public class PickupEntity extends CarEntity {

    /** A chest's worth. Any more and it would be a warehouse on wheels. */
    public static final int BED_SLOTS = 27;

    /** Ticks between sweeps. Every tick would be a vacuum cleaner, not a truck. */
    private static final int SWEEP_INTERVAL = 4;

    /** How far past the bodywork the bed reaches for loose items, in blocks. */
    private static final double SWEEP_REACH = 0.4;

    /** Top speed with an empty bed: a little down on a car, for the weight. */
    private static final float EMPTY_SPEED_FACTOR = 0.94F;

    /** Top speed with all {@link #BED_SLOTS} slots full. */
    private static final float LADEN_SPEED_FACTOR = 0.74F;

    private final SimpleContainer bed = new SimpleContainer(BED_SLOTS);

    public PickupEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    public Container getBed() {
        return this.bed;
    }

    /** 0 with an empty bed, 1 with every slot occupied. */
    public float getLoad() {
        int used = 0;
        for (int slot = 0; slot < this.bed.getContainerSize(); slot++) {
            if (!this.bed.getItem(slot).isEmpty()) {
                used++;
            }
        }
        return (float) used / BED_SLOTS;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.PICKUP;
    }

    /** How far back the bed is, and how high its floor rides. */
    private static final double BED_SEAT_BACK = -1.5;
    private static final double BED_SEAT_HEIGHT = 1.0;
    private static final double BED_SEAT_SIDEWAYS = 0.4;

    /** Two in the cab and two more sitting in the bed, on top of the load. */
    @Override
    protected int plainSeatCount() {
        return 4;
    }

    @Override
    protected net.minecraft.world.phys.Vec3 plainSeatPosition(int seat) {
        if (seat < 2) {
            return super.plainSeatPosition(seat);
        }
        return new net.minecraft.world.phys.Vec3(
                seat == 2 ? -BED_SEAT_SIDEWAYS : BED_SEAT_SIDEWAYS, BED_SEAT_HEIGHT, BED_SEAT_BACK);
    }

    /** A full bed is a third again at the pump, on top of whatever is on the hitch. */
    @Override
    public float loadThirst() {
        return super.loadThirst() * (1.0F + 0.3F * this.getLoad());
    }

    /** Straight line from empty to full, so the load is something you feel. */
    @Override
    public float getMaxSpeed() {
        float factor = EMPTY_SPEED_FACTOR
                + (LADEN_SPEED_FACTOR - EMPTY_SPEED_FACTOR) * this.getLoad();
        return super.getMaxSpeed() * factor;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() || this.isRemoved()) {
            return;
        }
        if (this.tickCount % SWEEP_INTERVAL == 0) {
            this.sweepUpLooseItems();
        }
    }

    /**
     * Anything loose the bodywork touches ends up in the bed. What will not fit
     * is put back on the ground rather than eaten.
     *
     * <p>Public so a test can run one sweep and look at the result in the same
     * tick. Waiting twenty ticks for the lorry to do it by itself turned out to
     * be waiting twenty ticks for somebody else's test player to walk past and
     * pocket the item first.
     */
    public void sweepUpLooseItems() {
        AABB reach = this.getBoundingBox().inflate(SWEEP_REACH);
        List<ItemEntity> loose = this.level().getEntitiesOfClass(
                ItemEntity.class, reach, item -> !item.isRemoved() && !item.hasPickUpDelay());
        if (loose.isEmpty()) {
            return;
        }

        boolean tookAnything = false;
        for (ItemEntity item : loose) {
            ItemStack left = this.bed.addItem(item.getItem().copy());
            if (left.getCount() == item.getItem().getCount()) {
                continue;
            }
            tookAnything = true;
            if (left.isEmpty()) {
                item.discard();
            } else {
                item.setItem(left);
            }
        }
        if (tookAnything) {
            this.level().playSound(null, this.blockPosition(),
                    SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.3F, 1.6F);
        }
    }

    // ---- sneaking is the bed ----

    @Override
    protected InteractionResult interactUnlocked(Player player, InteractionHand hand) {
        if (!player.isSecondaryUseActive()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, ignored) ->
                        ChestMenu.threeRows(syncId, playerInventory, this.bed),
                Component.translatable("container.car.pickup_bed")));
        return InteractionResult.CONSUME;
    }

    /** Wrecked, cleared away or killed, the bed tips out where it stood. */
    @Override
    protected void spillCargo(ServerLevel world) {
        Containers.dropContents(world, (Entity) this, this.bed);
        this.bed.clearContent();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        ContainerHelper.loadAllItems(view, this.bed.getItems());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        ContainerHelper.saveAllItems(view, this.bed.getItems());
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.PICKUP;
    }
}
