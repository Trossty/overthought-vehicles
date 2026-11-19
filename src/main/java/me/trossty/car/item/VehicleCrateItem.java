package me.trossty.car.item;

import me.trossty.car.block.entity.VehicleCrateBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A crated vehicle: called after what is in it, and telling you what that is. */
public class VehicleCrateItem extends BlockItem {

    public VehicleCrateItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public Component getName(ItemStack stack) {
        ItemStack inside = VehicleCrateBlockEntity.vehicleIn(stack);
        return inside.isEmpty()
                ? super.getName(stack)
                : Component.translatable("block.car.vehicle_crate.holding", inside.getHoverName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag type) {
        super.appendHoverText(stack, context, lines, type);
        ItemStack inside = VehicleCrateBlockEntity.vehicleIn(stack);
        if (!inside.isEmpty()) {
            inside.getItem().appendHoverText(inside, context, lines, type);
        }
        lines.add(Component.translatable("tooltip.car.crate_open").withStyle(ChatFormatting.DARK_GRAY));
    }
}
