// A trailed twin-disc fertiliser spreader: a big hopper on a two-wheeled
// frame, square at the top under a hinged grid, falling away in two funnels
// to the metering gates. Under them the two spinning discs with their vanes
// throw the fertiliser out behind; a guard bar curves round them. A single
// drawbar runs out to the hitch with the PTO shaft beside it.
const P = 'paint';
const DEG = Math.PI / 180;
const FRAME = C.DARK_STEEL, GRID = '#3A3B3F', DISC = '#A9ADB2', GUARD = '#E8C21A';

k.g('body');
// ---- the frame and the axle -------------------------------------------------------------------------
k.pair(7.4, 8.8, -12.0, 8.6, 11.0, 10.0, FRAME);
k.box(-8.6, 8.8, 8.8, 8.6, 11.0, 10.0, FRAME);
k.box(-8.6, 8.8, -4.0, 8.6, 10.4, -3.0, FRAME);
k.box(-15.0, 6.2, -0.8, 15.0, 7.8, 0.8, FRAME);
k.pair(7.4, 6.2, -1.4, 8.6, 8.8, 1.4, FRAME);
k.pair(15.0, 6.0, -1.0, 15.6, 8.0, 1.0, C.STEEL);
// the mudguards over the wheels
k.sym(() => {
    const arc = [];
    for (let i = 0; i <= 8; i++) {
        const a = (25 + 130 * i / 8) * DEG;
        arc.push([-Math.cos(a) * 8.2, 7.0 + Math.sin(a) * 8.2]);
    }
    k.loft(14.6, 19.6, arc, 0.6, C.BLACK, 1);
    k.box(13.6, 7.8, -1.0, 14.6, 15.4, 1.0, FRAME);
});

// ---- the hopper: square at the top, two funnels below ---------------------------------------------------
k.box(-14.5, 24.0, -9.0, 14.5, 30.4, 9.0, P);
k.box(-15.0, 30.4, -9.5, 15.0, 31.2, 9.5, P);                             // the rim
k.slab(-14.5, 14.5, 24.0, 9.0, 16.0, 4.0, 0.8, P, 1);
k.slab(-14.5, 14.5, 24.0, -9.0, 16.0, -4.0, 0.8, P, 1);
k.sym(() => {
    k.lean(-9.0, 9.0, 14.5, 24.0, 9.6, 16.0, 0.8, P, 1);                   // the outer wall of a funnel
    k.lean(-8.6, 8.6, 0.2, 22.0, 2.6, 16.0, 0.8, P, 1);                    // the ridge between the two
    k.box(2.4, 12.6, -4.0, 9.8, 16.2, 4.0, P);                             // the funnel's neck
    k.box(3.0, 11.6, -3.0, 9.2, 12.6, 3.0, FRAME);                         // its metering gate
    k.box(9.2, 11.8, -1.2, 10.4, 12.4, 2.6, C.BLACK);                      // the gate's lever
    k.box(4.4, 26.0, 9.0, 12.6, 28.6, 9.3, C.PLATE);                       // the sticker on the front
    k.box(10.0, 18.0, 9.2, 13.6, 20.6, 9.5, C.TAIL);                       // the reflectors
});
// the grid over the top, hinged at the back, and a ladder up the side
k.box(-14.4, 31.2, -9.0, 14.4, 31.6, -8.4, GRID);
k.box(-14.4, 31.2, 8.4, 14.4, 31.6, 9.0, GRID);
k.pair(13.8, 31.2, -8.4, 14.4, 31.6, 8.4, GRID);
for (let x = -11.4; x <= 11.41; x += 2.85) {
    k.box(x - 0.2, 31.2, -8.4, x + 0.2, 31.5, 8.4, GRID);
}
for (let z = -5.6; z <= 5.61; z += 2.8) {
    k.box(-13.8, 31.25, z - 0.2, 13.8, 31.45, z + 0.2, GRID);
}
k.box(-1.2, 31.6, -9.4, 1.2, 32.2, -8.8, C.BLACK);

// ---- the spinning discs, their drives and the guard behind them ------------------------------------------
k.box(-1.6, 10.6, 3.6, 1.6, 13.2, 7.6, FRAME);                                // the gearbox
k.box(-6.4, 11.4, -0.6, 6.4, 12.4, 0.6, C.STEEL);
k.sym(() => {
    const x = 6.0, z = -5.6, r = 4.2;
    k.box(x - 0.8, 9.6, z - 0.8, x + 0.8, 12.6, z + 0.8, FRAME);              // its drive leg
    k.beam([x, 12.0, 0.6], [x, 11.6, z + 0.8], 0.8, 0.8, FRAME);
    const flat = 2 * r * Math.tan(Math.PI / 8);
    for (const yaw of [-45, 0, 45, 90]) {
        k.box(x - flat / 2, 9.0, z - r, x + flat / 2, 9.4, z + r, DISC, {yaw: yaw});
    }
    for (const yaw of [15, -75]) {
        k.box(x - 0.25, 9.4, z - r + 0.4, x + 0.25, 10.4, z + r - 0.4, P, {yaw: yaw});
    }
    k.box(x - 0.9, 9.4, z - 0.9, x + 0.9, 10.0, z + 0.9, C.BLACK);
});
k.box(-13.4, 8.6, -11.6, 13.4, 9.6, -10.6, GUARD);
k.walls(8.6, 9.6, 13.4, -11.1, 14.0, -3.6, 1.0, GUARD);
k.pair(7.4, 8.4, -11.0, 8.4, 11.0, -10.6, FRAME);
k.sym(() => {
    k.lamp(9.6, 9.8, 12.2, 11.2, -11.6, C.TAIL, false, {depth: 0.3});
    k.box(9.8, 11.4, -11.8, 12.0, 12.2, -11.4, C.AMBER);
});
k.plate(-2.6, 9.8, 2.6, 11.6, -11.6, false);
// the ladder up the back to the hopper's lid, off to the right of the plate, standing on the guard
k.box(-5.6, 9.6, -11.2, -5.0, 32.6, -10.6, FRAME);
k.box(-3.0, 9.6, -11.2, -2.4, 32.6, -10.6, FRAME);
for (let y = 12.2; y < 31.0; y += 2.4) {
    k.box(-5.0, y, -11.1, -3.0, y + 0.5, -10.7, FRAME);
}
for (const y of [23.8, 30.4]) {
    k.box(-5.6, y, -10.6, -2.4, y + 0.6, -9.0, FRAME);                       // its ties to the hopper
}

// ---- the drawbar, the hitch, the PTO shaft, the stand ------------------------------------------------------
k.beam([0, 9.8, 9.6], [0, 6.0, 29.6], 1.6, 1.6, FRAME);
k.beams([7.6, 9.8, 9.6], [1.2, 8.6, 18.0], 1.0, 1.0, FRAME);
k.box(-1.2, 5.0, 29.0, 1.2, 6.6, 31.4, FRAME);
k.box(-0.5, 5.0, 31.4, 0.5, 6.6, 31.9, C.STEEL);
k.beam([-2.2, 11.8, 6.8], [-2.0, 8.2, 27.0], 1.0, 1.0, C.STEEL);              // the PTO shaft
k.beam([-2.2, 11.6, 9.0], [-2.0, 8.6, 25.0], 1.6, 1.6, GUARD);                // its guard
k.jockey(2.6, 22.0, 9.2, 0);
