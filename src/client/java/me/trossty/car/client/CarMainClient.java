package me.trossty.car.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.trossty.car.CarMain;
import me.trossty.car.client.hud.DashboardHud;
import me.trossty.car.client.hud.ServiceHud;
import me.trossty.car.client.render.CarEntityModel;
import me.trossty.car.client.render.CarEntityRenderer;
import me.trossty.car.client.render.CarServiceOverlay;
import me.trossty.car.client.render.ModEntityModelLayers;
import me.trossty.car.client.render.PickupEntityRenderer;
import me.trossty.car.client.render.TankerEntityRenderer;
import me.trossty.car.client.render.WorldLabels;
import me.trossty.car.client.screen.FuelRefineryScreen;
import me.trossty.car.client.screen.VehicleWorkbenchScreen;
import me.trossty.car.client.sound.CarEngineSoundInstance;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.network.CarNetworking;
import me.trossty.car.network.CrashC2SPayload;
import me.trossty.car.network.HornC2SPayload;
import me.trossty.car.network.ServicePartC2SPayload;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.registry.ModFluids;
import me.trossty.car.registry.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

public class CarMainClient implements ClientModInitializer {

    public static final KeyMapping HORN_KEY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.car.horn",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            KeyMapping.Category.MISC));

    /** Bare steel: an empty drum's paint. The drum's item definition says the same. */
    private static final int EMPTY_DRUM = 0x9AA1A8;

    /** Amber, the colour of the stuff in the jerry can's sight glass. */
    private static final int PETROL_TINT = 0xD9A334;

    /** Whether the use key was down last tick, to tell a press from a hold. */

    @Override
    public void onInitializeClient() {
        // A road paint tin used in the air opens its marking menu.
        me.trossty.car.item.RoadPaintItem.openMenu = hand -> net.minecraft.client.Minecraft.getInstance()
                .setScreen(new me.trossty.car.client.screen.RoadMarkingScreen(hand));
        // Road markings are painted on with holes in them, and tinted the colour of the paint.
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                me.trossty.car.registry.ModBlocks.ASPHALT, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, world, pos, tint) -> tint == 0
                        ? 0xFF000000 | state.getValue(me.trossty.car.block.AsphaltBlock.PAINT).rgb
                        : -1,
                me.trossty.car.registry.ModBlocks.ASPHALT);
        // A battery bank picked up with charge in it says how much.
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            Long stored = stack.get(me.trossty.car.registry.ModComponents.STORED_ENERGY);
            if (stored != null) {
                lines.add(net.minecraft.network.chat.Component.translatable("tooltip.car.stored_energy",
                                String.format(java.util.Locale.ROOT, "%,d", stored),
                                String.format(java.util.Locale.ROOT, "%,d",
                                        me.trossty.car.block.entity.BatteryBankBlockEntity.CAPACITY))
                        .withStyle(net.minecraft.ChatFormatting.AQUA));
            }
        });
        // A drum is painted the colour of what is in it, standing or carried.
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                me.trossty.car.registry.ModBlocks.FUEL_DRUM, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.BLOCK.register(
                (state, world, pos, tint) -> {
                    if (tint != 0) {
                        return -1;
                    }
                    Object liquid = world == null || pos == null ? null : world.getBlockEntityRenderData(pos);
                    return 0xFF000000 | me.trossty.car.client.render.LiquidColours.of(
                            liquid instanceof net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant variant
                                    ? variant : null, EMPTY_DRUM);
                },
                me.trossty.car.registry.ModBlocks.FUEL_DRUM);
        me.trossty.car.mixin.client.TintSourceTypesAccessor.car$getIdMapper().put(
                me.trossty.car.CarMain.id("stored_fluid"),
                me.trossty.car.client.render.item.StoredFluidTintSource.CODEC);
        // Vehicle items wear their paint: see PaintTintSource.
        me.trossty.car.mixin.client.TintSourceTypesAccessor.car$getIdMapper().put(
                me.trossty.car.CarMain.id("paint"), me.trossty.car.client.render.item.PaintTintSource.CODEC);
        EntityModelLayerRegistry.registerModelLayer(
                ModEntityModelLayers.CAR, CarEntityModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.CAR, CarEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(
                ModEntityModelLayers.FUEL_TANKER, CarEntityModel::getTankerModelData);
        EntityRendererRegistry.register(ModEntities.FUEL_TANKER, TankerEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(
                ModEntityModelLayers.PICKUP, CarEntityModel::getPickupModelData);
        EntityRendererRegistry.register(ModEntities.PICKUP, PickupEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TRAILER, CarEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.MOTORCYCLE, CarEntityRenderer::new);

        // Give every car that comes into view its own looping engine sound.
        ClientEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof CarEntity car && !(car instanceof me.trossty.car.entity.TrailerEntity)) {
                Minecraft.getInstance()
                        .getSoundManager()
                        .queueTickingSound(new CarEngineSoundInstance(car));
            }
        });

        MenuScreens.register(ModScreenHandlers.VEHICLE_WORKBENCH, VehicleWorkbenchScreen::new);
        MenuScreens.register(ModScreenHandlers.FUEL_REFINERY, FuelRefineryScreen::new);
        MenuScreens.register(ModScreenHandlers.MACHINE, me.trossty.car.client.screen.MachineScreen::new);
        MenuScreens.register(ModScreenHandlers.FUELLED_MACHINE, me.trossty.car.client.screen.MachineScreen::new);
        MenuScreens.register(ModScreenHandlers.VEHICLE_WORKSTATION,
                me.trossty.car.client.screen.VehicleWorkstationScreen::new);
        MenuScreens.register(ModScreenHandlers.CAR_SERVICE,
                me.trossty.car.client.screen.CarServiceScreen::new);

        // These never stand in the world, but the tanker's sight glass draws
        // them, and so will another mod's tank or pipe window: without a look
        // of their own they would come out as the missing-texture chequerboard.
        tankOnlyLook(ModFluids.PETROL, PETROL_TINT);
        tankOnlyLook(ModFluids.DIESEL, 0x9A7B2C);
        // Milk on the water texture comes out grey, the water being dark;
        // snow is the whitest thing in the block atlas.
        net.minecraft.resources.Identifier snow = net.minecraft.resources.Identifier.withDefaultNamespace("block/snow");
        FluidRenderHandlerRegistry.INSTANCE.register(ModFluids.MILK,
                new SimpleFluidRenderHandler(snow, snow, 0xFFFBF0));
        tankOnlyLook(ModFluids.HONEY, 0xF0A21E);
        tankOnlyLook(ModFluids.CRUDE_OIL, 0x1E1A16);
        tankOnlyLook(ModFluids.RAW_PETROL, 0xB0702A);
        tankOnlyLook(ModFluids.RAW_DIESEL, 0x4E4224);
        tankOnlyLook(ModFluids.BIOFUEL, 0x9CC23A);

        // An oil seep is a ragged puddle: the ground shows through its gaps.
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                me.trossty.car.registry.ModBlocks.OIL_SEEP, net.minecraft.client.renderer.chunk.ChunkSectionLayer.CUTOUT);
        // A tank's glass is see-through, and the liquid behind it is drawn by the cell.
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                me.trossty.car.registry.ModBlocks.FUEL_TANK, net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT);
        net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap.putBlock(
                me.trossty.car.registry.ModBlocks.FUEL_PUMP, net.minecraft.client.renderer.chunk.ChunkSectionLayer.TRANSLUCENT);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.trossty.car.registry.ModBlockEntities.FUEL_TANK,
                me.trossty.car.client.render.FuelTankRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.trossty.car.registry.ModBlockEntities.PUMPJACK,
                me.trossty.car.client.render.PumpjackRenderer::new);
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(
                me.trossty.car.registry.ModBlockEntities.FUEL_PUMP,
                me.trossty.car.client.render.FuelPumpRenderer::new);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR, CarMain.id("dashboard"), new DashboardHud());
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR, CarMain.id("service"), new ServiceHud());

        // Hands-on work. The parts of a car that is up on its ramp are drawn
        // where they really are, and this is
        // also where the aiming is worked out, because it has to follow the
        // camera frame by frame.
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            WorldLabels.begin(context);
            CarServiceOverlay.render(context);
            me.trossty.car.client.render.FuelHoseRenderer.render(context);
        });

        // Right clicking a part takes it off or puts one on. The server has no
        // way of telling which part was meant — the car is one entity, and the
        // vanilla packet says only "that entity" — so the side that did the
        // aiming has to say which.
        //
        // FAIL, not SUCCESS. Fabric's hook treats any accepted answer as "and
        // now tell the server the entity was used", which is exactly the
        // vanilla click this is here to replace; FAIL is the one answer that
        // stops the click dead. The price is that the arm no longer swings by
        // itself, so it is swung by hand.
        // A mark is often outside the car's own square hit box — a front
        // wheel, the radiator — so the click behind it may land on the lift,
        // the floor or thin air rather than on the car; all three are caught.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                servicePart(player, world, hand));
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                servicePart(player, world, hand));
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register(CarMainClient::servicePart);

        // The driving client is the only side that sees its car hit something,
        // so give the common code a way to pass that on to the server.
        CarNetworking.setCrashReporter(
                severity -> ClientPlayNetworking.send(new CrashC2SPayload(severity)));

        ClientTickEvents.END_CLIENT_TICK.register(CarMainClient::onClientTick);
    }

    private static void onClientTick(Minecraft client) {
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
            car.setInputs(player.input.keyPresses);
        }

        boolean honked = false;
        while (HORN_KEY.consumeClick()) {
            honked = true;
        }
        if (honked) {
            ClientPlayNetworking.send(HornC2SPayload.INSTANCE);
        }
    }

    private static void tankOnlyLook(net.minecraft.world.level.material.Fluid fluid, int tint) {
        FluidRenderHandlerRegistry.INSTANCE.register(fluid, new SimpleFluidRenderHandler(
                SimpleFluidRenderHandler.WATER_STILL, SimpleFluidRenderHandler.WATER_FLOWING, tint));
    }

    /** A right click while a mark on a raised car is aimed at: that part, by hand. */
    private static InteractionResult servicePart(net.minecraft.world.entity.player.Player player,
                                            net.minecraft.world.level.Level world, InteractionHand hand) {
        if (!world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        CarEntity car = CarServiceOverlay.aimedCar();
        CarPart part = CarServiceOverlay.aimedPart();
        if (car == null || part == null) {
            return InteractionResult.PASS;
        }
        ClientPlayNetworking.send(new ServicePartC2SPayload(car.getId(), part.key()));
        player.swing(hand);
        return InteractionResult.FAIL;
    }
}
