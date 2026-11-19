package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * A motorbike lift: a narrow table the bike is wheeled on to, its front wheel
 * in the clamp at the end, that raises it to about waist height. A car lift's
 * arms have nothing to take a bike by, so a bike goes up on this and a car on
 * the other. It is the same lift otherwise: the same three stops, a right
 * click up and a sneaking one down, nothing worked on while it is down.
 *
 * <p>The deck runs half a block past the base at either end, so it stands
 * in three blocks in a row, and the three over them carry what of the deck
 * and the wheel vise rises out of those, so a cursor looking across at a
 * raised deck finds it. They have nothing else in them: the bike stands in
 * them as it would in air.
 */
public class MotorcycleLiftBlock extends VehicleLiftBlock {

    public static final MapCodec<MotorcycleLiftBlock> CODEC = simpleCodec(MotorcycleLiftBlock::new);

    /** The base with the pump, the blocks in front of and behind it that the deck reaches into, and the three above. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

    private static final int[][] CELLS = {{0, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, -1}, {0, 1, 0}, {0, 1, 1}};

    public MotorcycleLiftBlock(Properties settings) {
        super(settings);
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

    /**
     * The floor frame, the deck at its height and, once it is up, the
     * scissor legs under it, spread as far as the model spreads them. The
     * foot pump and the wheel vise can be aimed at but are not solid, so the
     * bike, which is wider than the deck, is not caught on them going on.
     */
    @Override
    protected double[][] drawing(int level, boolean solid) {
        double top = 2.0 + 3.75 * level;
        double under = top - 1.2;
        List<double[]> boxes = new ArrayList<>();
        boxes.add(new double[] {3.6, 0, -6, 12.4, 0.8, 22});
        boxes.add(new double[] {3.95, under, -8.4, 12.05, top, 24});
        if (level > 0) {
            double run = (under - 0.8) / Math.tan(Math.toRadians(level > 2 ? 45.0 : 22.5));
            boxes.add(new double[] {4.5, 0.8, 8 - run / 2, 11.5, under, 8 + run / 2});
        }
        if (!solid) {
            boxes.add(new double[] {11.7, 0, 0.6, 15.2, 3, 7.4});
            boxes.add(new double[] {5.2, top, 19.6, 10.8, top + 6.4, 23.8});
        }
        return boxes.toArray(double[][]::new);
    }
}
