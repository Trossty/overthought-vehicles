package me.trossty.car.item;

import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

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

    public FuelNozzleItem(Settings settings) {
        super(settings);
    }

    /** A nozzle off the pump whose lower half is at {@code pump}. */
    public static ItemStack forPump(BlockPos pump) {
        ItemStack nozzle = new ItemStack(ModItems.FUEL_NOZZLE);
        nozzle.set(ModComponents.PUMP, pump.toImmutable());
        return nozzle;
    }

    public static boolean isFrom(ItemStack stack, BlockPos pump) {
        return stack.isOf(ModItems.FUEL_NOZZLE) && pump.equals(stack.get(ModComponents.PUMP));
    }

    /** The pump this nozzle hangs on, or null if it is not there any more. */
    public static @Nullable FuelPumpBlockEntity pumpOf(World world, ItemStack nozzle) {
        BlockPos pump = nozzle.get(ModComponents.PUMP);
        return pump == null ? null : FuelPumpBlock.tankAt(world, pump);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        FuelPumpBlockEntity pump = pumpOf(world, stack);
        if (pump == null || !pump.isHeldBy(entity)) {
            stack.setCount(0);
        }
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        CarEntity car = CarEntity.fillerInSight(user);
        if (car == null) {
            return ActionResult.PASS;
        }
        if (car.isTankFull()) {
            if (!world.isClient()) {
                car.fillingStopped(user);
            }
            return ActionResult.CONSUME;
        }
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack nozzle, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }
        CarEntity car = CarEntity.fillerInSight(player);
        if (car == null || car.isTankFull()) {
            if (car != null && !world.isClient()) {
                car.fillingStopped(player);
            }
            user.stopUsingItem();
            return;
        }
        if (world.isClient()) {
            return;
        }
        FuelPumpBlockEntity pump = pumpOf(world, nozzle);
        if (pump == null || !pump.isHeldBy(player)) {
            user.stopUsingItem();
            return;
        }
        Vec3d outlet = FuelPumpBlock.hoseOutlet(world, pump.getPos());
        Vec3d cap = car.fillerPos();
        if (outlet == null || cap == null
                || cap.squaredDistanceTo(outlet) > FuelPumpBlockEntity.HOSE_LENGTH * FuelPumpBlockEntity.HOSE_LENGTH) {
            player.sendMessage(Text.translatable("message.car.hose_too_short"), true);
            user.stopUsingItem();
            return;
        }

        int spent = pump.refuel(car, CarEntity.NOZZLE_FLOW);
        if (spent <= 0) {
            if (pump.getPetrol() < FuelPumpBlockEntity.PETROL_PER_FUEL_UNIT) {
                player.sendMessage(Text.translatable("message.car.pump_dry"), true);
            } else if (car.hasFuel() && car.getTankFuel() != CarSetup.Fuel.PETROL) {
                player.sendMessage(Text.translatable("message.car.tank_holds",
                        Text.translatable("fuel.car." + car.getTankFuel().key())), true);
            } else {
                car.fillingStopped(player);
            }
            user.stopUsingItem();
            return;
        }
        car.fillingTick(player, USE_TICKS - remainingUseTicks, false);
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    /** The gauge on the nozzle's own stack never changes, but don't bob the hand if it does. */
    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand,
                                                  ItemStack oldStack, ItemStack newStack) {
        return false;
    }
}
