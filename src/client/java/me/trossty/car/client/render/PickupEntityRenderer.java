package me.trossty.car.client.render;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Same renderer again, pointed at the model with the bed on the back. Paint,
 * damage wobble, steering and every texture are the car's.
 */
public class PickupEntityRenderer extends CarEntityRenderer {

    public PickupEntityRenderer(EntityRendererProvider.Context context) {
        super(context, ModEntityModelLayers.PICKUP);
    }
}
