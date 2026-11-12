package me.trossty.car.block;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.item.JerryCans;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModItems;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What a block that just holds a liquid does when you click it with something:
 * a yard tank and a drum both pour and draw the same way, and read out the
 * same way. Server side only.
 */
public final class LiquidStores {

    private LiquidStores() {
    }

    /**
     * A jerry can, a bucket, a bottle, another mod's cell: pours in or draws off.
     * True if something was done with the item, in which case the click is spent.
     */
    public static boolean useWith(World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack held,
                                  SingleSlotStorage<FluidVariant> store) {
        if (held.isOf(ModItems.JERRY_CAN)) {
            jerryCan(world, pos, player, held, store);
            return true;
        }
        // The transfer API knows which way a bucket goes and what it holds.
        return FluidStorageUtil.interactWithFluidStorage(store, player, hand);
    }

    /** A jerry can: draws off fuel if it has room for this one, otherwise pours in what it holds. */
    private static void jerryCan(World world, BlockPos pos, PlayerEntity player, ItemStack can,
                                 SingleSlotStorage<FluidVariant> store) {
        FluidVariant held = store.getResource();
        Fluid inCan = JerryCans.isEmpty(can) ? null : canFluid(can);
        Fluid stored = store.isResourceBlank() ? null : held.getFluid();
        boolean canTake = stored != null && canHolds(stored) && (inCan == null || inCan == stored);

        if (!JerryCans.isFull(can) && canTake) {
            int available = (int) (store.getAmount() / ModFluids.UNIT);
            if (JerryCans.topUp(can, available, units -> move(store, stored, units, false)) > 0) {
                markCan(can, stored);
                playSound(world, pos, 1.1F);
            }
            return;
        }
        if (inCan != null) {
            if (stored != null && stored != inCan) {
                player.sendMessage(Text.translatable("message.car.tank_holds_other"), true);
                return;
            }
            int room = (int) ((store.getCapacity() - store.getAmount()) / ModFluids.UNIT);
            if (JerryCans.emptyInto(can, room, units -> move(store, inCan, units, true)) > 0) {
                playSound(world, pos, 0.8F);
            }
        }
    }

    /** Moves whole units of one liquid in or out; how many went. */
    public static int move(SingleSlotStorage<FluidVariant> store, Fluid liquid, int units, boolean in) {
        if (units <= 0 || Transaction.isOpen()) {
            return 0;
        }
        FluidVariant variant = FluidVariant.of(liquid);
        long droplets = units * ModFluids.UNIT;
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = in ? store.insert(variant, droplets, transaction)
                    : store.extract(variant, droplets, transaction);
            transaction.commit();
            return (int) (moved / ModFluids.UNIT);
        }
    }

    /** What liquid a jerry can's fuel is. */
    private static Fluid canFluid(ItemStack can) {
        CarSetup.Fuel fuel = CarEntity.fuelIn(can);
        if (CarEntity.isRaw(can)) {
            return fuel == CarSetup.Fuel.DIESEL ? ModFluids.RAW_DIESEL : ModFluids.RAW_PETROL;
        }
        return ModFluids.fluidOf(fuel);
    }

    /** Whether a jerry can is a thing this liquid can go into. */
    private static boolean canHolds(Fluid fluid) {
        return ModFluids.fuelOf(fluid) != null || fluid == ModFluids.RAW_PETROL || fluid == ModFluids.RAW_DIESEL;
    }

    private static void markCan(ItemStack can, Fluid fluid) {
        if (fluid == ModFluids.RAW_PETROL || fluid == ModFluids.RAW_DIESEL) {
            CarEntity.markFuel(can, fluid == ModFluids.RAW_DIESEL ? CarSetup.Fuel.DIESEL : CarSetup.Fuel.PETROL);
            CarEntity.markRaw(can, true);
        } else {
            CarEntity.markFuel(can, ModFluids.fuelOf(fluid));
        }
    }

    /**
     * "Petrol · 12.5 / 32 B", or "Empty · 32 B", from the two keys given; the
     * capacity, and the liquid and amount when there is one, come first, then
     * {@code extra}.
     */
    public static void report(PlayerEntity player, SingleSlotStorage<FluidVariant> store,
                              String emptyKey, String contentsKey, Object... extra) {
        String capacity = buckets(store.getCapacity());
        Text message;
        if (store.isResourceBlank() || store.getAmount() <= 0) {
            message = Text.translatable(emptyKey, concat(new Object[] {capacity}, extra));
        } else {
            message = Text.translatable(contentsKey, concat(new Object[] {
                    FluidVariantAttributes.getName(store.getResource()).copy().formatted(Formatting.AQUA),
                    buckets(store.getAmount()), capacity}, extra));
        }
        player.sendMessage(message, true);
    }

    private static Object[] concat(Object[] first, Object[] second) {
        Object[] all = java.util.Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }

    /** Droplets as buckets: whole, or to one place. */
    public static String buckets(long droplets) {
        double value = (double) droplets / FluidConstants.BUCKET;
        return value == Math.floor(value) ? String.valueOf((long) value)
                : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** Fill as a comparator reads it: nought empty, at least one with anything in, fifteen full. */
    public static int comparatorOutput(long amount, long capacity) {
        if (amount <= 0 || capacity <= 0) {
            return 0;
        }
        return (int) Math.max(1, Math.min(15, 1 + amount * 14 / capacity));
    }

    private static void playSound(World world, BlockPos pos, float pitch) {
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7F, pitch);
    }
}
