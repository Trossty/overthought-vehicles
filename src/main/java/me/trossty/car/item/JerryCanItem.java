package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import java.util.function.Consumer;

/**
 * A jerry can that says what is in it.
 *
 * <p>There are two fuels now and they do not mix, so a can has to be able to
 * tell you which one it is holding before you pour it into something.
 *
 * <p>It is poured the way MrCrayfish's are: pointed at a car's filler cap
 * with the button held down, {@link CarEntity#CAN_FLOW} a tick until you let
 * go, look away, the tank is full or the can runs dry. An empty can held to
 * the cap the same way draws the tank off instead.
 */
public class JerryCanItem extends Item {

    private static final int USE_TICKS = 72000;

    /**
     * Who is drawing a tank off rather than pouring into it. Decided when the
     * button goes down, by whether the can was empty then, so a can poured dry
     * does not turn round and start taking it back.
     */
    private static final java.util.Set<java.util.UUID> SIPHONING_SERVER =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<java.util.UUID> SIPHONING_CLIENT =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Each side keeps its own, so a single-player game's two halves never share one. */
    private static java.util.Set<java.util.UUID> siphoning(Level world) {
        return world.isClientSide() ? SIPHONING_CLIENT : SIPHONING_SERVER;
    }

    public JerryCanItem(Properties settings) {
        super(settings);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        CarEntity car = CarEntity.fillerInSight(user);
        if (car == null) {
            return net.minecraft.world.InteractionResultHolder.pass(user.getItemInHand(hand));
        }
        ItemStack can = user.getItemInHand(hand);
        boolean siphon = JerryCans.isEmpty(can);
        if (siphon ? car.getFuel() < 1.0F : car.isTankFull()) {
            if (!world.isClientSide()) {
                car.fillingStopped(user);
            }
            return net.minecraft.world.InteractionResultHolder.consume(user.getItemInHand(hand));
        }
        if (siphon) {
            siphoning(world).add(user.getUUID());
        } else {
            siphoning(world).remove(user.getUUID());
        }
        user.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(user.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack can, int remainingUseTicks) {
        if (!(user instanceof Player player)) {
            return;
        }
        boolean siphon = siphoning(world).contains(player.getUUID());
        CarEntity car = CarEntity.fillerInSight(player);
        boolean done = car == null || (siphon
                ? car.getFuel() < 1.0F || JerryCans.isFull(can)
                : car.isTankFull() || JerryCans.isEmpty(can));
        if (done) {
            if (car != null && !world.isClientSide()) {
                car.fillingStopped(player);
            }
            user.releaseUsingItem();
            return;
        }
        if (world.isClientSide()) {
            return;
        }
        if (car.pourFrom(player, can, siphon) <= 0) {
            user.releaseUsingItem();
            return;
        }
        car.fillingTick(player, USE_TICKS - remainingUseTicks, siphon);
    }

    @Override
    public void releaseUsing(ItemStack can, Level world, LivingEntity user, int remainingUseTicks) {
        siphoning(world).remove(user.getUUID());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** The gauge moves every tick of a pour; the hand should not bob with it. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                java.util.List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        Consumer<Component> tooltip = lines::add;
        if (JerryCans.isEmpty(stack)) {
            tooltip.accept(Component.translatable("tooltip.overthought_vehicles.can_empty").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable("tooltip.overthought_vehicles.can_level",
                        Component.translatable("fuel.overthought_vehicles." + CarEntity.fuelIn(stack).key()),
                        JerryCans.litres(JerryCans.units(stack)),
                        JerryCans.litres(JerryCans.CAPACITY))
                .withStyle(ChatFormatting.GOLD));
        if (CarEntity.isRaw(stack)) {
            tooltip.accept(Component.translatable("tooltip.overthought_vehicles.can_raw").withStyle(ChatFormatting.RED));
        }
    }
}
