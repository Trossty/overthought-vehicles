package me.trossty.car.client.render;

import net.minecraft.world.item.DyeColor;

public class CarRenderState {
    /** Ticks the entity has lived, with the partial tick. */
    public float ageInTicks;
    public int lightCoords;
    public float yaw;
    /** Paint colour, picking which body texture the renderer uses. */
    public DyeColor color = DyeColor.RED;
    /** A sprayed colour as 0xRRGGBB, or -1 for {@link #color}. */
    public int paint = -1;

    /** The data-pack vehicle to draw instead of the built-in model, if any. */
    public net.minecraft.resources.ResourceLocation vehicleId;
    public me.trossty.car.vehicle.VehicleDefinition vehicle;
    public String tyre = "road";
    /** Whether the tyres are on; off, the wheels are drawn as bare hubs, or not at all with the discs off too. */
    public boolean tyresOn = true;
    public boolean discsOn = true;
    /** The service plates hanging over the car, if you are looking at it. */
    public java.util.List<CarServiceOverlay.PartLabel> labels = java.util.List.of();
    public float wheelAngle;
    public float steerAngle;
    /** Signed speed in blocks a tick, for the lean and the idle shake. */
    public float speed;
    /** Smoothed change in speed, for the body's squat and dive. */
    public float acceleration;
    /** Whether the engine is actually turning: somebody aboard, fuel in the tank. */
    public boolean engineRunning;
    /** How settled into its parked pose: a bike on its stand, a trailer on its jockey wheel. */
    public float restPose;
    public int damageWobbleSide;
    public float damageWobbleTicks;
    public float damageWobbleStrength;
}
