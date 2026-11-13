package me.trossty.car.test;

import me.trossty.car.block.BatteryBankBlock;
import me.trossty.car.block.ChargingPostBlock;
import me.trossty.car.block.EnergyCableBlock;
import me.trossty.car.block.entity.BatteryBankBlockEntity;
import me.trossty.car.block.entity.ChargingPostBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.entity.FaultCodes;
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
            context.assertTrue(first.getEnergy() >= last.getEnergy(),
                    "charge should fall along the line, got " + first.getEnergy() + " then " + last.getEnergy());
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
            context.assertTrue(context.<EnergyCableBlockEntity>getBlockEntity(a).getEnergy() == 0
                    && context.<EnergyCableBlockEntity>getBlockEntity(b).getEnergy() == 0,
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
        context.runAtTickTime(140, () -> {
            FuelTankBlockEntity tank = context.<FuelTankBlockEntity>getBlockEntity(tankAt);
            int inPipes = context.<FuelPipeBlockEntity>getBlockEntity(p1).getPetrol()
                    + context.<FuelPipeBlockEntity>getBlockEntity(p2).getPetrol();
            int total = plant.getOutput().getUnits() + inPipes + tank.getPetrol();
            context.assertTrue(tank.getPetrol() > 0, "no petrol reached the tank");
            context.assertTrue(total == 400, "petrol is not conserved: " + total + " of 400");
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
        cable(context, new BlockPos(3, 1, 4), new BlockPos(3, 1, 2));
        EnergyCableBlockEntity front = context.<EnergyCableBlockEntity>getBlockEntity(new BlockPos(3, 1, 4));
        EnergyCableBlockEntity back = context.<EnergyCableBlockEntity>getBlockEntity(new BlockPos(3, 1, 2));
        context.runAtTickTime(20, () -> {
            context.assertTrue(front.getEnergyStorage().getEnergyStored() > 0, "the cable on the front got nothing");
            context.assertTrue(back.getEnergyStorage().getEnergyStored() == 0,
                    "the battery gave " + back.getEnergyStorage().getEnergyStored() + " out of its back");
            context.assertTrue(bank.getEnergy() < 50_000, "the battery did not drain");
            context.assertTrue(context.getBlockState(at).getValue(BatteryBankBlock.LEVEL) == 1,
                    "a quarter-full battery shows " + context.getBlockState(at).getValue(BatteryBankBlock.LEVEL) + " bars");
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
    public static void chargingPostIgnoresAPetrolCar(GameTestHelper context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlock(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.<ChargingPostBlockEntity>getBlockEntity(at);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), false);
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
        car.setCondition(CarPart.BRAKE_PADS, CarPart.MAX_CONDITION * 0.10F);
        car.setCondition(CarPart.WHEELS, CarPart.MAX_CONDITION * 0.30F);
        car.setCondition(CarPart.AIR_FILTER, CarPart.MAX_CONDITION * 0.45F);
        car.setCondition(CarPart.EXHAUST, 0.0F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0420", "C0040", "C0750", "P0101")),
                "the reader listed " + codes);
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void codeReaderSkipsPlugsOnAnElectricCar(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), true);
        car.setCondition(CarPart.SPARK_PLUGS, CarPart.MAX_CONDITION * 0.05F);
        car.setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION * 0.20F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0A1A")), "an electric car read " + codes);
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void oldCarsAndTiredPartsWearFaster(GameTestHelper context) {
        CarEntity fresh = car(context, new BlockPos(2, 1, 3), false);
        CarEntity old = car(context, new BlockPos(5, 1, 3), false);
        old.setOdometer(CarEntity.HIGH_MILEAGE * 3);
        old.setCondition(CarPart.CLUTCH, CarPart.MAX_CONDITION * 0.5F);
        context.assertTrue(Math.abs(fresh.ageFactor(CarPart.ENGINE) - 1.0F) < 1e-4F,
                "a new car's engine wears at " + fresh.ageFactor(CarPart.ENGINE));
        context.assertTrue(Math.abs(old.ageFactor(CarPart.ENGINE) - (1.0F + CarEntity.MILEAGE_WEAR)) < 1e-4F,
                "an old car's fresh engine wears at " + old.ageFactor(CarPart.ENGINE));
        float tired = (1.0F + CarEntity.MILEAGE_WEAR) * (1.0F + CarEntity.TIRED_WEAR * 0.5F);
        context.assertTrue(Math.abs(old.ageFactor(CarPart.CLUTCH) - tired) < 1e-4F,
                "a half-worn clutch on an old car wears at " + old.ageFactor(CarPart.CLUTCH) + ", not " + tired);
        context.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void cableGlowsOnlyWhileCarrying(GameTestHelper context) {
        cable(context, new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(5, 1, 3));
        context.<EnergyCableBlockEntity>getBlockEntity(new BlockPos(2, 1, 3)).setEnergy(800);
        context.runAtTickTime(25, () -> {
            context.assertTrue(context.getBlockState(new BlockPos(3, 1, 3)).getValue(EnergyCableBlock.POWERED),
                    "a cable carrying power is dark");
            context.assertTrue(!context.getBlockState(new BlockPos(5, 1, 3)).getValue(EnergyCableBlock.POWERED),
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
        context.assertTrue(car.isFitted(part), "a new car has no air filter");

        car.serviceByHand(player, part);
        context.assertFalse(car.isFitted(part), "the air filter did not come off");
        context.assertTrue(player.getMainHandItem().isEmpty(), "the air filter landed in the working hand");
        context.assertTrue(player.getInventory().countItem(part.item()) == 1, "the air filter was not handed over");

        car.serviceByHand(player, part);
        context.assertFalse(car.isFitted(part), "a second click with an empty hand put it back");
        var creative = context.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        car.serviceByHand(creative, part);
        context.assertFalse(car.isFitted(part), "creative conjured an air filter into the empty slot");

        int pocket = -1;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(part.item())) {
                pocket = slot;
            }
        }
        ItemStack spare = player.getInventory().removeItemNoUpdate(pocket);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spare);
        car.serviceByHand(player, part);
        context.assertTrue(car.isFitted(part), "holding the spare and clicking did not fit it");
        context.assertTrue(player.getMainHandItem().isEmpty(), "fitting it did not use the spare up");
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

    /** A trailer has no engine: nothing about one can be worked on, read or worn. */
    @GameTest(template = "empty")
    public static void trailerHasNoEngine(GameTestHelper context) {
        CarEntity trailer = context.spawn(ModEntities.TRAILER, new BlockPos(2, 1, 2));
        context.assertFalse(trailer.hasPart(CarPart.ENGINE), "a trailer has an engine");
        context.assertFalse(trailer.canReach(CarPart.ENGINE), "a trailer's engine can be reached");
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
        float before = car.getHealth();
        car.hurt(context.getLevel().damageSources().playerAttack(player), 2.0F);
        context.assertFalse(car.isRemoved(), "a punch took the car away");
        context.assertTrue(car.getHealth() < before, "a punch did not dent the car");
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
        float before = car.getHealth();
        car.hurt(context.getLevel().damageSources().playerAttack(player), 1.0F);
        float lost = before - car.getHealth();
        context.assertTrue(lost > 0.0F && lost <= 0.3F, "a punch took " + lost + " off the car");
        car.discard();
        context.succeed();
    }

    /** A worn rack makes the car pull, always the same way; a sound one runs straight. */
    @GameTest(template = "empty")
    public static void aWornRackPulls(GameTestHelper context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        context.assertTrue(car.pull() == 0.0F, "a fresh car pulls " + car.pull());
        car.setCondition(CarPart.STEERING_RACK, 10.0F);
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
        float filter = car.getConditionFraction(CarPart.AIR_FILTER);
        for (BlockPos at : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(6, 3, 6))) {
            context.setBlock(at, net.minecraft.world.level.block.Blocks.WATER);
        }
        context.runAfterDelay(10, () -> {
            context.assertTrue(car.isFlooded(), "the car did not flood under water");
            context.assertTrue(car.getConditionFraction(CarPart.AIR_FILTER) < filter - 0.2F,
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
                car.serviceByHand(player, part);
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
        context.assertTrue(car.pull() < 0.0F, "a flat front left did not pull the car left");
        var player = context.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack spare = new ItemStack(me.trossty.car.registry.ModItems.WHEEL, 2);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spare);
        car.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        context.assertFalse(car.hasFlatTyre(), "the spare did not go on");
        context.assertTrue(spare.getCount() == 1, "changing one wheel did not take one spare");
        context.assertTrue(Math.abs(car.getMaxSpeed() - sound) < 1.0E-4F, "the car still limped on its spare");
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
