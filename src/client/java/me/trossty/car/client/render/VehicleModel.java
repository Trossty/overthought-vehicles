package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * A model of a vehicle, posed from a {@link CarRenderState} rather than from
 * the entity, so the built-in car and the data-pack ones pose the same way.
 */
public abstract class VehicleModel extends Model {

    protected final ModelPart root;

    protected VehicleModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
    }

    public ModelPart root() {
        return this.root;
    }

    /** Puts every part back as it was built; subclasses then pose it. */
    public void setupAnim(CarRenderState state) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer buffer, int light, int overlay, int color) {
        this.root.render(matrices, buffer, light, overlay, color);
    }
}
