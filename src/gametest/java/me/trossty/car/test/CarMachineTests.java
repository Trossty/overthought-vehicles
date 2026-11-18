package me.trossty.car.test;

import me.trossty.car.block.BatteryBankBlock;
import me.trossty.car.block.ChargingPostBlock;
import me.trossty.car.block.EnergyCableBlock;
import me.trossty.car.block.EngineHoistBlock;
import me.trossty.car.block.entity.EngineHoistBlockEntity;
import me.trossty.car.block.entity.BatteryBankBlockEntity;
import me.trossty.car.block.entity.ChargingPostBlockEntity;
import me.trossty.car.entity.BodyZone;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarParts;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.entity.EngineInternals.Snapshot;
import me.trossty.car.entity.FaultCodes;
import me.trossty.car.entity.Workshop;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.block.entity.EnergyCableBlockEntity;
import me.trossty.car.block.entity.FuelPipeBlockEntity;
import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.block.entity.GeneratorBlockEntity;
import me.trossty.car.block.entity.HydrotreaterBlockEntity;
import me.trossty.car.block.entity.PumpjackBlockEntity;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.CarMain;
import net.minecraft.gametest.framework.GameTest;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Headless checks with real numbers: each one builds a little piece of the
 * production line in an empty 8x8x8 box, lets it run, and fails the build if
 * what came out is not what went in. These are the regressions the photo test
 * can only show, not catch.
 *
 * <p>Run with {@code ./gradlew runGametest}.
 */
/** Every test stands on the mod's own empty template, a plot of air over the test floor. */
@GameTestHolder(CarMain.MOD_ID)
@PrefixGameTestTemplate(false)
public class CarMachineTests {

    private static final int BUCKET = net.neoforged.neoforge.fluids.FluidType.BUCKET_VOLUME;

    // ---- helpers ----

    /** Sets a machine down facing south, every cell of it, as placing it by hand would. */
    private static void machine(GameTestHelper context, Block block, BlockPos at) {
        BlockState state = block.defaultBlockState().setValue(MultiblockBlock.FACING, Direction.SOUTH);
        context.setBlock(at, state);
        ((MultiblockBlock) block).placeParts(context.getLevel(), context.absolutePos(at), state);
    }

    /** Lays cable along these spots, then lets each length join up as a hand-laid one would. */
    private static void cable(GameTestHelper context, BlockPos... run) {
        for (BlockPos at : run) {
            context.setBlock(at, ModBlocks.ENERGY_CABLE);
        }
        ServerLevel world = context.getLevel();
        for (BlockPos at : run) {
            BlockPos pos = context.absolutePos(at);
            BlockState state = world.getBlockState(pos);
            for (Direction side : Direction.values()) {
                state = state.updateShape(side, world.getBlockState(pos.relative(side)), world, pos,
                        pos.relative(side));
            }
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Joins up pipe already laid at these spots, as placing each by hand would. */
    private static void joinPipes(GameTestHelper context, BlockPos... run) {
        ServerLevel world = context.getLevel();
        for (BlockPos at : run) {
            BlockPos pos = context.absolutePos(at);
            BlockState state = world.getBlockState(pos);
            for (Direction side : Direction.values()) {
                state = state.updateShape(side, world.getBlockState(pos.relative(side)), world, pos,
                        pos.relative(side));
            }
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
    }

    private static GeneratorBlockEntity coalGenerator(GameTestHelper context, BlockPos at, int coal) {
        context.setBlock(at, ModBlocks.GENERATOR);
        GeneratorBlockEntity generator = context.<GeneratorBlockEntity>getBlockEntity(at);
        generator.setItem(0, new ItemStack(Items.COAL, coal));
        return generator;
    }

    // ---- generators ----

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void coalGeneratorBurnsAndFills(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        GeneratorBlockEntity generator = coalGenerator(context, at, 4);
        context.runAtTickTime(20, () -> {
            context.assertTrue(generator.isBurning(), "coal generator is not burning");
            context.assertTrue(generator.getEnergy() > 0, "coal generator made no energy");
            context.assertTrue(context.getBlockState(at).getValue(me.trossty.car.block.GeneratorBlock.LIT),
                    "a burning generator is not lit");
            context.assertTrue(generator.getItem(0).getCount() == 3, "it should have burnt exactly one coal");
            context.assertTrue(!generator.getEnergyStorage().canReceive(),
                    "a generator must not take energy in");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void pumpTakesDieselFromAPipe(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.FUEL_PUMP);
        var pump = context.<me.trossty.car.block.entity.FuelPumpBlockEntity>getBlockEntity(at);
        var pipe = pump.getFluidStorage();
        var execute = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        int diesel = pipe.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.DIESEL, 300), execute);
        context.assertTrue(diesel == 300, "the pump took " + diesel + " of 300 diesel");
        context.assertTrue(pump.getFuel() == me.trossty.car.entity.CarSetup.Fuel.DIESEL, "the pump does not hold diesel");
        int petrol = pipe.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.PETROL, 100), execute);
        context.assertTrue(petrol == 0, "petrol went into a pump holding diesel");
        int crude = pipe.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.CRUDE_OIL, 100), execute);
        context.assertTrue(crude == 0, "crude oil went into a pump");
        pump.drain(300);
        int bio = pipe.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.BIOFUEL, 50), execute);
        context.assertTrue(bio == 50 && pump.getFuel() == me.trossty.car.entity.CarSetup.Fuel.BIOFUEL,
                "an emptied pump did not take biofuel");
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void dieselGeneratorDrinksItsTank(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.DIESEL_GENERATOR);
        GeneratorBlockEntity generator = context.<GeneratorBlockEntity>getBlockEntity(at);
        generator.getTank().set(ModFluids.DIESEL, 100);
        context.runAtTickTime(21, () -> {
            int used = 100 - generator.getTank().getUnits();
            // a unit every four ticks
            context.assertTrue(used >= 4 && used <= 6, "diesel generator used " + used + " units in 20 ticks");
            context.assertTrue(generator.getEnergy() >= 19 * GeneratorBlockEntity.Kind.DIESEL.output,
                    "diesel generator made only " + generator.getEnergy());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rapeseedGrowsAndFeedsTheRefinery(GameTestHelper context) {
        BlockPos soil = new BlockPos(2, 1, 2);
        context.setBlock(soil, net.minecraft.world.level.block.Blocks.FARMLAND);
        BlockPos at = soil.above();
        var crop = (net.minecraft.world.level.block.CropBlock) ModBlocks.RAPESEED_CROP;
        context.setBlock(at, crop.getStateForAge(0));
        context.assertTrue(context.getBlockState(at).canSurvive(context.getLevel(), context.absolutePos(at)),
                "rapeseed will not stand on farmland");
        var ripe = crop.getStateForAge(crop.getMaxAge());
        int seeds = 0;
        for (ItemStack drop : net.minecraft.world.level.block.Block.getDrops(ripe, context.getLevel(),
                context.absolutePos(at), null)) {
            context.assertTrue(drop.is(me.trossty.car.registry.ModItems.RAPESEED), "a rapeseed crop dropped " + drop);
            seeds += drop.getCount();
        }
        context.assertTrue(seeds >= 2, "a ripe crop gave only " + seeds + " seed");
        ItemStack seed = new ItemStack(me.trossty.car.registry.ModItems.RAPESEED);
        context.assertTrue(seed.is(me.trossty.car.registry.ModTags.REFINERY_ORGANIC), "the refinery will not take rapeseed");
        context.assertTrue(me.trossty.car.block.entity.FuelRefineryBlockEntity.biomassOf(seed)
                == 3 * me.trossty.car.block.entity.FuelRefineryBlockEntity.biomassOf(new ItemStack(Items.WHEAT)),
                "rapeseed is not worth three of wheat");
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void theStarterDrawsOnTheBattery(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(2, 1, 2));
        CarEntity weak = context.spawn(ModEntities.CAR, new BlockPos(6, 1, 2));
        weak.parts().setCondition(CarPart.BATTERY, 3.0F);
        var driver = context.makeMockServerPlayerInLevel();
        var other = context.makeMockServerPlayerInLevel();
        context.runAtTickTime(2, () -> {
            driver.startRiding(car, true);
            other.startRiding(weak, true);
        });
        context.runAtTickTime(6, () -> {
            context.assertTrue(car.parts().getStarterDebt() > 0.0F
                    && car.parts().getCondition(CarPart.BATTERY) < CarPart.MAX_CONDITION - 1.0F,
                    "starting did not draw on the battery: " + car.parts().getCondition(CarPart.BATTERY));
            context.assertTrue(car.isEngineRunning(), "a good battery did not start the car");
            context.assertTrue(weak.isBatteryFlat() && !weak.isEngineRunning(),
                    "a nearly flat battery started the car and kept it running");
        });
        context.runAtTickTime(240, () -> {
            context.assertTrue(car.parts().getStarterDebt() == 0.0F
                    && car.parts().getCondition(CarPart.BATTERY) >= CarPart.MAX_CONDITION - 0.01F,
                    "the alternator did not put the start back: " + car.parts().getCondition(CarPart.BATTERY));
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aFullGeneratorBanksItsFire(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        GeneratorBlockEntity generator = coalGenerator(context, at, 4);
        int[] banked = new int[1];
        context.runAtTickTime(10, () -> generator.setEnergy(GeneratorBlockEntity.CAPACITY));
        context.runAtTickTime(12, () -> banked[0] = generator.getBurnTime());
        context.runAtTickTime(40, () -> {
            context.assertTrue(banked[0] > 0 && generator.getBurnTime() == banked[0],
                    "a full generator burnt its coal down from " + banked[0] + " to " + generator.getBurnTime());
            context.assertTrue(generator.getItem(0).getCount() == 3, "a full generator lit more coal");
            context.assertTrue(!generator.isBurning()
                    && !context.getBlockState(at).getValue(me.trossty.car.block.GeneratorBlock.LIT),
                    "a banked generator still shows lit");
            generator.setEnergy(0);
        });
        context.runAtTickTime(45, () -> {
            context.assertTrue(generator.isBurning() && generator.getBurnTime() < banked[0],
                    "the banked fire did not pick up again");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void emptyGeneratorStaysCold(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        GeneratorBlockEntity generator = coalGenerator(context, at, 0);
        context.runAtTickTime(20, () -> {
            context.assertTrue(!generator.isBurning() && generator.getEnergy() == 0,
                    "an empty generator made energy");
            context.succeed();
        });
    }

    // ---- cables ----

    @GameTest(template = "empty", timeoutTicks = 220)
    public static void cablesCarryPowerToAMachine(GameTestHelper context) {
        coalGenerator(context, new BlockPos(0, 1, 2), 16);
        BlockPos[] run = new BlockPos[6];
        for (int i = 0; i < run.length; i++) {
            run[i] = new BlockPos(1 + i, 1, 2);
        }
        cable(context, run);
        BlockPos plantAt = new BlockPos(7, 1, 2);
        machine(context, ModBlocks.HYDROTREATER, plantAt);
        HydrotreaterBlockEntity plant = context.<HydrotreaterBlockEntity>getBlockEntity(plantAt);
        plant.getInput().set(ModFluids.RAW_PETROL, 1000);

        context.runAtTickTime(200, () -> {
            EnergyCableBlockEntity first = context.<EnergyCableBlockEntity>getBlockEntity(run[0]);
            EnergyCableBlockEntity last = context.<EnergyCableBlockEntity>getBlockEntity(run[run.length - 1]);
            context.assertTrue(first.network() == last.network() && first.network().size() == run.length,
                    "the six lengths are not one run");
            context.assertTrue(plant.getOutput().getUnits() > 0,
                    "the hydrotreater at the end of the cable never ran");
            context.assertTrue(context.getBlockState(run[0]).getValue(EnergyCableBlock.SIDES.get(Direction.WEST))
                    == EnergyCableBlock.Side.DEVICE, "the first length does not plug into the generator");
            context.assertTrue(context.getBlockState(run[2]).getValue(EnergyCableBlock.SIDES.get(Direction.EAST))
                    == EnergyCableBlock.Side.CABLE, "the lengths do not join each other");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void cableWithNoSourceStaysEmpty(GameTestHelper context) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = new BlockPos(3, 1, 2);
        cable(context, a, b);
        context.runAtTickTime(40, () -> {
            context.assertTrue(context.<EnergyCableBlockEntity>getBlockEntity(a).getFlow() == 0,
                    "cable made energy out of nothing");
            context.succeed();
        });
    }

    // ---- crude: the pumpjack ----

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void pumpjackDrawsCrudeFromADeposit(GameTestHelper context) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 3; z <= 5; z++) {
                context.setBlock(new BlockPos(x, 0, z), ModBlocks.OIL_DEPOSIT);
            }
        }
        BlockPos at = new BlockPos(3, 1, 5);
        machine(context, ModBlocks.PUMPJACK, at);
        PumpjackBlockEntity jack = context.<PumpjackBlockEntity>getBlockEntity(at);
        jack.setEnergy(PumpjackBlockEntity.ENERGY_CAPACITY);
        context.runAtTickTime(60, () -> {
            context.assertTrue(jack.isWorking(), "pumpjack over a deposit with power is not working");
            int crude = jack.getCrude().getUnits();
            context.assertTrue(crude >= 400, "pumpjack drew only " + crude + " units in 60 ticks");
            context.assertTrue(jack.getCrude().getFluid() == ModFluids.CRUDE_OIL, "pumpjack drew something but crude");
            context.assertTrue(jack.estimateReserve() == 9 * PumpjackBlockEntity.CRUDE_PER_DEPOSIT - crude,
                    "reserve " + jack.estimateReserve() + " does not match " + crude + " drawn");
            context.succeed();
        });
    }

    /** A well that has run dry still trickles: a tenth of what it drew. One that never struck gives nothing. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void aDryWellStillTrickles(GameTestHelper context) {
        context.setBlock(new BlockPos(3, 0, 4), ModBlocks.OIL_DEPOSIT);
        BlockPos at = new BlockPos(3, 1, 5);
        machine(context, ModBlocks.PUMPJACK, at);
        PumpjackBlockEntity jack = context.<PumpjackBlockEntity>getBlockEntity(at);
        jack.setEnergy(PumpjackBlockEntity.ENERGY_CAPACITY);
        context.runAtTickTime(130, () -> {
            context.assertTrue(!context.getBlockState(new BlockPos(3, 0, 4)).is(ModBlocks.OIL_DEPOSIT),
                    "the deposit block was not spent");
            int before = jack.getCrude().getUnits();
            context.assertTrue(jack.hasStruck() && before >= PumpjackBlockEntity.CRUDE_PER_DEPOSIT,
                    "the jack did not draw the whole block: " + before);
            context.runAfterDelay(20, () -> {
                int trickled = jack.getCrude().getUnits() - before;
                context.assertTrue(trickled > 0 && trickled <= 20 * PumpjackBlockEntity.TRICKLE,
                        "a dry well gave " + trickled + " in 20 ticks");
                context.succeed();
            });
        });
    }

    /** The survey kit finds oil under the hammer, and points the way to oil that is not. */
    @GameTest(template = "empty")
    public static void theSurveyKitFindsOil(GameTestHelper context) {
        context.setBlock(new BlockPos(2, 0, 2), ModBlocks.OIL_DEPOSIT);
        context.setBlock(new BlockPos(2, 0, 3), ModBlocks.OIL_DEPOSIT);
        var world = context.getLevel();
        var here = me.trossty.car.item.SeismicSurveyItem.survey(world, context.absolutePos(new BlockPos(2, 2, 2)));
        context.assertTrue(here.underfoot() && here.belowHere() == 2 && here.depositBlocks() == 2,
                "the kit did not hear the two blocks underfoot: " + here);
        var away = me.trossty.car.item.SeismicSurveyItem.survey(world, context.absolutePos(new BlockPos(8, 2, 2)));
        context.assertTrue(!away.underfoot() && away.nearest() != null
                        && away.nearest().equals(context.absolutePos(new BlockPos(2, 0, 2))),
                "the kit did not point at the deposit six blocks off: " + away);
        context.assertTrue(me.trossty.car.item.SeismicSurveyItem.report(away, context.absolutePos(new BlockPos(8, 2, 2)))
                .getString().contains("west"), "the deposit to the west was not called west");
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void pumpjackWithoutPowerStands(GameTestHelper context) {
        context.setBlock(new BlockPos(3, 0, 4), ModBlocks.OIL_DEPOSIT);
        BlockPos at = new BlockPos(3, 1, 5);
        machine(context, ModBlocks.PUMPJACK, at);
        PumpjackBlockEntity jack = context.<PumpjackBlockEntity>getBlockEntity(at);
        context.runAtTickTime(20, () -> {
            context.assertTrue(!jack.isWorking() && jack.getCrude().isEmpty(), "pumpjack ran with no power");
            context.succeed();
        });
    }

    // ---- the refinery ----

    @GameTest(template = "empty", timeoutTicks = 140)
    public static void refineryCracksCrudeOneForOne(GameTestHelper context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.<FuelRefineryBlockEntity>getBlockEntity(at);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setItem(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setTemperature(FuelRefineryBlockEntity.HOT_TEMPERATURE);
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        context.runAtTickTime(120, () -> {
            int feed = refinery.getBiomass();
            int product = refinery.getPetrol();
            context.assertTrue(product >= 2 * FuelRefineryBlockEntity.PETROL_PER_REFINE,
                    "a hot refinery made only " + product + " in 120 ticks");
            context.assertTrue(feed + product == 1000, "crude is not conserved: feed " + feed + " + product " + product);
            context.assertTrue(refinery.getProduct() == FuelRefineryBlockEntity.PRODUCT_RAW_PETROL,
                    "crude with redstone should make raw petrol");
            ItemStack tar = refinery.getItem(FuelRefineryBlockEntity.TAR_SLOT);
            context.assertTrue(tar.is(me.trossty.car.registry.ModItems.TAR)
                            && tar.getCount() == product / FuelRefineryBlockEntity.PETROL_PER_REFINE,
                    "expected a tar a batch, got " + tar);
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void refineryCansRawFuelAsRaw(GameTestHelper context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.<FuelRefineryBlockEntity>getBlockEntity(at);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setItem(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setTemperature(FuelRefineryBlockEntity.HOT_TEMPERATURE);
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        ItemStack can = new ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
        me.trossty.car.item.JerryCans.setUnits(can, 0);
        refinery.setItem(FuelRefineryBlockEntity.CAN_SLOT, can);
        context.runAtTickTime(140, () -> {
            ItemStack filled = refinery.getItem(FuelRefineryBlockEntity.CAN_SLOT);
            context.assertTrue(!me.trossty.car.item.JerryCans.isEmpty(filled), "the can stayed empty");
            context.assertTrue(CarEntity.isRaw(filled), "raw petrol came out as clean");
            context.assertTrue(CarEntity.fuelIn(filled) == CarSetup.Fuel.PETROL, "it is not petrol");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void coldRefineryOnlyWarms(GameTestHelper context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.<FuelRefineryBlockEntity>getBlockEntity(at);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setItem(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        context.runAtTickTime(40, () -> {
            context.assertTrue(refinery.getPetrol() == 0, "a cold column cracked crude");
            context.assertTrue(refinery.getTemperature() > 0, "the heater did not warm the column");
            context.succeed();
        });
    }

    // ---- the hydrotreater and the pipes ----

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void hydrotreaterCleansEveryUnit(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, at);
        HydrotreaterBlockEntity plant = context.<HydrotreaterBlockEntity>getBlockEntity(at);
        plant.getInput().set(ModFluids.RAW_DIESEL, 100);
        plant.setEnergy(HydrotreaterBlockEntity.ENERGY_CAPACITY);
        context.runAtTickTime(40, () -> {
            context.assertTrue(plant.getInput().isEmpty(), "raw diesel left: " + plant.getInput().getUnits());
            context.assertTrue(plant.getOutput().getUnits() == 100 && plant.getOutput().getFluid() == ModFluids.DIESEL,
                    "expected 100 clean diesel, got " + plant.getOutput().getUnits());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void pipesCarryPetrolIntoATank(GameTestHelper context) {
        BlockPos at = new BlockPos(1, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, at);
        HydrotreaterBlockEntity plant = context.<HydrotreaterBlockEntity>getBlockEntity(at);
        plant.getOutput().set(ModFluids.PETROL, 400);
        BlockPos p1 = new BlockPos(2, 1, 3);
        BlockPos p2 = new BlockPos(3, 1, 3);
        BlockPos tankAt = new BlockPos(4, 1, 3);
        context.setBlock(p1, ModBlocks.FUEL_PIPE);
        context.setBlock(p2, ModBlocks.FUEL_PIPE);
        context.setBlock(tankAt, ModBlocks.FUEL_TANK);
        joinPipes(context, p1, p2);
        context.runAtTickTime(140, () -> {
            FuelTankBlockEntity tank = context.<FuelTankBlockEntity>getBlockEntity(tankAt);
            int total = plant.getOutput().getUnits() + tank.getPetrol();
            context.assertTrue(tank.getPetrol() > 0, "no petrol reached the tank");
            context.assertTrue(total == 400, "petrol is not conserved: " + total + " of 400");
            context.succeed();
        });
    }

    // ---- pipes carry any liquid ----

    /** A pumpjack, two pipes and a yard tank: crude reaches the tank. */
    private static FuelTankBlockEntity crudeLine(GameTestHelper context, boolean valve) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 2; z <= 4; z++) {
                context.setBlock(new BlockPos(x, 0, z), ModBlocks.OIL_DEPOSIT);
            }
        }
        BlockPos at = new BlockPos(3, 1, 4);
        machine(context, ModBlocks.PUMPJACK, at);
        context.<PumpjackBlockEntity>getBlockEntity(at).setEnergy(PumpjackBlockEntity.ENERGY_CAPACITY);
        BlockPos p1 = new BlockPos(3, 1, 5);
        BlockPos middle = new BlockPos(3, 1, 6);
        BlockPos p2 = new BlockPos(3, 1, 7);
        context.setBlock(p1, ModBlocks.FUEL_PIPE);
        if (valve) {
            context.setBlock(middle, ModBlocks.FUEL_VALVE.defaultBlockState()
                    .setValue(me.trossty.car.block.FuelValveBlock.FACING, Direction.SOUTH)
                    .setValue(me.trossty.car.block.FuelValveBlock.OPEN, false));
        } else {
            context.setBlock(middle, ModBlocks.FUEL_PIPE);
        }
        context.setBlock(p2, ModBlocks.FUEL_PIPE);
        context.setBlock(new BlockPos(3, 1, 8), ModBlocks.FUEL_TANK);
        joinPipes(context, valve ? new BlockPos[] {p1, p2} : new BlockPos[] {p1, middle, p2});
        return context.<FuelTankBlockEntity>getBlockEntity(new BlockPos(3, 1, 8));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void pipesCarryCrudeFromAPumpjack(GameTestHelper context) {
        FuelTankBlockEntity tank = crudeLine(context, false);
        context.runAtTickTime(180, () -> {
            context.assertTrue(tank.getFluid() == ModFluids.CRUDE_OIL && tank.getAmount() > 0,
                    "no crude reached the tank: " + tank.getFluid() + " " + tank.getAmount());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void aClosedValveHoldsTheLineAndAnOpenOneRunsIt(GameTestHelper context) {
        FuelTankBlockEntity tank = crudeLine(context, true);
        BlockPos valveAt = new BlockPos(3, 1, 6);
        context.runAtTickTime(100, () -> {
            context.assertTrue(tank.getAmount() == 0, "crude got past a closed valve: " + tank.getAmount());
            BlockPos pos = context.absolutePos(valveAt);
            BlockState state = context.getLevel().getBlockState(pos);
            context.getLevel().setBlock(pos, state.setValue(me.trossty.car.block.FuelValveBlock.OPEN, true), Block.UPDATE_ALL);
        });
        context.runAtTickTime(240, () -> {
            context.assertTrue(tank.getFluid() == ModFluids.CRUDE_OIL && tank.getAmount() > 0,
                    "an open valve passed nothing: " + tank.getAmount());
            context.succeed();
        });
    }

    /** Tank, pipe, pipe, tank, with a valve at the start if asked for; the first tank holds 500 petrol. */
    private static FuelTankBlockEntity[] tankToTank(GameTestHelper context, boolean valve) {
        BlockPos from = new BlockPos(1, 1, 3);
        BlockPos to = new BlockPos(5, 1, 3);
        context.setBlock(from, ModBlocks.FUEL_TANK);
        context.setBlock(to, ModBlocks.FUEL_TANK);
        if (valve) {
            context.setBlock(new BlockPos(2, 1, 3), ModBlocks.FUEL_VALVE.defaultBlockState()
                    .setValue(me.trossty.car.block.FuelValveBlock.FACING, Direction.EAST));
        } else {
            context.setBlock(new BlockPos(2, 1, 3), ModBlocks.FUEL_PIPE);
        }
        context.setBlock(new BlockPos(3, 1, 3), ModBlocks.FUEL_PIPE);
        context.setBlock(new BlockPos(4, 1, 3), ModBlocks.FUEL_PIPE);
        joinPipes(context, valve ? new BlockPos[] {new BlockPos(3, 1, 3), new BlockPos(4, 1, 3)}
                : new BlockPos[] {new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(4, 1, 3)});
        FuelTankBlockEntity first = context.<FuelTankBlockEntity>getBlockEntity(from);
        first.fill(ModFluids.PETROL, 500);
        return new FuelTankBlockEntity[] {first, context.<FuelTankBlockEntity>getBlockEntity(to)};
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void twoTanksOnALineSitStill(GameTestHelper context) {
        FuelTankBlockEntity[] tanks = tankToTank(context, false);
        context.runAtTickTime(40, () -> {
            context.assertTrue(tanks[0].getAmount() == 500 && tanks[1].getAmount() == 0,
                    "a line moved petrol between two tanks with no valve: " + tanks[0].getAmount() + " / " + tanks[1].getAmount());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aValvePullsOneTankIntoAnother(GameTestHelper context) {
        FuelTankBlockEntity[] tanks = tankToTank(context, true);
        context.runAtTickTime(40, () -> {
            context.assertTrue(tanks[0].getAmount() == 0 && tanks[1].getAmount() == 500
                            && tanks[1].getFluid() == ModFluids.PETROL,
                    "the valve did not empty one tank into the other: " + tanks[0].getAmount() + " / " + tanks[1].getAmount());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void aLineTakesTwoLiquidsInTurnWithoutMixing(GameTestHelper context) {
        // Two drums, each with a valve pushing into one line, and two empty tanks on it.
        BlockPos[] line = {new BlockPos(2, 1, 2), new BlockPos(2, 1, 3), new BlockPos(2, 1, 4)};
        for (BlockPos at : line) {
            context.setBlock(at, ModBlocks.FUEL_PIPE);
        }
        context.setBlock(new BlockPos(0, 1, 2), ModBlocks.FUEL_TANK);
        context.setBlock(new BlockPos(1, 1, 2), ModBlocks.FUEL_VALVE.defaultBlockState()
                .setValue(me.trossty.car.block.FuelValveBlock.FACING, Direction.EAST));
        context.setBlock(new BlockPos(0, 1, 4), ModBlocks.FUEL_TANK);
        context.setBlock(new BlockPos(1, 1, 4), ModBlocks.FUEL_VALVE.defaultBlockState()
                .setValue(me.trossty.car.block.FuelValveBlock.FACING, Direction.EAST));
        context.setBlock(new BlockPos(3, 1, 2), ModBlocks.FUEL_TANK);
        context.setBlock(new BlockPos(3, 1, 4), ModBlocks.FUEL_TANK);
        joinPipes(context, line);
        context.<FuelTankBlockEntity>getBlockEntity(new BlockPos(0, 1, 2)).fill(ModFluids.PETROL, 300);
        context.<FuelTankBlockEntity>getBlockEntity(new BlockPos(0, 1, 4)).fill(ModFluids.DIESEL, 300);
        context.runAtTickTime(60, () -> {
            FuelTankBlockEntity a = context.<FuelTankBlockEntity>getBlockEntity(new BlockPos(3, 1, 2));
            FuelTankBlockEntity b = context.<FuelTankBlockEntity>getBlockEntity(new BlockPos(3, 1, 4));
            context.assertTrue(a.getAmount() == 300 && b.getAmount() == 300 && a.getFluid() != b.getFluid(),
                    "expected one tank of each, got " + a.getFluid() + " " + a.getAmount()
                            + " and " + b.getFluid() + " " + b.getAmount());
            context.succeed();
        });
    }

    // ---- energy API sanity ----

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void machinesOfferPowerDoors(GameTestHelper context) {
        BlockPos plantAt = new BlockPos(3, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, plantAt);
        context.runAtTickTime(2, () -> {
            for (Direction side : Direction.values()) {
                IEnergyStorage door = context.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,
                        context.absolutePos(plantAt.above()), side);
                context.assertTrue(door != null && door.canReceive(),
                        "the hydrotreater's upper cell has no power door on " + side);
            }
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 10)
    public static void liftPostsAreSolid(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockState lift = ModBlocks.VEHICLE_LIFT.defaultBlockState()
                    .setValue(me.trossty.car.block.VehicleLiftBlock.FACING, facing);
            context.setBlock(at, lift);
            ((MultiblockBlock) lift.getBlock()).placeParts(context.getLevel(), context.absolutePos(at), lift);
            // the posts stand a block either side, across the way a car drives on, two blocks high
            Direction side = facing.getClockWise();
            for (Direction out : new Direction[] {side, side.getOpposite()}) {
                double px = 0.5 + out.getStepX() * 0.3;
                double pz = 0.5 + out.getStepZ() * 0.3;
                for (BlockPos post : new BlockPos[] {at.relative(out), at.relative(out).above()}) {
                    var shape = context.getBlockState(post).getCollisionShape(context.getLevel(), context.absolutePos(post));
                    context.assertTrue(shape.toAabbs().stream().anyMatch(box -> box.contains(px, 0.5, pz)),
                            "no post " + out + " of a lift facing " + facing + " at " + post);
                }
            }
            var plate = context.getBlockState(at).getCollisionShape(context.getLevel(), context.absolutePos(at));
            context.assertTrue(plate.toAabbs().stream().noneMatch(box -> box.contains(0.5, 1.0, 0.5)),
                    "the middle of a lift facing " + facing + " is not clear to stand in");
            context.setBlock(at, net.minecraft.world.level.block.Blocks.AIR);
            context.assertBlockNotPresent(ModBlocks.VEHICLE_LIFT, at.relative(side).above());
        }
        context.succeed();
    }

    /** Looking at a column finds the lift, and a click on it works the lift like one on the plate. */
    @GameTest(template = "empty", timeoutTicks = 10)
    public static void liftColumnsAreTheLift(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        BlockState lift = ModBlocks.VEHICLE_LIFT.defaultBlockState();
        context.setBlock(at, lift);
        ((MultiblockBlock) lift.getBlock()).placeParts(context.getLevel(), context.absolutePos(at), lift);
        // facing north the columns are east and west; look at the top of the west one from the south
        BlockPos post = context.absolutePos(at.west().above());
        var eye = new net.minecraft.world.phys.Vec3(post.getX() + 0.2, post.getY() + 0.4, post.getZ() + 3.5);
        var hit = clip(context, eye, eye.add(0, 0, -4));
        context.assertTrue(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && hit.getBlockPos().equals(post),
                "looking at the column's top found " + hit.getBlockPos() + ", not the column");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        context.getLevel().getBlockState(post).useWithoutItem(context.getLevel(), player, hit);
        context.assertTrue(context.<me.trossty.car.block.entity.VehicleLiftBlockEntity>getBlockEntity(at).getStop() == 1,
                "a click on the column did not send the lift up a stop");
        context.succeed();
    }

    /** The bike lift's deck is where it is drawn at every height, from one end to the other. */
    @GameTest(template = "empty", timeoutTicks = 10)
    public static void bikeLiftDeckIsWhereItIsDrawn(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        BlockState lift = ModBlocks.MOTORCYCLE_LIFT.defaultBlockState();
        var block = (me.trossty.car.block.VehicleLiftBlock) lift.getBlock();
        context.setBlock(at, lift);
        block.placeParts(context.getLevel(), context.absolutePos(at), lift);
        // facing north the deck runs from half a block out north to half a block out south
        BlockPos[] cells = {at.north(), at, at.south()};
        double[] along = {0.75, 0.5, 0.1};
        for (int level = 0; level <= me.trossty.car.block.entity.VehicleLiftBlockEntity.MAX_LEVEL; level++) {
            block.setLevel(context.getLevel(), context.absolutePos(at), context.getBlockState(at), level);
            double top = (2.0 + 3.75 * level) / 16.0;
            for (int i = 0; i < cells.length; i++) {
                double z = along[i];
                for (double y : new double[] {top - 0.02, top + 0.1}) {
                    // a deck risen out of its block is in the one above
                    BlockPos cell = y >= 1.0 ? cells[i].above() : cells[i];
                    double local = y >= 1.0 ? y - 1.0 : y;
                    var outline = context.getBlockState(cell).getShape(context.getLevel(), context.absolutePos(cell));
                    boolean found = outline.toAabbs().stream().anyMatch(box -> box.contains(0.5, local, z));
                    context.assertTrue(found == (y < top), (y < top ? "no deck in " : "something over the deck in ")
                            + cell + " at level " + level);
                }
            }
            // from straight above, the deck's end is found where it is
            BlockPos front = context.absolutePos(at.north());
            var eye = new net.minecraft.world.phys.Vec3(front.getX() + 0.5, front.getY() + 3, front.getZ() + 0.75);
            var hit = clip(context, eye, eye.add(0, -4, 0));
            context.assertTrue(Math.abs(hit.getLocation().y - front.getY() - top) < 1.0E-4
                            && context.getLevel().getBlockState(hit.getBlockPos()).is(ModBlocks.MOTORCYCLE_LIFT),
                    "looking down on the deck's end at level " + level + " found " + hit.getLocation());
            // and from the side, level with it, so is the wheel vise on the back end
            BlockPos back = context.absolutePos(at.south());
            var side = new net.minecraft.world.phys.Vec3(back.getX() + 3.5, back.getY() + top + 0.2, back.getZ() + 0.35);
            hit = clip(context, side, side.add(-4, 0, 0));
            context.assertTrue(hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                            && context.getLevel().getBlockState(hit.getBlockPos()).is(ModBlocks.MOTORCYCLE_LIFT),
                    "looking across at the vise at level " + level + " found " + hit.getBlockPos());
        }
        context.succeed();
    }

    private static net.minecraft.world.phys.BlockHitResult clip(GameTestHelper context, net.minecraft.world.phys.Vec3 from,
                                                               net.minecraft.world.phys.Vec3 to) {
        return context.getLevel().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty()));
    }

    // ---- battery bank and charging post ----

    private static BatteryBankBlockEntity battery(GameTestHelper context, BlockPos at, Direction front, long energy) {
        context.setBlock(at, ModBlocks.BATTERY_BANK.defaultBlockState().setValue(BatteryBankBlock.FACING, front));
        BatteryBankBlockEntity bank = context.<BatteryBankBlockEntity>getBlockEntity(at);
        bank.setEnergy(energy);
        return bank;
    }

    private static CarEntity car(GameTestHelper context, BlockPos at, boolean electric) {
        CarEntity car = context.spawn(ModEntities.CAR, at);
        int setup = CarSetup.DEFAULT;
        if (electric) {
            setup = CarSetup.withVariant(setup, CarPart.ENGINE, CarSetup.ELECTRIC_MOTOR);
        }
        car.setSetup(setup);
        car.setFuel(0.0F);
        return car;
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void batteryGivesOnlyOutOfItsFront(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        BatteryBankBlockEntity bank = battery(context, at, Direction.SOUTH, 50_000);
        // A second battery takes in through its back off each side's cable.
        BatteryBankBlockEntity ahead = battery(context, new BlockPos(3, 1, 5), Direction.SOUTH, 0);
        BatteryBankBlockEntity behind = battery(context, new BlockPos(3, 1, 1), Direction.NORTH, 0);
        cable(context, new BlockPos(3, 1, 4), new BlockPos(3, 1, 2));
        context.runAtTickTime(20, () -> {
            context.assertTrue(ahead.getEnergy() > 0, "the battery on the front got nothing");
            context.assertTrue(behind.getEnergy() == 0,
                    "the battery gave " + behind.getEnergy() + " out of its back");
            context.assertTrue(bank.getEnergy() < 50_000, "the battery did not drain");
            context.assertTrue(context.getBlockState(at).getValue(BatteryBankBlock.LEVEL) == 1,
                    "a quarter-full battery shows " + context.getBlockState(at).getValue(BatteryBankBlock.LEVEL) + " bars");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void batteryFeedsAMachineDownACable(GameTestHelper context) {
        BatteryBankBlockEntity bank = battery(context, new BlockPos(3, 1, 1), Direction.SOUTH, 50_000);
        cable(context, new BlockPos(3, 1, 2), new BlockPos(3, 1, 3));
        context.setBlock(new BlockPos(3, 1, 4), ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(new BlockPos(3, 1, 4));
        post.setEnergy(0);
        context.runAtTickTime(20, () -> {
            context.assertTrue(post.getEnergy() > 0, "the post got nothing from the battery's front");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void batteryOnTheSideOfARunStillFeedsIt(GameTestHelper context) {
        // The bank sits beside the run, front turned away; the run feeds a post.
        BatteryBankBlockEntity bank = battery(context, new BlockPos(2, 1, 2), Direction.WEST, 50_000);
        cable(context, new BlockPos(3, 1, 2), new BlockPos(3, 1, 3));
        context.setBlock(new BlockPos(3, 1, 4), ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(new BlockPos(3, 1, 4));
        post.setEnergy(0);
        context.runAtTickTime(20, () -> {
            context.assertTrue(post.getEnergy() > 0, "a bank on the run's side gave nothing: " + bank.getEnergy());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void batteryFillsFromAGenerator(GameTestHelper context) {
        coalGenerator(context, new BlockPos(3, 1, 1), 4);
        cable(context, new BlockPos(3, 1, 2));
        BlockPos at = new BlockPos(3, 1, 3);
        BatteryBankBlockEntity bank = battery(context, at, Direction.SOUTH, 0);
        context.runAtTickTime(100, () -> {
            context.assertTrue(bank.getEnergy() > 0, "the battery took nothing from the generator");
            context.assertTrue(context.getBlockState(at).getValue(BatteryBankBlock.LEVEL) >= 1, "no bar lit on a charged battery");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void chargingPostChargesAnElectricCar(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(at);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), true);
        context.runAtTickTime(2, () -> context.assertTrue(post.plugInto(car, null), "the plug would not go in"));
        context.runAtTickTime(20, () -> {
            context.assertTrue(car.isElectric(), "the test car is not electric");
            context.assertTrue(car.getFuel() > 0.0F, "the electric car was not charged");
            long used = ChargingPostBlockEntity.CAPACITY - post.getEnergy();
            float expected = used / (float) CarEntity.ENERGY_PER_CHARGE_UNIT;
            context.assertTrue(Math.abs(car.getFuel() - expected) < 0.01F,
                    "charge is not conserved: post gave " + used + " E, car got " + car.getFuel());
            context.assertTrue(context.getBlockState(at).getValue(ChargingPostBlock.LIT), "the post screen is dark while charging");
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void chargingPostLeavesAnUnpluggedCarAlone(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(at);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), true);
        context.runAtTickTime(20, () -> {
            context.assertTrue(car.getFuel() == 0.0F, "a car took charge without the plug in: " + car.getFuel());
            context.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void chargingPostIgnoresAPetrolCar(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(at);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), false);
        context.runAtTickTime(2, () -> context.assertFalse(post.plugInto(car, null), "the plug went into a petrol car"));
        context.runAtTickTime(20, () -> {
            context.assertTrue(car.getFuel() == 0.0F, "a petrol car took charge: " + car.getFuel());
            context.assertTrue(post.getEnergy() == ChargingPostBlockEntity.CAPACITY, "the post spent energy on nothing");
            context.assertTrue(car.charge(10.0F) == 0.0F, "charge() filled a petrol tank");
            context.succeed();
        });
    }

    // ---- the mechanic's side: mileage and the code reader ----

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void codeReaderListsWorstFirst(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.parts().setCondition(CarPart.BRAKE_PADS, CarPart.MAX_CONDITION * 0.10F);
        car.parts().setCondition(CarPart.WHEELS, CarPart.MAX_CONDITION * 0.30F);
        car.parts().setCondition(CarPart.AIR_FILTER, CarPart.MAX_CONDITION * 0.45F);
        car.parts().setCondition(CarPart.EXHAUST, 0.0F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0420", "C0040", "C0750", "P0101")),
                "the reader listed " + codes);
        context.succeed();
    }

    /** The wheel at this end (1 front, -1 rear) on this side (1 right, -1 left). */
    private static int corner(CarEntity car, int end, int side) {
        for (int wheel = 0; wheel < car.parts().placesOf(CarPart.WHEELS); wheel++) {
            if (car.parts().axle(wheel) == end && car.parts().lateral(wheel) == side) {
                return wheel;
            }
        }
        throw new AssertionError("no wheel at " + end + "/" + side);
    }

    /** The front does most of the stopping, so the front pads go first; one weak corner pulls the car round. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void frontPadsWearFirstAndAWeakCornerPulls(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        for (int stop = 0; stop < 400; stop++) {
            car.parts().wearBrakes(1.0F);
            car.parts().wearBrakes(0.6F);
        }
        float front = car.parts().getCondition(CarPart.BRAKE_PADS, corner(car, 1, -1))
                + car.parts().getCondition(CarPart.BRAKE_PADS, corner(car, 1, 1));
        float rear = car.parts().getCondition(CarPart.BRAKE_PADS, corner(car, -1, -1))
                + car.parts().getCondition(CarPart.BRAKE_PADS, corner(car, -1, 1));
        context.assertTrue(front < rear && rear < 2 * CarPart.MAX_CONDITION,
                "front pads at " + front + ", rear at " + rear);

        car.parts().setCondition(CarPart.BRAKE_PADS, CarPart.MAX_CONDITION);
        context.assertTrue(Math.abs(car.parts().brakePull()) < 1e-3F, "even brakes pulled " + car.parts().brakePull());
        car.parts().setCondition(CarPart.BRAKE_PADS, corner(car, 1, -1), CarPart.MAX_CONDITION * 0.05F);
        float left = car.parts().brakePull();
        car.parts().setCondition(CarPart.BRAKE_PADS, CarPart.MAX_CONDITION);
        car.parts().setCondition(CarPart.BRAKE_PADS, corner(car, 1, 1), CarPart.MAX_CONDITION * 0.05F);
        float right = car.parts().brakePull();
        context.assertTrue(Math.abs(left) > 0.01F && Math.signum(left) == -Math.signum(right),
                "a worn left pad pulled " + left + ", a worn right one " + right);
        // The reader names the corner, not the set.
        var pads = FaultCodes.read(car).stream().filter(f -> f.code().equals("C0040")).findFirst();
        context.assertTrue(pads.isPresent() && pads.get().at() != null, "the reader did not say which pad");
        context.succeed();
    }

    /** One fouled plug sets its own cylinder's code; three at once only say the engine runs rough. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void misfireNamesItsCylinder(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.parts().setCondition(CarPart.SPARK_PLUGS, 2, CarPart.MAX_CONDITION * 0.2F);
        var faults = FaultCodes.read(car);
        var misfire = faults.stream().filter(f -> f.code().equals("P0303")).findFirst();
        context.assertTrue(misfire.isPresent(), "a fouled third plug read " + faults.stream().map(FaultCodes.Fault::code).toList());
        context.assertTrue(misfire.get().suspects().contains(CarPart.ENGINE),
                "a misfire only blamed " + misfire.get().suspects());
        car.parts().setCondition(CarPart.SPARK_PLUGS, 0, CarPart.MAX_CONDITION * 0.2F);
        car.parts().setCondition(CarPart.SPARK_PLUGS, 1, CarPart.MAX_CONDITION * 0.2F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.contains("P0300") && !codes.contains("P0303"), "three bad plugs read " + codes);
        context.succeed();
    }

    /** A slipping drive reads the same whether the box or the clutch is going: one code, both suspects. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void slippingDriveBlamesBoxAndClutch(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.parts().setCondition(CarPart.CLUTCH, CarPart.MAX_CONDITION * 0.2F);
        var faults = FaultCodes.read(car);
        var codes = faults.stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.contains("P0730") && !codes.contains("P0805"), "a worn clutch read " + codes);
        context.assertTrue(faults.stream().filter(f -> f.code().equals("P0730")).findFirst().get().suspects()
                .equals(java.util.List.of(CarPart.TRANSMISSION, CarPart.CLUTCH)), "the slip did not name both");
        context.succeed();
    }

    /** A V8 takes eight plugs, and what is bolted over a part hides how worn it is. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void eightPlugsAndHiddenWear(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.setSetup(CarSetup.withVariant(car.getSetup(), CarPart.ENGINE,
                CarSetup.engineVariant(CarSetup.Layout.V8, CarSetup.Fuel.PETROL)));
        context.assertTrue(car.parts().countOf(CarPart.SPARK_PLUGS) == 8 && car.parts().placesOf(CarPart.SPARK_PLUGS) == 8,
                "a V8 takes " + car.parts().countOf(CarPart.SPARK_PLUGS) + " plugs");
        context.assertTrue(car.workshop().canInspect(CarPart.AIR_FILTER), "the air filter on top could not be seen");
        context.assertTrue(car.workshop().hiddenBy(CarPart.CLUTCH) == CarPart.DRIVESHAFT, "the clutch was hidden by "
                + car.workshop().hiddenBy(CarPart.CLUTCH));
        context.assertTrue(!car.workshop().canInspect(CarPart.BRAKE_PADS), "pads were seen through a wheel");
        context.succeed();
    }

    /** Each vehicle has its own box: a lorry eight short gears, a scooter one; the torque curve averages out. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void gearboxComesFromTheVehicle(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        context.assertTrue(car.gearbox().gearCount() == 5, "a plain car has " + car.gearbox().gearCount() + " gears");
        car.setModel(net.minecraft.resources.ResourceLocation.parse("car:mercedes_atego"));
        context.assertTrue(car.gearbox().gearCount() == 8, "the lorry has " + car.gearbox().gearCount() + " gears");
        car.setModel(net.minecraft.resources.ResourceLocation.parse("car:vespa_primavera"));
        context.assertTrue(car.gearbox().gearCount() == 1, "the scooter has " + car.gearbox().gearCount() + " gears");
        for (CarSetup.Layout layout : CarSetup.Layout.values()) {
            float sum = 0.0F;
            for (int i = 0; i < 16; i++) {
                sum += CarSetup.torque(layout, CarSetup.Fuel.PETROL,
                        net.minecraft.util.Mth.lerp((i + 0.5F) / 16, CarSetup.DOWNSHIFT, CarSetup.UPSHIFT));
            }
            context.assertTrue(Math.abs(sum / 16 - 1.0F) < 1e-3F, layout + " averages " + sum / 16);
        }
        context.assertTrue(CarSetup.torque(CarSetup.Layout.INLINE4, CarSetup.Fuel.DIESEL, 0.4F)
                        > CarSetup.torque(CarSetup.Layout.INLINE4, CarSetup.Fuel.DIESEL, 0.85F),
                "a diesel pulled harder at the top than low down");
        context.assertTrue(CarSetup.torque(CarSetup.Layout.INLINE4, CarSetup.Fuel.ELECTRIC, 0.1F)
                        > CarSetup.torque(CarSetup.Layout.INLINE4, CarSetup.Fuel.ELECTRIC, 0.9F),
                "an electric motor ran out of push at standstill");
        context.succeed();
    }

    // ---- where a car is hit ----

    /** The side that faces a point or a direction; a point beside the front wheel is on the side, not the front. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aKnockLandsOnTheSideFacingIt(GameTestHelper context) {
        net.minecraft.world.phys.Vec3 ahead = new net.minecraft.world.phys.Vec3(0, 0, 1);
        context.assertTrue(BodyZone.facing(0.0F, ahead, 1.0F) == BodyZone.FRONT, "ahead was not the front");
        context.assertTrue(BodyZone.facing(0.0F, ahead.reverse(), 1.0F) == BodyZone.REAR, "behind was not the rear");
        context.assertTrue(BodyZone.facing(0.0F, new net.minecraft.world.phys.Vec3(1, 0, 0), 1.0F) == BodyZone.LEFT,
                "+x was not the left of a car facing +z");
        context.assertTrue(BodyZone.facing(0.0F, new net.minecraft.world.phys.Vec3(-1, 0, 0), 1.0F) == BodyZone.RIGHT,
                "-x was not the right of a car facing +z");
        context.assertTrue(BodyZone.facing(0.0F, new net.minecraft.world.phys.Vec3(1.2, 0, 1.3), BodyZone.LENGTH_TO_WIDTH)
                == BodyZone.LEFT, "a point beside the front wheel counted as the front");
        context.assertTrue(BodyZone.facing(90.0F, new net.minecraft.world.phys.Vec3(-1, 0, 0), 1.0F) == BodyZone.FRONT,
                "turned to face -x, -x was not the front");
        context.assertTrue(BodyZone.facing(0.0F, new net.minecraft.world.phys.Vec3(0, 1, 0), 1.0F) == null,
                "straight up faced a side");
        context.succeed();
    }

    /** A head-on stoves in the front: the radiator and the front springs, never the tank, and the back is untouched. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aHeadOnHitsTheRadiatorNotTheTank(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.setFuel(car.getMaxFuel());
        car.damage().applyReportedCrash(context.getLevel(), 8.0F, BodyZone.FRONT);
        context.assertTrue(car.damage().getZone(BodyZone.FRONT) < CarEntity.MAX_HEALTH, "the front took no dent");
        context.assertTrue(car.damage().getZone(BodyZone.REAR) == CarEntity.MAX_HEALTH, "the back was dented by a head-on");
        context.assertTrue(car.parts().getCondition(CarPart.RADIATOR) < CarPart.MAX_CONDITION, "the radiator came through a head-on");
        context.assertTrue(car.parts().getCondition(CarPart.EXHAUST) == CarPart.MAX_CONDITION, "the exhaust took a head-on");
        int front = corner(car, 1, 1);
        int rear = corner(car, -1, 1);
        context.assertTrue(car.parts().getCondition(CarPart.SUSPENSION, front) < CarPart.MAX_CONDITION,
                "the front springs came through a head-on");
        context.assertTrue(car.parts().getCondition(CarPart.SUSPENSION, rear) == CarPart.MAX_CONDITION,
                "the rear springs took a head-on");
        context.assertFalse(car.isLeaking(), "a head-on holed the tank through the engine bay");
        context.assertTrue(car.upkeep().getAlignment() != 0.0F, "a head-on left the steering true");
        context.assertTrue(FaultCodes.read(car).stream().noneMatch(f -> f.code().startsWith("B")),
                "a dent the body can take was read as damage");
        context.succeed();
    }

    /** Backed into a wall: the exhaust and the back springs take it, the radiator and the steering do not. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aRearKnockHitsTheExhaust(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.damage().applyReportedCrash(context.getLevel(), 6.0F, BodyZone.REAR);
        context.assertTrue(car.damage().getZone(BodyZone.REAR) < CarEntity.MAX_HEALTH, "the back took no dent");
        context.assertTrue(car.damage().getZone(BodyZone.FRONT) == CarEntity.MAX_HEALTH, "the front was dented from behind");
        context.assertTrue(car.parts().getCondition(CarPart.EXHAUST) < CarPart.MAX_CONDITION, "the exhaust came through");
        context.assertTrue(car.parts().getCondition(CarPart.RADIATOR) == CarPart.MAX_CONDITION, "the radiator was hit from behind");
        context.assertTrue(car.parts().getCondition(CarPart.SUSPENSION, corner(car, -1, -1)) < CarPart.MAX_CONDITION,
                "the rear springs came through");
        context.assertTrue(car.parts().getCondition(CarPart.SUSPENSION, corner(car, 1, -1)) == CarPart.MAX_CONDITION,
                "the front springs took a knock from behind");
        context.assertTrue(car.upkeep().getAlignment() == 0.0F, "a knock on the back bent the steering");
        context.succeed();
    }

    /** Scraped down the left: the left tyres and springs wear, the right ones do not, and it pulls left. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aSideSwipeKerbsThatSide(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.damage().applyReportedCrash(context.getLevel(), 6.0F, BodyZone.LEFT);
        context.assertTrue(car.damage().getZone(BodyZone.LEFT) < CarEntity.MAX_HEALTH, "the left took no dent");
        context.assertTrue(car.damage().getZone(BodyZone.RIGHT) == CarEntity.MAX_HEALTH, "the right was dented");
        for (int end : new int[] {1, -1}) {
            context.assertTrue(car.parts().getCondition(CarPart.WHEELS, corner(car, end, -1)) < CarPart.MAX_CONDITION,
                    "a left tyre came through a side-swipe");
            context.assertTrue(car.parts().getCondition(CarPart.WHEELS, corner(car, end, 1)) == CarPart.MAX_CONDITION,
                    "a right tyre was kerbed by a hit on the left");
            context.assertTrue(car.parts().getCondition(CarPart.SUSPENSION, corner(car, end, 1)) == CarPart.MAX_CONDITION,
                    "a right spring took a hit on the left");
        }
        context.assertTrue(car.parts().getCondition(CarPart.RADIATOR) == CarPart.MAX_CONDITION, "the radiator took a side-swipe");
        context.assertTrue(car.upkeep().getAlignment() < 0.0F, "a hit on the left did not pull it left");
        context.succeed();
    }

    /** One side stove in wrecks the car; the same knock on a sound side does not. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void aWreckComesFromOneSideGiving(GameTestHelper context) {
        CarEntity sound = car(context, new BlockPos(2, 1, 3), false);
        sound.damage().setZone(BodyZone.FRONT, 3.0F);
        sound.damage().applyReportedCrash(context.getLevel(), 4.0F, BodyZone.REAR);
        context.assertFalse(sound.isRemoved(), "a knock on a sound back wrecked a car with a dented front");
        context.assertTrue(sound.damage().getHealth() == 3.0F, "the body reads " + sound.damage().getHealth()
                + " instead of its worst side");
        CarEntity stoved = car(context, new BlockPos(5, 1, 3), false);
        stoved.damage().setZone(BodyZone.FRONT, 3.0F);
        stoved.damage().applyReportedCrash(context.getLevel(), 4.0F, BodyZone.FRONT);
        context.assertTrue(stoved.isRemoved(), "a front with nothing left did not wreck the car");
        sound.discard();
        context.succeed();
    }

    /** The hammer goes to the dented side the player stands at, else to the worst one; the reader names the side. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void theHammerFindsTheDentedSide(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.setYRot(0.0F);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(car.getX() + 1.5, car.getY(), car.getZ());
        context.assertTrue(car.damage().sideToBeatOut(player) == null, "a sound car had a side to beat out");
        car.damage().setZone(BodyZone.REAR, 5.0F);
        context.assertTrue(car.damage().sideToBeatOut(player) == BodyZone.REAR,
                "by a sound side, the hammer went to " + car.damage().sideToBeatOut(player));
        car.damage().setZone(BodyZone.LEFT, 15.0F);
        context.assertTrue(car.damage().sideToBeatOut(player) == BodyZone.LEFT,
                "by the dented left, the hammer went to " + car.damage().sideToBeatOut(player));
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.contains("B1002") && !codes.contains("B1003"), "the reader listed " + codes);
        car.discard();
        context.succeed();
    }

    /** Each side keeps its dents through a save, and an old save's single number lands on every side. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void sideDentsSurviveASave(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.damage().setZone(BodyZone.RIGHT, 7.5F);
        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        car.saveWithoutId(saved);
        CarEntity copy = ModEntities.CAR.create(context.getLevel());
        copy.load(saved);
        context.assertTrue(copy.damage().getZone(BodyZone.RIGHT) == 7.5F
                        && copy.damage().getZone(BodyZone.LEFT) == CarEntity.MAX_HEALTH,
                "the sides came back as " + copy.damage().getZone(BodyZone.RIGHT) + "/" + copy.damage().getZone(BodyZone.LEFT));
        saved.remove("Body");
        saved.putFloat("Health", 8.0F);
        CarEntity old = ModEntities.CAR.create(context.getLevel());
        old.load(saved);
        for (BodyZone zone : BodyZone.values()) {
            context.assertTrue(old.damage().getZone(zone) == 8.0F, "an old save's body came back as "
                    + old.damage().getZone(zone) + " on the " + zone.key());
        }
        car.discard();
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void codeReaderSkipsPlugsOnAnElectricCar(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), true);
        car.parts().setCondition(CarPart.SPARK_PLUGS, CarPart.MAX_CONDITION * 0.05F);
        car.parts().setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION * 0.20F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0A1A")), "an electric car read " + codes);
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oldCarsAndTiredPartsWearFaster(GameTestHelper context) {
        CarEntity fresh = car(context, new BlockPos(2, 1, 3), false);
        CarEntity old = car(context, new BlockPos(5, 1, 3), false);
        old.parts().setOdometer(CarParts.HIGH_MILEAGE * 3);
        old.parts().setCondition(CarPart.CLUTCH, CarPart.MAX_CONDITION * 0.5F);
        context.assertTrue(Math.abs(fresh.parts().ageFactor(CarPart.ENGINE) - 1.0F) < 1e-4F,
                "a new car's engine wears at " + fresh.parts().ageFactor(CarPart.ENGINE));
        context.assertTrue(Math.abs(old.parts().ageFactor(CarPart.ENGINE) - (1.0F + CarParts.MILEAGE_WEAR)) < 1e-4F,
                "an old car's fresh engine wears at " + old.parts().ageFactor(CarPart.ENGINE));
        float tired = (1.0F + CarParts.MILEAGE_WEAR) * (1.0F + CarParts.TIRED_WEAR * 0.5F);
        context.assertTrue(Math.abs(old.parts().ageFactor(CarPart.CLUTCH) - tired) < 1e-4F,
                "a half-worn clutch on an old car wears at " + old.parts().ageFactor(CarPart.CLUTCH) + ", not " + tired);
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cableGlowsOnlyWhileCarrying(GameTestHelper context) {
        battery(context, new BlockPos(1, 1, 3), Direction.EAST, 50_000);
        battery(context, new BlockPos(4, 1, 3), Direction.EAST, 0);
        cable(context, new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(6, 1, 3));
        context.runAtTickTime(25, () -> {
            context.assertTrue(context.getBlockState(new BlockPos(3, 1, 3)).getValue(EnergyCableBlock.POWERED),
                    "a cable carrying power is dark");
            context.assertTrue(!context.getBlockState(new BlockPos(6, 1, 3)).getValue(EnergyCableBlock.POWERED),
                    "a cable on its own glows");
            context.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rawFuelMixesByVolumeAndShowsOnTheReader(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.addFuel(10.0F, CarSetup.Fuel.PETROL, false);
        car.addFuel(10.0F, CarSetup.Fuel.PETROL, true);
        context.assertTrue(Math.abs(car.getDirtyFuel() - 0.5F) < 1e-4F,
                "half a tank of raw petrol reads " + car.getDirtyFuel());
        context.assertTrue(me.trossty.car.entity.FaultCodes.read(car).stream()
                        .anyMatch(fault -> fault.code().equals("P0172")),
                "the reader misses dirty fuel");
        car.setFuel(0.0F);
        context.assertTrue(car.getDirtyFuel() == 0.0F, "an empty tank is still dirty");
        context.succeed();
    }

    @GameTest(template = "empty")
    public static void asphaltSparesTheTyres(GameTestHelper context) {
        context.setBlock(new BlockPos(3, 0, 3), ModBlocks.ASPHALT);
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        CarEntity off = car(context, new BlockPos(6, 1, 3), false);
        context.assertTrue(car.onAsphalt(), "a car on asphalt is not on asphalt");
        context.assertTrue(!off.onAsphalt(), "a car off the road is on asphalt");
        context.succeed();
    }

    @GameTest(template = "empty")
    public static void electricMotorItemSaves(GameTestHelper context) {
        var codec = me.trossty.car.registry.ModComponents.PART_VARIANT.codec();
        context.assertTrue(codec != null && codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
                        CarSetup.ELECTRIC_MOTOR).isSuccess(),
                "an electric motor's part variant cannot be saved");
        context.succeed();
    }

    /** Clicks the top of this block with whatever the player holds, as a right click would. */
    private static net.minecraft.world.InteractionResult clickTop(GameTestHelper context,
                                                            net.minecraft.world.entity.player.Player player,
                                                            BlockPos relative) {
        BlockPos at = context.absolutePos(relative);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(0.0, 0.5, 0.0),
                Direction.UP, at, false);
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof net.minecraft.world.item.BlockItem block) {
            return block.place(new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, held, hit));
        }
        return held.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = "empty")
    public static void asphaltIsLaidInLayersLikeSnow(GameTestHelper context) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos road = ground.above();
        context.setBlock(ground, net.minecraft.world.level.block.Blocks.STONE);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.ASPHALT, 16));

        clickTop(context, player, ground);
        context.assertTrue(context.getBlockState(road).getValue(me.trossty.car.block.AsphaltBlock.LAYERS) == 1,
                "the first asphalt is not one layer");
        for (int i = 0; i < 3; i++) {
            clickTop(context, player, road);
        }
        BlockState four = context.getBlockState(road);
        context.assertTrue(four.getValue(me.trossty.car.block.AsphaltBlock.LAYERS) == 4, "four clicks are not four layers");
        double top = four.getCollisionShape(context.getLevel(), context.absolutePos(road)).max(Direction.Axis.Y);
        context.assertTrue(Math.abs(top - 0.5) < 1.0E-6, "four layers do not stand half a block high: " + top);

        // A painted road keeps its paint when it is built up.
        context.setBlock(road, four.setValue(me.trossty.car.block.AsphaltBlock.MARKING,
                me.trossty.car.block.AsphaltBlock.Marking.SOLID));
        for (int i = 0; i < 5; i++) {
            clickTop(context, player, road);
        }
        BlockState full = context.getBlockState(road);
        context.assertTrue(full.getValue(me.trossty.car.block.AsphaltBlock.LAYERS) == 8, "eight layers are not a whole block");
        context.assertTrue(full.getValue(me.trossty.car.block.AsphaltBlock.MARKING) == me.trossty.car.block.AsphaltBlock.Marking.SOLID,
                "building the road up scrubbed its paint off");
        context.assertTrue(context.getBlockState(road.above()).is(ModBlocks.ASPHALT)
                        && context.getBlockState(road.above()).getValue(me.trossty.car.block.AsphaltBlock.LAYERS) == 1,
                "the ninth layer did not start a new block on top");
        context.succeed();
    }

    @GameTest(template = "empty")
    public static void roadPaintPaintsTurnsAndErases(GameTestHelper context) {
        BlockPos road = new BlockPos(2, 1, 2);
        context.setBlock(road.below(), net.minecraft.world.level.block.Blocks.STONE);
        context.setBlock(road, ModBlocks.ASPHALT);
        context.setBlock(new BlockPos(4, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setYRot(180.0F);
        ItemStack tin = new ItemStack(me.trossty.car.registry.ModItems.YELLOW_ROAD_PAINT);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tin);
        var marking = me.trossty.car.block.AsphaltBlock.MARKING;
        var facing = me.trossty.car.block.AsphaltBlock.FACING;

        clickTop(context, player, road);
        BlockState painted = context.getBlockState(road);
        context.assertTrue(painted.getValue(marking) == me.trossty.car.block.AsphaltBlock.Marking.DASHED
                        && painted.getValue(me.trossty.car.block.AsphaltBlock.PAINT) == me.trossty.car.block.AsphaltBlock.Paint.YELLOW
                        && painted.getValue(facing) == Direction.NORTH,
                "a fresh tin did not paint a yellow dashed line running north: " + painted);
        context.assertTrue(tin.getDamageValue() == 1, "painting took no paint");

        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road).getValue(facing) == Direction.EAST,
                "painting the same marking again did not turn it");
        context.assertTrue(tin.getDamageValue() == 1, "turning a marking took more paint");

        context.assertTrue(clickTop(context, player, new BlockPos(4, 0, 2)) == net.minecraft.world.InteractionResult.PASS,
                "road paint went on bare stone");

        me.trossty.car.item.RoadPaintItem.select(tin, me.trossty.car.block.AsphaltBlock.Marking.NONE);
        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road).getValue(marking) == me.trossty.car.block.AsphaltBlock.Marking.NONE,
                "erase left the marking on");

        // The menu's choice, as the server receives it; a bad index changes nothing.
        me.trossty.car.item.RoadPaintItem.choose(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK.ordinal());
        context.assertTrue(me.trossty.car.item.RoadPaintItem.selected(tin) == me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK,
                "the menu's choice was not taken");
        me.trossty.car.item.RoadPaintItem.choose(player, net.minecraft.world.InteractionHand.MAIN_HAND, 99);
        context.assertTrue(me.trossty.car.item.RoadPaintItem.selected(tin) == me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK,
                "a bad index from the menu changed the marking");
        // Sneaking on road opens the menu (client side) and paints nothing.
        BlockState before = context.getBlockState(road);
        player.setShiftKeyDown(true);
        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road) == before, "sneaking painted instead of opening the menu");
        context.succeed();
    }

    @GameTest(template = "empty")
    public static void obdTabletPairsAndLosesSignal(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos stand = context.absolutePos(new BlockPos(1, 1, 1));
        player.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack tablet = new ItemStack(me.trossty.car.registry.ModItems.OBD_SCANNER);
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == null,
                "an unpaired tablet reads a car");
        me.trossty.car.item.ObdScannerItem.pair(tablet, car, player);
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == car,
                "a paired tablet does not read its car");
        car.setPos(car.getX() + 2.0 * me.trossty.car.item.ObdScannerItem.RANGE, car.getY(), car.getZ());
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == null,
                "a tablet still reads a car out of range");
        car.discard();
        context.succeed();
    }

    // ---- servicing by hand ----

    /**
     * Every vehicle gets its own map of where its parts are: the tyre box on
     * its own front wheel, nothing hanging off the body, nothing hidden inside
     * a bigger box where it could never be aimed at, and a lorry's engine is
     * not where a hatchback's is.
     */
    @GameTest(template = "empty")
    public static void partBoxesFitEachVehicle(GameTestHelper context) {
        var registry = context.getLevel().registryAccess()
                .registryOrThrow(me.trossty.car.vehicle.VehicleDefinition.KEY);
        java.util.Map<String, CarPart.Anchor> engines = new java.util.HashMap<>();
        int checked = 0;
        for (var entry : registry.holders().toList()) {
            me.trossty.car.vehicle.VehicleDefinition definition = entry.value();
            String name = String.valueOf(registry.getKey(definition));
            if (definition.wheels().isEmpty()) {
                continue;
            }
            checked++;
            var map = me.trossty.car.entity.PartLayout.of(definition);
            CarPart.Anchor tyre = map.get(CarPart.WHEELS);
            boolean onAWheel = false;
            for (var wheel : definition.wheels()) {
                onAWheel |= Math.abs(tyre.x() + wheel.at().x() / 16.0F) < 1.0E-3F
                        && Math.abs(tyre.y() - wheel.at().y() / 16.0F) < 1.0E-3F
                        && Math.abs(tyre.z() + wheel.at().z() / 16.0F) < 1.0E-3F;
            }
            context.assertTrue(onAWheel, "the tyre box of " + name + " is not on one of its wheels");

            float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            float top = 0.0F;
            for (var box : definition.shape()) {
                minX = Math.min(minX, Math.min(box.from().x(), box.to().x()));
                maxX = Math.max(maxX, Math.max(box.from().x(), box.to().x()));
                minZ = Math.min(minZ, Math.min(box.from().z(), box.to().z()));
                maxZ = Math.max(maxZ, Math.max(box.from().z(), box.to().z()));
                top = Math.max(top, Math.max(box.from().y(), box.to().y()));
            }
            boolean trailer = definition.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.TRAILER;
            for (CarPart part : CarPart.values()) {
                if (trailer && part != CarPart.WHEELS && part != CarPart.SUSPENSION
                        && part != CarPart.BRAKE_PADS && part != CarPart.BRAKE_DISCS) {
                    // A trailer has only its running gear (TrailerEntity#hasPart).
                    continue;
                }
                CarPart.Anchor a = map.get(part);
                // Back into definition pixels to compare with the body.
                float x = -a.x() * 16.0F, y = a.y() * 16.0F, z = -a.z() * 16.0F;
                context.assertTrue(x > minX - 2.0F && x < maxX + 2.0F && z > minZ - 2.0F && z < maxZ + 2.0F
                                && y > 0.0F && y < top,
                        part.key() + " on " + name + " is off the vehicle at " + x + ", " + y + ", " + z);
                for (CarPart other : CarPart.values()) {
                    CarPart.Anchor b = map.get(other);
                    if (other == part || b.size() <= a.size()) {
                        continue;
                    }
                    boolean buried = Math.abs(a.x() - b.x()) + a.size() <= b.size()
                            && Math.abs(a.y() - b.y()) + a.size() <= b.size()
                            && Math.abs(a.z() - b.z()) + a.size() <= b.size();
                    context.assertFalse(buried, part.key() + " on " + name + " is buried inside " + other.key());
                }
            }
            engines.put(registry.getKey(definition).getPath(), map.get(CarPart.ENGINE));
        }
        context.assertTrue(checked >= 5, "only " + checked + " vehicles had a part map");
        CarPart.Anchor hatch = engines.get("toyota_corolla");
        CarPart.Anchor lorry = engines.get("mercedes_atego");
        context.assertTrue(hatch != null && lorry != null && !hatch.equals(lorry),
                "a hatchback and a lorry have their engines in the same place");
        context.succeed();
    }

    /**
     * A part that has come off stays off: clicking the empty slot again with
     * nothing in hand does nothing — not even in creative, where it used to
     * conjure a new one — and the part did not land in the hand you were
     * working with. Holding the spare and clicking puts it back.
     */
    @GameTest(template = "empty")
    public static void removedPartStaysOffUntilRefitted(GameTestHelper context) {
        BlockPos lift = new BlockPos(2, 1, 2);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        CarPart part = CarPart.AIR_FILTER;
        context.assertTrue(car.parts().isFitted(part), "a new car has no air filter");
        car.setLidOpen(CarEntity.BONNET_BIT, true);

        car.workshop().serviceByHand(player, part);
        context.assertFalse(car.parts().isFitted(part), "the air filter did not come off");
        context.assertTrue(player.getMainHandItem().isEmpty(), "the air filter landed in the working hand");
        context.assertTrue(player.getInventory().countItem(part.item()) == 1, "the air filter was not handed over");

        car.workshop().serviceByHand(player, part);
        context.assertFalse(car.parts().isFitted(part), "a second click with an empty hand put it back");
        var creative = context.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        car.workshop().serviceByHand(creative, part);
        context.assertFalse(car.parts().isFitted(part), "creative conjured an air filter into the empty slot");

        int pocket = -1;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(part.item())) {
                pocket = slot;
            }
        }
        ItemStack spare = player.getInventory().removeItemNoUpdate(pocket);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spare);
        car.workshop().serviceByHand(player, part);
        context.assertTrue(car.parts().isFitted(part), "holding the spare and clicking did not fit it");
        context.assertTrue(player.getMainHandItem().isEmpty(), "fitting it did not use the spare up");
        car.discard();
        context.succeed();
    }

    /** A car left roughly beside a lift is fetched square on to it when the lift goes up. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void liftFetchesANearbyCarOnToItself(GameTestHelper context) {
        BlockPos lift = new BlockPos(3, 1, 3);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.FACING, Direction.SOUTH));
        CarEntity car = context.spawn(ModEntities.CAR, new net.minecraft.world.phys.Vec3(5.2, 1.0, 4.4));
        car.setYRot(200.0F);
        context.<me.trossty.car.block.entity.VehicleLiftBlockEntity>getBlockEntity(lift).step(true);
        context.runAtTickTime(40, () -> {
            BlockPos at = context.absolutePos(lift);
            context.assertTrue(Math.abs(car.getX() - (at.getX() + 0.5)) < 0.01
                    && Math.abs(car.getZ() - (at.getZ() + 0.5)) < 0.01, "the car was not set in the middle of the lift");
            context.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(car.getYRot() - 180.0F)) < 0.5F,
                    "the car was not turned to run along the lift: " + car.getYRot());
            context.assertTrue(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), car),
                    "the fetched car is not up for servicing");
            car.discard();
            context.succeed();
        });
    }

    /** The lift stops on the floor, a little way up and right up; nothing is worked on while it is down. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void liftStopsAtThreeHeights(GameTestHelper context) {
        BlockPos lift = new BlockPos(3, 1, 3);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState());
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        var ram = context.<me.trossty.car.block.entity.VehicleLiftBlockEntity>getBlockEntity(lift);
        java.util.function.IntSupplier level = () -> context.getBlockState(lift).getValue(me.trossty.car.block.VehicleLiftBlock.LEVEL);
        context.runAtTickTime(2, () -> {
            context.assertFalse(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), car),
                    "a car on a lowered lift counted as up for servicing");
            ram.step(true);
        });
        context.runAtTickTime(20, () -> {
            context.assertTrue(level.getAsInt() == 1, "the first stop is not a little way up: " + level.getAsInt());
            context.assertTrue(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), car),
                    "a car a little way up was not up for servicing");
            ram.step(true);
            context.assertTrue(ram.step(true) == 2, "the lift went past its top stop");
        });
        context.runAtTickTime(50, () -> {
            context.assertTrue(level.getAsInt() == me.trossty.car.block.entity.VehicleLiftBlockEntity.MAX_LEVEL,
                    "the top stop is not right up: " + level.getAsInt());
            ram.step(false);
            ram.step(false);
        });
        context.runAtTickTime(90, () -> {
            context.assertTrue(level.getAsInt() == 0, "the lift did not come back down: " + level.getAsInt());
            car.discard();
            context.succeed();
        });
    }

    /** A motorbike goes up on the motorbike lift; the car lift's arms leave it alone. */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void bikesGoOnTheBikeLift(GameTestHelper context) {
        BlockPos carLift = new BlockPos(1, 1, 2);
        BlockPos bikeLift = new BlockPos(6, 1, 2);
        context.setBlock(carLift, ModBlocks.VEHICLE_LIFT.defaultBlockState());
        context.setBlock(bikeLift, ModBlocks.MOTORCYCLE_LIFT.defaultBlockState());
        CarEntity onCarLift = context.spawn(ModEntities.MOTORCYCLE, carLift.above());
        CarEntity onBikeLift = context.spawn(ModEntities.MOTORCYCLE, bikeLift.above());
        context.<me.trossty.car.block.entity.VehicleLiftBlockEntity>getBlockEntity(carLift).step(true);
        context.<me.trossty.car.block.entity.VehicleLiftBlockEntity>getBlockEntity(bikeLift).step(true);
        context.runAtTickTime(40, () -> {
            context.assertTrue(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), onBikeLift),
                    "a bike on the raised bike lift is not up for servicing");
            context.assertFalse(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), onCarLift),
                    "the car lift took a bike");
            onCarLift.discard();
            onBikeLift.discard();
            context.succeed();
        });
    }

    /** A car parked beside a raised lift is not on it; one over its platform is. */
    @GameTest(template = "empty")
    public static void besideALiftIsNotOnIt(GameTestHelper context) {
        BlockPos lift = new BlockPos(1, 1, 1);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity beside = context.spawn(ModEntities.CAR, new BlockPos(5, 1, 1));
        CarEntity on = context.spawn(ModEntities.CAR, lift.above());
        context.assertFalse(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), beside),
                "a car beside the lift counted as up on it");
        context.assertTrue(me.trossty.car.block.VehicleLiftBlock.isServicing(context.getLevel(), on),
                "a car over the raised lift did not count as up on it");
        beside.discard();
        on.discard();
        context.succeed();
    }

    /**
     * What sits under the bonnet is worked on with the bonnet up, lift or no
     * lift; the engine itself wants a lift or a hoist beside it.
     */
    @GameTest(template = "empty")
    public static void bonnetPartsWantTheBonnetUp(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 3));
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        car.workshop().removePart(player, CarPart.AIR_FILTER);
        context.assertTrue(car.parts().isFitted(CarPart.AIR_FILTER), "the air filter came out through a shut bonnet");
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        car.workshop().removePart(player, CarPart.AIR_FILTER);
        context.assertFalse(car.parts().isFitted(CarPart.AIR_FILTER), "the air filter did not come out with the bonnet up");
        context.assertFalse(car.workshop().isWorkable(CarPart.ENGINE), "the engine came free without a lift or a hoist");
        context.assertFalse(car.workshop().isWorkable(CarPart.WHEELS), "the wheels came off a car on the ground");
        hoistAtNose(context, car);
        context.assertTrue(car.workshop().isWorkable(CarPart.ENGINE), "a hoist beside the open bonnet did not reach the engine");
        car.setLidOpen(CarEntity.BONNET_BIT, false);
        context.assertFalse(car.workshop().isWorkable(CarPart.ENGINE), "the hoist reached the engine through a shut bonnet");
        car.discard();
        context.succeed();
    }

    /** Motor oil goes in with the bonnet up and hands the bottle back. */
    @GameTest(template = "empty")
    public static void oilAndCoolantTopUp(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 3));
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        car.upkeep().setOilLevel(0.1F);
        car.upkeep().setCoolantLevel(0.1F);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.MOTOR_OIL));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.upkeep().getOilLevel() < 0.2F, "oil went in through a shut bonnet");
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(Math.abs(car.upkeep().getOilLevel() - 0.6F) < 0.01F, "a bottle of oil did not top the sump up");
        context.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "the empty oil bottle was not handed back");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.upkeep().getCoolantLevel() > 0.99F, "a bucket of water did not fill the coolant");
        context.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the empty bucket was not handed back");
        car.discard();
        context.succeed();
    }

    /** The exhaust colour reads the fault: blue for oil, white for a gasket, black for a choked filter. */
    @GameTest(template = "empty")
    public static void exhaustColourReadsTheFault(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 3));
        context.assertFalse(car.upkeep().isBurningOil() || car.upkeep().isGasketBlown() || car.upkeep().isRunningRich(), "a new car smokes");
        car.parts().setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION * 0.3F);
        context.assertTrue(car.upkeep().isBurningOil() && !car.upkeep().isGasketBlown(), "a worn engine did not burn oil");
        car.parts().setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION * 0.1F);
        context.assertTrue(car.upkeep().isGasketBlown() && car.upkeep().isKnocking(), "a worn-out engine kept its gasket");
        car.parts().setCondition(CarPart.AIR_FILTER, CarPart.MAX_CONDITION * 0.2F);
        context.assertTrue(car.upkeep().isRunningRich(), "a choked filter did not run rich");
        car.discard();
        context.succeed();
    }

    /** A drain pan under a car on the lift takes its oil, black and all, and pipes it off as crude. */
    @GameTest(template = "empty")
    public static void oilChangeIntoADrainPan(GameTestHelper context) {
        BlockPos lift = new BlockPos(2, 1, 2);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        context.setBlock(lift.east(), ModBlocks.DRAIN_PAN.defaultBlockState());
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        car.upkeep().setOilAge(0.8F);
        BlockPos pan = context.absolutePos(lift.east());
        context.assertTrue(me.trossty.car.block.DrainPanBlock.carAbove(context.getLevel(), pan) == car,
                "the pan did not find the car on the lift");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        context.getLevel().getBlockState(pan).useWithoutItem(context.getLevel(), player,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pan),
                        Direction.UP, pan, false));
        context.assertTrue(car.upkeep().getOilLevel() == 0.0F && car.upkeep().getOilAge() == 0.0F, "the sump did not drain");
        var drained = ((me.trossty.car.block.entity.DrainPanBlockEntity) context.getLevel().getBlockEntity(pan))
                .getOutlet().drain(5000, IFluidHandler.FluidAction.EXECUTE);
        context.assertTrue(drained.getAmount() == 1000 && drained.getFluid() == me.trossty.car.registry.ModFluids.CRUDE_OIL,
                "the pan did not give the oil up as crude");
        car.discard();
        context.succeed();
    }

    /** The compression tester tells a healthy engine from worn rings, a blown gasket and bent valves. */
    @GameTest(template = "empty")
    public static void compressionTesterReadsTheEngine(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(2, 1, 2));
        var good = me.trossty.car.item.CompressionTesterItem.read(car);
        context.assertTrue(good.verdict() == me.trossty.car.item.CompressionTesterItem.Verdict.GOOD,
                "a new engine did not read good: " + good.verdict());
        context.assertTrue(good.cylinders().length == car.getLayout().cylinders(), "not one reading per cylinder");
        for (float bar : good.cylinders()) {
            context.assertTrue(bar >= good.spec() * 0.9F, "a new cylinder read only " + bar);
        }
        context.assertTrue(java.util.Arrays.equals(good.cylinders(), me.trossty.car.item.CompressionTesterItem.read(car).cylinders()),
                "a second test read different numbers");

        car.parts().setCondition(CarPart.ENGINE, 40.0F);
        context.assertTrue(me.trossty.car.item.CompressionTesterItem.read(car).verdict()
                == me.trossty.car.item.CompressionTesterItem.Verdict.WORN, "a tired engine did not read worn");

        car.parts().setCondition(CarPart.ENGINE, 10.0F);
        var gasket = me.trossty.car.item.CompressionTesterItem.read(car);
        context.assertTrue(gasket.verdict() == me.trossty.car.item.CompressionTesterItem.Verdict.GASKET,
                "a blown gasket did not show");
        int low = -1;
        for (int i = 0; i + 1 < gasket.cylinders().length; i++) {
            if (gasket.cylinders()[i] < gasket.spec() * 0.4F && gasket.cylinders()[i + 1] < gasket.spec() * 0.4F) {
                low = i;
            }
        }
        context.assertTrue(low >= 0, "no two cylinders side by side read low for the gasket");

        car.parts().setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION);
        car.upkeep().snapBelt();
        var bent = me.trossty.car.item.CompressionTesterItem.read(car);
        context.assertTrue(bent.verdict() == me.trossty.car.item.CompressionTesterItem.Verdict.BENT_VALVES
                && bent.cylinders()[0] < bent.spec() * 0.2F, "a snapped belt did not show bent valves");

        // Used for real: it wants the bonnet up and the plugs out, and does not get used up.
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.COMPRESSION_TESTER));
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        car.parts().setCondition(CarPart.SPARK_PLUGS, 0.0F);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(player.getMainHandItem().is(me.trossty.car.registry.ModItems.COMPRESSION_TESTER),
                "the tester was used up");
        car.discard();
        context.succeed();
    }

    /** A worn-out belt stops the engine and says so; a new one under the open bonnet sets it going. */
    @GameTest(template = "empty")
    public static void timingBelt(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(2, 1, 2));
        context.assertTrue(car.canRun(), "a new car would not start");
        car.upkeep().setBeltWear(1.0F);
        context.assertTrue(car.upkeep().isBeltSnapped() && !car.canRun(), "a snapped belt still turned the engine");
        context.assertTrue(FaultCodes.read(car).stream().anyMatch(f -> f.code().equals("P0016")
                && f.place() == CarPart.ENGINE), "the scanner did not point at the belt");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.TIMING_BELT));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.upkeep().isBeltSnapped(), "a belt went on through a shut bonnet");
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        car.setLidOpen(CarEntity.BONNET_BIT, false);
        context.assertTrue(car.upkeep().getBeltWear() == 0.0F && car.canRun(), "the new belt did not take");
        context.assertTrue(player.getMainHandItem().isEmpty(), "the belt was not used up");
        car.discard();
        context.succeed();
    }

    /** A choked converter costs power, soft tyres cost grip; a new converter and the pump put both right. */
    @GameTest(template = "empty")
    public static void converterAndTyrePressure(GameTestHelper context) {
        BlockPos lift = new BlockPos(2, 1, 2);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        float speed = car.getMaxSpeed();
        float brakes = car.brakeStrength();
        car.upkeep().setCatClog(1.0F);
        car.upkeep().setTyrePressure(0.3F);
        context.assertTrue(car.getMaxSpeed() < speed * 0.6F, "a blocked converter did not hold the engine back");
        context.assertTrue(car.brakeStrength() < brakes, "soft tyres gripped as well as hard ones");
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.contains("P0421") && codes.contains("C0755"), "the scanner missed them: " + codes);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.CATALYTIC_CONVERTER));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.upkeep().getCatClog() == 0.0F, "the new converter did not go on");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.TYRE_PUMP));
        for (int i = 0; i < 4; i++) {
            car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        context.assertTrue(car.upkeep().getTyrePressure() == 1.0F && car.upkeep().tyreSoftness() == 0.0F, "the pump did not fill the tyres");
        context.assertTrue(Math.abs(car.brakeStrength() - brakes) < 1.0E-4F, "pumped tyres did not grip again");
        car.discard();
        context.succeed();
    }

    /** On the rollers the wheels turn and the car stays put. */
    @GameTest(template = "empty")
    public static void dynoHoldsTheCar(GameTestHelper context) {
        BlockPos dyno = new BlockPos(2, 1, 2);
        context.setBlock(dyno, ModBlocks.DYNO);
        CarEntity car = context.spawn(ModEntities.CAR, dyno.above());
        context.runAfterDelay(5, () -> {
            net.minecraft.world.phys.Vec3 from = car.position();
            car.setSpeed(0.5F);
            context.runAfterDelay(10, () -> {
                context.assertTrue(car.upkeep().isOnDyno(), "the car did not find the rollers under it");
                context.assertTrue(car.position().horizontalDistanceSqr() - from.horizontalDistanceSqr() < 1.0E-4
                        && car.position().subtract(from).horizontalDistance() < 0.01,
                        "the car rolled off the dyno: " + from + " -> " + car.position());
                car.discard();
                context.succeed();
            });
        });
    }

    /** A full pan pours its old oil, as crude, into a drum set beside it. */
    @GameTest(template = "empty")
    public static void drainPanEmptiesIntoADrum(GameTestHelper context) {
        BlockPos pan = new BlockPos(2, 1, 2);
        context.setBlock(pan, ModBlocks.DRAIN_PAN.defaultBlockState());
        context.setBlock(pan.east(), ModBlocks.FUEL_DRUM);
        context.<me.trossty.car.block.entity.DrainPanBlockEntity>getBlockEntity(pan).catchOil(1.0F);
        var drum = context.<me.trossty.car.block.entity.FuelDrumBlockEntity>getBlockEntity(pan.east());
        context.succeedWhen(() -> {
            var held = drum.getFluidStorage().getFluidInTank(0);
            context.assertTrue(held.getFluid() == me.trossty.car.registry.ModFluids.CRUDE_OIL && held.getAmount() == 1000,
                    "the drum did not take the pan's oil");
        });
    }

    /** No brake fluid is soft brakes; a knock pulls the car; the wrench on the lift sets it straight. */
    @GameTest(template = "empty")
    public static void brakeFluidAndAlignment(GameTestHelper context) {
        BlockPos lift = new BlockPos(2, 1, 2);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        float firm = car.brakeStrength();
        car.upkeep().setBrakeFluid(0.0F);
        context.assertTrue(car.brakeStrength() < firm * 0.5F, "dry brakes still bit");
        float straight = car.pull();
        car.upkeep().setAlignment(0.5F);
        context.assertTrue(car.pull() > straight, "bent tracking did not pull");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.WRENCH));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.upkeep().getAlignment() == 0.0F, "the wrench did not set the tracking straight");
        context.assertFalse(me.trossty.car.block.DynoBlock.readout(car).isEmpty(), "the dyno read nothing");
        car.discard();
        context.succeed();
    }

    /** Leads from a good battery to a flat one, both bonnets up, charge the flat one. */
    @GameTest(template = "empty")
    public static void jumpStart(GameTestHelper context) {
        CarEntity donor = context.spawn(ModEntities.CAR, new BlockPos(2, 1, 2));
        CarEntity flat = context.spawn(ModEntities.CAR, new BlockPos(5, 1, 2));
        flat.parts().setCondition(CarPart.BATTERY, 1.5F);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack leads = new ItemStack(me.trossty.car.registry.ModItems.JUMPER_CABLES);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, leads);
        donor.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(leads.get(me.trossty.car.registry.ModComponents.OBD_LINK) == null,
                "the leads clipped on under a shut bonnet");
        donor.setLidOpen(CarEntity.BONNET_BIT, true);
        flat.setLidOpen(CarEntity.BONNET_BIT, true);
        donor.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        flat.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(flat.parts().getCondition(CarPart.BATTERY) > 20.0F, "the flat battery took no charge");
        context.assertTrue(donor.parts().getCondition(CarPart.BATTERY) < CarPart.MAX_CONDITION, "the donor gave nothing");
        donor.discard();
        flat.discard();
        context.succeed();
    }

    /** An engine opened on its stand comes apart into three insides and goes back as their average. */
    @GameTest(template = "empty")
    public static void engineTeardown(GameTestHelper context) {
        BlockPos at = new BlockPos(2, 1, 2);
        context.setBlock(at, ModBlocks.ENGINE_STAND.defaultBlockState());
        var stand = (me.trossty.car.block.entity.EngineStandBlockEntity) context.getBlockEntity(at);

        // An engine alike all through comes apart alike all through, and goes back the same.
        ItemStack plain = new ItemStack(me.trossty.car.registry.ModItems.ENGINE);
        plain.set(me.trossty.car.registry.ModComponents.PART_CONDITION, 40);
        stand.setEngine(plain);
        stand.strip();
        context.assertTrue(stand.getInternals().size() == 4, "the stand does not keep four insides");
        for (ItemStack inside : stand.getInternals()) {
            context.assertTrue(Math.round(Workshop.conditionOf(inside)) == 40
                    && !inside.has(me.trossty.car.registry.ModComponents.PART_CYLINDERS),
                    "an even engine came apart uneven: " + inside);
        }
        stand.rebuild();
        context.assertTrue(Math.round(Workshop.conditionOf(stand.getEngine())) == 40
                && !stand.getEngine().has(me.trossty.car.registry.ModComponents.ENGINE_INTERNALS),
                "an even engine put back as it was came out different");

        // One worn unevenly comes apart cylinder by cylinder, bent valves and all.
        Snapshot worn = new Snapshot(java.util.List.of(90, 30, 85, 80), java.util.List.of(0, 100, 0, 95), 70, 50);
        ItemStack engine = new ItemStack(me.trossty.car.registry.ModItems.ENGINE);
        engine.set(me.trossty.car.registry.ModComponents.PART_CONDITION, Math.round(worn.condition()));
        worn.stamp(engine);
        stand.setEngine(engine);
        stand.strip();
        var internals = stand.getInternals();
        context.assertTrue(worn.rings().equals(internals.get(0).get(me.trossty.car.registry.ModComponents.PART_CYLINDERS)),
                "the pistons did not keep each cylinder's rings");
        context.assertTrue(worn.valves().equals(internals.get(1).get(me.trossty.car.registry.ModComponents.PART_CYLINDERS)),
                "the valves did not keep each cylinder's wear");
        context.assertTrue(Math.round(Workshop.conditionOf(internals.get(2))) == 70
                && Math.round(Workshop.conditionOf(internals.get(3))) == 50, "the gasket or the bearings came out wrong");
        stand.rebuild();
        context.assertTrue(Snapshot.of(stand.getEngine()).equals(worn), "a worn engine put back as it was came out different");

        // New valves in place of the bent ones make a better engine, and nothing else changes.
        stand.strip();
        stand.setInternal(1, new ItemStack(me.trossty.car.registry.ModItems.VALVES));
        stand.rebuild();
        Snapshot fixed = Snapshot.of(stand.getEngine());
        context.assertTrue(fixed.rings().equals(worn.rings()) && fixed.valves().stream().allMatch(v -> v == 100)
                && fixed.gasket() == 70 && fixed.bearings() == 50, "new valves did not go in on their own: " + fixed);
        context.assertTrue(Workshop.conditionOf(stand.getEngine()) > worn.condition(), "new valves did not freshen the engine");
        context.succeed();
    }

    /**
     * What is inside the engine is the engine's own and stays with it: up on
     * the hook, onto the stand, apart and back together, and into the car
     * again. Each fault comes from its own part of it: a worn set of rings
     * burns oil, bent valves leave their cylinders with nothing, and worn
     * bearings knock however much oil there is.
     */
    @GameTest(template = "empty")
    public static void engineKeepsItsInsidesThroughTheGarage(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        for (CarPart above : CarPart.ENGINE.blockedBy()) {
            car.parts().setCondition(above, 0.0F);
        }
        var engine = car.parts().engine();
        int cylinders = engine.cylinders();
        java.util.List<Integer> rings = new java.util.ArrayList<>(java.util.Collections.nCopies(cylinders, 90));
        rings.set(1, 30);
        engine.set(new Snapshot(rings, java.util.Collections.nCopies(cylinders, 100), 70, 50));
        context.assertTrue(car.upkeep().isBurningOil() && !car.upkeep().isGasketBlown() && !car.upkeep().isKnocking(),
                "one worn set of rings did not burn oil on its own");
        float ringMean = (90.0F * (cylinders - 1) + 30.0F) / cylinders;
        context.assertTrue(Math.abs(car.parts().getCondition(CarPart.ENGINE) - (ringMean + 100.0F + 70.0F + 50.0F) / 4.0F) < 0.01F,
                "the engine's condition is not the average of its insides: " + car.parts().getCondition(CarPart.ENGINE));

        // A snapped belt bends the first cylinder's valves, and a new belt does not straighten them.
        car.upkeep().snapBelt();
        car.upkeep().setBeltWear(0.0F);
        context.assertTrue(engine.isBent(0) && car.parts().firing() < 1.0F, "the snapped belt bent no valves");
        var reading = me.trossty.car.item.CompressionTesterItem.read(car);
        context.assertTrue(reading.verdict() == me.trossty.car.item.CompressionTesterItem.Verdict.BENT_VALVES
                && reading.cylinders()[0] < reading.spec() * 0.2F, "the tester did not find the bent valves");
        Snapshot before = engine.snapshot();

        BlockPos at = hoistAtNose(context, car);
        var hoist = (EngineHoistBlockEntity) context.getLevel().getBlockEntity(at);
        clickHoist(context, at, player, false);
        context.assertTrue(Snapshot.of(hoist.getEngine()).equals(before), "the engine left its insides in the car");

        BlockPos standAt = context.absolutePos(new BlockPos(0, 1, 0));
        context.getLevel().setBlockAndUpdate(standAt, ModBlocks.ENGINE_STAND.defaultBlockState());
        var stand = (me.trossty.car.block.entity.EngineStandBlockEntity) context.getLevel().getBlockEntity(standAt);
        stand.setEngine(hoist.takeEngine());
        stand.strip();
        stand.setInternal(1, new ItemStack(me.trossty.car.registry.ModItems.VALVES));
        stand.rebuild();
        hoist.setEngine(stand.getEngine());
        stand.setEngine(ItemStack.EMPTY);
        clickHoist(context, at, player, false);
        context.assertTrue(car.parts().isFitted(CarPart.ENGINE), "the rebuilt engine did not go back in");
        Snapshot after = engine.snapshot();
        context.assertTrue(after.rings().equals(before.rings()) && after.gasket() == 70 && after.bearings() == 50
                && after.valves().stream().allMatch(v -> v == 100), "the engine went back in different: " + after);
        context.assertTrue(!engine.isBent(0) && car.parts().firing() == 1.0F, "new valves did not cure the bent ones");
        context.assertTrue(me.trossty.car.item.CompressionTesterItem.read(car).verdict()
                == me.trossty.car.item.CompressionTesterItem.Verdict.WORN, "the worn rings did not read worn");

        // Bearings worn through knock with the sump full.
        engine.set(new Snapshot(rings, java.util.Collections.nCopies(cylinders, 100), 70, 10));
        context.assertTrue(car.upkeep().isKnocking() && !car.upkeep().isGasketBlown(), "worn bearings did not knock");
        car.discard();
        context.succeed();
    }

    /**
     * A hoist at the car's nose with its boom over the bonnet, the way a
     * player standing in front of the car puts one down. Its world position.
     */
    private static BlockPos hoistAtNose(GameTestHelper context, CarEntity car) {
        Direction ahead = Direction.fromYRot(car.getYRot());
        net.minecraft.world.phys.Vec3 nose = car.bonnetCentre().add(
                ahead.getStepX() * EngineHoistBlock.HOOK_OUT, 0.0, ahead.getStepZ() * EngineHoistBlock.HOOK_OUT);
        BlockPos at = BlockPos.containing(nose).atY(car.blockPosition().getY());
        context.getLevel().setBlockAndUpdate(at, ModBlocks.ENGINE_HOIST.defaultBlockState()
                .setValue(EngineHoistBlock.FACING, ahead.getOpposite()));
        return at;
    }

    /** An empty-handed click on a hoist, sneaking or not. */
    private static void clickHoist(GameTestHelper context, BlockPos at, net.minecraft.world.entity.player.Player player,
                                   boolean sneaking) {
        player.setShiftKeyDown(sneaking);
        context.getLevel().getBlockState(at).useWithoutItem(context.getLevel(), player,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.UP, at, false));
        player.setShiftKeyDown(false);
    }

    /**
     * The engine of a car with a bonnet comes out the way it does in a
     * garage: up onto the hoist's hook, never into the pockets. The boom
     * swings round over a stand and sets it down there; the same the other
     * way puts it back in. A hand gets it neither out nor in.
     */
    @GameTest(template = "empty")
    public static void engineGoesOutOnTheHoistAndOntoTheStand(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        for (CarPart above : CarPart.ENGINE.blockedBy()) {
            car.parts().setCondition(above, 0.0F);
        }
        car.parts().setCondition(CarPart.ENGINE, 60.0F);
        car.workshop().serviceByHand(player, CarPart.ENGINE);
        context.assertTrue(car.parts().isFitted(CarPart.ENGINE), "the engine came out with no hoist");

        BlockPos at = hoistAtNose(context, car);
        var hoist = (EngineHoistBlockEntity) context.getLevel().getBlockEntity(at);
        clickHoist(context, at, player, false);
        context.assertFalse(car.parts().isFitted(CarPart.ENGINE), "clicking the hoist did not lift the engine out");
        context.assertTrue(hoist.getEngine().is(me.trossty.car.registry.ModItems.ENGINE)
                && Math.round(Workshop.conditionOf(hoist.getEngine())) == 60, "the engine is not on the hook as it was");
        context.assertTrue(player.getInventory().countItem(me.trossty.car.registry.ModItems.ENGINE) == 0,
                "the engine went into the pockets");

        // A quarter turn clockwise from over the bay, and the hook hangs over a stand.
        Direction boom = context.getLevel().getBlockState(at).getValue(EngineHoistBlock.FACING);
        BlockPos standAt = at.relative(boom.getClockWise());
        context.getLevel().setBlockAndUpdate(standAt, ModBlocks.ENGINE_STAND.defaultBlockState());
        var stand = (me.trossty.car.block.entity.EngineStandBlockEntity) context.getLevel().getBlockEntity(standAt);
        clickHoist(context, at, player, true);
        context.assertTrue(context.getLevel().getBlockState(at).getValue(EngineHoistBlock.FACING) == boom.getClockWise(),
                "sneaking did not swing the boom");
        context.assertTrue(context.getLevel().getBlockEntity(at) == hoist && hoist.isHolding(),
                "the engine fell off the hook in the swing");
        clickHoist(context, at, player, false);
        context.assertFalse(hoist.isHolding(), "the engine stayed on the hook over the stand");
        context.assertTrue(Math.round(Workshop.conditionOf(stand.getEngine())) == 60,
                "the stand did not get the engine as it was");

        // An engine held up to the empty bay does not go in by hand.
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.ENGINE));
        car.workshop().serviceByHand(player, CarPart.ENGINE);
        context.assertFalse(car.parts().isFitted(CarPart.ENGINE), "an engine went in by hand");
        context.assertTrue(player.getMainHandItem().is(me.trossty.car.registry.ModItems.ENGINE),
                "the engine in hand was used up");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);

        // And back: off the stand, round over the bay, and down into it.
        clickHoist(context, at, player, false);
        context.assertTrue(hoist.isHolding() && stand.getEngine().isEmpty(), "the hook did not take the engine off the stand");
        for (int turn = 0; turn < 3; turn++) {
            clickHoist(context, at, player, true);
        }
        clickHoist(context, at, player, false);
        context.assertTrue(car.parts().isFitted(CarPart.ENGINE) && Math.round(car.parts().getCondition(CarPart.ENGINE)) == 60,
                "the engine did not go back in off the hook");
        context.assertFalse(hoist.isHolding(), "the engine stayed on the hook as well");
        car.discard();
        context.succeed();
    }

    /** What hangs on the hook is kept with the hoist, and falls when the hoist is knocked down. */
    @GameTest(template = "empty")
    public static void hoistKeepsAndDropsItsEngine(GameTestHelper context) {
        BlockPos at = context.absolutePos(new BlockPos(2, 1, 2));
        context.getLevel().setBlockAndUpdate(at, ModBlocks.ENGINE_HOIST.defaultBlockState());
        var hoist = (EngineHoistBlockEntity) context.getLevel().getBlockEntity(at);
        ItemStack engine = new ItemStack(me.trossty.car.registry.ModItems.ENGINE);
        engine.set(me.trossty.car.registry.ModComponents.PART_CONDITION, 35);
        hoist.setEngine(engine);
        var registries = context.getLevel().registryAccess();
        var copy = new EngineHoistBlockEntity(at, hoist.getBlockState());
        copy.loadWithComponents(hoist.saveWithoutMetadata(registries), registries);
        context.assertTrue(Math.round(Workshop.conditionOf(copy.getEngine())) == 35, "the hook forgot its engine");
        context.getLevel().removeBlock(at, false);
        context.assertTrue(!context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(at).inflate(2.0), e -> e.getItem().is(me.trossty.car.registry.ModItems.ENGINE)).isEmpty(),
                "the engine vanished with the hoist");
        context.succeed();
    }

    /** A lift reaches everything under a car, but the engine of one with a bonnet comes out of the top. */
    @GameTest(template = "empty")
    public static void aLiftDoesNotFreeTheEngine(GameTestHelper context) {
        BlockPos lift = new BlockPos(3, 1, 3);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        car.moveTo(car.getX(), car.getY(), car.getZ(), 0.0F, 0.0F);
        car.setLidOpen(CarEntity.BONNET_BIT, true);
        context.assertTrue(car.workshop().isWorkable(CarPart.WHEELS), "the raised lift did not reach the wheels");
        context.assertFalse(car.workshop().isWorkable(CarPart.ENGINE), "the lift freed the engine without a hoist");
        car.discard();
        context.succeed();
    }

    // ---- tanks ----

    /**
     * Tanks against each other are one tank: liquid poured in at the top
     * fills the bottom cell first, every face hands out the whole of it, and
     * any liquid goes — water, which is no fuel at all.
     */
    @GameTest(template = "empty")
    public static void tanksMergeAndFillFromTheBottom(GameTestHelper context) {
        BlockPos low = new BlockPos(2, 1, 2);
        BlockPos high = low.above();
        BlockPos side = low.east();
        context.setBlock(low, ModBlocks.FUEL_TANK);
        context.setBlock(high, ModBlocks.FUEL_TANK);
        context.setBlock(side, ModBlocks.FUEL_TANK);
        context.runAtTickTime(2, () -> {
            context.assertTrue(context.getBlockState(low).getValue(me.trossty.car.block.FuelTankBlock.UP),
                    "the lower cell does not show itself joined to the one above");
            IFluidHandler door = context.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    context.absolutePos(high), Direction.UP);
            context.assertTrue(door != null, "the tank has no fluid door");
            int bucket = BUCKET;
            int in = door.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 40 * bucket),
                    IFluidHandler.FluidAction.EXECUTE);
            context.assertTrue(in == 40 * bucket, "three cells did not take forty buckets: " + in / bucket);
            context.assertTrue(context.<FuelTankBlockEntity>getBlockEntity(low).getGroup().getCapacity() == 48 * bucket, "three cells are not one tank of 48 buckets");
            FuelTankBlockEntity bottom = context.<FuelTankBlockEntity>getBlockEntity(low);
            FuelTankBlockEntity beside = context.<FuelTankBlockEntity>getBlockEntity(side);
            FuelTankBlockEntity top = context.<FuelTankBlockEntity>getBlockEntity(high);
            context.assertTrue(bottom.getAmount() == 16 * bucket && beside.getAmount() == 16 * bucket,
                    "the bottom layer did not fill first");
            context.assertTrue(top.getAmount() == 8 * bucket, "the top cell holds " + top.getAmount() / bucket);
            // Lava will not go into a tank of water.
            int lava = door.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, bucket),
                    IFluidHandler.FluidAction.SIMULATE);
            context.assertTrue(lava == 0, "lava mixed into the water");
            context.succeed();
        });
    }

    /** Breaking a cell out of a tank loses that cell's share, and the rest is still one tank. */
    @GameTest(template = "empty")
    public static void brokenTankCellSplitsTheTank(GameTestHelper context) {
        BlockPos a = new BlockPos(1, 1, 2);
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlock(a, ModBlocks.FUEL_TANK);
        context.setBlock(b, ModBlocks.FUEL_TANK);
        context.setBlock(c, ModBlocks.FUEL_TANK);
        context.runAtTickTime(2, () -> {
            FuelTankBlockEntity first = context.<FuelTankBlockEntity>getBlockEntity(a);
            context.assertTrue(first.fill(ModFluids.DIESEL, 30000) == 30000, "the row did not take 30 buckets");
            context.setBlock(b, Block.byItem(Items.AIR).defaultBlockState());
        });
        context.runAtTickTime(4, () -> {
            FuelTankBlockEntity first = context.<FuelTankBlockEntity>getBlockEntity(a);
            FuelTankBlockEntity last = context.<FuelTankBlockEntity>getBlockEntity(c);
            context.assertTrue(first.getGroup().size() == 1 && last.getGroup().size() == 1,
                    "the two ends are still one tank");
            context.assertTrue(first.getGroup().getAmount() + last.getGroup().getAmount() == 20
                            * BUCKET,
                    "the ends do not hold the 20 buckets that were in them");
            context.succeed();
        });
    }

    /** A cell broken out of a tank keeps its share in the item it drops. */
    @GameTest(template = "empty")
    public static void brokenTankCellKeepsItsShare(GameTestHelper context) {
        BlockPos a = new BlockPos(1, 1, 2);
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlock(a, ModBlocks.FUEL_TANK);
        context.setBlock(b, ModBlocks.FUEL_TANK);
        context.setBlock(c, ModBlocks.FUEL_TANK);
        context.runAtTickTime(2, () -> {
            context.<FuelTankBlockEntity>getBlockEntity(a).fill(ModFluids.DIESEL, 30000);
            context.getLevel().destroyBlock(context.absolutePos(b), true);
        });
        context.runAtTickTime(4, () -> {
            var drops = context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(context.absolutePos(b)).inflate(2.0),
                    drop -> drop.getItem().is(ModBlocks.FUEL_TANK.asItem()));
            context.assertTrue(drops.size() == 1, "the broken cell dropped " + drops.size() + " items");
            var held = drops.get(0).getItem().get(me.trossty.car.registry.ModComponents.STORED_FLUID);
            context.assertTrue(held != null && held.fluid() == ModFluids.DIESEL && held.amount() == 10 * BUCKET,
                    "the dropped cell does not carry its 10 buckets of diesel: " + held);
            context.succeed();
        });
    }

    /** Locked with a wrench, a tank takes nothing but its liquid, even once empty. */
    @GameTest(template = "empty")
    public static void aLockedTankTakesOnlyItsLiquid(GameTestHelper context) {
        BlockPos at = new BlockPos(2, 1, 2);
        context.setBlock(at, ModBlocks.FUEL_TANK);
        context.setBlock(at.above(), ModBlocks.FUEL_TANK);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        context.runAtTickTime(2, () -> {
            FuelTankBlockEntity tank = context.<FuelTankBlockEntity>getBlockEntity(at);
            var whole = tank.getGroup();
            var execute = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
            whole.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.DIESEL, 1000), execute);
            tank.toggleLock(player);
            context.assertTrue(context.<FuelTankBlockEntity>getBlockEntity(at.above()).getLock() == ModFluids.DIESEL,
                    "the upper cell is not locked with the tank");
            whole.drain(1000, execute);
            int petrol = whole.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.PETROL, 500), execute);
            context.assertTrue(petrol == 0, "an empty tank locked to diesel took petrol");
            int diesel = whole.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.DIESEL, 500), execute);
            context.assertTrue(diesel == 500, "a tank locked to diesel turned diesel away");
            tank.toggleLock(player);
            whole.drain(500, execute);
            context.assertTrue(whole.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.PETROL, 500), execute) == 500,
                    "an unlocked empty tank turned petrol away");
            context.succeed();
        });
    }

    /** A locked drum turns other liquids away too. */
    @GameTest(template = "empty")
    public static void aLockedDrumTakesOnlyItsLiquid(GameTestHelper context) {
        BlockPos at = new BlockPos(2, 1, 2);
        context.setBlock(at, ModBlocks.FUEL_DRUM);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var drum = context.<me.trossty.car.block.entity.FuelDrumBlockEntity>getBlockEntity(at);
        var store = drum.getFluidStorage();
        var execute = net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        store.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.BIOFUEL, 1000), execute);
        drum.toggleLock(player);
        store.drain(1000, execute);
        context.assertTrue(store.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.PETROL, 100), execute) == 0,
                "an empty drum locked to biofuel took petrol");
        context.assertTrue(store.fill(new net.neoforged.neoforge.fluids.FluidStack(ModFluids.BIOFUEL, 100), execute) == 100,
                "a drum locked to biofuel turned biofuel away");
        context.succeed();
    }

    /** A tank feeds a pump standing against it, diesel as well as petrol. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aTankFeedsATouchingPump(GameTestHelper context) {
        BlockPos tankAt = new BlockPos(1, 1, 3);
        BlockPos pumpAt = tankAt.east();
        context.setBlock(tankAt, ModBlocks.FUEL_TANK);
        context.setBlock(pumpAt, ModBlocks.FUEL_PUMP);
        context.runAtTickTime(2, () -> context.<FuelTankBlockEntity>getBlockEntity(tankAt).fill(ModFluids.DIESEL, 5000));
        context.succeedWhen(() -> {
            var pump = context.<me.trossty.car.block.entity.FuelPumpBlockEntity>getBlockEntity(pumpAt);
            context.assertTrue(pump.getFuel() == me.trossty.car.entity.CarSetup.Fuel.DIESEL,
                    "the pump has not been fed diesel from the tank");
            context.assertTrue(context.<FuelTankBlockEntity>getBlockEntity(tankAt).getGroup().getAmount() < 5000,
                    "the tank still holds all its diesel");
        });
    }

    /** A trailer has no engine: nothing about one can be worked on, read or worn. */
    @GameTest(template = "empty")
    public static void trailerHasNoEngine(GameTestHelper context) {
        CarEntity trailer = context.spawn(ModEntities.TRAILER, new BlockPos(2, 1, 2));
        context.assertFalse(trailer.hasPart(CarPart.ENGINE), "a trailer has an engine");
        context.assertFalse(trailer.workshop().canReach(CarPart.ENGINE), "a trailer's engine can be reached");
        context.assertTrue(trailer.hasPart(CarPart.WHEELS), "a trailer has no wheels");
        trailer.discard();
        context.succeed();
    }

    // ---- a car once put down stays a car ----

    private static int itemsAround(GameTestHelper context, CarEntity car, net.minecraft.world.item.Item item) {
        return context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                car.getBoundingBox().inflate(4.0), drop -> drop.getItem().is(item)).size();
    }

    /** A survival player's punch dents a car; it does not turn it back into an item. */
    @GameTest(template = "empty")
    public static void punchDentsInsteadOfPocketing(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        float before = car.damage().getHealth();
        car.hurt(context.getLevel().damageSources().playerAttack(player), 2.0F);
        context.assertFalse(car.isRemoved(), "a punch took the car away");
        context.assertTrue(car.damage().getHealth() < before, "a punch did not dent the car");
        context.assertTrue(itemsAround(context, car, me.trossty.car.registry.ModItems.CAR) == 0,
                "a punch dropped the car as an item");
        car.discard();
        context.succeed();
    }

    /** A bare fist is no crowbar: one punch takes a sliver, not a twentieth of the car. */
    @GameTest(template = "empty")
    public static void aPunchBarelyDents(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        float before = car.damage().getHealth();
        car.hurt(context.getLevel().damageSources().playerAttack(player), 1.0F);
        float lost = before - car.damage().getHealth();
        context.assertTrue(lost > 0.0F && lost <= 0.3F, "a punch took " + lost + " off the car");
        car.discard();
        context.succeed();
    }

    /** A worn rack makes the car pull, always the same way; a sound one runs straight. */
    @GameTest(template = "empty")
    public static void aWornRackPulls(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        context.assertTrue(car.pull() == 0.0F, "a fresh car pulls " + car.pull());
        car.parts().setCondition(CarPart.STEERING_RACK, 10.0F);
        float pull = car.pull();
        context.assertTrue(Math.abs(pull) > 0.5F, "a worn-out rack pulls only " + pull);
        context.assertTrue(car.pull() == pull, "the pull changed sides");
        car.discard();
        context.succeed();
    }

    /** Going under soaks the airbox and floods the engine; it starts again once dry. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void waterSoaksTheAirbox(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        float filter = car.parts().getConditionFraction(CarPart.AIR_FILTER);
        for (BlockPos at : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(6, 3, 6))) {
            context.setBlock(at, net.minecraft.world.level.block.Blocks.WATER);
        }
        context.runAfterDelay(10, () -> {
            context.assertTrue(car.isFlooded(), "the car did not flood under water");
            context.assertTrue(car.parts().getConditionFraction(CarPart.AIR_FILTER) < filter - 0.2F,
                    "the airbox stayed dry under water");
            car.discard();
            context.succeed();
        });
    }

    /** A creative player clears a car away, and nothing comes back from it. */
    @GameTest(template = "empty")
    public static void creativeClearsWithoutAnItem(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        car.hurt(context.getLevel().damageSources().playerAttack(player), 1.0F);
        context.assertTrue(car.isRemoved(), "a creative hit left the car standing");
        context.assertTrue(itemsAround(context, car, me.trossty.car.registry.ModItems.CAR) == 0,
                "a creative hit dropped the car as an item");
        context.succeed();
    }

    /** A motorboat put on deep water floats at its waterline and its engine stays dry; it has no wheels to need. */
    @GameTest(template = "empty", timeoutTicks = 120)
    public static void motorboatFloats(GameTestHelper context) {
        for (int x = 0; x < 6; x++) {
            for (int z = 0; z < 6; z++) {
                for (int y = 1; y <= 3; y++) {
                    context.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.WATER);
                }
            }
        }
        me.trossty.car.entity.MotorboatEntity boat = context.spawn(
                me.trossty.car.registry.ModEntities.MOTORBOAT, new net.minecraft.world.phys.Vec3(3.0, 4.5, 3.0));
        context.runAtTickTime(100, () -> {
            double surface = context.absolutePos(new BlockPos(0, 3, 0)).getY() + 8.0 / 9.0;
            double sunk = surface - boat.getY();
            context.assertTrue(sunk > 0.2 && sunk < 0.5, "the boat sits %.2f below the surface".formatted(sunk));
            context.assertTrue(!boat.isFlooded(), "the water drowned an outboard");
            context.assertTrue(boat.missingEssential() == null,
                    "a boat is missing " + boat.missingEssential());
            context.assertTrue(!boat.hasPart(me.trossty.car.entity.CarPart.WHEELS), "a boat has wheels");
            context.succeed();
        });
    }

    /**
     * Each machine has the driveline it really would: a car shifts through a
     * gearbox, an electric car, a propeller and an outboard never do, and
     * nothing has parts it could not have.
     */
    @GameTest(template = "empty")
    public static void drivelinesMatchTheMachine(GameTestHelper context) {
        CarEntity petrol = car(context, new BlockPos(1, 1, 1), false);
        CarEntity electric = car(context, new BlockPos(5, 1, 1), true);
        me.trossty.car.entity.AirplaneEntity plane = context.spawn(
                me.trossty.car.registry.ModEntities.AIRPLANE, new BlockPos(1, 1, 5));
        me.trossty.car.entity.MotorboatEntity boat = context.spawn(
                me.trossty.car.registry.ModEntities.MOTORBOAT, new BlockPos(5, 1, 5));
        context.assertTrue(petrol.hasGearbox() && petrol.hasPart(CarPart.EXHAUST), "a petrol car lost its gearbox");
        context.assertTrue(!electric.hasGearbox(), "an electric car shifts gear");
        for (CarPart part : new CarPart[]{CarPart.EXHAUST, CarPart.SPARK_PLUGS, CarPart.AIR_FILTER,
                CarPart.OIL_FILTER, CarPart.CLUTCH}) {
            context.assertTrue(!electric.hasPart(part), "an electric car has " + part);
        }
        context.assertTrue(!plane.hasGearbox(), "a plane shifts gear");
        for (CarPart part : new CarPart[]{CarPart.TRANSMISSION, CarPart.CLUTCH, CarPart.DRIVESHAFT,
                CarPart.RADIATOR}) {
            context.assertTrue(!plane.hasPart(part), "a plane has " + part);
        }
        context.assertTrue(plane.hasPart(CarPart.BRAKE_PADS) && plane.hasPart(CarPart.WHEELS),
                "a plane lost its wheel brakes");
        context.assertTrue(!plane.areBrakeLampsOn(), "a plane shows brake lamps");
        context.assertTrue(!boat.hasGearbox() && !boat.hasPart(CarPart.RADIATOR) && !boat.hasPart(CarPart.CLUTCH),
                "an outboard has a gearbox, radiator or clutch");
        context.assertTrue(!me.trossty.car.workbench.ModVehicleRecipes.chassisParts(
                        me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE).stream()
                        .anyMatch(m -> m.item() == me.trossty.car.registry.ModItems.TRANSMISSION),
                "a plane is built with a gearbox");
        context.succeed();
    }

    /** The clipboard reads nothing off a car standing on its wheels: no lift, no service window. */
    @GameTest(template = "empty")
    public static void clipboardNeedsALift(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                new ItemStack(me.trossty.car.registry.ModItems.DIAGNOSTIC_SHEET));
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(player.containerMenu == player.inventoryMenu,
                "the clipboard opened a service window off the lift");
        context.succeed();
    }

    /** A locked car keeps its parts, however the request to take one arrives. */
    @GameTest(template = "empty")
    public static void lockedCarRefusesService(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        car.setLockCode(77L);
        ItemStack key = new ItemStack(me.trossty.car.registry.ModItems.CAR_KEY);
        key.set(me.trossty.car.registry.ModComponents.VEHICLE_KEY, 77L);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        player.setShiftKeyDown(true);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(car.isLocked(), "the key did not lock the car");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(false);
        for (CarPart part : CarPart.values()) {
            if (car.hasPart(part)) {
                car.workshop().serviceByHand(player, part);
                context.assertTrue(car.hasPart(part), "a locked car gave up its " + part);
            }
        }
        car.discard();
        context.succeed();
    }

    /** A wrecked pickup tips its bed out, the way a broken chest does. */
    @GameTest(template = "empty")
    public static void wreckedPickupSpillsItsBed(GameTestHelper context) {
        var pickup = context.spawn(ModEntities.PICKUP, new BlockPos(3, 1, 3));
        pickup.getBed().setItem(0, new ItemStack(Items.DIAMOND, 5));
        pickup.hurt(context.getLevel().damageSources().generic(), 1000.0F);
        context.assertTrue(pickup.isRemoved(), "the pickup was not wrecked");
        context.assertTrue(itemsAround(context, pickup, Items.DIAMOND) > 0, "the bed did not spill");
        context.assertTrue(itemsAround(context, pickup, me.trossty.car.registry.ModItems.CAR) == 0,
                "a wreck dropped the vehicle as an item");
        context.succeed();
    }

    /**
     * A hitched trailer is dragged, not teleported: towed at a wall, it stops
     * against it instead of passing through.
     */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void towedTrailerStopsAtAWall(GameTestHelper context) {
        CarEntity tractor = car(context, new BlockPos(3, 1, 5), false);
        var trailer = context.spawn(ModEntities.TRAILER, new BlockPos(3, 1, 1));
        net.minecraft.world.phys.Vec3 ball = tractor.hitchPoint();
        // Coupler right on the tow ball, both facing the same way.
        tractor.moveTo(tractor.getX(), tractor.getY(), tractor.getZ(), 0.0F, 0.0F);
        trailer.moveTo(ball.x, trailer.getY(), ball.z - 2.0, 0.0F, 0.0F);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // Take the drawbar, then click the car to drop it on the tow ball.
        player.moveTo(ball.x + 1.0, trailer.getY(), ball.z);
        trailer.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        tractor.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(trailer.isHitched(), "the trailer did not hitch");
        double startZ = trailer.getZ();
        // A wall a block ahead of the trailer's front, across its whole width.
        int wallZ = net.minecraft.util.Mth.floor(trailer.getBoundingBox().maxZ) + 1;
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                context.getLevel().setBlockAndUpdate(new BlockPos(
                        net.minecraft.util.Mth.floor(trailer.getX()) + x,
                        net.minecraft.util.Mth.floor(trailer.getY()) + y, wallZ),
                        net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
        }
        context.runAtTickTime(2, () -> tractor.setPos(tractor.getX(), tractor.getY(), tractor.getZ() + 3.0));
        context.runAtTickTime(6, () -> {
            context.assertTrue(trailer.getZ() > startZ, "the trailer did not move towards the tractor");
            context.assertTrue(trailer.getBoundingBox().maxZ <= wallZ + 1.0E-3,
                    "the trailer went into the wall: front at " + trailer.getBoundingBox().maxZ + ", wall at " + wallZ);
            trailer.discard();
            tractor.discard();
            context.succeed();
        });
    }

    /** A plane cannot take a trailer: walked up to its tail and clicked, the drawbar stays in hand. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void planeWillNotTow(GameTestHelper context) {
        var plane = context.spawn(ModEntities.AIRPLANE, new BlockPos(3, 1, 6));
        var trailer = context.spawn(ModEntities.TRAILER, new BlockPos(3, 1, 1));
        plane.moveTo(plane.getX(), plane.getY(), plane.getZ(), 0.0F, 0.0F);
        net.minecraft.world.phys.Vec3 ball = plane.hitchPoint();
        trailer.moveTo(ball.x, trailer.getY(), ball.z - 2.0, 0.0F, 0.0F);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.moveTo(ball.x + 1.0, trailer.getY(), ball.z);
        trailer.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        plane.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertFalse(trailer.isHitched(), "a plane took a trailer on");
        trailer.discard();
        plane.discard();
        context.succeed();
    }

    /** A flat slows the car and pulls it to its side; a spare wheel clicked on fixes it where it stands. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void flatTyreAndSpare(GameTestHelper context) {
        var car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 3));
        float sound = car.getMaxSpeed();
        context.assertTrue(car.pull() == 0.0F, "a new car pulled to one side");
        car.setFlatTyre(0);
        context.assertTrue(car.getMaxSpeed() < sound * 0.6F, "a flat did not slow the car");
        // The first wheel is the front right one: the model's minus x.
        context.assertTrue(car.pull() > 0.0F, "a flat front right did not pull the car right");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack spare = new ItemStack(me.trossty.car.registry.ModItems.WHEEL, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spare);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertFalse(car.hasFlatTyre(), "the spare did not go on");
        context.assertTrue(spare.getCount() == 1, "changing one wheel did not take one spare");
        context.assertTrue(Math.abs(car.getMaxSpeed() - sound) < 1.0E-4F, "the car still limped on its spare");
        context.succeed();
    }

    /**
     * An empty hand on the bonnet or the boot lifts it; into the open boot
     * it finds a chest that keeps what is put in, through a save too; on the
     * raised lid it lets it down; and a wrecked car tips the boot out.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void bonnetAndBootOpen(GameTestHelper context) {
        CarEntity car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 3));
        car.moveTo(car.getX(), car.getY(), car.getZ(), 30.0F, 0.0F);
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var defined = car.getDefinition();
        // Points the player at a spot on the car, in its pixels, from beyond
        // that end of it and a block above.
        java.util.function.BiConsumer<net.minecraft.world.phys.Vec3, Double> pointAt = (pixels, end) -> {
            float yaw = -car.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD;
            net.minecraft.world.phys.Vec3 spot = car.position().add(pixels.scale(1.0 / 16.0).yRot(yaw));
            net.minecraft.world.phys.Vec3 out = new net.minecraft.world.phys.Vec3(0.0, 0.0, end).yRot(yaw);
            player.moveTo(spot.x + out.x * 1.8, spot.y + 1.0 - player.getEyeHeight(), spot.z + out.z * 1.8);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, spot);
        };
        java.util.function.Function<net.minecraft.world.phys.AABB, net.minecraft.world.phys.Vec3> top =
                box -> new net.minecraft.world.phys.Vec3(box.getCenter().x, box.maxY, box.getCenter().z);
        var bonnet = defined.lidBounds(me.trossty.car.vehicle.VehicleDefinition.Box.BONNET);
        var boot = defined.lidBounds(me.trossty.car.vehicle.VehicleDefinition.Box.BOOT);
        context.assertTrue(bonnet != null && boot != null, "the Corolla has no bonnet or no boot lid");
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;

        pointAt.accept(top.apply(bonnet), 1.0);
        car.interact(player, hand);
        context.assertTrue(car.isLidOpen(CarEntity.BONNET_BIT), "the bonnet did not lift");
        context.assertFalse(car.isLidOpen(CarEntity.BOOT_BIT), "the boot opened with the bonnet");
        context.assertFalse(player.isPassenger(), "touching the bonnet put the player in the seat");
        context.assertFalse(car.canRun(), "the car would drive off with the bonnet up");

        pointAt.accept(top.apply(boot), -1.0);
        car.interact(player, hand);
        context.assertTrue(car.isLidOpen(CarEntity.BOOT_BIT), "the boot did not open");
        car.interact(player, hand);
        context.assertTrue(player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu,
                "reaching into the open boot found nothing to put things in");
        car.getBoot().setItem(0, new ItemStack(Items.BREAD, 5));
        context.assertTrue(car.shownBoot().size() == 1 && car.shownBoot().get(0).is(Items.BREAD),
                "the boot's load is not shown to the client");
        player.closeContainer();

        net.minecraft.nbt.CompoundTag saved = new net.minecraft.nbt.CompoundTag();
        car.saveWithoutId(saved);
        CarEntity copy = ModEntities.CAR.create(context.getLevel());
        copy.load(saved);
        context.assertTrue(copy.getBoot().getItem(0).getCount() == 5, "the boot's load was lost in a save");
        context.assertTrue(copy.isLidOpen(CarEntity.BONNET_BIT) && copy.isLidOpen(CarEntity.BOOT_BIT),
                "the lids shut themselves in a save");

        pointAt.accept(top.apply(CarEntity.raisedLid(boot, CarEntity.BOOT_BIT)), -1.0);
        car.interact(player, hand);
        context.assertFalse(car.isLidOpen(CarEntity.BOOT_BIT), "the raised boot lid would not come down");
        pointAt.accept(top.apply(CarEntity.raisedLid(bonnet, CarEntity.BONNET_BIT)), 1.0);
        car.interact(player, hand);
        context.assertFalse(car.isLidOpen(CarEntity.BONNET_BIT), "the raised bonnet would not come down");
        context.assertTrue(car.canRun(), "with both lids shut the car still would not drive");

        // Beside the door, a bare hand still gets in.
        net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(1.0, 0.0, 0.0)
                .yRot(-car.getYRot() * net.minecraft.util.Mth.DEG_TO_RAD);
        player.moveTo(car.getX() + side.x * 2.0, car.getY(), car.getZ() + side.z * 2.0);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, car.position().add(0, 0.8, 0));
        car.interact(player, hand);
        context.assertTrue(player.getVehicle() == car, "the door no longer lets the player in");
        player.stopRiding();

        car.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        context.assertFalse(context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                car.getBoundingBox().inflate(3.0), item -> item.getItem().is(Items.BREAD)).isEmpty(),
                "the boot did not tip out when the car went");
        copy.discard();
        context.succeed();
    }

    /**
     * Tyres come off one at a time, the one pointed at; the brakes behind them
     * are reached once all are off; they go back one at a time, all of a kind.
     */
    @GameTest(template = "empty")
    public static void tyresOneAtATime(GameTestHelper context) {
        BlockPos lift = new BlockPos(3, 1, 3);
        context.setBlock(lift, ModBlocks.VEHICLE_LIFT.defaultBlockState()
                .setValue(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawn(ModEntities.CAR, lift.above());
        car.moveTo(car.getX(), car.getY(), car.getZ(), 0.0F, 0.0F);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        java.util.function.IntConsumer pointAt = wheel -> {
            net.minecraft.world.phys.Vec3 spot = car.wheelSpot(wheel);
            net.minecraft.world.phys.Vec3 away = spot.subtract(car.position()).multiply(1.0, 0.0, 1.0).normalize();
            player.moveTo(spot.x + away.x * 1.5, spot.y - 1.0, spot.z + away.z * 1.5);
            player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, spot);
        };
        float sound = car.getMaxSpeed();
        pointAt.accept(2);
        car.workshop().serviceByHand(player, CarPart.WHEELS);
        context.assertFalse(car.isTyreOn(2), "the tyre pointed at did not come off");
        context.assertTrue(car.tyresOn() == car.tyreCount() - 1, "more than one tyre came off");
        context.assertTrue(player.getInventory().countItem(CarPart.WHEELS.item()) == 1, "not one tyre was handed over");
        context.assertTrue(car.getMaxSpeed() < sound * 0.3F, "the car would drive on a bare hub");
        car.workshop().serviceByHand(player, CarPart.WHEELS);
        context.assertFalse(car.isTyreOn(2), "an empty hand put the tyre back");
        car.workshop().serviceByHand(player, CarPart.BRAKE_PADS);
        context.assertTrue(car.parts().isFitted(CarPart.BRAKE_PADS), "the brakes came off through the other tyres");
        for (int wheel = 0; wheel < car.tyreCount(); wheel++) {
            if (wheel != 2) {
                pointAt.accept(wheel);
                car.workshop().serviceByHand(player, CarPart.WHEELS);
            }
        }
        context.assertFalse(car.parts().isFitted(CarPart.WHEELS), "with every tyre off the wheels still counted as on");
        context.assertTrue(player.getInventory().countItem(CarPart.WHEELS.item()) == car.tyreCount(),
                "the tyres were not all handed over");
        ItemStack road = new ItemStack(CarPart.WHEELS.item(), 4);
        player.getInventory().clearContent();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, road);
        pointAt.accept(1);
        car.workshop().serviceByHand(player, CarPart.WHEELS);
        context.assertTrue(car.isTyreOn(1) && car.tyresOn() == 1, "one tyre did not go on by itself");
        context.assertTrue(road.getCount() == 3, "fitting one tyre did not take one");
        ItemStack snow = new ItemStack(CarPart.WHEELS.item(), 1);
        snow.set(me.trossty.car.registry.ModComponents.PART_VARIANT, CarSetup.Tyre.SNOW.ordinal());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, snow);
        pointAt.accept(0);
        car.workshop().serviceByHand(player, CarPart.WHEELS);
        context.assertFalse(car.isTyreOn(0), "a snow tyre went on beside a road tyre");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, road);
        for (int wheel : new int[] {0, 2, 3}) {
            pointAt.accept(wheel);
            car.workshop().serviceByHand(player, CarPart.WHEELS);
        }
        context.assertTrue(car.tyresOn() == 4 && road.isEmpty(), "the set did not go back on");
        context.assertTrue(Math.abs(car.getMaxSpeed() - sound) < 1.0E-4F, "the car still limped with four tyres");
        context.succeed();
    }

    /** A locked car stays where it is: the winch only takes it for someone with its key. */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void winchLeavesLockedCars(GameTestHelper context) {
        var bed = context.spawn(ModEntities.TRAILER, new BlockPos(3, 1, 7));
        bed.setModel(me.trossty.car.CarMain.id("flatbed_trailer"));
        bed.moveTo(bed.getX(), bed.getY(), bed.getZ(), 0.0F, 0.0F);
        var car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 1));
        car.moveTo(car.getX(), car.getY(), car.getZ(), 0.0F, 0.0F);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        car.saveWithoutId(tag);
        tag.putLong("LockCode", 7L);
        tag.putBoolean("Locked", true);
        car.load(tag);
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        player.moveTo(bed.getX() + 1.5, bed.getY(), bed.getZ());
        bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertFalse(bed.hasCargo(), "the winch took a locked car");
        context.assertFalse(car.isRemoved(), "the locked car left the ground");
        ItemStack key = new ItemStack(me.trossty.car.registry.ModItems.CAR_KEY);
        key.set(me.trossty.car.registry.ModComponents.VEHICLE_KEY, 7L);
        player.getInventory().setItem(9, key);
        bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(bed.hasCargo(), "the owner could not winch their own car");
        bed.discard();
        context.succeed();
    }

    /**
     * A wheel lift goes on the back of the tow truck and nothing else: walked
     * up to a plain pickup it is refused, to the tow truck it hitches.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void wheelLiftOnlyOnTowTruck(GameTestHelper context) {
        var lift = context.spawn(ModEntities.TRAILER, new BlockPos(3, 1, 1));
        lift.setModel(me.trossty.car.CarMain.id("wheel_lift"));
        lift.moveTo(lift.getX(), lift.getY(), lift.getZ(), 0.0F, 0.0F);
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.moveTo(lift.getX() + 1.5, lift.getY(), lift.getZ());
        lift.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertTrue(lift.getHolder() == player, "the wheel lift was not picked up by its stinger");
        for (String model : new String[] {"toyota_hilux", "hilux_tow"}) {
            var truck = context.spawn(ModEntities.PICKUP, new BlockPos(3, 1, 1));
            truck.setModel(me.trossty.car.CarMain.id(model));
            // Its tow ball just where the stinger's eye is.
            double back = lift.getZ() + 1.25 - truck.hitchPoint().z + truck.getZ();
            truck.moveTo(lift.getX(), lift.getY(), back, 0.0F, 0.0F);
            truck.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            boolean onIt = lift.getTractor() == truck;
            context.assertTrue(onIt == model.equals("hilux_tow"),
                    model + (onIt ? " took the wheel lift" : " would not take the wheel lift"));
            truck.discard();
        }
        lift.discard();
        context.succeed();
    }

    /**
     * A caravan is slept in at night from its door, keeps its sleeper asleep
     * though there is no bed block, and puts them out beside it in the
     * morning; by day, or from the drawbar end, a click is not for sleeping.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void caravanSleeps(GameTestHelper context) {
        var caravan = context.spawn(ModEntities.TRAILER, new BlockPos(4, 1, 4));
        caravan.setModel(me.trossty.car.CarMain.id("caravan"));
        caravan.moveTo(caravan.getX(), caravan.getY(), caravan.getZ(), 0.0F, 0.0F);
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        var level = context.getLevel();
        long time = level.getDayTime();
        try {
            level.setDayTime(6000L);
            level.updateSkyBrightness();
            player.moveTo(caravan.getX() - 2.0, caravan.getY(), caravan.getZ());
            caravan.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            context.assertFalse(player.isSleeping(), "slept in a caravan at noon");
            level.setDayTime(18000L);
            level.updateSkyBrightness();
            player.moveTo(caravan.getX(), caravan.getY(), caravan.getZ() + 3.0);
            caravan.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            context.assertFalse(player.isSleeping(), "the drawbar end put the player to bed");
            player.moveTo(caravan.getX() - 2.0, caravan.getY(), caravan.getZ());
            caravan.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            context.assertTrue(player.isSleeping(), "could not sleep in the caravan at night");
            player.doTick();
            player.doTick();
            context.assertTrue(player.isSleeping(), "woken at once for want of a bed block");
            player.stopSleepInBed(false, true);
            context.assertFalse(caravan.getBoundingBox().contains(player.position()), "woke up inside the caravan");
            context.assertTrue(player.getX() < caravan.getX() - 1.0, "woke up away from the door");
        } finally {
            level.setDayTime(time);
            level.updateSkyBrightness();
        }
        caravan.discard();
        context.succeed();
    }

    /**
     * A key clicked at nothing finds its own car, not a stranger's, and not
     * one beyond the button's reach.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void keyFindsItsCar(GameTestHelper context) {
        var mine = context.spawn(ModEntities.CAR, new BlockPos(1, 1, 1));
        var other = context.spawn(ModEntities.CAR, new BlockPos(6, 1, 6));
        mine.setLockCode(7L);
        other.setLockCode(9L);
        var player = context.makeMockServerPlayerInLevel();
        player.moveTo(other.getX() + 1.0, other.getY(), other.getZ());
        ItemStack key = new ItemStack(me.trossty.car.registry.ModItems.CAR_KEY);
        key.set(me.trossty.car.registry.ModComponents.VEHICLE_KEY, 7L);
        context.assertTrue(me.trossty.car.item.CarKeyItem.find(context.getLevel(), player, 7L) == mine,
                "the key did not find its car");
        context.assertTrue(me.trossty.car.item.CarKeyItem.find(context.getLevel(), player, 5L) == null,
                "a key with no car found one");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, key);
        context.assertTrue(key.use(context.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND)
                .getResult().consumesAction(), "clicking the key at nothing did nothing");
        player.moveTo(mine.getX() + me.trossty.car.item.CarKeyItem.FOB_RANGE + 4.0, mine.getY(), mine.getZ());
        context.assertTrue(me.trossty.car.item.CarKeyItem.find(context.getLevel(), player, 7L) == null,
                "the key reached a car out of range");
        mine.discard();
        other.discard();
        context.succeed();
    }

    /**
     * Walking a trailer by hand slows the walker, more with a car on the bed,
     * and letting go gives them their pace back.
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void drawbarSlowsByWeight(GameTestHelper context) {
        var bed = context.spawn(ModEntities.TRAILER, new BlockPos(3, 1, 7));
        bed.setModel(me.trossty.car.CarMain.id("flatbed_trailer"));
        bed.moveTo(bed.getX(), bed.getY(), bed.getZ(), 0.0F, 0.0F);
        var car = context.spawn(ModEntities.CAR, new BlockPos(3, 1, 1));
        car.moveTo(car.getX(), car.getY(), car.getZ(), 0.0F, 0.0F);
        var player = context.makeMockServerPlayerInLevel();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.moveTo(bed.getX() + 1.5, bed.getY(), bed.getZ());
        var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double free = speed.getValue();
        bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.runAfterDelay(3, () -> {
            double empty = speed.getValue();
            context.assertTrue(empty < free, "an empty trailer did not slow the walk");
            player.setShiftKeyDown(true);
            bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            player.setShiftKeyDown(false);
            context.assertTrue(bed.hasCargo(), "the car did not go on the bed");
            context.runAfterDelay(3, () -> {
                double laden = speed.getValue();
                context.assertTrue(laden < empty * 0.75, "a car on the bed did not slow the walk further: "
                        + empty + " -> " + laden);
                bed.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                context.assertTrue(Math.abs(speed.getValue() - free) < 1.0E-6, "letting go did not give the pace back");
                bed.discard();
                context.succeed();
            });
        });
    }

    /**
     * A jack pumped up by hand stays up when something is built next to it;
     * only redstone coming on or going off moves it.
     */
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void jackStaysUpBesideNewBlocks(GameTestHelper context) {
        BlockPos jack = new BlockPos(2, 1, 2);
        context.setBlock(jack, me.trossty.car.registry.ModBlocks.AIRCRAFT_JACK.defaultBlockState()
                .setValue(me.trossty.car.block.AircraftJackBlock.RAISED, true));
        context.setBlock(jack.east(), net.minecraft.world.level.block.Blocks.STONE);
        context.assertBlockProperty(jack, me.trossty.car.block.AircraftJackBlock.RAISED, true);
        context.setBlock(jack.west(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
        context.assertBlockProperty(jack, me.trossty.car.block.AircraftJackBlock.RAISED, true);
        context.setBlock(jack.west(), net.minecraft.world.level.block.Blocks.AIR);
        context.assertBlockProperty(jack, me.trossty.car.block.AircraftJackBlock.RAISED, false);
        context.succeed();
    }

    // ---- the drum ----

    private static void clickDrum(GameTestHelper context, net.minecraft.world.entity.player.Player player,
                                  BlockPos relative) {
        BlockPos at = context.absolutePos(relative);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),
                Direction.NORTH, at, false);
        BlockState state = context.getLevel().getBlockState(at);
        ItemStack held = player.getMainHandItem();
        if (state.useItemOn(held, context.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit)
                == net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            state.useWithoutItem(context.getLevel(), player, hit);
        }
    }

    /**
     * A drum takes a bucket and a jerry can, refuses a second liquid, reads on
     * a comparator, and is carried away full: broken and put down again it
     * holds what it held.
     */
    @GameTest(template = "empty")
    public static void drumFillsAndTravelsFull(GameTestHelper context) {
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos water = new BlockPos(1, 1, 1);
        BlockPos petrol = new BlockPos(3, 1, 1);
        context.setBlock(water, me.trossty.car.registry.ModBlocks.FUEL_DRUM);
        context.setBlock(petrol, me.trossty.car.registry.ModBlocks.FUEL_DRUM);
        var wet = context.<me.trossty.car.block.entity.FuelDrumBlockEntity>getBlockEntity(water);
        var fuel = context.<me.trossty.car.block.entity.FuelDrumBlockEntity>getBlockEntity(petrol);

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        clickDrum(context, player, water);
        context.assertTrue(wet.getAmount() == BUCKET
                && wet.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER), "a bucket of water did not go in");
        context.assertTrue(player.getMainHandItem().is(Items.BUCKET), "the bucket did not come back empty");

        ItemStack can = new ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
        CarEntity.markFuel(can, CarSetup.Fuel.PETROL);
        me.trossty.car.item.JerryCans.setUnits(can, me.trossty.car.item.JerryCans.CAPACITY);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, can);
        clickDrum(context, player, water);
        context.assertTrue(wet.getFluid().isSame(net.minecraft.world.level.material.Fluids.WATER)
                        && me.trossty.car.item.JerryCans.isFull(can), "petrol went into a drum of water");
        clickDrum(context, player, petrol);
        long poured = fuel.getAmount();
        context.assertTrue(poured > 0 && fuel.getFluid().isSame(ModFluids.PETROL), "the can did not pour petrol in");
        context.assertTrue(context.getBlockState(petrol).getAnalogOutputSignal(context.getLevel(),
                context.absolutePos(petrol)) > 0, "a comparator reads an empty drum");

        context.getLevel().destroyBlock(context.absolutePos(petrol), true);
        var drops = context.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(context.absolutePos(petrol)).inflate(2.0),
                drop -> drop.getItem().is(me.trossty.car.registry.ModBlocks.FUEL_DRUM.asItem()));
        context.assertTrue(drops.size() == 1, "breaking the drum dropped " + drops.size() + " drums");
        ItemStack carried = drops.get(0).getItem().copy();
        drops.get(0).discard();
        var held = carried.get(me.trossty.car.registry.ModComponents.STORED_FLUID);
        context.assertTrue(held != null && held.amount() == poured && held.fluid().isSame(ModFluids.PETROL),
                "the drum item does not carry the petrol");

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, carried);
        context.setBlock(new BlockPos(5, 0, 3), net.minecraft.world.level.block.Blocks.STONE);
        clickTop(context, player, new BlockPos(5, 0, 3));
        var again = context.<me.trossty.car.block.entity.FuelDrumBlockEntity>getBlockEntity(new BlockPos(5, 1, 3));
        context.assertTrue(again.getAmount() == poured && again.getFluid().isSame(ModFluids.PETROL),
                "put down again, the drum holds " + again.getAmount() + " of " + poured);
        context.succeed();
    }
}
