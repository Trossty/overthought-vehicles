package me.trossty.car.client.render;

import net.minecraft.client.render.entity.EntityRendererFactory;

/**
 * Same renderer again, pointed at the model with the bed on the back. Paint,
 * damage wobble, steering and every texture are the car's.
 */
public class PickupEntityRenderer extends CarEntityRenderer {

    public PickupEntityRenderer(EntityRendererFactory.Context context) {
        super(context, ModEntityModelLayers.PICKUP);
    }
}
