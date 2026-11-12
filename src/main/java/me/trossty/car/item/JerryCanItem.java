package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

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
    private static java.util.Set<java.util.UUID> siphoning(World world) {
        return world.isClient() ? SIPHONING_CLIENT : SIPHONING_SERVER;
    }

    public JerryCanItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        CarEntity car = CarEntity.fillerInSight(user);
        if (car == null) {
            return ActionResult.PASS;
        }
        ItemStack can = user.getStackInHand(hand);
        boolean siphon = JerryCans.isEmpty(can);
        if (siphon ? car.getFuel() < 1.0F : car.isTankFull()) {
            if (!world.isClient()) {
                car.fillingStopped(user);
            }
            return ActionResult.CONSUME;
        }
        if (siphon) {
            siphoning(world).add(user.getUuid());
        } else {
            siphoning(world).remove(user.getUuid());
        }
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack can, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) {
            return;
        }
        boolean siphon = siphoning(world).contains(player.getUuid());
        CarEntity car = CarEntity.fillerInSight(player);
        boolean done = car == null || (siphon
                ? car.getFuel() < 1.0F || JerryCans.isFull(can)
                : car.isTankFull() || JerryCans.isEmpty(can));
        if (done) {
            if (car != null && !world.isClient()) {
                car.fillingStopped(player);
            }
            user.stopUsingItem();
            return;
        }
        if (world.isClient()) {
            return;
        }
        if (car.pourFrom(player, can, siphon) <= 0) {
            user.stopUsingItem();
            return;
        }
        car.fillingTick(player, USE_TICKS - remainingUseTicks, siphon);
    }

    @Override
    public boolean onStoppedUsing(ItemStack can, World world, LivingEntity user, int remainingUseTicks) {
        siphoning(world).remove(user.getUuid());
        return false;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    /** The gauge moves every tick of a pour; the hand should not bob with it. */
    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand,
                                                  ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context,
                              TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, display, tooltip, type);
        if (JerryCans.isEmpty(stack)) {
            tooltip.accept(Text.translatable("tooltip.car.can_empty").formatted(Formatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Text.translatable("tooltip.car.can_level",
                        Text.translatable("fuel.car." + CarEntity.fuelIn(stack).key()),
                        JerryCans.litres(JerryCans.units(stack)),
                        JerryCans.litres(JerryCans.CAPACITY))
                .formatted(Formatting.GOLD));
        if (CarEntity.isRaw(stack)) {
            tooltip.accept(Text.translatable("tooltip.car.can_raw").formatted(Formatting.RED));
        }
    }
}
