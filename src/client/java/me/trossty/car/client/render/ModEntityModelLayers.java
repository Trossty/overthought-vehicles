package me.trossty.car.client.render;

import me.trossty.car.CarMain;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModEntityModelLayers {

    public static final ModelLayerLocation CAR = new ModelLayerLocation(CarMain.id("car"), "main");

    /** Same texture, one more axle and a barrel. */
    public static final ModelLayerLocation FUEL_TANKER =
            new ModelLayerLocation(CarMain.id("fuel_tanker"), "main");

    public static final ModelLayerLocation PICKUP =
            new ModelLayerLocation(CarMain.id("pickup"), "main");

    private ModEntityModelLayers() {
    }
}
