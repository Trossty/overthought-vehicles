package me.trossty.car.client.compat;

import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.entity.CarEntity;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITargetRedirector;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaClientPlugin;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The same as {@link JadeCarPlugin}, for WTHIT: the {@link CarLookLines} under
 * a car, and a look at a machine's cell taken to its controller, whose block
 * entity is the one WTHIT can read tanks and charge from.
 *
 * <p>WTHIT finds this class through {@code wthit_plugins.json} and is the only
 * thing that ever loads it.
 */
public class WthitCarPlugin implements IWailaClientPlugin {

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(CarInfo.INSTANCE, CarEntity.class);
        registrar.redirect(MachineCell.INSTANCE, MultiblockBlock.class);
    }

    enum CarInfo implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (accessor.getEntity() instanceof CarEntity car) {
                CarLookLines.of(car).forEach(tooltip::addLine);
            }
        }
    }

    enum MachineCell implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public ITargetRedirector.Result redirect(ITargetRedirector redirect, IBlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() == null
                    && MultiblockBlock.controllerEntity(accessor.getLevel(), accessor.getPosition()) instanceof BlockEntity controller) {
                return redirect.to(accessor.getBlockHitResult().withPosition(controller.getBlockPos()));
            }
            return null;
        }
    }
}
