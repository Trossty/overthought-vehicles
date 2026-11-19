package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hydraulic jack set under a plane's belly. A plane is too wide and too
 * long for a car lift, so it is jacked where it stands: taxi it over the
 * jack, get out and pump it up — a click, as with the lift, or redstone — and
 * the wheels come off the floor so it can be worked on. Pumped down again, it
 * taxies away.
 */
public class AircraftJackBlock extends ServiceStandBlock {

    public static final MapCodec<AircraftJackBlock> CODEC = simpleCodec(AircraftJackBlock::new);

    public static final BooleanProperty RAISED = BooleanProperty.create("raised");
    /** Whether redstone was on last time a neighbour changed: only a change of it moves the ram. */
    public static final BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    /**
     * How far the ram lifts the plane, in blocks. Its pad meets the belly of a
     * plane on its wheels, 9 pixels up, and rises this far with it.
     */
    public static final double RISE = 0.5;

    private static final VoxelShape DOWN = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            Block.box(5.0, 3.0, 5.0, 11.0, 9.0, 11.0));
    private static final VoxelShape UP = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 3.0, 14.0),
            Block.box(5.0, 3.0, 5.0, 11.0, 17.0, 11.0));

    public AircraftJackBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.defaultBlockState().setValue(RAISED, false).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(RAISED, POWERED);
    }

    @Override
    public VehicleDefinition.Chassis takes() {
        return VehicleDefinition.Chassis.PLANE;
    }

    @Override
    public double seat(BlockState state) {
        return state.getValue(RAISED) ? RISE : Double.NaN;
    }

    /** The belly between the main wheels and the nose wheel is ahead of a plane's middle. */
    @Override
    public double under() {
        return 1.6;
    }

    @Override
    protected VoxelShape body(BlockState state) {
        return state.getValue(RAISED) ? UP : DOWN;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean up = !state.getValue(RAISED);
        world.setBlock(pos, state.setValue(RAISED, up), Block.UPDATE_ALL);
        world.playSound(null, pos, up ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.BLOCKS, 0.4F, 0.7F);
        player.displayClientMessage(Component.translatable(up ? "message.overthought_vehicles.jack_up" : "message.overthought_vehicles.jack_down"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Redstone pumps it up when it comes on and lets it down when it goes
     * off; a jack pumped by hand stays where it was put whatever else changes
     * round it.
     */
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from,
                                   boolean moved) {
        boolean powered = world.hasNeighborSignal(pos);
        if (!world.isClientSide() && powered != state.getValue(POWERED)) {
            world.setBlock(pos, state.setValue(POWERED, powered).setValue(RAISED, powered), Block.UPDATE_ALL);
            world.playSound(null, pos, powered ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                    SoundSource.BLOCKS, 0.4F, 0.7F);
        }
    }
}
