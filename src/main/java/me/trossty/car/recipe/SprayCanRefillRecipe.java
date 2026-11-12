package me.trossty.car.recipe;

import me.trossty.car.item.SprayCanItem;
import me.trossty.car.registry.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * One spray can and up to eight dyes: a full can of the mixed colour. A can
 * that still has paint in it mixes the new dyes into that paint, exactly as
 * dyeing leather armour does; an empty one starts from clean.
 */
public class SprayCanRefillRecipe extends SpecialCraftingRecipe {

    public static final RecipeSerializer<SprayCanRefillRecipe> SERIALIZER =
            new SpecialRecipeSerializer<>(SprayCanRefillRecipe::new);

    public SprayCanRefillRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    private record Parts(ItemStack can, List<DyeItem> dyes) {
    }

    private static Parts find(CraftingRecipeInput input) {
        ItemStack can = ItemStack.EMPTY;
        List<DyeItem> dyes = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isOf(ModItems.SPRAY_CAN)) {
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
    public boolean matches(CraftingRecipeInput input, World world) {
        return find(input) != null;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        Parts parts = find(input);
        if (parts == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = parts.can().copyWithCount(1);
        Integer old = SprayCanItem.isEmpty(result) ? null : SprayCanItem.colourOf(result);
        result.set(DataComponentTypes.DYED_COLOR, new DyedColorComponent(mix(old, parts.dyes())));
        result.setDamage(0);
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
            colours.add(dye.getColor().getEntityColor() & 0xFFFFFF);
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
