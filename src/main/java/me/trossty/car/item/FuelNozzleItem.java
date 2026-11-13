package me.trossty.car.item;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The pump's nozzle, on the end of its hose.
 *
 * <p>Taken off the pump with a bare hand, it is pointed at a car's filler cap
 * and the button held down, and petrol runs out of the pump's own tank into
 * the car's for as long as it is held — {@link CarEntity#NOZZLE_FLOW} a tick.
 * The hose is drawn from the pump to the hand while it is out.
 *
 * <p>It belongs to the pump. Hung back up, put away, dropped, or carried
 * further than the hose reaches, it is back on its hook; a nozzle that finds
 * itself somewhere its pump does not know about just goes.
 */
public class FuelNozzleItem extends Item {

    private static final int USE_TICKS = 72000;

    public FuelNozzleItem(Properties settings) {
        super(settings);
    }

    /** A nozzle off the pump whose lower half is at {@code pump}. */
    public static ItemStack forPump(BlockPos pump) {
        ItemStack nozzle = new ItemStack(ModItems.FUEL_NOZZLE);
        nozzle.set(ModComponents.PUMP, pump.immutable());
        return nozzle;
    }

    public static boolean isFrom(ItemStack stack, BlockPos pump) {
        return stack.is(ModItems.FUEL_NOZZLE) && pump.equals(stack.get(ModComponents.PUMP));
    }

    /** The pump this nozzle hangs on, or null if it is not there any more. */
    public static @Nullable FuelPumpBlockEntity pumpOf(Level world, ItemStack nozzle) {
        BlockPos pump = nozzle.get(ModComponents.PUMP);
        return pump == null ? null : FuelPumpBlock.tankAt(world, pump);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide()) {
            return;
        }
        FuelPumpBlockEntity pump = pumpOf(world, stack);
        if (pump == null || !pump.isHeldBy(entity)) {
            stack.setCount(0);
        }
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        CarEntity car = CarEntity.fillerInSight(user);
        if (car == null) {
            return net.minecraft.world.InteractionResultHolder.pass(user.getItemInHand(hand));
        }
        if (car.isTankFull()) {
            if (!world.isClientSide()) {
                car.fillingStopped(user);
            }
            return net.minecraft.world.InteractionResultHolder.consume(user.getItemInHand(hand));
        }
        user.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(user.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack nozzle, int remainingUseTicks) {
        if (!(user instanceof Player player)) {
            return;
        }
        CarEntity car = CarEntity.fillerInSight(player);
        if (car == null || car.isTankFull()) {
            if (car != null && !world.isClientSide()) {
                car.fillingStopped(player);
            }
            user.releaseUsingItem();
            return;
        }
        if (world.isClientSide()) {
            return;
        }
        FuelPumpBlockEntity pump = pumpOf(world, nozzle);
        if (pump == null || !pump.isHeldBy(player)) {
            user.releaseUsingItem();
            return;
        }
        Vec3 outlet = FuelPumpBlock.hoseOutlet(world, pump.getBlockPos());
        Vec3 cap = car.fillerPos();
        if (outlet == null || cap == null
                || cap.distanceToSqr(outlet) > FuelPumpBlockEntity.HOSE_LENGTH * FuelPumpBlockEntity.HOSE_LENGTH) {
            player.displayClientMessage(Component.translatable("message.car.hose_too_short"), true);
            user.releaseUsingItem();
            return;
        }

        int spent = pump.refuel(car, CarEntity.NOZZLE_FLOW);
        if (spent <= 0) {
            if (pump.getPetrol() < FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT) {
                player.displayClientMessage(Component.translatable("message.car.pump_dry"), true);
            } else if (car.hasFuel() && car.getTankFuel() != CarSetup.Fuel.PETROL) {
                player.displayClientMessage(Component.translatable("message.car.tank_holds",
                        Component.translatable("fuel.car." + car.getTankFuel().key())), true);
            } else {
                car.fillingStopped(player);
            }
            user.releaseUsingItem();
            return;
        }
        car.fillingTick(player, USE_TICKS - remainingUseTicks, false);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** The gauge on the nozzle's own stack never changes, but don't bob the hand if it does. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
