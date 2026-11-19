package me.trossty.car.item;

import me.trossty.car.block.entity.ChargingPostBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A charging post's plug, on the end of its lead.
 *
 * <p>Taken off the post with a bare hand, it is pushed into an electric car's
 * charge port with a right click and stays there, charging, until someone
 * takes it out again or the car drives off with it. Like the pump's nozzle it
 * belongs to its post: put away, dropped, or pulled further than the lead
 * reaches, it is back on its holster.
 */
public class ChargingPlugItem extends Item {

    public ChargingPlugItem(Properties settings) {
        super(settings);
    }

    public static ItemStack forPost(BlockPos post) {
        ItemStack plug = new ItemStack(ModItems.CHARGING_PLUG);
        plug.set(ModComponents.CHARGER, post.immutable());
        return plug;
    }

    public static boolean isFrom(ItemStack stack, BlockPos post) {
        return stack.is(ModItems.CHARGING_PLUG) && post.equals(stack.get(ModComponents.CHARGER));
    }

    public static @Nullable ChargingPostBlockEntity postOf(Level world, ItemStack plug) {
        BlockPos post = plug.get(ModComponents.CHARGER);
        return post != null && world.getBlockEntity(post) instanceof ChargingPostBlockEntity found ? found : null;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide()) {
            return;
        }
        ChargingPostBlockEntity post = postOf(world, stack);
        if (post == null || !post.isHeldBy(entity)) {
            stack.setCount(0);
        }
    }

    /** Pointed at a car's charge port, it goes in. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack plug = user.getItemInHand(hand);
        CarEntity car = CarEntity.fillerInSight(user);
        if (car == null) {
            return InteractionResultHolder.pass(plug);
        }
        if (!world.isClientSide()) {
            ChargingPostBlockEntity post = postOf(world, plug);
            if (post != null && post.isHeldBy(user)) {
                post.plugInto(car, user);
            }
        }
        return InteractionResultHolder.sidedSuccess(plug, world.isClientSide());
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }
}
