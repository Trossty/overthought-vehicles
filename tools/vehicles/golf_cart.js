// Club Car Precedent, 2010s: a two-seat golf cart. A rounded front cowl
// over the front wheels with two lamps in its nose, a black dash with
// cubbies, a bench seat on the body pod, the bag well behind with its rack,
// and a canopy on four struts with a single upright windscreen.
const P = 'paint';
const DEG = Math.PI / 180;
const ROOF = '#D9D2C2', SEAT = '#B9A88C', SEAM = '#8E7F66', MAT = '#232326';
const FW = {y: 3.7, z: 12.0}, RW = {y: 3.7, z: -14.0}, ARCH = 4.9;

// the cowl's top, back to front
const COWL = [[3.0, 15.0], [6.0, 14.8], [10.6, 13.6], [14.6, 12.0], [17.6, 10.2], [19.2, 8.2], [19.8, 5.6]];
// a side panel of the body from bottom to top over z1..z2, cut round the wheel w
const side = (z1, z2, top, w, bottom) => {
    const step = 1.2;
    for (let z = z1; z < z2 - 1e-6; z += step) {
        const zb = Math.min(z + step, z2);
        let lo = bottom;
        if (w) {
            const d = Math.max(Math.abs(z - w.z), Math.abs(zb - w.z));
            const nearest = Math.min(Math.abs(z - w.z), Math.abs(zb - w.z));
            const over = (z <= w.z && zb >= w.z) ? 0 : nearest;
            if (over < ARCH) {
                lo = Math.max(bottom, w.y + Math.sqrt(ARCH * ARCH - over * over));
            }
            void d;
        }
        const hi = typeof top === 'function' ? Math.min(top(z), top(zb)) : top;
        if (hi > lo) {
            k.pair(8.0, lo, z, 8.8, hi, zb, P);
        }
    }
};
// the lip round a wheel opening, and the dark well behind it
const lip = w => {
    const arc = [];
    for (let i = 0; i <= 10; i++) {
        const a = (12 + 156 * i / 10) * DEG;
        arc.push([w.z - Math.cos(a) * ARCH, w.y + Math.sin(a) * ARCH]);
    }
    k.sym(() => {
        k.loft(8.4, 9.2, arc, 0.7, P, 1);
        k.loft(5.0, 8.4, arc.map(([z, y]) => [z, y + 0.1]), 0.3, C.WELL, 1);
    });
};

k.g('body');
// ---- the front cowl ------------------------------------------------------------------------
k.loft(-8.0, 8.0, COWL, 0.8, P);
k.sym(() => k.loft(8.0, 8.8, COWL.map(([z, y]) => [z, y - 0.4]), 0.8, P));
side(6.8, 19.4, z => k.at(COWL, z) - 0.6, FW, 4.6);
lip(FW);
k.box(-8.8, 4.4, 18.6, 8.8, 6.0, 19.6, P);
k.sym(() => k.lamp(4.0, 8.2, 6.8, 9.6, 18.9, C.LAMP, true, {depth: 0.3}));
k.box(-1.4, 10.2, 17.5, 1.4, 11.4, 17.9, C.CHROME);               // the badge
// the bumper: a black bar round the nose
k.box(-8.0, 2.6, 19.6, 8.0, 4.8, 21.0, C.BLACK);
k.walls(2.6, 4.8, 8.0, 21.0, 9.2, 19.0, 1.0, C.BLACK);

// ---- the dash, the floor, the windscreen -----------------------------------------------------
// the dash is a shelf high enough for the knees to go under it, a footwell behind
k.box(-8.2, 13.4, 2.4, 8.2, 15.6, 4.2, C.BLACK);
k.box(-6.8, 13.8, 2.3, -1.4, 15.0, 2.4, '#141416');               // the cubbies
k.box(-0.8, 14.0, 2.3, 0.8, 15.0, 2.4, C.DIAL);                   // the charge meter
k.box(1.4, 13.8, 2.3, 2.6, 15.0, 2.4, C.STEEL);                   // the key
k.box(-8.0, 4.4, 6.8, 8.0, 13.6, 7.4, MAT);
k.box(-8.0, 12.8, 4.2, 8.0, 13.4, 6.8, MAT);
k.box(-8.0, 3.4, -1.6, 8.0, 4.4, 6.8, MAT);
k.sym(() => {
    for (let z = -1.0; z < 6.4; z += 1.0) {
        k.box(0.4, 4.4, z, 7.6, 4.6, z + 0.4, C.BLACK);
    }
});
k.pair(8.0, 3.2, -1.6, 8.8, 4.8, 3.0, C.BLACK);                    // the rocker under each side
k.pedals(5.0, 6.0, 4.4);
k.g('glass');
k.slab(-7.6, 7.6, 15.6, 5.0, 31.6, 6.4, 0.08, C.GLASS, 0);
k.g('body');

// ---- the seat pod and the bench ----------------------------------------------------------------
k.box(-8.8, 4.4, -11.4, 8.8, 9.2, -1.6, P);
k.box(-6.0, 5.4, -1.7, 6.0, 8.4, -1.6, C.WELL);                     // the storage under the seat
k.box(8.8, 6.6, -4.2, 8.9, 8.0, -2.6, C.BLACK);                     // the charge socket
k.box(-8.2, 9.2, -10.0, 8.2, 12.3, -2.0, SEAT);
k.slab(-8.2, 8.2, 12.3, -2.0, 9.6, -1.4, 0.8, SEAT, -1);
k.box(-0.15, 9.3, -10.02, 0.15, 12.32, -1.4, SEAM);
k.slab(-8.2, 8.2, 12.6, -10.0, 21.2, -11.8, 1.6, SEAT, -1);
k.slab(-0.15, 0.15, 12.8, -10.02, 21.0, -11.72, 0.1, SEAM, 1);
k.slab(-8.4, 8.4, 12.0, -11.6, 21.6, -13.6, 0.6, C.BLACK, 1);
k.sym(() => {
    // the hip restraints and their hand holds
    k.box(8.4, 12.0, -10.0, 9.0, 16.2, -9.0, C.BLACK);
    k.box(8.4, 15.6, -9.0, 9.0, 16.2, -4.4, C.BLACK);
    k.box(8.4, 12.0, -5.0, 9.0, 15.6, -4.4, C.BLACK);
});

// ---- the rear body, the bag well and the rack ---------------------------------------------------
k.box(-9.0, 9.2, -20.0, 9.0, 10.0, -11.4, P);
side(-20.0, -11.4, 9.8, RW, 4.4);
lip(RW);
k.slab(-9.0, 9.0, 10.0, -20.0, 4.4, -20.8, 0.8, P, -1);
k.box(-8.6, 2.6, -22.0, 8.6, 4.8, -20.4, C.BLACK);
k.walls(2.6, 4.8, 8.6, -22.0, 9.4, -20.0, 1.0, C.BLACK);
k.sym(() => k.lamp(5.6, 7.0, 7.8, 8.4, -20.5, C.TAIL, false, {depth: 0.3}));
k.plate(-2.0, 5.4, 2.0, 7.4, -20.6, false);
k.box(-7.6, 10.0, -19.6, 7.6, 10.2, -12.0, MAT);                    // the bag well's mat
k.box(-8.4, 19.4, -16.8, 8.4, 20.4, -15.8, C.BLACK);                // the bag bracket
k.box(-6.4, 10.0, -19.8, 6.4, 13.6, -19.0, C.BLACK);                // the bag strap's foot
k.box(-6.0, 20.4, -15.6, 6.0, 20.8, -12.4, C.BLACK);                // the sweater basket
k.box(-6.0, 20.8, -15.6, 6.0, 23.4, -15.2, C.BLACK);
k.box(-6.0, 20.8, -12.8, 6.0, 23.4, -12.4, C.BLACK);
k.pair(5.6, 20.8, -15.2, 6.0, 23.4, -12.8, C.BLACK);

// ---- the canopy on its four struts ---------------------------------------------------------------
k.sym(() => {
    k.beam([8.3, 14.8, 4.4], [8.3, 32.4, 6.8], 0.9, 0.9, C.BLACK);
    k.box(8.0, 9.8, -16.8, 8.8, 32.4, -16.0, C.BLACK);
    k.box(7.8, 31.2, -17.4, 9.0, 32.0, 7.8, C.BLACK);                // the rail under the roof edge
});
k.box(-9.4, 32.0, -18.2, 9.4, 33.0, 8.8, ROOF);
k.box(-8.6, 33.0, -17.4, 8.6, 33.6, 8.0, ROOF);
k.sym(() => k.lean(-18.2, 8.8, 9.4, 32.0, 9.8, 33.0, 0.4, ROOF, 1));
k.slab(-9.4, 9.4, 33.0, 8.8, 32.0, 9.6, 0.4, ROOF, 1);
// the mirror under the roof, kept clear of the driver's side so the game
// does not take it for the dash it hangs the wheel from
k.box(-1.1, 30.2, 5.8, 1.1, 31.4, 6.6, C.BLACK);
k.box(-0.9, 30.4, 5.7, 0.9, 31.2, 5.8, C.MIRROR);
k.box(-0.3, 31.4, 6.0, 0.3, 32.0, 6.4, C.BLACK);

// ---- the steering column: the wheel lies back like a golf cart's, so its column drops steeply
// from under the game's wheel into the dash shelf, in a boot where it goes through
k.g('body');
const col = k.column();
k.beam(col.end, col.end.map((v, i) => v + col.dir[i] * 1.3), 1.0, 1.0, C.BLACK);
k.box(col.end[0] - 0.8, 15.6, 3.2, col.end[0] + 0.8, 16.0, 4.2, '#141416');
