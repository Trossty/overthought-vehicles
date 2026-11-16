package me.trossty.car.client.compat;

import me.trossty.car.CarMain;
import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.entity.CarEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * What Jade shows when you look at a car: the {@link CarLookLines}.
 *
 * <p>Machines need nothing of their own: Jade reads their tanks and charge
 * through the same capabilities pipes and cables use. It only asks a block
 * that has a block entity, though, and of a machine's cells only the
 * controller has one, so a look at any other cell is turned into a look at
 * the controller.
 *
 * <p>Jade finds this class by its {@link WailaPlugin} annotation and is the
 * only thing that ever loads it: without Jade it is never touched and the mod
 * runs the same.
 */
@WailaPlugin
public class JadeCarPlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CarInfo.INSTANCE, CarEntity.class);
        registration.addRayTraceCallback((hit, accessor, original) -> {
            if (accessor instanceof BlockAccessor block && block.getBlockEntity() == null
                    && block.getBlock() instanceof MultiblockBlock
                    && MultiblockBlock.controllerEntity(block.getLevel(), block.getPosition()) instanceof BlockEntity controller) {
                return registration.blockAccessor().from(block)
                        .hit(block.getHitResult().withPosition(controller.getBlockPos()))
                        .blockState(controller.getBlockState())
                        .blockEntity(controller)
                        .build();
            }
            return accessor;
        });
    }

    enum CarInfo implements IEntityComponentProvider {
        INSTANCE;

        private static final ResourceLocation UID = CarMain.id("car_info");

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof CarEntity car) {
                CarLookLines.of(car).forEach(tooltip::add);
            }
        }
    }
}
