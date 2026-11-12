package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.HydrotreaterBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.fluid.UnitTank;
import me.trossty.car.item.JerryCans;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The hydrotreater: a reactor vessel two blocks tall on a skid, with the
 * hydrogen bottle strapped alongside. The block {@link HydrotreaterBlockEntity}
 * lives in.
 */
public class HydrotreaterBlock extends MultiblockBlock {

    public static final MapCodec<HydrotreaterBlock> CODEC = createCodec(HydrotreaterBlock::new);

    public static final int[][] CELLS = {{0, 0, 0}, {0, 1, 0}};

    public static final IntProperty PART = IntProperty.of("part", 0, CELLS.length - 1);

    private static final VoxelShape TOP = BlockWithEntity.createCuboidShape(2, 0, 2, 14, 13, 14);

    public HydrotreaterBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(LIT, false).with(FACING, Direction.NORTH).with(PART, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public int[][] cells() {
        return CELLS;
    }

    @Override
    public IntProperty partProperty() {
        return PART;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(PART) == 1 ? TOP : super.getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == 0 ? new HydrotreaterBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient() || state.get(PART) != 0) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.HYDROTREATER, HydrotreaterBlockEntity::tick);
    }

    /** A jerry can held against it fills with whatever clean fuel it has. */
    @Override
    protected ActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!held.isOf(ModItems.JERRY_CAN)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant
                && !JerryCans.isEmpty(held) && CarEntity.isRaw(held)) {
            // A can of raw fuel tips into the feed tank, to come out clean.
            UnitTank in = plant.getInput();
            net.minecraft.fluid.Fluid raw = CarEntity.fuelIn(held) == CarSetup.Fuel.DIESEL
                    ? ModFluids.RAW_DIESEL : ModFluids.RAW_PETROL;
            if (!in.canTake(raw)) {
                player.sendMessage(Text.translatable("message.car.hydrotreater_busy"), true);
            } else {
                int taken = JerryCans.emptyInto(held, in.room(), units -> in.fill(raw, units));
                if (taken > 0) {
                    plant.markDirty();
                    world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.6F, 0.9F);
                }
            }
            return ActionResult.CONSUME;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant) {
            UnitTank out = plant.getOutput();
            CarSetup.Fuel fuel = ModFluids.fuelOf(out.getFluid());
            if (fuel == null || out.isEmpty()) {
                player.sendMessage(Text.translatable("message.car.hydrotreater_empty"), true);
            } else if (!JerryCans.isEmpty(held) && CarEntity.fuelIn(held) != fuel) {
                player.sendMessage(Text.translatable("message.car.can_other_fuel"), true);
            } else {
                int units = JerryCans.topUp(held, out.getUnits(), out::drain);
                if (units > 0) {
                    CarEntity.markFuel(held, fuel);
                    plant.markDirty();
                    world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.6F, 1.1F);
                }
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant) {
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            plant, null),
                    this.getName()));
        }
        return ActionResult.CONSUME;
    }

    private static Text name(UnitTank tank) {
        if (tank.isEmpty()) {
            return Text.translatable("fluid.car.nothing");
        }
        return Text.translatable("fluid.car." + net.minecraft.registry.Registries.FLUID.getId(tank.getFluid()).getPath());
    }

    /** Steam off the relief valve on top while it works. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT) || state.get(PART) != 1) {
            return;
        }
        if (random.nextInt(3) == 0) {
            world.addParticleClient(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
        }
        // A smaller vessel than the refinery's column: the same boil, higher.
        world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.REFINERY_BUBBLE, net.minecraft.sound.SoundCategory.BLOCKS,
                0.35F, 1.3F + random.nextFloat() * 0.2F, false);
    }
}
