package me.trossty.car.workbench;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * The workbench window. Materials go in a 3x3 grid, a recipe is picked from the
 * list on the left, and the build button turns one into the other.
 *
 * <p>Like a vanilla crafting table the grid is scratch space, not storage: it is
 * handed back to the player when the window closes, so nothing can be lost in a
 * block that has no inventory of its own.
 */
public class VehicleWorkbenchScreenHandler extends ScreenHandler {

    public static final int INPUT_SIZE = 9;
    /** Button id for "build"; ids below this select the recipe with that index. */
    public static final int BUILD_BUTTON_ID = 100;
    /** Grade buttons: this id plus a {@code CarPart.Quality} ordinal. */
    public static final int QUALITY_BUTTON_ID = 110;

    /**
     * The materials tray: one row of nine under the catalogue rather than a
     * three by three block beside it.
     *
     * <p>It used to be a square at 44, 18 — which is underneath the catalogue
     * ever since the catalogue became a scrolling grid. The slots were still
     * there and still worked, but nothing was drawn and a click on them picked
     * a recipe instead of putting anything down, so the only way to load the
     * bench was to shift-click. A client-test screenshot is what showed it.
     */
    private static final int INPUT_LEFT = 7;
    private static final int INPUT_TOP = 90;
    // Far enough down that the "Inventory" heading has a line of its own
    // between the tray and the player's own slots.
    private static final int PLAYER_INVENTORY_TOP = 122;
    private static final int HOTBAR_TOP = 180;

    private final Inventory input = new SimpleInventory(INPUT_SIZE) {
        @Override
        public void markDirty() {
            super.markDirty();
            VehicleWorkbenchScreenHandler.this.onContentChanged(this);
        }
    };
    private final ScreenHandlerContext context;
    private final Property selectedRecipe = Property.create();
    private final Property selectedQuality = Property.create();
    private final List<VehicleRecipe> recipes;
    private final Block block;

    /** Client-side constructor for the workbench: no world access, so the context is empty. */
    public VehicleWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public VehicleWorkbenchScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        this(ModScreenHandlers.VEHICLE_WORKBENCH, syncId, playerInventory, context,
                ModVehicleRecipes.WORKBENCH, ModBlocks.VEHICLE_WORKBENCH);
    }

    private VehicleWorkbenchScreenHandler(
            ScreenHandlerType<VehicleWorkbenchScreenHandler> type,
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext context,
            List<VehicleRecipe> recipes,
            Block block) {
        super(type, syncId);
        this.context = context;
        this.recipes = recipes;
        this.block = block;

        for (int slot = 0; slot < INPUT_SIZE; slot++) {
            this.addSlot(new Slot(this.input, slot, INPUT_LEFT + slot * 18, INPUT_TOP));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, PLAYER_INVENTORY_TOP + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, HOTBAR_TOP));
        }

        this.addProperty(this.selectedRecipe);
        this.addProperty(this.selectedQuality);
    }

    public int getSelectedRecipeIndex() {
        return this.selectedRecipe.get();
    }

    /** Translation key for the action button: build, or refurbish a tired part. */
    public String getActionKey() {
        return this.getSelectedRecipe().refurbished() ? "gui.car.refurbish" : "gui.car.build";
    }

    /** What this bench can build. */
    public List<VehicleRecipe> getRecipes() {
        return this.recipes;
    }

    public VehicleRecipe getSelectedRecipe() {
        return ModVehicleRecipes.byIndex(this.recipes, this.selectedRecipe.get());
    }

    /** The grade the three buttons are set to. */
    public CarPart.Quality getQuality() {
        return CarPart.Quality.byOrdinal(this.selectedQuality.get());
    }

    /**
     * Whether a grade can be chosen at all. Only the bench builds parts; a body
     * shell, a barrel or a jerry can is one thing and comes at one price, and
     * the lift builds whole cars out of what the bench made.
     */
    public boolean choosesQuality() {
        return BenchWork.choosesQuality(this.getSelectedRecipe());
    }

    /** What one material really costs at the grade now selected. */
    public java.util.List<VehicleRecipe.Material> getMaterials() {
        return BenchWork.materialsFor(this.getSelectedRecipe(), this.getQuality());
    }

    /**
     * How many of an item sit in the tray and are tired enough to be worth
     * rebuilding. A part that is already better than a rebuild would leave it
     * is not a candidate: feeding a fresh gearbox into a rebuild would be
     * paying materials to make it worse.
     */
    public int countWornInput(Item item) {
        return BenchWork.countWorn(this.input, item);
    }

    /** How many of an item sit in the grid. Valid on both sides; slots are synced. */
    public int countInput(Item item) {
        return BenchWork.count(this.input, item);
    }

    public boolean hasMaterialsFor(VehicleRecipe recipe) {
        return BenchWork.hasMaterials(this.input, recipe, this.getQuality());
    }

    public boolean canBuild() {
        return this.hasMaterialsFor(this.getSelectedRecipe());
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == BUILD_BUTTON_ID) {
            return this.build(player);
        }
        if (id >= QUALITY_BUTTON_ID
                && id < QUALITY_BUTTON_ID + CarPart.Quality.values().length) {
            this.selectedQuality.set(id - QUALITY_BUTTON_ID);
            return true;
        }
        if (id >= 0 && id < this.recipes.size()) {
            this.selectedRecipe.set(id);
            return true;
        }
        return false;
    }

    private boolean build(PlayerEntity player) {
        VehicleRecipe recipe = this.getSelectedRecipe();
        // The work itself is BenchWork's, shared with the buttons on the bench
        // top; all that is this window's own is where the materials are.
        ItemStack result = BenchWork.build(this.input, recipe, this.getQuality());
        if (result.isEmpty()) {
            return false;
        }
        if (!player.getInventory().insertStack(result)) {
            player.dropItem(result, false);
        }
        this.input.markDirty();
        this.sendContentUpdates();
        boolean rebuilt = recipe.refurbished();
        this.context.run((world, pos) -> world.playSound(null, pos,
                rebuilt ? SoundEvents.BLOCK_GRINDSTONE_USE : SoundEvents.BLOCK_ANVIL_USE,
                SoundCategory.BLOCKS, rebuilt ? 0.7F : 0.6F, rebuilt ? 1.0F : 1.6F));
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        // The grid is scratch space; give everything back rather than eating it.
        this.context.run((world, pos) -> this.dropInventory(player, this.input));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(this.context, player, this.block);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (slotIndex < INPUT_SIZE) {
            // Out of the grid, into the player.
            if (!this.insertItem(stack, INPUT_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.insertItem(stack, 0, INPUT_SIZE, false)) {
            // Into the grid, or failing that between inventory and hotbar.
            int inventoryEnd = INPUT_SIZE + 27;
            if (slotIndex < inventoryEnd) {
                if (!this.insertItem(stack, inventoryEnd, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(stack, INPUT_SIZE, inventoryEnd, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }
}
