// A two-post lift. The block is the plate a car is driven on to; a blue
// column stands a block and a half out to either side, a carriage rides up
// each one and swings two telescopic arms under the sills. The pads on the
// ends of the arms are what the car sits on, so their tops stay where
// VehicleLiftBlock holds it: two up, and seven and a half more a level.
// The power unit hangs on the south face of the east column.
const POST = C.BLUE, POST_DARK = C.BLUE_DARK, ARM = C.YELLOW;
const HAZARD = 'car:block/shop_hazard', PLATE = 'car:block/lift_plate';
const SIN = Math.sin(Math.PI / 8), COS = Math.cos(Math.PI / 8);

function lift(level) {
    const rise = 7.5 * level;

    // ---- the plate --------------------------------------------------------------
    k.g('plate');
    k.box(0, 0, 0, 16, 0.6, 16, HAZARD);
    k.box(1, 0.6, 1, 15, 2, 15, C.DARK_STEEL, {faces: {up: PLATE}});
    // the cover over the cables that run between the columns
    k.sym(() => {
        k.box(-7.2, 0, 6.6, 0, 0.4, 9.4, C.DARK_STEEL, {faces: {up: PLATE}});
    });

    k.sym(() => {
        // ---- a column ---------------------------------------------------------------
        k.g('column');
        k.box(-16, 0, 2.6, -9, 0.8, 13.4, C.DARK_STEEL);
        for (const [x, z] of [[-15.6, 3], [-10, 3], [-15.6, 12.4], [-10, 12.4]]) {
            k.box(x, 0.8, z, x + 0.6, 1.1, z + 0.6, C.CHROME);
        }
        // a channel, open towards the car
        k.box(-15, 0.8, 5, -13.9, 31, 11, POST);
        k.box(-13.9, 0.8, 5, -10.5, 31, 6, POST);
        k.box(-13.9, 0.8, 10, -10.5, 31, 11, POST);
        k.box(-13.9, 0.8, 6, -13.7, 31, 10, C.BLACK, {only: ['east']});
        k.box(-15.2, 31, 4.8, -10.3, 31.6, 11.2, POST_DARK);
        k.box(-15.05, 0.8, 4.95, -10.45, 3.6, 11.05, HAZARD, {no: ['up', 'down', 'east']});
        // the ram and the chain that carries the carriage
        const foot = Math.max(0.8, rise - 0.6), head = Math.min(30.8, rise + 6.6);
        if (foot > 0.8) {
            k.round('y', -12.2, (0.8 + foot) / 2, 8, 0.8, foot - 0.8, C.CHROME);
        }
        if (head < 30.8) {
            k.box(-12.5, head, 7.5, -11.9, 31, 8.5, C.DARK_STEEL);
        }

        // ---- the carriage -----------------------------------------------------------
        k.g('carriage');
        k.box(-13.6, foot, 6.2, -10.5, head, 9.8, C.BLACK);
        k.box(-10.5, foot, 5.6, -9.6, head, 10.4, C.DARK_STEEL);
        k.box(-9.6, rise + 0.2, 6.6, -7.4, rise + 1.9, 9.4, C.DARK_STEEL);
        k.round('y', -8.5, rise + 1.05, 6.6, 0.45, 1.85, C.CHROME);
        k.round('y', -8.5, rise + 1.05, 9.4, 0.45, 1.85, C.CHROME);

        // ---- the arms ---------------------------------------------------------------
        k.g('arms');
        for (const way of [-1, 1]) {
            const pin = 8 + way * 1.4, turn = ['y', way * 22.5, [-8.5, rise + 1, pin]];
            const z = (a, b) => [pin + way * a, pin + way * b].sort((p, q) => p - q);
            const [o1, o2] = z(-0.9, 5.4), [i1, i2] = z(5.4, 9.6), [c1, c2] = z(5, 5.4);
            k.box(-9.4, rise + 0.45, o1, -7.6, rise + 1.65, o2, ARM, {rot: turn});
            k.box(-9.5, rise + 0.4, c1, -7.5, rise + 1.7, c2, C.YELLOW_DARK, {rot: turn});
            k.box(-9.1, rise + 0.6, i1, -7.9, rise + 1.5, i2, C.STEEL, {rot: turn});
            const px = -8.5 + 9.3 * SIN, pz = pin + way * 9.3 * COS;
            k.round('y', px, rise + 1.2, pz, 1.2, 0.9, C.DARK_STEEL);
            k.round('y', px, rise + 1.825, pz, 1.7, 0.35, C.RUBBER, {sides: 16});
        }
    });

    // ---- the power unit ---------------------------------------------------------
    k.g('power');
    k.box(27.6, 17, 11, 30.4, 22, 11.5, C.DARK_STEEL);
    k.round('y', 29, 22.4, 12.8, 1.35, 4.4, C.BLACK, {sides: 16});
    k.round('y', 29, 24.9, 12.8, 1.05, 0.6, C.STEEL);
    k.box(27.8, 16.4, 11.5, 30.2, 20.2, 14.2, C.WHITE);
    k.box(27.6, 20.2, 11.5, 30.4, 20.8, 14.4, C.DARK_STEEL);
    k.box(28.6, 16, 12.4, 29.4, 16.4, 13.2, C.BLACK);
    // the lever that lets it down, and the button that sends it up
    k.box(26.9, 20.3, 12.6, 27.6, 20.7, 13, C.CHROME);
    k.box(25.4, 20.25, 12.55, 26.9, 20.75, 13.05, C.RED);
    k.box(27.2, 22.6, 11.6, 27.7, 23.8, 12.6, C.YELLOW);
    k.box(27.1, 23, 11.9, 27.2, 23.4, 12.3, C.LED_GREEN, {only: ['west', 'up', 'down', 'north', 'south'], shade: false});
    // the hose down to the ram
    k.box(30.4, 1.2, 11, 30.8, 20.6, 11.4, C.BLACK);
}

for (let level = 0; level <= 4; level++) {
    k.model(level ? 'vehicle_lift_' + level : 'vehicle_lift', {ao: false, particle: 'car:block/lift_plate'}, () => lift(level));
}
