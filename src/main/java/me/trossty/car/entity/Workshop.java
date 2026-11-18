package me.trossty.car.entity;

import me.trossty.car.block.EngineHoistBlock;
import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.block.entity.EngineHoistBlockEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Working on the car: what has to come off before a part can be reached or
 * even seen, where the car has to be for the job (up on a lift, bonnet open,
 * a hoist beside it), and taking parts off and fitting them, by hand or from
 * the service screen.
 */
public final class Workshop {

    private final CarEntity car;

    Workshop(CarEntity car) {
        this.car = car;
    }

    /** Takes the tyre off the wheel the player points at, and hands it over. */
    private InteractionResult removeTyre(Player player) {
        Component name = Component.translatable("part.car.wheel");
        int wheel = this.car.aimedWheel(player, true);
        if (wheel < 0) {
            player.displayClientMessage(Component.translatable("message.car.part_absent",
                    Component.translatable("part.car." + CarPart.WHEELS.key())), true);
            return InteractionResult.CONSUME;
        }
        float condition = this.car.parts().getCondition(CarPart.WHEELS, wheel);
        CarPart.Quality quality = this.car.parts().getQuality(CarPart.WHEELS);
        int variant = this.car.getVariant(CarPart.WHEELS);
        if (this.car.getFlatTyre() == wheel) {
            // The flat one off: there is nothing flat on the car now.
            this.car.getEntityData().set(CarEntity.FLAT_TYRE, (byte) -1);
        }
        this.car.getEntityData().set(CarEntity.TYRES_OFF, (byte) (this.car.tyresOff() | 1 << wheel));
        if (this.car.tyresOn() == 0) {
            // The last one off: the slot is empty, as for any part taken off.
            this.car.parts().setCondition(CarPart.WHEELS, 0.0F);
            this.car.parts().setQuality(CarPart.WHEELS, CarPart.Quality.DEFAULT);
            this.car.setVariant(CarPart.WHEELS, 0);
        } else {
            // The set is now the ones left on.
            this.car.parts().settlePlaces(CarPart.WHEELS);
        }
        this.handAside(player, asStack(CarPart.WHEELS, condition, quality, variant, 1));
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.part_removed", name,
                Math.round(condition / CarPart.MAX_CONDITION * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Puts one tyre on the wheel the player points at: on a bare hub it goes
     * on, over a tyre it is a swap. All the tyres on a car are one kind, so
     * a different kind goes on only once the others are off. Each tyre keeps
     * its own wear, so a new one among worn ones stays new.
     */
    private InteractionResult fitTyre(Player player, ItemStack spare) {
        Component name = Component.translatable("part.car.wheel");
        int wheel = this.car.aimedWheel(player, null);
        float fitting = conditionOf(spare);
        CarPart.Quality fittingQuality = qualityOf(spare);
        int fittingVariant = CarEntity.variantOf(spare);
        boolean swap = this.car.isTyreOn(wheel);
        float now = swap ? this.car.parts().getCondition(CarPart.WHEELS, wheel) : 0.0F;
        int on = this.car.tyresOn();
        int others = swap ? on - 1 : on;
        if (others > 0 && fittingVariant != this.car.getVariant(CarPart.WHEELS)) {
            player.displayClientMessage(Component.translatable("message.car.tyre_mixed"), true);
            return InteractionResult.CONSUME;
        }
        if (swap && now >= CarPart.MAX_CONDITION && fittingQuality == this.car.parts().getQuality(CarPart.WHEELS)
                && fittingVariant == this.car.getVariant(CarPart.WHEELS)) {
            player.displayClientMessage(Component.translatable("message.car.part_already_new", name), true);
            return InteractionResult.CONSUME;
        }
        if (swap) {
            this.handAside(player, asStack(CarPart.WHEELS, now, this.car.parts().getQuality(CarPart.WHEELS),
                    this.car.getVariant(CarPart.WHEELS), 1));
            if (this.car.getFlatTyre() == wheel) {
                this.car.getEntityData().set(CarEntity.FLAT_TYRE, (byte) -1);
            }
        }
        if (others == 0) {
            this.car.parts().setCondition(CarPart.WHEELS, fitting);
            this.car.parts().setQuality(CarPart.WHEELS, fittingQuality);
            this.car.setVariant(CarPart.WHEELS, fittingVariant);
            if (!swap) {
                // The first one on a bare car: the rest are still off.
                this.car.getEntityData().set(CarEntity.TYRES_OFF, (byte) (((1 << this.car.tyreCount()) - 1) & ~(1 << wheel)));
            }
        } else {
            this.car.getEntityData().set(CarEntity.TYRES_OFF, (byte) (this.car.tyresOff() & ~(1 << wheel)));
            this.car.parts().setCondition(CarPart.WHEELS, wheel, Math.max(CarPart.WORN_OUT, fitting));
        }
        if (!player.isCreative()) {
            spare.shrink(1);
        }
        this.playWrench();
        int fittedPercent = Math.round(fitting / CarPart.MAX_CONDITION * 100.0F);
        player.displayClientMessage(swap
                ? Component.translatable("message.car.part_fitted", name,
                        Math.round(now / CarPart.MAX_CONDITION * 100.0F), fittedPercent)
                : Component.translatable("message.car.part_fitted_new", name, fittedPercent), true);
        return InteractionResult.CONSUME;
    }

    /**
     * The part in the way of this one, or null if it can be reached. Only the
     * outermost one is named: there is no point telling somebody to take the
     * clutch out when the driveshaft is still bolted on.
     */
    public CarPart blockedBy(CarPart part) {
        // blockedBy() lists them outermost first, and the outermost is the one
        // worth naming: there is no point telling somebody to take the clutch
        // out while the driveshaft is still bolted on.
        for (CarPart above : part.blockedBy()) {
            if (this.car.parts().isFitted(above)) {
                return above;
            }
        }
        return null;
    }

    /**
     * What is bolted over this part and hides how worn it is, or null when it
     * can be seen. A mechanic reads a pad through the gap a wheel leaves, a
     * disc behind its pads, but not a clutch through the gearbox in front of
     * it or a bore with its plug still in: those are judged by what the car
     * does, the reader and the gauges, until they are out in the open.
     */
    public @Nullable CarPart hiddenBy(CarPart part) {
        for (CarPart above : part.blockedBy()) {
            if (!this.car.parts().isFitted(above) || above == CarPart.BRAKE_PADS) {
                continue;
            }
            if (above == CarPart.WHEELS && this.car.tyresOn() < this.car.tyreCount()) {
                // One wheel off is enough to see behind it.
                continue;
            }
            return above;
        }
        return null;
    }

    public boolean canInspect(CarPart part) {
        return this.hiddenBy(part) == null;
    }

    /** Whether this part can be touched at all right now. */
    public boolean canReach(CarPart part) {
        return this.car.hasPart(part) && this.blockedBy(part) == null;
    }

    /**
     * Whether a part is got at from above, under the bonnet: what is in the
     * engine bay and the electrics, on a vehicle that has a bonnet. Those
     * want the bonnet up and no lift; everything else is got at from
     * underneath, and wants the car up in the air.
     */
    public boolean underBonnet(CarPart part) {
        return (part.group() == CarPart.Group.ENGINE_BAY || part.group() == CarPart.Group.ELECTRICS)
                && this.car.hasLid(CarEntity.BONNET_BIT);
    }

    /**
     * Whether a part can be worked on where the car is now: an under-bonnet
     * part with the bonnet up, the engine itself only with a hoist's hook over
     * the open bay as well, anything else on a raised lift.
     */
    public boolean isWorkable(CarPart part) {
        if (!this.underBonnet(part)) {
            return VehicleLiftBlock.isServicing(this.car.level(), this.car);
        }
        if (this.onTheHoist(part)) {
            return this.car.isLidOpen(CarEntity.BONNET_BIT) && EngineHoistBlock.isNear(this.car.level(), this.car);
        }
        return this.car.isLidOpen(CarEntity.BONNET_BIT);
    }

    /**
     * Whether this part comes out and goes in on a hoist's hook rather than
     * by hand: the engine of a car with a bonnet, which is lifted out of the
     * top. Without a bonnet — a motorcycle, a boat — it comes off the lift by hand.
     */
    public boolean onTheHoist(CarPart part) {
        return part == CarPart.ENGINE && this.underBonnet(part);
    }

    /** Whether any part at all can be worked on where the car is now. */
    public boolean isWorkable() {
        return VehicleLiftBlock.isServicing(this.car.level(), this.car)
                || this.car.hasLid(CarEntity.BONNET_BIT) && this.car.isLidOpen(CarEntity.BONNET_BIT);
    }

    /** Whether this part can be taken off or fitted, here and now. */
    public boolean canWorkOn(CarPart part) {
        return this.canReach(part) && this.isWorkable(part);
    }

    /** What to tell somebody who cannot get at this part where the car is. */
    public Component cannotWork(CarPart part) {
        if (!this.underBonnet(part)) {
            return VehicleLiftBlock.needsLift(this.car);
        }
        if (!this.car.isLidOpen(CarEntity.BONNET_BIT)) {
            return Component.translatable("message.car.open_bonnet");
        }
        return Component.translatable("message.car.needs_hoist");
    }

    /** What is inside the engine, to go with it off the car; null for any other part, or none in. */
    private EngineInternals.@Nullable Snapshot insideOf(CarPart part) {
        return part == CarPart.ENGINE && this.car.parts().isFitted(part) ? this.car.parts().engine().snapshot() : null;
    }

    /** The stack a part in the given condition comes off as: {@code count} of it, a set or one. */
    private static ItemStack asStack(CarPart part, float condition, CarPart.Quality quality,
                                     int variant, int count) {
        ItemStack stack = new ItemStack(part.item(), count);
        if (variant != 0) {
            stack.set(ModComponents.PART_VARIANT, variant);
        }
        if (condition < CarPart.MAX_CONDITION) {
            stack.set(ModComponents.PART_CONDITION, Math.max(1, Math.round(condition)));
        }
        if (quality != CarPart.Quality.DEFAULT) {
            stack.set(ModComponents.PART_QUALITY, quality.ordinal());
        }
        return stack;
    }

    /**
     * Takes a part off and hands it over. The car keeps nothing: whatever state
     * it was in is what comes out, and the slot is left empty, which is what
     * makes the part behind it reachable.
     */
    public InteractionResult removePart(Player player, CarPart part) {
        return this.removePart(player, part, null);
    }

    /**
     * Takes a part off; an engine that comes out on a hoist goes onto the
     * hook of the one given, or of the nearest with an empty hook over the bay
     * when none is, never into the player's pockets.
     */
    public InteractionResult removePart(Player player, CarPart part, @Nullable EngineHoistBlockEntity hoist) {
        if (!this.car.hasPart(part)) {
            // Not a part this vehicle has: a trailer has no engine to fit.
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.car.isLocked()) {
            // Whichever window or click it came from, a locked car keeps its parts.
            player.displayClientMessage(Component.translatable("message.car.locked"), true);
            return InteractionResult.CONSUME;
        }
        Component name = Component.translatable("part.car." + part.key());
        if (!this.isWorkable(part)) {
            player.displayClientMessage(this.cannotWork(part), true);
            return InteractionResult.CONSUME;
        }
        if (!this.car.parts().isFitted(part)) {
            player.displayClientMessage(Component.translatable("message.car.part_absent", name), true);
            return InteractionResult.CONSUME;
        }
        CarPart inTheWay = this.blockedBy(part);
        if (inTheWay != null) {
            player.displayClientMessage(Component.translatable("message.car.part_blocked",
                    Component.translatable("part.car." + inTheWay.key()), name), true);
            return InteractionResult.CONSUME;
        }
        if (part == CarPart.WHEELS) {
            return this.removeTyre(player);
        }
        boolean hooked = this.onTheHoist(part);
        if (hooked) {
            if (hoist == null) {
                hoist = EngineHoistBlock.hoistOver(this.car.level(), this.car, true);
            }
            if (hoist == null || hoist.isHolding()) {
                // Every hook over the bay has an engine on it already.
                player.displayClientMessage(Component.translatable("message.car.hoist_full"), true);
                return InteractionResult.CONSUME;
            }
        }

        float condition = this.car.parts().getCondition(part);
        CarPart.Quality quality = this.car.parts().getQuality(part);
        EngineInternals.Snapshot inside = this.insideOf(part);
        this.car.parts().setCondition(part, 0.0F);
        // An empty slot has no quality to speak of, so it goes back to the
        // yardstick rather than keeping the last part's grade.
        this.car.parts().setQuality(part, CarPart.Quality.DEFAULT);
        int variant = this.car.getVariant(part);
        boolean turbo = part == CarPart.ENGINE && CarSetup.turbo(this.car.getSetup());
        this.car.setVariant(part, 0);
        ItemStack out = asStack(part, condition, quality, variant, this.car.parts().countOf(part));
        if (inside != null) {
            inside.stamp(out);
        }
        if (hooked) {
            hoist.setEngine(out);
        } else {
            this.handAside(player, out);
        }
        if (turbo) {
            // The turbo is bolted to the engine, so it comes away with it.
            this.car.setTurbo(false);
            this.handAside(player, new ItemStack(ModItems.TURBO));
        }
        this.playWrench();
        int percent = Math.round(condition / CarPart.MAX_CONDITION * 100.0F);
        player.displayClientMessage(hooked ? Component.translatable("message.car.engine_lifted", percent)
                : Component.translatable("message.car.part_removed", name, percent), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Puts what came off into the player's pockets, but not into the hand they
     * are working with: a part that landed in an empty hand would go straight
     * back on at the next click, and the click after that would take it off
     * again, which feels like the car never let go of it. What does not fit
     * is dropped at the car.
     */
    private void handAside(Player player, ItemStack stack) {
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        int hand = inventory.selected;
        int size = net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE;
        // Onto a stack of the same first, then into the first empty pocket,
        // the backpack before the hotbar.
        for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
            ItemStack there = inventory.getItem(slot);
            if (slot != hand && ItemStack.isSameItemSameComponents(there, stack)
                    && there.getCount() < there.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                there.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            int slot = (i + net.minecraft.world.entity.player.Inventory.getSelectionSize()) % size;
            if (slot != hand && inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                stack.setCount(0);
            }
        }
        if (!stack.isEmpty() && this.car.level() instanceof ServerLevel world) {
            this.car.spawnAtLocation(stack);
        }
    }

    /**
     * Fitting a part, which is the other half of what a lift is for. Away from
     * one the click is a question — it reads the fitted part's condition back —
     * and over a raised one it is the job.
     *
     * <p>Into an empty slot it simply goes in. Onto an occupied one it is a
     * swap, and the old part comes out in whatever state it was in, so you can
     * refit it later if you are the sort who keeps a shed.
     */
    public InteractionResult fitPart(Player player, ItemStack spares, CarPart part) {
        return this.fitPart(player, spares, part, null);
    }

    /**
     * Fits a part; an engine that goes in on a hoist goes in only off the
     * hook of the one given, whose engine {@code spares} is, and only into an
     * empty bay — the old one comes out on a hook first.
     */
    public InteractionResult fitPart(Player player, ItemStack spares, CarPart part,
                                     @Nullable EngineHoistBlockEntity hoist) {
        if (!this.car.hasPart(part)) {
            // Not a part this vehicle has: a trailer has no engine to fit.
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.car.isLocked()) {
            // Whichever window or click it came from, a locked car keeps its parts.
            player.displayClientMessage(Component.translatable("message.car.locked"), true);
            return InteractionResult.CONSUME;
        }
        if (part == CarPart.WHEELS && this.car.hasFlatTyre()) {
            // One wheel off and the spare on, where it stopped: no lift for that.
            this.car.parts().setCondition(CarPart.WHEELS, this.car.getFlatTyre(), Math.max(CarPart.WORN_OUT, conditionOf(spares)));
            this.car.getEntityData().set(CarEntity.FLAT_TYRE, (byte) -1);
            spares.consume(1, player);
            this.car.playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.car.tyre_changed"), true);
            return InteractionResult.CONSUME;
        }
        Component name = Component.translatable("part.car." + part.key());
        int fittedNow = Math.round(this.car.parts().getConditionFraction(part) * 100.0F);

        if (!this.isWorkable(part)) {
            // A closed bonnet says what is wrong; off a lift, what it reads.
            CarPart hidden = this.hiddenBy(part);
            player.displayClientMessage(this.underBonnet(part) ? this.cannotWork(part)
                    : hidden != null ? Component.translatable("message.car.part_hidden", name,
                            Component.translatable("part.car." + hidden.key()))
                    : Component.translatable("message.car.part_reading", name, fittedNow), true);
            return InteractionResult.CONSUME;
        }
        CarPart inTheWay = this.blockedBy(part);
        if (inTheWay != null) {
            player.displayClientMessage(Component.translatable("message.car.part_blocked",
                    Component.translatable("part.car." + inTheWay.key()), name), true);
            return InteractionResult.CONSUME;
        }
        if (part == CarPart.WHEELS) {
            return this.fitTyre(player, spares);
        }
        if (this.onTheHoist(part)) {
            if (hoist == null) {
                // Too heavy to hold up and bolt in: it goes in on the hook.
                player.displayClientMessage(Component.translatable("message.car.engine_by_hoist"), true);
                return InteractionResult.CONSUME;
            }
            if (this.car.parts().isFitted(part)) {
                player.displayClientMessage(Component.translatable("message.car.engine_bay_full"), true);
                return InteractionResult.CONSUME;
            }
        }
        if (spares.getCount() < this.car.parts().countOf(part)) {
            player.displayClientMessage(
                    Component.translatable("message.car.part_needs", this.car.parts().countOf(part), name), true);
            return InteractionResult.CONSUME;
        }

        float removed = this.car.parts().getCondition(part);
        CarPart.Quality removedQuality = this.car.parts().getQuality(part);
        EngineInternals.Snapshot removedInside = this.insideOf(part);
        float fitting = conditionOf(spares);
        CarPart.Quality fittingQuality = qualityOf(spares);
        int removedVariant = this.car.getVariant(part);
        int fittingVariant = CarEntity.variantOf(spares);
        if (removed >= CarPart.MAX_CONDITION
                && fittingQuality == removedQuality
                // A different kind of part is a reason to swap a sound one.
                && fittingVariant == removedVariant) {
            // Nothing to gain and a part to lose: a misclick in the garage
            // should not cost you an engine. A better grade is something to
            // gain, though, so that one goes through — otherwise the only way
            // to put a good gearbox in a sound car would be to wear the
            // standard one out first.
            player.displayClientMessage(Component.translatable("message.car.part_already_new", name), true);
            return InteractionResult.CONSUME;
        }
        this.car.parts().setCondition(part, fitting);
        this.car.parts().setQuality(part, fittingQuality);
        this.car.setVariant(part, fittingVariant);
        if (part == CarPart.ENGINE) {
            // Read before the hook lets go of it: the engine is what it was inside.
            this.car.parts().engine().set(EngineInternals.Snapshot.of(spares));
            fitting = this.car.parts().getCondition(part);
        }
        if (hoist != null) {
            // Off the hook and into the bay, creative or not: it is the same engine.
            hoist.takeEngine();
        } else if (!player.isCreative()) {
            spares.shrink(this.car.parts().countOf(part));
        }

        int fittedPercent = Math.round(fitting / CarPart.MAX_CONDITION * 100.0F);
        if (removed <= 0.0F) {
            // The slot was empty, so there is nothing to hand back.
            this.playWrench();
            player.displayClientMessage(
                    Component.translatable("message.car.part_fitted_new", name, fittedPercent), true);
            return InteractionResult.CONSUME;
        }
        // The old one goes into your pockets rather than on the floor.
        ItemStack old = asStack(part, removed, removedQuality, removedVariant, this.car.parts().countOf(part));
        if (removedInside != null) {
            removedInside.stamp(old);
        }
        this.handAside(player, old);
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.part_fitted", name,
                fittedNow, fittedPercent), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Bolts a turbo to the engine. It is not a slot of its own: it goes on
     * the engine that is there, and it comes off when that engine does.
     */
    public InteractionResult fitTurbo(Player player, ItemStack turbo) {
        // A turbo is driven by exhaust, and a motor has none.
        if (!this.car.hasPart(CarPart.ENGINE) || this.car.isElectric()) {
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // The turbo bolts on in the engine bay, from above.
        if (!this.isWorkable(CarPart.AIR_FILTER)) {
            player.displayClientMessage(this.cannotWork(CarPart.AIR_FILTER), true);
            return InteractionResult.CONSUME;
        }
        if (!this.car.parts().isFitted(CarPart.ENGINE)) {
            player.displayClientMessage(Component.translatable("message.car.turbo_no_engine"), true);
            return InteractionResult.CONSUME;
        }
        if (this.car.hasTurbo()) {
            player.displayClientMessage(Component.translatable("message.car.turbo_already"), true);
            return InteractionResult.CONSUME;
        }
        this.car.setTurbo(true);
        if (!player.isCreative()) {
            turbo.shrink(1);
        }
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.turbo_fitted"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * The best spare the player is carrying for this part, or an empty stack.
     * Grade first, then condition: a good part that has done some work beats a
     * cheap one in its wrapper, because the cheap one will be worn out again
     * before the good one has got going.
     */
    public static ItemStack bestSpare(Player player, CarPart part) {
        ItemStack best = ItemStack.EMPTY;
        int bestGrade = -1;
        float bestCondition = -1.0F;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(part.item()) || stack.getCount() < part.count()) {
                continue;
            }
            int grade = qualityOf(stack).rank();
            float condition = conditionOf(stack);
            if (grade > bestGrade || (grade == bestGrade && condition > bestCondition)) {
                best = stack;
                bestGrade = grade;
                bestCondition = condition;
            }
        }
        return best;
    }

    /**
     * One part, one click, no window: what is there comes off, what is missing
     * goes on. This is what the hands-on service mode calls when the player
     * points at a part on the car itself and right clicks it.
     *
     * <p>It decides nothing of its own — the lift, the teardown order and what
     * counts as a spare are all still {@link #removePart} and {@link #fitPart}.
     */
    public InteractionResult serviceByHand(Player player, CarPart part) {
        // A locked car keeps its parts: the service packet is as much a way in
        // as a click is, so it is turned away at the same door.
        if (this.car.isLocked()) {
            if (!this.car.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.locked"), true);
            }
            return InteractionResult.CONSUME;
        }
        // What goes on is what is in your hand: an empty slot stays empty
        // until you pick the part up and offer it, so a slot you have just
        // emptied cannot quietly fill itself again on the next click.
        ItemStack held = player.getMainHandItem();
        boolean offering = held.is(part.item());
        if (part == CarPart.WHEELS && this.car.parts().isFitted(part)) {
            int wheel = this.car.aimedWheel(player, null);
            if (wheel >= 0 && !this.car.isTyreOn(wheel) && !offering) {
                if (!this.car.level().isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.car.part_hold_spare",
                            Component.translatable("part.car." + part.key())), true);
                }
                return InteractionResult.CONSUME;
            }
        }
        if (this.car.parts().isFitted(part)) {
            // A spare in hand on a fitted part is a swap; an empty hand takes it off.
            return offering ? this.fitPart(player, held, part) : this.removePart(player, part);
        }
        if (!offering) {
            if (!this.car.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.part_hold_spare",
                        Component.translatable("part.car." + part.key())), true);
            }
            return InteractionResult.CONSUME;
        }
        return this.fitPart(player, held, part);
    }

    /**
     * Opens the service window on this car. The clipboard is the proper way in;
     * crouching at it is the shortcut for somebody already holding a spanner's
     * worth of parts.
     */
    public InteractionResult openService(Player player) {
        // Nothing to read off a car standing shut on its wheels: the sheet
        // is filled in on the lift, or with the bonnet up, where the parts
        // can be seen.
        if (!this.isWorkable()) {
            if (!this.car.level().isClientSide()) {
                player.displayClientMessage(VehicleLiftBlock.needsLift(this.car), true);
            }
            return InteractionResult.sidedSuccess(this.car.level().isClientSide());
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, opener) ->
                        new me.trossty.car.service.CarServiceScreenHandler(syncId, inventory, this.car.getId()),
                this.car.getDisplayName()));
        return InteractionResult.CONSUME;
    }

    private void playWrench() {
        this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.7F, 1.5F);
    }

    /** A spare with no condition on it is a new one. */
    public static float conditionOf(ItemStack spare) {
        Integer stored = spare.get(ModComponents.PART_CONDITION);
        return stored == null ? CarPart.MAX_CONDITION : stored;
    }

    /** A spare with no grade on it is a standard one. */
    public static CarPart.Quality qualityOf(ItemStack spare) {
        Integer stored = spare.get(ModComponents.PART_QUALITY);
        return stored == null ? CarPart.Quality.DEFAULT : CarPart.Quality.byOrdinal(stored);
    }
}
