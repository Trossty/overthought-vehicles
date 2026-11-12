package me.trossty.car.service;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * The service window: every part on one car, what state it is in, and the two
 * buttons that are the whole job.
 *
 * <p>It owns no slots. Nothing is dragged in or out — the car's own
 * {@link CarEntity#fitPart} and {@link CarEntity#removePart} reach into the
 * player's inventory, and they are the same two methods a plain right click
 * goes through, so the rules about lifts and what is bolted on top of what are
 * stated once and obeyed everywhere.
 *
 * <p>The conditions themselves are never sent through here. They are tracked
 * data on the car, so every client that can see the car already has them; the
 * only thing this window has to carry across is which car it is about.
 */
public class CarServiceScreenHandler extends ScreenHandler {

    /** Button ids above the part list. */
    public static final int REMOVE_BUTTON = 100;
    public static final int FIT_BUTTON = 101;

    private final PlayerInventory inventory;
    /** The car's entity id, so the client can find the same car. */
    private final Property car = Property.create();
    private final Property selected = Property.create();

    public CarServiceScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, 0);
    }

    public CarServiceScreenHandler(int syncId, PlayerInventory inventory, int carId) {
        super(ModScreenHandlers.CAR_SERVICE, syncId);
        this.inventory = inventory;
        this.car.set(carId);
        this.addProperty(this.car);
        this.addProperty(this.selected);
    }

    /** The car this window is about, or null while the id has not arrived. */
    public CarEntity car() {
        if (this.car.get() == 0) {
            return null;
        }
        Entity entity = this.inventory.player.getEntityWorld().getEntityById(this.car.get());
        return entity instanceof CarEntity found && !found.isRemoved() ? found : null;
    }

    public CarPart selectedPart() {
        int index = this.selected.get();
        CarPart[] parts = CarPart.values();
        return parts[index < 0 || index >= parts.length ? 0 : index];
    }

    /**
     * The best spare the player has for this part, or an empty stack. Best
     * rather than first: nobody digs through a toolbox to fit the worse of two
     * gearboxes.
     *
     * <p>Grade outranks condition. A good part that has done some work is still
     * the one you want over a cheap one in its wrapper, because the cheap one
     * will be worn out again before the good one has got going.
     */
    public ItemStack spareFor(CarPart part) {
        ItemStack best = ItemStack.EMPTY;
        int bestGrade = -1;
        float bestCondition = -1.0F;
        for (int slot = 0; slot < this.inventory.size(); slot++) {
            ItemStack stack = this.inventory.getStack(slot);
            if (!stack.isOf(part.item()) || stack.getCount() < part.count()) {
                continue;
            }
            int grade = CarEntity.qualityOf(stack).ordinal();
            float condition = CarEntity.conditionOf(stack);
            if (grade > bestGrade || (grade == bestGrade && condition > bestCondition)) {
                best = stack;
                bestGrade = grade;
                bestCondition = condition;
            }
        }
        return best;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id < CarPart.values().length) {
            this.selected.set(id);
            return true;
        }
        CarEntity car = this.car();
        if (car == null) {
            return false;
        }
        CarPart part = this.selectedPart();
        if (id == REMOVE_BUTTON) {
            car.removePart(player, part);
            return true;
        }
        if (id == FIT_BUTTON) {
            ItemStack spares = this.spareFor(part);
            if (!spares.isEmpty()) {
                car.fitPart(player, spares, part);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        CarEntity car = this.car();
        return car != null && car.isAlive() && car.squaredDistanceTo(player) < 64.0;
    }

    /** No slots, so there is nothing to shift-click anywhere. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    /** Never called — kept because the superclass expects it to exist. */
    @Override
    public Slot getSlot(int index) {
        return super.getSlot(index);
    }
}
