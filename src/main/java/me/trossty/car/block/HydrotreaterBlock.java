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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The hydrotreater: a reactor vessel two blocks tall on a skid, with the
 * hydrogen bottle strapped alongside. The block {@link HydrotreaterBlockEntity}
 * lives in.
 */
public class HydrotreaterBlock extends MultiblockBlock {

    public static final MapCodec<HydrotreaterBlock> CODEC = simpleCodec(HydrotreaterBlock::new);

    public static final int[][] CELLS = {{0, 0, 0}, {0, 1, 0}};

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);

    private static final VoxelShape TOP = BaseEntityBlock.box(2, 0, 2, 14, 13, 14);

    public HydrotreaterBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LIT, false).setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int[][] cells() {
        return CELLS;
    }

    @Override
    public IntegerProperty partProperty() {
        return PART;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == 1 ? TOP : super.getShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new HydrotreaterBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || state.getValue(PART) != 0) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.HYDROTREATER, HydrotreaterBlockEntity::tick);
    }

    /** A jerry can held against it fills with whatever clean fuel it has. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(ModItems.JERRY_CAN)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClientSide()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant
                && !JerryCans.isEmpty(held) && CarEntity.isRaw(held)) {
            // A can of raw fuel tips into the feed tank, to come out clean.
            UnitTank in = plant.getInput();
            net.minecraft.world.level.material.Fluid raw = CarEntity.fuelIn(held) == CarSetup.Fuel.DIESEL
                    ? ModFluids.RAW_DIESEL : ModFluids.RAW_PETROL;
            if (!in.canTake(raw)) {
                player.displayClientMessage(Component.translatable("message.car.hydrotreater_busy"), true);
            } else {
                int taken = JerryCans.emptyInto(held, in.room(), units -> in.fill(raw, units));
                if (taken > 0) {
                    plant.setChanged();
                    world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
                }
            }
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant) {
            UnitTank out = plant.getOutput();
            CarSetup.Fuel fuel = ModFluids.fuelOf(out.getFluid());
            if (fuel == null || out.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.car.hydrotreater_empty"), true);
            } else if (!JerryCans.isEmpty(held) && CarEntity.fuelIn(held) != fuel) {
                player.displayClientMessage(Component.translatable("message.car.can_other_fuel"), true);
            } else {
                int units = JerryCans.topUp(held, out.getUnits(), out::drain);
                if (units > 0) {
                    CarEntity.markFuel(held, fuel);
                    plant.setChanged();
                    world.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.1F);
                }
            }
        }
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof HydrotreaterBlockEntity plant) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            plant, null),
                    this.getName()));
        }
        return InteractionResult.CONSUME;
    }

    private static Component name(UnitTank tank) {
        if (tank.isEmpty()) {
            return Component.translatable("fluid.car.nothing");
        }
        return Component.translatable("fluid.car." + net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tank.getFluid()).getPath());
    }

    /** Steam off the relief valve on top while it works. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(PART) != 1) {
            return;
        }
        if (random.nextInt(3) == 0) {
            world.addParticle(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
        }
        // A smaller vessel than the refinery's column: the same boil, higher.
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.REFINERY_BUBBLE, net.minecraft.sounds.SoundSource.BLOCKS,
                0.35F, 1.3F + random.nextFloat() * 0.2F, false);
    }
}
