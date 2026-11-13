package me.trossty.car.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.trossty.car.CarMain;
import me.trossty.car.client.hud.DashboardHud;
import me.trossty.car.client.hud.ServiceHud;
import me.trossty.car.client.render.CarEntityModel;
import me.trossty.car.client.render.CarEntityRenderer;
import me.trossty.car.client.render.CarServiceOverlay;
import me.trossty.car.client.render.FuelHoseRenderer;
import me.trossty.car.client.render.FuelPumpRenderer;
import me.trossty.car.client.render.FuelTankRenderer;
import me.trossty.car.client.render.LiquidColours;
import me.trossty.car.client.render.ModEntityModelLayers;
import me.trossty.car.client.render.PickupEntityRenderer;
import me.trossty.car.client.render.PumpjackRenderer;
import me.trossty.car.client.render.TankerEntityRenderer;
import me.trossty.car.client.render.WorldLabels;
import me.trossty.car.client.render.WorldRenderContext;
import me.trossty.car.client.render.item.ItemLooks;
import me.trossty.car.client.screen.CarServiceScreen;
import me.trossty.car.client.screen.FuelRefineryScreen;
import me.trossty.car.client.screen.MachineScreen;
import me.trossty.car.client.screen.RoadMarkingScreen;
import me.trossty.car.client.screen.VehicleWorkbenchScreen;
import me.trossty.car.client.screen.VehicleWorkstationScreen;
import me.trossty.car.client.sound.CarEngineSoundInstance;
import me.trossty.car.block.AsphaltBlock;
import me.trossty.car.block.entity.BatteryBankBlockEntity;
import me.trossty.car.block.entity.FuelDrumBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.item.RoadPaintItem;
import me.trossty.car.network.CarNetworking;
import me.trossty.car.network.CrashC2SPayload;
import me.trossty.car.network.HornC2SPayload;
import me.trossty.car.network.ServicePartC2SPayload;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModBlocks;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModScreenHandlers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@Mod(value = CarMain.MOD_ID, dist = Dist.CLIENT)
public class CarMainClient {

    /** The brake pedal as the server last heard it from this client. */
    private static boolean brakeSent;

    public static final KeyMapping HORN_KEY = new KeyMapping(
            "key.car.horn",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.misc");

    /** Amber, the colour of the stuff in the jerry can's sight glass. */
    private static final int PETROL_TINT = 0xD9A334;

    public CarMainClient(IEventBus modBus) {
        // A road paint tin used in the air opens its marking menu.
        RoadPaintItem.openMenu = hand -> Minecraft.getInstance().setScreen(new RoadMarkingScreen(hand));
        // The driving client is the only side that sees its car hit something,
        // so give the common code a way to pass that on to the server.
        CarNetworking.setCrashReporter(severity -> PacketDistributor.sendToServer(new CrashC2SPayload(severity)));

        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(HORN_KEY));
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            // Road markings are painted on with holes in them; an oil seep is a
            // ragged puddle; a drum's paint has gaps. A tank's and a pump's
            // glass is see-through, and the liquid behind it is drawn by the block entity.
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.ASPHALT, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FUEL_DRUM, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.OIL_SEEP, RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FUEL_TANK, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FUEL_PUMP, RenderType.translucent());
            ItemLooks.registerProperties();
        }));
        modBus.addListener((RegisterColorHandlersEvent.Block event) -> {
            // Markings are tinted the colour of the paint.
            event.register((state, world, pos, tint) -> tint == 0
                    ? 0xFF000000 | state.getValue(AsphaltBlock.PAINT).rgb
                    : -1, ModBlocks.ASPHALT);
            // A drum is painted the colour of what is in it.
            event.register((state, world, pos, tint) -> {
                if (tint != 0) {
                    return -1;
                }
                Fluid liquid = world != null && pos != null
                        && world.getBlockEntity(pos) instanceof FuelDrumBlockEntity drum ? (Fluid) drum.getRenderData() : null;
                return 0xFF000000 | LiquidColours.of(liquid, ItemLooks.EMPTY_DRUM);
            }, ModBlocks.FUEL_DRUM);
        });
        modBus.addListener(ItemLooks::registerColors);
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(ModEntityModelLayers.CAR, CarEntityModel::getTexturedModelData);
            event.registerLayerDefinition(ModEntityModelLayers.FUEL_TANKER, CarEntityModel::getTankerModelData);
            event.registerLayerDefinition(ModEntityModelLayers.PICKUP, CarEntityModel::getPickupModelData);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> {
            event.registerEntityRenderer(ModEntities.CAR, CarEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.FUEL_TANKER, TankerEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.PICKUP, PickupEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.TRAILER, CarEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.MOTORCYCLE, CarEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.MOTORBOAT, CarEntityRenderer::new);
            event.registerEntityRenderer(ModEntities.AIRPLANE, CarEntityRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.FUEL_TANK, FuelTankRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.PUMPJACK, PumpjackRenderer::new);
            event.registerBlockEntityRenderer(ModBlockEntities.FUEL_PUMP, FuelPumpRenderer::new);
        });
        modBus.addListener((RegisterMenuScreensEvent event) -> {
            event.register(ModScreenHandlers.VEHICLE_WORKBENCH, VehicleWorkbenchScreen::new);
            event.register(ModScreenHandlers.FUEL_REFINERY, FuelRefineryScreen::new);
            event.register(ModScreenHandlers.MACHINE, MachineScreen::new);
            event.register(ModScreenHandlers.FUELLED_MACHINE, MachineScreen::new);
            event.register(ModScreenHandlers.VEHICLE_WORKSTATION, VehicleWorkstationScreen::new);
            event.register(ModScreenHandlers.CAR_SERVICE, CarServiceScreen::new);
        });
        modBus.addListener((RegisterGuiLayersEvent event) -> {
            event.registerAbove(VanillaGuiLayers.HOTBAR, CarMain.id("dashboard"), new DashboardHud());
            event.registerAbove(VanillaGuiLayers.HOTBAR, CarMain.id("service"), new ServiceHud());
        });
        // These never stand in the world, but the tanker's sight glass draws
        // them, and so will another mod's tank or pipe window: without a look
        // of their own they would come out as the missing-texture chequerboard.
        modBus.addListener((RegisterClientExtensionsEvent event) -> {
            tankOnlyLook(event, ModFluids.PETROL, PETROL_TINT);
            tankOnlyLook(event, ModFluids.DIESEL, 0x9A7B2C);
            // Milk on the water texture comes out grey, the water being dark;
            // snow is the whitest thing in the block atlas.
            ResourceLocation snow = ResourceLocation.withDefaultNamespace("block/snow");
            look(event, ModFluids.MILK, snow, snow, 0xFFFBF0);
            tankOnlyLook(event, ModFluids.HONEY, 0xF0A21E);
            tankOnlyLook(event, ModFluids.CRUDE_OIL, 0x1E1A16);
            tankOnlyLook(event, ModFluids.RAW_PETROL, 0xB0702A);
            tankOnlyLook(event, ModFluids.RAW_DIESEL, 0x4E4224);
            tankOnlyLook(event, ModFluids.BIOFUEL, 0x9CC23A);
        });

        // A battery bank picked up with charge in it says how much.
        NeoForge.EVENT_BUS.addListener((ItemTooltipEvent event) -> {
            Long stored = event.getItemStack().get(ModComponents.STORED_ENERGY);
            if (stored != null) {
                event.getToolTip().add(Component.translatable("tooltip.car.stored_energy",
                                String.format(Locale.ROOT, "%,d", stored),
                                String.format(Locale.ROOT, "%,d", BatteryBankBlockEntity.CAPACITY))
                        .withStyle(ChatFormatting.AQUA));
            }
        });
        // The head lamps' light on the blocks and mobs in front.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) ->
                me.trossty.car.client.render.HeadlightGlow.tick(Minecraft.getInstance()));
        // A looping engine sound for each car that is running or rolling.
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            if (Minecraft.getInstance().level != null && !Minecraft.getInstance().isPaused()) {
                CarEngineSoundInstance.listen(Minecraft.getInstance().level);
            }
        });
        // Hands-on work. The parts of a car that is up on its ramp are drawn
        // where they really are, and this is also where the aiming is worked
        // out, because it has to follow the camera frame by frame.
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            Vec3 camera = event.getCamera().getPosition();
            WorldRenderContext context = new WorldRenderContext(event.getPoseStack(),
                    client.renderBuffers().bufferSource(), camera,
                    new Matrix4f(event.getModelViewMatrix()).mul(event.getPoseStack().last().pose()));
            WorldLabels.begin(context);
            CarServiceOverlay.render(context);
            FuelHoseRenderer.render(context);
        });
        // Right clicking a part takes it off or puts one on. The server has no
        // way of telling which part was meant — the car is one entity, and the
        // vanilla packet says only "that entity" — so the side that did the
        // aiming has to say which. A mark is often outside the car's own hit
        // box — a front wheel, the radiator — so the click is caught before it
        // is worked out what it landed on.
        NeoForge.EVENT_BUS.addListener(CarMainClient::servicePart);
        // Looked at from behind, an aircraft or a boat needs the camera further
        // back than a car does, or it sits on the tail with the view full of fin.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent event) -> {
            net.minecraft.world.entity.Entity viewer = event.getCamera().getEntity();
            if (viewer != null && viewer.getVehicle() instanceof me.trossty.car.entity.CarEntity vehicle) {
                float back = switch (vehicle.chassis()) {
                    case PLANE -> 11.0F;
                    case BOAT -> 7.0F;
                    default -> 0.0F;
                };
                event.setDistance(Math.max(event.getDistance(), back * event.getEntityScalingFactor()));
            }
        });
        // Behind the wheel the hands are on it, not hanging over the dials.
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.RenderHandEvent event) -> {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.getVehicle() instanceof me.trossty.car.entity.CarEntity car
                    && car.getControllingPassenger() == player && event.getItemStack().isEmpty()) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(CarMainClient::onClientTick);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || !(player.getVehicle() instanceof CarEntity car)) {
            // Drain any presses made outside a car so they do not fire on entry.
            while (HORN_KEY.consumeClick()) {
                // discard
            }
            return;
        }

        // The controlling client simulates the car, so feed it that player's keys.
        if (car.getControllingPassenger() == player) {
            car.setInputs(player.input.up, player.input.down, player.input.left, player.input.right, player.input.jumping);
            // Everybody else sees the brake lamps from the server, so tell it when the pedal moves.
            boolean pedal = car.brakePedal();
            if (pedal != brakeSent) {
                brakeSent = pedal;
                PacketDistributor.sendToServer(new me.trossty.car.network.BrakeC2SPayload(pedal));
            }
        }

        boolean honked = false;
        while (HORN_KEY.consumeClick()) {
            honked = true;
        }
        if (honked) {
            PacketDistributor.sendToServer(HornC2SPayload.INSTANCE);
        }
    }

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");

    private static void tankOnlyLook(RegisterClientExtensionsEvent event, Fluid fluid, int tint) {
        look(event, fluid, WATER_STILL, WATER_FLOWING, tint);
    }

    private static void look(RegisterClientExtensionsEvent event, Fluid fluid,
                             ResourceLocation still, ResourceLocation flowing, int tint) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }

            @Override
            public int getTintColor() {
                return 0xFF000000 | tint;
            }
        }, fluid.getFluidType());
    }

    /** A right click while a mark on a raised car is aimed at: that part, by hand. */
    private static void servicePart(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        CarEntity car = CarServiceOverlay.aimedCar();
        CarPart part = CarServiceOverlay.aimedPart();
        if (car == null || part == null) {
            return;
        }
        PacketDistributor.sendToServer(new ServicePartC2SPayload(car.getId(), part.key()));
        // Cancelled, the click goes no further: not to the car, not to the
        // block behind it, not to the item in hand.
        event.setSwingHand(true);
        event.setCanceled(true);
    }
}
