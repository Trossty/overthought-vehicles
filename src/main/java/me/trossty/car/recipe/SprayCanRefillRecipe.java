package me.trossty.car.recipe;

import me.trossty.car.item.SprayCanItem;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/**
 * One spray can and up to eight dyes: a full can of the mixed colour. A can
 * that still has paint in it mixes the new dyes into that paint, exactly as
 * dyeing leather armour does; an empty one starts from clean.
 */
public class SprayCanRefillRecipe extends CustomRecipe {

    public static final RecipeSerializer<SprayCanRefillRecipe> SERIALIZER =
            new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(SprayCanRefillRecipe::new);

    public SprayCanRefillRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    private record Parts(ItemStack can, List<DyeItem> dyes) {
    }

    private static Parts find(CraftingInput input) {
        ItemStack can = ItemStack.EMPTY;
        List<DyeItem> dyes = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.SPRAY_CAN)) {
                if (!can.isEmpty()) {
                    return null;
                }
                can = stack;
            } else if (stack.getItem() instanceof DyeItem dye) {
                dyes.add(dye);
            } else {
                return null;
            }
        }
        return can.isEmpty() || dyes.isEmpty() ? null : new Parts(can, dyes);
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        return find(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Parts parts = find(input);
        if (parts == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = parts.can().copyWithCount(1);
        Integer old = SprayCanItem.isEmpty(result) ? null : SprayCanItem.colourOf(result);
        result.set(DataComponents.DYED_COLOR, new DyedItemColor(mix(old, parts.dyes()), true));
        result.setDamageValue(0);
        return result;
    }

    /** Leather armour's mixing rule: average the channels, keep the brightness. */
    static int mix(Integer base, List<DyeItem> dyes) {
        int r = 0;
        int g = 0;
        int b = 0;
        int brightest = 0;
        int count = 0;
        List<Integer> colours = new ArrayList<>();
        if (base != null) {
            colours.add(base);
        }
        for (DyeItem dye : dyes) {
            colours.add(dye.getDyeColor().getTextureDiffuseColor() & 0xFFFFFF);
        }
        for (int colour : colours) {
            int cr = colour >> 16 & 0xFF;
            int cg = colour >> 8 & 0xFF;
            int cb = colour & 0xFF;
            brightest += Math.max(cr, Math.max(cg, cb));
            r += cr;
            g += cg;
            b += cb;
            count++;
        }
        r /= count;
        g /= count;
        b /= count;
        float averageMax = (float) brightest / count;
        float max = Math.max(r, Math.max(g, b));
        if (max > 0) {
            r = (int) (r * averageMax / max);
            g = (int) (g * averageMax / max);
            b = (int) (b * averageMax / max);
        }
        return r << 16 | g << 8 | b;
    }

    @Override
    public RecipeSerializer<SprayCanRefillRecipe> getSerializer() {
        return SERIALIZER;
    }
}
