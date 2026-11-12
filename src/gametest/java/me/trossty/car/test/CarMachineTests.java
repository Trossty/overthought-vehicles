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
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import team.reborn.energy.api.EnergyStorage;

/**
 * Headless checks with real numbers: each one builds a little piece of the
 * production line in an empty 8x8x8 box, lets it run, and fails the build if
 * what came out is not what went in. These are the regressions the photo test
 * can only show, not catch.
 *
 * <p>Run with {@code ./gradlew runGametest}.
 */
public class CarMachineTests {

    // ---- helpers ----

    /** Sets a machine down facing south, every cell of it, as placing it by hand would. */
    private static void machine(TestContext context, Block block, BlockPos at) {
        BlockState state = block.getDefaultState().with(MultiblockBlock.FACING, Direction.SOUTH);
        context.setBlockState(at, state);
        ((MultiblockBlock) block).placeParts(context.getWorld(), context.getAbsolutePos(at), state);
    }

    /** Lays cable along these spots, then lets each length join up as a hand-laid one would. */
    private static void cable(TestContext context, BlockPos... run) {
        for (BlockPos at : run) {
            context.setBlockState(at, ModBlocks.ENERGY_CABLE);
        }
        ServerWorld world = context.getWorld();
        for (BlockPos at : run) {
            BlockPos pos = context.getAbsolutePos(at);
            BlockState state = world.getBlockState(pos);
            for (Direction side : Direction.values()) {
                state = state.getStateForNeighborUpdate(world, world, pos, side, pos.offset(side),
                        world.getBlockState(pos.offset(side)), world.getRandom());
            }
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }

    private static GeneratorBlockEntity coalGenerator(TestContext context, BlockPos at, int coal) {
        context.setBlockState(at, ModBlocks.GENERATOR);
        GeneratorBlockEntity generator = context.getBlockEntity(at, GeneratorBlockEntity.class);
        generator.setStack(0, new ItemStack(Items.COAL, coal));
        return generator;
    }

    // ---- generators ----

    @GameTest(maxTicks = 40)
    public void coalGeneratorBurnsAndFills(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        GeneratorBlockEntity generator = coalGenerator(context, at, 4);
        context.runAtTick(20, () -> {
            context.assertTrue(generator.isBurning(), "coal generator is not burning");
            context.assertTrue(generator.getEnergy() > 0, "coal generator made no energy");
            context.assertTrue(context.getBlockState(at).get(me.trossty.car.block.GeneratorBlock.LIT),
                    "a burning generator is not lit");
            context.assertTrue(generator.getStack(0).getCount() == 3, "it should have burnt exactly one coal");
            context.assertTrue(!generator.getEnergyStorage().supportsInsertion(),
                    "a generator must not take energy in");
            context.complete();
        });
    }

    @GameTest(maxTicks = 40)
    public void dieselGeneratorDrinksItsTank(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlockState(at, ModBlocks.DIESEL_GENERATOR);
        GeneratorBlockEntity generator = context.getBlockEntity(at, GeneratorBlockEntity.class);
        generator.getTank().set(ModFluids.DIESEL, 100);
        context.runAtTick(21, () -> {
            int used = 100 - generator.getTank().getUnits();
            // a unit every four ticks
            context.assertTrue(used >= 4 && used <= 6, "diesel generator used " + used + " units in 20 ticks");
            context.assertTrue(generator.getEnergy() >= 19 * GeneratorBlockEntity.Kind.DIESEL.output,
                    "diesel generator made only " + generator.getEnergy());
            context.complete();
        });
    }

    @GameTest(maxTicks = 30)
    public void emptyGeneratorStaysCold(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        GeneratorBlockEntity generator = coalGenerator(context, at, 0);
        context.runAtTick(20, () -> {
            context.assertTrue(!generator.isBurning() && generator.getEnergy() == 0,
                    "an empty generator made energy");
            context.complete();
        });
    }

    // ---- cables ----

    @GameTest(maxTicks = 220)
    public void cablesCarryPowerToAMachine(TestContext context) {
        coalGenerator(context, new BlockPos(0, 1, 2), 16);
        BlockPos[] run = new BlockPos[6];
        for (int i = 0; i < run.length; i++) {
            run[i] = new BlockPos(1 + i, 1, 2);
        }
        cable(context, run);
        BlockPos plantAt = new BlockPos(7, 1, 2);
        machine(context, ModBlocks.HYDROTREATER, plantAt);
        HydrotreaterBlockEntity plant = context.getBlockEntity(plantAt, HydrotreaterBlockEntity.class);
        plant.getInput().set(ModFluids.RAW_PETROL, 1000);

        context.runAtTick(200, () -> {
            EnergyCableBlockEntity first = context.getBlockEntity(run[0], EnergyCableBlockEntity.class);
            EnergyCableBlockEntity last = context.getBlockEntity(run[run.length - 1], EnergyCableBlockEntity.class);
            context.assertTrue(first.getEnergy() >= last.getEnergy(),
                    "charge should fall along the line, got " + first.getEnergy() + " then " + last.getEnergy());
            context.assertTrue(plant.getOutput().getUnits() > 0,
                    "the hydrotreater at the end of the cable never ran");
            context.assertTrue(context.getBlockState(run[0]).get(EnergyCableBlock.SIDES.get(Direction.WEST))
                    == EnergyCableBlock.Side.DEVICE, "the first length does not plug into the generator");
            context.assertTrue(context.getBlockState(run[2]).get(EnergyCableBlock.SIDES.get(Direction.EAST))
                    == EnergyCableBlock.Side.CABLE, "the lengths do not join each other");
            context.complete();
        });
    }

    @GameTest(maxTicks = 60)
    public void cableWithNoSourceStaysEmpty(TestContext context) {
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = new BlockPos(3, 1, 2);
        cable(context, a, b);
        context.runAtTick(40, () -> {
            context.assertTrue(context.getBlockEntity(a, EnergyCableBlockEntity.class).getEnergy() == 0
                    && context.getBlockEntity(b, EnergyCableBlockEntity.class).getEnergy() == 0,
                    "cable made energy out of nothing");
            context.complete();
        });
    }

    // ---- crude: the pumpjack ----

    @GameTest(maxTicks = 80)
    public void pumpjackDrawsCrudeFromADeposit(TestContext context) {
        for (int x = 2; x <= 4; x++) {
            for (int z = 3; z <= 5; z++) {
                context.setBlockState(new BlockPos(x, 0, z), ModBlocks.OIL_DEPOSIT);
            }
        }
        BlockPos at = new BlockPos(3, 1, 5);
        machine(context, ModBlocks.PUMPJACK, at);
        PumpjackBlockEntity jack = context.getBlockEntity(at, PumpjackBlockEntity.class);
        jack.setEnergy(PumpjackBlockEntity.ENERGY_CAPACITY);
        context.runAtTick(60, () -> {
            context.assertTrue(jack.isWorking(), "pumpjack over a deposit with power is not working");
            int crude = jack.getCrude().getUnits();
            context.assertTrue(crude >= 400, "pumpjack drew only " + crude + " units in 60 ticks");
            context.assertTrue(jack.getCrude().getFluid() == ModFluids.CRUDE_OIL, "pumpjack drew something but crude");
            context.assertTrue(jack.estimateReserve() == 9 * PumpjackBlockEntity.CRUDE_PER_DEPOSIT - crude,
                    "reserve " + jack.estimateReserve() + " does not match " + crude + " drawn");
            context.complete();
        });
    }

    @GameTest(maxTicks = 40)
    public void pumpjackWithoutPowerStands(TestContext context) {
        context.setBlockState(new BlockPos(3, 0, 4), ModBlocks.OIL_DEPOSIT);
        BlockPos at = new BlockPos(3, 1, 5);
        machine(context, ModBlocks.PUMPJACK, at);
        PumpjackBlockEntity jack = context.getBlockEntity(at, PumpjackBlockEntity.class);
        context.runAtTick(20, () -> {
            context.assertTrue(!jack.isWorking() && jack.getCrude().isEmpty(), "pumpjack ran with no power");
            context.complete();
        });
    }

    // ---- the refinery ----

    @GameTest(maxTicks = 140)
    public void refineryCracksCrudeOneForOne(TestContext context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.getBlockEntity(at, FuelRefineryBlockEntity.class);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setStack(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setTemperature(FuelRefineryBlockEntity.HOT_TEMPERATURE);
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        context.runAtTick(120, () -> {
            int feed = refinery.getBiomass();
            int product = refinery.getPetrol();
            context.assertTrue(product >= 2 * FuelRefineryBlockEntity.PETROL_PER_REFINE,
                    "a hot refinery made only " + product + " in 120 ticks");
            context.assertTrue(feed + product == 1000, "crude is not conserved: feed " + feed + " + product " + product);
            context.assertTrue(refinery.getProduct() == FuelRefineryBlockEntity.PRODUCT_RAW_PETROL,
                    "crude with redstone should make raw petrol");
            ItemStack tar = refinery.getStack(FuelRefineryBlockEntity.TAR_SLOT);
            context.assertTrue(tar.isOf(me.trossty.car.registry.ModItems.TAR)
                            && tar.getCount() == product / FuelRefineryBlockEntity.PETROL_PER_REFINE,
                    "expected a tar a batch, got " + tar);
            context.complete();
        });
    }

    @GameTest(maxTicks = 160)
    public void refineryCansRawFuelAsRaw(TestContext context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.getBlockEntity(at, FuelRefineryBlockEntity.class);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setStack(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setTemperature(FuelRefineryBlockEntity.HOT_TEMPERATURE);
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        ItemStack can = new ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
        me.trossty.car.item.JerryCans.setUnits(can, 0);
        refinery.setStack(FuelRefineryBlockEntity.CAN_SLOT, can);
        context.runAtTick(140, () -> {
            ItemStack filled = refinery.getStack(FuelRefineryBlockEntity.CAN_SLOT);
            context.assertTrue(!me.trossty.car.item.JerryCans.isEmpty(filled), "the can stayed empty");
            context.assertTrue(CarEntity.isRaw(filled), "raw petrol came out as clean");
            context.assertTrue(CarEntity.fuelIn(filled) == CarSetup.Fuel.PETROL, "it is not petrol");
            context.complete();
        });
    }

    @GameTest(maxTicks = 60)
    public void coldRefineryOnlyWarms(TestContext context) {
        BlockPos at = new BlockPos(5, 1, 5);
        machine(context, ModBlocks.FUEL_REFINERY, at);
        FuelRefineryBlockEntity refinery = context.getBlockEntity(at, FuelRefineryBlockEntity.class);
        refinery.setFeed(FuelRefineryBlockEntity.FEED_CRUDE, 1000);
        refinery.setStack(FuelRefineryBlockEntity.CATALYST_SLOT, new ItemStack(Items.REDSTONE, 16));
        refinery.setEnergy(FuelRefineryBlockEntity.ENERGY_CAPACITY);
        context.runAtTick(40, () -> {
            context.assertTrue(refinery.getPetrol() == 0, "a cold column cracked crude");
            context.assertTrue(refinery.getTemperature() > 0, "the heater did not warm the column");
            context.complete();
        });
    }

    // ---- the hydrotreater and the pipes ----

    @GameTest(maxTicks = 60)
    public void hydrotreaterCleansEveryUnit(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, at);
        HydrotreaterBlockEntity plant = context.getBlockEntity(at, HydrotreaterBlockEntity.class);
        plant.getInput().set(ModFluids.RAW_DIESEL, 100);
        plant.setEnergy(HydrotreaterBlockEntity.ENERGY_CAPACITY);
        context.runAtTick(40, () -> {
            context.assertTrue(plant.getInput().isEmpty(), "raw diesel left: " + plant.getInput().getUnits());
            context.assertTrue(plant.getOutput().getUnits() == 100 && plant.getOutput().getFluid() == ModFluids.DIESEL,
                    "expected 100 clean diesel, got " + plant.getOutput().getUnits());
            context.complete();
        });
    }

    @GameTest(maxTicks = 160)
    public void pipesCarryPetrolIntoATank(TestContext context) {
        BlockPos at = new BlockPos(1, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, at);
        HydrotreaterBlockEntity plant = context.getBlockEntity(at, HydrotreaterBlockEntity.class);
        plant.getOutput().set(ModFluids.PETROL, 400);
        BlockPos p1 = new BlockPos(2, 1, 3);
        BlockPos p2 = new BlockPos(3, 1, 3);
        BlockPos tankAt = new BlockPos(4, 1, 3);
        context.setBlockState(p1, ModBlocks.FUEL_PIPE);
        context.setBlockState(p2, ModBlocks.FUEL_PIPE);
        context.setBlockState(tankAt, ModBlocks.FUEL_TANK);
        context.runAtTick(140, () -> {
            FuelTankBlockEntity tank = context.getBlockEntity(tankAt, FuelTankBlockEntity.class);
            int inPipes = context.getBlockEntity(p1, FuelPipeBlockEntity.class).getPetrol()
                    + context.getBlockEntity(p2, FuelPipeBlockEntity.class).getPetrol();
            int total = plant.getOutput().getUnits() + inPipes + tank.getPetrol();
            context.assertTrue(tank.getPetrol() > 0, "no petrol reached the tank");
            context.assertTrue(total == 400, "petrol is not conserved: " + total + " of 400");
            context.complete();
        });
    }

    // ---- energy API sanity ----

    @GameTest(maxTicks = 20)
    public void machinesOfferPowerDoors(TestContext context) {
        BlockPos plantAt = new BlockPos(3, 1, 3);
        machine(context, ModBlocks.HYDROTREATER, plantAt);
        context.runAtTick(2, () -> {
            for (Direction side : Direction.values()) {
                EnergyStorage door = EnergyStorage.SIDED.find(context.getWorld(),
                        context.getAbsolutePos(plantAt.up()), side);
                context.assertTrue(door != null && door.supportsInsertion(),
                        "the hydrotreater's upper cell has no power door on " + side);
            }
            context.complete();
        });
    }

    // ---- battery bank and charging post ----

    private static BatteryBankBlockEntity battery(TestContext context, BlockPos at, Direction front, long energy) {
        context.setBlockState(at, ModBlocks.BATTERY_BANK.getDefaultState().with(BatteryBankBlock.FACING, front));
        BatteryBankBlockEntity bank = context.getBlockEntity(at, BatteryBankBlockEntity.class);
        bank.setEnergy(energy);
        return bank;
    }

    private static CarEntity car(TestContext context, BlockPos at, boolean electric) {
        CarEntity car = context.spawnEntity(ModEntities.CAR, at);
        int setup = CarSetup.DEFAULT;
        if (electric) {
            setup = CarSetup.withVariant(setup, CarPart.ENGINE, CarSetup.ELECTRIC_MOTOR);
        }
        car.setSetup(setup);
        car.setFuel(0.0F);
        return car;
    }

    @GameTest(maxTicks = 40)
    public void batteryGivesOnlyOutOfItsFront(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        BatteryBankBlockEntity bank = battery(context, at, Direction.SOUTH, 50_000);
        cable(context, new BlockPos(3, 1, 4), new BlockPos(3, 1, 2));
        EnergyCableBlockEntity front = context.getBlockEntity(new BlockPos(3, 1, 4), EnergyCableBlockEntity.class);
        EnergyCableBlockEntity back = context.getBlockEntity(new BlockPos(3, 1, 2), EnergyCableBlockEntity.class);
        context.runAtTick(20, () -> {
            context.assertTrue(front.getEnergyStorage().getAmount() > 0, "the cable on the front got nothing");
            context.assertTrue(back.getEnergyStorage().getAmount() == 0,
                    "the battery gave " + back.getEnergyStorage().getAmount() + " out of its back");
            context.assertTrue(bank.getEnergy() < 50_000, "the battery did not drain");
            context.assertTrue(context.getBlockState(at).get(BatteryBankBlock.LEVEL) == 1,
                    "a quarter-full battery shows " + context.getBlockState(at).get(BatteryBankBlock.LEVEL) + " bars");
            context.complete();
        });
    }

    @GameTest(maxTicks = 120)
    public void batteryFillsFromAGenerator(TestContext context) {
        coalGenerator(context, new BlockPos(3, 1, 1), 4);
        cable(context, new BlockPos(3, 1, 2));
        BlockPos at = new BlockPos(3, 1, 3);
        BatteryBankBlockEntity bank = battery(context, at, Direction.SOUTH, 0);
        context.runAtTick(100, () -> {
            context.assertTrue(bank.getEnergy() > 0, "the battery took nothing from the generator");
            context.assertTrue(context.getBlockState(at).get(BatteryBankBlock.LEVEL) >= 1, "no bar lit on a charged battery");
            context.complete();
        });
    }

    @GameTest(maxTicks = 40)
    public void chargingPostChargesAnElectricCar(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlockState(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.getBlockEntity(at, ChargingPostBlockEntity.class);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), true);
        context.runAtTick(20, () -> {
            context.assertTrue(car.isElectric(), "the test car is not electric");
            context.assertTrue(car.getFuel() > 0.0F, "the electric car was not charged");
            long used = ChargingPostBlockEntity.CAPACITY - post.getEnergy();
            float expected = used / (float) CarEntity.ENERGY_PER_CHARGE_UNIT;
            context.assertTrue(Math.abs(car.getFuel() - expected) < 0.01F,
                    "charge is not conserved: post gave " + used + " E, car got " + car.getFuel());
            context.assertTrue(context.getBlockState(at).get(ChargingPostBlock.LIT), "the post screen is dark while charging");
            context.complete();
        });
    }

    @GameTest(maxTicks = 40)
    public void chargingPostIgnoresAPetrolCar(TestContext context) {
        BlockPos at = new BlockPos(3, 1, 3);
        context.setBlockState(at, ModBlocks.CHARGING_POST);
        ChargingPostBlockEntity post = context.getBlockEntity(at, ChargingPostBlockEntity.class);
        post.setEnergy(ChargingPostBlockEntity.CAPACITY);
        CarEntity car = car(context, new BlockPos(3, 1, 5), false);
        context.runAtTick(20, () -> {
            context.assertTrue(car.getFuel() == 0.0F, "a petrol car took charge: " + car.getFuel());
            context.assertTrue(post.getEnergy() == ChargingPostBlockEntity.CAPACITY, "the post spent energy on nothing");
            context.assertTrue(car.charge(10.0F) == 0.0F, "charge() filled a petrol tank");
            context.complete();
        });
    }

    // ---- the mechanic's side: mileage and the code reader ----

    @GameTest(maxTicks = 20)
    public void codeReaderListsWorstFirst(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        car.setCondition(CarPart.BRAKE_PADS, CarPart.MAX_CONDITION * 0.10F);
        car.setCondition(CarPart.WHEELS, CarPart.MAX_CONDITION * 0.30F);
        car.setCondition(CarPart.AIR_FILTER, CarPart.MAX_CONDITION * 0.45F);
        car.setCondition(CarPart.EXHAUST, 0.0F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0420", "C0040", "C0750", "P0101")),
                "the reader listed " + codes);
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void codeReaderSkipsPlugsOnAnElectricCar(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), true);
        car.setCondition(CarPart.SPARK_PLUGS, CarPart.MAX_CONDITION * 0.05F);
        car.setCondition(CarPart.ENGINE, CarPart.MAX_CONDITION * 0.20F);
        var codes = FaultCodes.read(car).stream().map(FaultCodes.Fault::code).toList();
        context.assertTrue(codes.equals(java.util.List.of("P0A1A")), "an electric car read " + codes);
        context.complete();
    }

    @GameTest(maxTicks = 20)
    public void oldCarsAndTiredPartsWearFaster(TestContext context) {
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
        context.complete();
    }

    @GameTest(maxTicks = 40)
    public void cableGlowsOnlyWhileCarrying(TestContext context) {
        cable(context, new BlockPos(2, 1, 3), new BlockPos(3, 1, 3), new BlockPos(5, 1, 3));
        context.getBlockEntity(new BlockPos(2, 1, 3), EnergyCableBlockEntity.class).setEnergy(800);
        context.runAtTick(25, () -> {
            context.assertTrue(context.getBlockState(new BlockPos(3, 1, 3)).get(EnergyCableBlock.POWERED),
                    "a cable carrying power is dark");
            context.assertTrue(!context.getBlockState(new BlockPos(5, 1, 3)).get(EnergyCableBlock.POWERED),
                    "a cable on its own glows");
            context.complete();
        });
    }

    @GameTest
    public void rawFuelMixesByVolumeAndShowsOnTheReader(TestContext context) {
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
        context.complete();
    }

    @GameTest
    public void abandonedCarsNeedWork(TestContext context) {
        CarEntity car = me.trossty.car.world.AbandonedVehicles.abandoned(context.getWorld(),
                net.minecraft.util.math.random.Random.create(42));
        context.assertTrue(car != null, "no abandoned car was made");
        context.assertTrue(car.getHealth() < CarEntity.MAX_HEALTH * 0.75F, "the body is like new");
        context.assertTrue(car.getOdometer() >= CarEntity.HIGH_MILEAGE, "it has barely been driven");
        for (CarPart part : CarPart.values()) {
            context.assertTrue(car.getConditionFraction(part) < 0.65F, part.name() + " is like new");
        }
        context.assertTrue(car.isElectric() || car.getDirtyFuel() >= 0.6F, "the fuel in it is fresh");
        context.assertTrue(!me.trossty.car.entity.FaultCodes.read(car).isEmpty(), "the reader finds nothing");
        car.discard();
        context.complete();
    }

    @GameTest
    public void asphaltSparesTheTyres(TestContext context) {
        context.setBlockState(new BlockPos(3, 0, 3), ModBlocks.ASPHALT);
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        CarEntity off = car(context, new BlockPos(6, 1, 3), false);
        context.assertTrue(car.onAsphalt(), "a car on asphalt is not on asphalt");
        context.assertTrue(!off.onAsphalt(), "a car off the road is on asphalt");
        context.complete();
    }

    @GameTest
    public void electricMotorItemSaves(TestContext context) {
        var codec = me.trossty.car.registry.ModComponents.PART_VARIANT.getCodec();
        context.assertTrue(codec != null && codec.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,
                        CarSetup.ELECTRIC_MOTOR).isSuccess(),
                "an electric motor's part variant cannot be saved");
        context.complete();
    }

    /** Clicks the top of this block with whatever the player holds, as a right click would. */
    private static net.minecraft.util.ActionResult clickTop(TestContext context,
                                                            net.minecraft.entity.player.PlayerEntity player,
                                                            BlockPos relative) {
        BlockPos at = context.getAbsolutePos(relative);
        var hit = new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(at).add(0.0, 0.5, 0.0),
                Direction.UP, at, false);
        ItemStack held = player.getMainHandStack();
        if (held.getItem() instanceof net.minecraft.item.BlockItem block) {
            return block.place(new net.minecraft.item.ItemPlacementContext(player, net.minecraft.util.Hand.MAIN_HAND, held, hit));
        }
        return held.useOnBlock(new net.minecraft.item.ItemUsageContext(player, net.minecraft.util.Hand.MAIN_HAND, hit));
    }

    @GameTest
    public void asphaltIsLaidInLayersLikeSnow(TestContext context) {
        BlockPos ground = new BlockPos(2, 0, 2);
        BlockPos road = ground.up();
        context.setBlockState(ground, net.minecraft.block.Blocks.STONE);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(ModBlocks.ASPHALT, 16));

        clickTop(context, player, ground);
        context.assertTrue(context.getBlockState(road).get(me.trossty.car.block.AsphaltBlock.LAYERS) == 1,
                "the first asphalt is not one layer");
        for (int i = 0; i < 3; i++) {
            clickTop(context, player, road);
        }
        BlockState four = context.getBlockState(road);
        context.assertTrue(four.get(me.trossty.car.block.AsphaltBlock.LAYERS) == 4, "four clicks are not four layers");
        double top = four.getCollisionShape(context.getWorld(), context.getAbsolutePos(road)).getMax(Direction.Axis.Y);
        context.assertTrue(Math.abs(top - 0.5) < 1.0E-6, "four layers do not stand half a block high: " + top);

        // A painted road keeps its paint when it is built up.
        context.setBlockState(road, four.with(me.trossty.car.block.AsphaltBlock.MARKING,
                me.trossty.car.block.AsphaltBlock.Marking.SOLID));
        for (int i = 0; i < 5; i++) {
            clickTop(context, player, road);
        }
        BlockState full = context.getBlockState(road);
        context.assertTrue(full.get(me.trossty.car.block.AsphaltBlock.LAYERS) == 8, "eight layers are not a whole block");
        context.assertTrue(full.get(me.trossty.car.block.AsphaltBlock.MARKING) == me.trossty.car.block.AsphaltBlock.Marking.SOLID,
                "building the road up scrubbed its paint off");
        context.assertTrue(context.getBlockState(road.up()).isOf(ModBlocks.ASPHALT)
                        && context.getBlockState(road.up()).get(me.trossty.car.block.AsphaltBlock.LAYERS) == 1,
                "the ninth layer did not start a new block on top");
        context.complete();
    }

    @GameTest
    public void roadPaintPaintsTurnsAndErases(TestContext context) {
        BlockPos road = new BlockPos(2, 1, 2);
        context.setBlockState(road.down(), net.minecraft.block.Blocks.STONE);
        context.setBlockState(road, ModBlocks.ASPHALT);
        context.setBlockState(new BlockPos(4, 1, 2), net.minecraft.block.Blocks.STONE);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        player.setYaw(180.0F);
        ItemStack tin = new ItemStack(me.trossty.car.registry.ModItems.YELLOW_ROAD_PAINT);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, tin);
        var marking = me.trossty.car.block.AsphaltBlock.MARKING;
        var facing = me.trossty.car.block.AsphaltBlock.FACING;

        clickTop(context, player, road);
        BlockState painted = context.getBlockState(road);
        context.assertTrue(painted.get(marking) == me.trossty.car.block.AsphaltBlock.Marking.DASHED
                        && painted.get(me.trossty.car.block.AsphaltBlock.PAINT) == me.trossty.car.block.AsphaltBlock.Paint.YELLOW
                        && painted.get(facing) == Direction.NORTH,
                "a fresh tin did not paint a yellow dashed line running north: " + painted);
        context.assertTrue(tin.getDamage() == 1, "painting took no paint");

        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road).get(facing) == Direction.EAST,
                "painting the same marking again did not turn it");
        context.assertTrue(tin.getDamage() == 1, "turning a marking took more paint");

        context.assertTrue(clickTop(context, player, new BlockPos(4, 0, 2)) == net.minecraft.util.ActionResult.PASS,
                "road paint went on bare stone");

        me.trossty.car.item.RoadPaintItem.select(tin, me.trossty.car.block.AsphaltBlock.Marking.NONE);
        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road).get(marking) == me.trossty.car.block.AsphaltBlock.Marking.NONE,
                "erase left the marking on");

        // The menu's choice, as the server receives it; a bad index changes nothing.
        me.trossty.car.item.RoadPaintItem.choose(player, net.minecraft.util.Hand.MAIN_HAND,
                me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK.ordinal());
        context.assertTrue(me.trossty.car.item.RoadPaintItem.selected(tin) == me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK,
                "the menu's choice was not taken");
        me.trossty.car.item.RoadPaintItem.choose(player, net.minecraft.util.Hand.MAIN_HAND, 99);
        context.assertTrue(me.trossty.car.item.RoadPaintItem.selected(tin) == me.trossty.car.block.AsphaltBlock.Marking.CROSSWALK,
                "a bad index from the menu changed the marking");
        // Sneaking on road opens the menu (client side) and paints nothing.
        BlockState before = context.getBlockState(road);
        player.setSneaking(true);
        clickTop(context, player, road);
        context.assertTrue(context.getBlockState(road) == before, "sneaking painted instead of opening the menu");
        context.complete();
    }

    @GameTest
    public void obdTabletPairsAndLosesSignal(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        BlockPos stand = context.getAbsolutePos(new BlockPos(1, 1, 1));
        player.refreshPositionAndAngles(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.0F, 0.0F);
        ItemStack tablet = new ItemStack(me.trossty.car.registry.ModItems.OBD_SCANNER);
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == null,
                "an unpaired tablet reads a car");
        me.trossty.car.item.ObdScannerItem.pair(tablet, car, player);
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == car,
                "a paired tablet does not read its car");
        car.setPosition(car.getX() + 2.0 * me.trossty.car.item.ObdScannerItem.RANGE, car.getY(), car.getZ());
        context.assertTrue(me.trossty.car.item.ObdScannerItem.reading(tablet, player) == null,
                "a tablet still reads a car out of range");
        car.discard();
        context.complete();
    }

    // ---- servicing by hand ----

    /**
     * Every vehicle gets its own map of where its parts are: the tyre box on
     * its own front wheel, nothing hanging off the body, nothing hidden inside
     * a bigger box where it could never be aimed at, and a lorry's engine is
     * not where a hatchback's is.
     */
    @GameTest
    public void partBoxesFitEachVehicle(TestContext context) {
        var registry = context.getWorld().getRegistryManager()
                .getOrThrow(me.trossty.car.vehicle.VehicleDefinition.KEY);
        java.util.Map<String, CarPart.Anchor> engines = new java.util.HashMap<>();
        int checked = 0;
        for (var entry : registry.getIndexedEntries()) {
            me.trossty.car.vehicle.VehicleDefinition definition = entry.value();
            String name = String.valueOf(registry.getId(definition));
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
            for (CarPart part : CarPart.values()) {
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
            engines.put(registry.getId(definition).getPath(), map.get(CarPart.ENGINE));
        }
        context.assertTrue(checked >= 5, "only " + checked + " vehicles had a part map");
        CarPart.Anchor hatch = engines.get("toyota_corolla");
        CarPart.Anchor lorry = engines.get("mercedes_atego");
        context.assertTrue(hatch != null && lorry != null && !hatch.equals(lorry),
                "a hatchback and a lorry have their engines in the same place");
        context.complete();
    }

    /**
     * A part that has come off stays off: clicking the empty slot again with
     * nothing in hand does nothing — not even in creative, where it used to
     * conjure a new one — and the part did not land in the hand you were
     * working with. Holding the spare and clicking puts it back.
     */
    @GameTest
    public void removedPartStaysOffUntilRefitted(TestContext context) {
        BlockPos lift = new BlockPos(2, 1, 2);
        context.setBlockState(lift, ModBlocks.VEHICLE_LIFT.getDefaultState()
                .with(me.trossty.car.block.VehicleLiftBlock.LEVEL, 2));
        CarEntity car = context.spawnEntity(ModEntities.CAR, lift.up());
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        CarPart part = CarPart.AIR_FILTER;
        context.assertTrue(car.isFitted(part), "a new car has no air filter");

        car.serviceByHand(player, part);
        context.assertFalse(car.isFitted(part), "the air filter did not come off");
        context.assertTrue(player.getMainHandStack().isEmpty(), "the air filter landed in the working hand");
        context.assertTrue(player.getInventory().count(part.item()) == 1, "the air filter was not handed over");

        car.serviceByHand(player, part);
        context.assertFalse(car.isFitted(part), "a second click with an empty hand put it back");
        var creative = context.createMockPlayer(net.minecraft.world.GameMode.CREATIVE);
        car.serviceByHand(creative, part);
        context.assertFalse(car.isFitted(part), "creative conjured an air filter into the empty slot");

        int pocket = -1;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getStack(slot).isOf(part.item())) {
                pocket = slot;
            }
        }
        ItemStack spare = player.getInventory().removeStack(pocket);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, spare);
        car.serviceByHand(player, part);
        context.assertTrue(car.isFitted(part), "holding the spare and clicking did not fit it");
        context.assertTrue(player.getMainHandStack().isEmpty(), "fitting it did not use the spare up");
        car.discard();
        context.complete();
    }

    // ---- tanks ----

    /**
     * Tanks against each other are one tank: liquid poured in at the top
     * fills the bottom cell first, every face hands out the whole of it, and
     * any liquid goes — water, which is no fuel at all.
     */
    @GameTest
    public void tanksMergeAndFillFromTheBottom(TestContext context) {
        BlockPos low = new BlockPos(2, 1, 2);
        BlockPos high = low.up();
        BlockPos side = low.east();
        context.setBlockState(low, ModBlocks.FUEL_TANK);
        context.setBlockState(high, ModBlocks.FUEL_TANK);
        context.setBlockState(side, ModBlocks.FUEL_TANK);
        context.runAtTick(2, () -> {
            context.assertTrue(context.getBlockState(low).get(me.trossty.car.block.FuelTankBlock.UP),
                    "the lower cell does not show itself joined to the one above");
            var door = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.find(context.getWorld(),
                    context.getAbsolutePos(high), Direction.UP);
            context.assertTrue(door != null, "the tank has no fluid door");
            long bucket = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
            var water = net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(net.minecraft.fluid.Fluids.WATER);
            long in;
            try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                in = door.insert(water, 40 * bucket, t);
                t.commit();
            }
            context.assertTrue(in == 40 * bucket, "three cells did not take forty buckets: " + in / bucket);
            context.assertTrue(context.getBlockEntity(low, FuelTankBlockEntity.class).getGroup().getCapacity() == 48 * bucket, "three cells are not one tank of 48 buckets");
            FuelTankBlockEntity bottom = context.getBlockEntity(low, FuelTankBlockEntity.class);
            FuelTankBlockEntity beside = context.getBlockEntity(side, FuelTankBlockEntity.class);
            FuelTankBlockEntity top = context.getBlockEntity(high, FuelTankBlockEntity.class);
            context.assertTrue(bottom.getAmount() == 16 * bucket && beside.getAmount() == 16 * bucket,
                    "the bottom layer did not fill first");
            context.assertTrue(top.getAmount() == 8 * bucket, "the top cell holds " + top.getAmount() / bucket);
            // Lava will not go into a tank of water.
            try (var t = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long lava = door.insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(
                        net.minecraft.fluid.Fluids.LAVA), bucket, t);
                context.assertTrue(lava == 0, "lava mixed into the water");
            }
            context.complete();
        });
    }

    /** Breaking a cell out of a tank loses that cell's share, and the rest is still one tank. */
    @GameTest
    public void brokenTankCellSplitsTheTank(TestContext context) {
        BlockPos a = new BlockPos(1, 1, 2);
        BlockPos b = a.east();
        BlockPos c = b.east();
        context.setBlockState(a, ModBlocks.FUEL_TANK);
        context.setBlockState(b, ModBlocks.FUEL_TANK);
        context.setBlockState(c, ModBlocks.FUEL_TANK);
        context.runAtTick(2, () -> {
            FuelTankBlockEntity first = context.getBlockEntity(a, FuelTankBlockEntity.class);
            context.assertTrue(first.fill(ModFluids.DIESEL, 30000) == 30000, "the row did not take 30 buckets");
            context.setBlockState(b, Block.getBlockFromItem(Items.AIR).getDefaultState());
        });
        context.runAtTick(4, () -> {
            FuelTankBlockEntity first = context.getBlockEntity(a, FuelTankBlockEntity.class);
            FuelTankBlockEntity last = context.getBlockEntity(c, FuelTankBlockEntity.class);
            context.assertTrue(first.getGroup().size() == 1 && last.getGroup().size() == 1,
                    "the two ends are still one tank");
            context.assertTrue(first.getGroup().getAmount() + last.getGroup().getAmount() == 20
                            * net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET,
                    "the ends do not hold the 20 buckets that were in them");
            context.complete();
        });
    }

    /** A trailer has no engine: nothing about one can be worked on, read or worn. */
    @GameTest
    public void trailerHasNoEngine(TestContext context) {
        CarEntity trailer = context.spawnEntity(ModEntities.TRAILER, new BlockPos(2, 1, 2));
        context.assertFalse(trailer.hasPart(CarPart.ENGINE), "a trailer has an engine");
        context.assertFalse(trailer.canReach(CarPart.ENGINE), "a trailer's engine can be reached");
        context.assertTrue(trailer.hasPart(CarPart.WHEELS), "a trailer has no wheels");
        trailer.discard();
        context.complete();
    }

    // ---- a car once put down stays a car ----

    private static int itemsAround(TestContext context, CarEntity car, net.minecraft.item.Item item) {
        return context.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                car.getBoundingBox().expand(4.0), drop -> drop.getStack().isOf(item)).size();
    }

    /** A survival player's punch dents a car; it does not turn it back into an item. */
    @GameTest
    public void punchDentsInsteadOfPocketing(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        float before = car.getHealth();
        car.damage(context.getWorld(), context.getWorld().getDamageSources().playerAttack(player), 2.0F);
        context.assertFalse(car.isRemoved(), "a punch took the car away");
        context.assertTrue(car.getHealth() < before, "a punch did not dent the car");
        context.assertTrue(itemsAround(context, car, me.trossty.car.registry.ModItems.CAR) == 0,
                "a punch dropped the car as an item");
        car.discard();
        context.complete();
    }

    /** A creative player clears a car away, and nothing comes back from it. */
    @GameTest
    public void creativeClearsWithoutAnItem(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.CREATIVE);
        car.damage(context.getWorld(), context.getWorld().getDamageSources().playerAttack(player), 1.0F);
        context.assertTrue(car.isRemoved(), "a creative hit left the car standing");
        context.assertTrue(itemsAround(context, car, me.trossty.car.registry.ModItems.CAR) == 0,
                "a creative hit dropped the car as an item");
        context.complete();
    }

    /** A locked car keeps its parts, however the request to take one arrives. */
    @GameTest
    public void lockedCarRefusesService(TestContext context) {
        CarEntity car = car(context, new BlockPos(3, 1, 3), false);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        car.setLockCode(77L);
        ItemStack key = new ItemStack(me.trossty.car.registry.ModItems.CAR_KEY);
        key.set(me.trossty.car.registry.ModComponents.VEHICLE_KEY, 77L);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, key);
        player.setSneaking(true);
        car.interact(player, net.minecraft.util.Hand.MAIN_HAND);
        context.assertTrue(car.isLocked(), "the key did not lock the car");
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
        player.setSneaking(false);
        for (CarPart part : CarPart.values()) {
            if (car.hasPart(part)) {
                car.serviceByHand(player, part);
                context.assertTrue(car.hasPart(part), "a locked car gave up its " + part);
            }
        }
        car.discard();
        context.complete();
    }

    /** A wrecked pickup tips its bed out, the way a broken chest does. */
    @GameTest
    public void wreckedPickupSpillsItsBed(TestContext context) {
        var pickup = context.spawnEntity(ModEntities.PICKUP, new BlockPos(3, 1, 3));
        pickup.getBed().setStack(0, new ItemStack(Items.DIAMOND, 5));
        pickup.damage(context.getWorld(), context.getWorld().getDamageSources().generic(), 1000.0F);
        context.assertTrue(pickup.isRemoved(), "the pickup was not wrecked");
        context.assertTrue(itemsAround(context, pickup, Items.DIAMOND) > 0, "the bed did not spill");
        context.assertTrue(itemsAround(context, pickup, me.trossty.car.registry.ModItems.CAR) == 0,
                "a wreck dropped the vehicle as an item");
        context.complete();
    }

    /**
     * A hitched trailer is dragged, not teleported: towed at a wall, it stops
     * against it instead of passing through.
     */
    @GameTest(maxTicks = 60)
    public void towedTrailerStopsAtAWall(TestContext context) {
        CarEntity tractor = car(context, new BlockPos(3, 1, 5), false);
        var trailer = context.spawnEntity(ModEntities.TRAILER, new BlockPos(3, 1, 1));
        net.minecraft.util.math.Vec3d ball = tractor.hitchPoint();
        // Coupler right on the tow ball, both facing the same way.
        tractor.refreshPositionAndAngles(tractor.getX(), tractor.getY(), tractor.getZ(), 0.0F, 0.0F);
        trailer.refreshPositionAndAngles(ball.x, trailer.getY(), ball.z - 2.0, 0.0F, 0.0F);
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, ItemStack.EMPTY);
        trailer.interact(player, net.minecraft.util.Hand.MAIN_HAND);
        context.assertTrue(trailer.isHitched(), "the trailer did not hitch");
        double startZ = trailer.getZ();
        // A wall a block ahead of the trailer's front, across its whole width.
        int wallZ = net.minecraft.util.math.MathHelper.floor(trailer.getBoundingBox().maxZ) + 1;
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                context.getWorld().setBlockState(new BlockPos(
                        net.minecraft.util.math.MathHelper.floor(trailer.getX()) + x,
                        net.minecraft.util.math.MathHelper.floor(trailer.getY()) + y, wallZ),
                        net.minecraft.block.Blocks.STONE.getDefaultState());
            }
        }
        context.runAtTick(2, () -> tractor.setPosition(tractor.getX(), tractor.getY(), tractor.getZ() + 3.0));
        context.runAtTick(6, () -> {
            context.assertTrue(trailer.getZ() > startZ, "the trailer did not move towards the tractor");
            context.assertTrue(trailer.getBoundingBox().maxZ <= wallZ + 1.0E-3,
                    "the trailer went into the wall: front at " + trailer.getBoundingBox().maxZ + ", wall at " + wallZ);
            trailer.discard();
            tractor.discard();
            context.complete();
        });
    }

    // ---- the drum ----

    private static void clickDrum(TestContext context, net.minecraft.entity.player.PlayerEntity player,
                                  BlockPos relative) {
        BlockPos at = context.getAbsolutePos(relative);
        var hit = new net.minecraft.util.hit.BlockHitResult(net.minecraft.util.math.Vec3d.ofCenter(at),
                Direction.NORTH, at, false);
        BlockState state = context.getWorld().getBlockState(at);
        ItemStack held = player.getMainHandStack();
        if (state.onUseWithItem(held, context.getWorld(), player, net.minecraft.util.Hand.MAIN_HAND, hit)
                == net.minecraft.util.ActionResult.PASS) {
            state.onUse(context.getWorld(), player, hit);
        }
    }

    /**
     * A drum takes a bucket and a jerry can, refuses a second liquid, reads on
     * a comparator, and is carried away full: broken and put down again it
     * holds what it held.
     */
    @GameTest
    public void drumFillsAndTravelsFull(TestContext context) {
        var player = context.createMockPlayer(net.minecraft.world.GameMode.SURVIVAL);
        BlockPos water = new BlockPos(1, 1, 1);
        BlockPos petrol = new BlockPos(3, 1, 1);
        context.setBlockState(water, me.trossty.car.registry.ModBlocks.FUEL_DRUM);
        context.setBlockState(petrol, me.trossty.car.registry.ModBlocks.FUEL_DRUM);
        var wet = context.getBlockEntity(water, me.trossty.car.block.entity.FuelDrumBlockEntity.class);
        var fuel = context.getBlockEntity(petrol, me.trossty.car.block.entity.FuelDrumBlockEntity.class);

        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        clickDrum(context, player, water);
        context.assertTrue(wet.getAmount() == net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET
                && wet.getFluid().isOf(net.minecraft.fluid.Fluids.WATER), "a bucket of water did not go in");
        context.assertTrue(player.getMainHandStack().isOf(Items.BUCKET), "the bucket did not come back empty");

        ItemStack can = new ItemStack(me.trossty.car.registry.ModItems.JERRY_CAN);
        CarEntity.markFuel(can, CarSetup.Fuel.PETROL);
        me.trossty.car.item.JerryCans.setUnits(can, me.trossty.car.item.JerryCans.CAPACITY);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, can);
        clickDrum(context, player, water);
        context.assertTrue(wet.getFluid().isOf(net.minecraft.fluid.Fluids.WATER)
                        && me.trossty.car.item.JerryCans.isFull(can), "petrol went into a drum of water");
        clickDrum(context, player, petrol);
        long poured = fuel.getAmount();
        context.assertTrue(poured > 0 && fuel.getFluid().isOf(ModFluids.PETROL), "the can did not pour petrol in");
        context.assertTrue(context.getBlockState(petrol).getComparatorOutput(context.getWorld(),
                context.getAbsolutePos(petrol), Direction.NORTH) > 0, "a comparator reads an empty drum");

        context.getWorld().breakBlock(context.getAbsolutePos(petrol), true);
        var drops = context.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                new net.minecraft.util.math.Box(context.getAbsolutePos(petrol)).expand(2.0),
                drop -> drop.getStack().isOf(me.trossty.car.registry.ModBlocks.FUEL_DRUM.asItem()));
        context.assertTrue(drops.size() == 1, "breaking the drum dropped " + drops.size() + " drums");
        ItemStack carried = drops.get(0).getStack().copy();
        drops.get(0).discard();
        var held = carried.get(me.trossty.car.registry.ModComponents.STORED_FLUID);
        context.assertTrue(held != null && held.amount() == poured && held.fluid().isOf(ModFluids.PETROL),
                "the drum item does not carry the petrol");

        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, carried);
        context.setBlockState(new BlockPos(5, 0, 3), net.minecraft.block.Blocks.STONE);
        clickTop(context, player, new BlockPos(5, 0, 3));
        var again = context.getBlockEntity(new BlockPos(5, 1, 3),
                me.trossty.car.block.entity.FuelDrumBlockEntity.class);
        context.assertTrue(again.getAmount() == poured && again.getFluid().isOf(ModFluids.PETROL),
                "put down again, the drum holds " + again.getAmount() + " of " + poured);
        context.complete();
    }
}
