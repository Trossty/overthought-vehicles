package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import net.minecraft.client.render.entity.model.EntityModelLayer;

public final class ModEntityModelLayers {

    public static final EntityModelLayer CAR = new EntityModelLayer(CarMain.id("car"), "main");

    /** Same texture, one more axle and a barrel. */
    public static final EntityModelLayer FUEL_TANKER =
            new EntityModelLayer(CarMain.id("fuel_tanker"), "main");

    public static final EntityModelLayer PICKUP =
            new EntityModelLayer(CarMain.id("pickup"), "main");

    private ModEntityModelLayers() {
    }
}
