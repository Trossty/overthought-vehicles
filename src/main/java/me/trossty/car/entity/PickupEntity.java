package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

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

    private final SimpleInventory bed = new SimpleInventory(BED_SLOTS);

    public PickupEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    public Inventory getBed() {
        return this.bed;
    }

    /** 0 with an empty bed, 1 with every slot occupied. */
    public float getLoad() {
        int used = 0;
        for (int slot = 0; slot < this.bed.size(); slot++) {
            if (!this.bed.getStack(slot).isEmpty()) {
                used++;
            }
        }
        return (float) used / BED_SLOTS;
    }

    @Override
    protected Item asItem() {
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
    protected net.minecraft.util.math.Vec3d plainSeatPosition(int seat) {
        if (seat < 2) {
            return super.plainSeatPosition(seat);
        }
        return new net.minecraft.util.math.Vec3d(
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
        if (this.getEntityWorld().isClient() || this.isRemoved()) {
            return;
        }
        if (this.age % SWEEP_INTERVAL == 0) {
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
        Box reach = this.getBoundingBox().expand(SWEEP_REACH);
        List<ItemEntity> loose = this.getEntityWorld().getEntitiesByClass(
                ItemEntity.class, reach, item -> !item.isRemoved() && !item.cannotPickup());
        if (loose.isEmpty()) {
            return;
        }

        boolean tookAnything = false;
        for (ItemEntity item : loose) {
            ItemStack left = this.bed.addStack(item.getStack().copy());
            if (left.getCount() == item.getStack().getCount()) {
                continue;
            }
            tookAnything = true;
            if (left.isEmpty()) {
                item.discard();
            } else {
                item.setStack(left);
            }
        }
        if (tookAnything) {
            this.getEntityWorld().playSound(null, this.getBlockPos(),
                    SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.NEUTRAL, 0.3F, 1.6F);
        }
    }

    // ---- sneaking is the bed ----

    @Override
    protected ActionResult interactUnlocked(PlayerEntity player, Hand hand) {
        if (!player.shouldCancelInteraction()) {
            return super.interactUnlocked(player, hand);
        }
        if (this.getEntityWorld().isClient()) {
            return ActionResult.SUCCESS;
        }
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) ->
                        GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this.bed),
                Text.translatable("container.car.pickup_bed")));
        return ActionResult.CONSUME;
    }

    /** Wrecked, cleared away or killed, the bed tips out where it stood. */
    @Override
    protected void spillCargo(ServerWorld world) {
        ItemScatterer.spawn(world, (Entity) this, this.bed);
        this.bed.clear();
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        Inventories.readData(view, this.bed.getHeldStacks());
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        Inventories.writeData(view, this.bed.getHeldStacks());
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.PICKUP;
    }
}
