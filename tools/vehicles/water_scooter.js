// Yamaha WaveRunner VX, 2010s: a three-seat jet ski. A white V hull with a
// dark rub rail round it; the coloured deck rises from the bow into a hood
// over the bars; a long saddle with footwells either side, a grab handle
// behind it and a black boarding mat on the stern deck; the jet nozzle
// under the stern. The bars and the pod in their middle turn when it steers.
const P = 'paint';
const HULL = '#F2F2F0', RAIL = '#2A2A2C', SEAT = '#1F1F22', SEAT_SIDE = '#5A5D63', MAT = '#232326';

// the hull in plan, half its width at the rub rail, stern to bow
const PLAN = [[-27.0, 9.2], [-20.0, 10.0], [8.0, 10.0], [16.0, 9.0], [22.0, 7.0], [26.0, 4.6], [29.0, 2.0],
    [30.4, 0.4]];
// the keel, rising towards the bow; the bottom is a V of 2.6 from keel to chine
const KEEL = [[-27.0, 0.4], [12.0, 0.0], [20.0, 1.4], [25.0, 3.4], [28.0, 5.4], [30.4, 7.0]];
const half = z => k.at(PLAN, z), keel = z => k.at(KEEL, z);
// the hood's top over the middle line, from the bars to where the nose
// takes over, and the nose's own top on to the bow
const HOOD = [[2.4, 15.6], [6.0, 15.2], [10.0, 14.4], [14.0, 13.2], [17.0, 12.2], [19.4, 11.4]];
const NOSE = [[18.8, 11.6], [22.0, 11.0], [25.0, 10.4], [27.4, 9.8], [29.4, 9.0]];

k.g('body');
// ---- the hull: the V bottom, the chine flats and the sides, slice by slice ---------------------
const CUTS = [-27.0, -20.0, 8.0, 14.0, 19.0, 23.0, 26.4, 29.0];
for (let i = 0; i + 1 < CUTS.length; i++) {
    const z1 = CUTS[i], z2 = CUTS[i + 1];
    const h = Math.min(half(z1), half(z2)), ky = Math.max(keel(z1), keel(z2));
    const cx = h * 0.76;
    k.leans(z1, z2, 0, ky, cx, ky + 2.6, 0.8, HULL);
    k.leans(z1, z2, cx, ky + 2.6, h, ky + 3.0, 0.8, HULL);
}
// the sides up to the rail, and the rail round them, along the plan
for (let i = 0; i + 1 < PLAN.length; i++) {
    const [zA, xA] = PLAN[i], [zB, xB] = PLAN[i + 1];
    const bottom = Math.max(keel(zA), keel(zB)) + 2.8;
    if (bottom < 7.2) {
        k.walls(bottom, 7.4, xA, zA, xB, zB, 0.8, HULL);
    }
    k.walls(7.2, 8.4, xA, zA, xB, zB, 0.8, RAIL, 1);
}
k.box(-9.2, 0.6, -27.8, 9.2, 7.4, -27.0, HULL);                       // the transom
k.box(-9.2, 7.2, -28.6, 9.2, 8.4, -27.0, RAIL);
// the sponsons on the hull's sides by the stern, the bow eye
k.pair(9.6, 3.0, -24.0, 10.4, 4.0, -12.0, RAIL);
k.slab(9.6, 10.4, 4.0, -12.0, 3.0, -10.0, 0.8, RAIL, -1);
k.slab(-10.4, -9.6, 4.0, -12.0, 3.0, -10.0, 0.8, RAIL, -1);
k.box(-0.4, 7.6, 30.2, 0.4, 8.6, 31.0, C.STEEL);

// ---- the deck: the gunwales, the footwells, the stern deck and its mat ---------------------------
k.sym(() => {
    k.box(8.4, 8.4, -20.0, 10.0, 10.2, 8.0, P);
    k.wall(8.4, 10.2, 9.2, -27.0, 10.0, -20.0, 1.6, P, -1);
    k.box(4.0, 8.4, -18.0, 8.4, 8.8, 8.0, MAT);                       // the footwell, on under the hood
    k.box(8.2, 10.2, -18.0, 8.4, 10.4, 8.0, RAIL);
});
k.box(-9.0, 8.4, -27.0, 9.0, 9.8, -18.0, P);
k.box(-8.0, 9.8, -27.0, 8.0, 10.0, -21.4, MAT);
for (let z = -26.4; z < -22.0; z += 1.2) {
    k.box(-7.4, 10.0, z, 7.4, 10.2, z + 0.5, RAIL);
}
// the jet nozzle under the stern, its reverse bucket, the reboarding step
k.box(-1.6, 2.0, -29.0, 1.6, 4.6, -27.8, C.BUMPER);
k.box(-2.0, 1.6, -29.8, 2.0, 5.0, -29.0, RAIL);
k.box(-1.6, 5.2, -29.4, 1.6, 5.8, -27.8, C.BLACK);

// ---- the hood and the nose, on a deck rim that follows the hull forward ----------------------------
// the rim: the deck between the rub rail and the hood, in yawed strips along the plan
for (let i = 2; i + 1 < PLAN.length; i++) {
    const [zA, xA] = PLAN[i], [zB, xB] = PLAN[i + 1];
    k.walls(8.4, 10.0, xA, zA, xB, zB, Math.min(3.4, Math.max(xA, xB)), P);
}
// a top over the middle, a shoulder a step lower each side, and the side filled down to the rim
const cowl = (line, top, shoulder, z1, z2) => {
    k.loft(-top, top, line, 0.8, P);
    k.sym(() => {
        k.loft(top, shoulder, line.map(([z, y]) => [z, y - 0.5]), 0.8, P);
        for (let z = z1; z < z2 - 1e-6; z += 1.2) {
            const zb = Math.min(z + 1.2, z2);
            k.box(shoulder - 0.8, 8.4, z, shoulder, Math.min(k.at(line, z), k.at(line, zb)) - 0.9, zb, P);
        }
    });
};
cowl(HOOD, 5.6, 6.8, 2.4, 19.4);
cowl(NOSE, 3.0, 4.2, 19.4, 29.0);
k.box(-6.8, 8.4, 2.0, 6.8, 15.6, 3.0, P);                               // the hood's back wall
k.box(-3.0, 12.0, 1.6, 3.0, 15.0, 2.0, RAIL);                           // the glovebox lid
// the storage hatch's seams, on the hood's top
k.loft(-0.3, 0.3, [[9.0, k.at(HOOD, 9.0)], [14.0, 13.2], [17.0, 12.2], [18.6, k.at(HOOD, 18.6)]], 0.1, RAIL, 1);
k.box(-5.6, k.at(HOOD, 9.0) - 0.1, 8.8, 5.6, k.at(HOOD, 9.0) + 0.1, 9.2, RAIL);
k.mirrors({x: 6.8, y: 13.8, z: 9.0, w: 1.6, h: 1.2, d: 0.8, color: RAIL});

// ---- the saddle and the grab handle ------------------------------------------------------------------
k.box(-4.2, 8.8, -21.0, 4.2, 15.2, -1.0, SEAT);
k.slab(-4.2, 4.2, 15.2, -1.0, 12.4, 1.6, 0.8, SEAT, -1);
k.box(-4.2, 8.4, -1.0, 4.2, 12.4, 2.0, SEAT);                           // the saddle's front, down to the deck
k.slab(-4.2, 4.2, 15.2, -21.0, 12.6, -22.6, 0.8, SEAT, -1);
k.pair(4.2, 9.4, -20.4, 4.4, 14.0, -1.6, SEAT_SIDE);
k.box(-0.1, 15.2, -20.4, 0.1, 15.24, -1.6, SEAT_SIDE);                  // its seam
k.box(-4.0, 15.2, -11.2, 4.0, 15.6, -10.0, SEAT_SIDE);                  // the step between the seats
k.box(-3.6, 12.6, -24.0, 3.6, 13.6, -23.0, C.BLACK);
k.pair(3.0, 9.8, -23.8, 3.8, 12.6, -23.2, C.BLACK);

// ---- the bars, the pod and its gauge: they turn together ------------------------------------------------
// the gauge hood is the highest piece and sits on the middle line, so the
// game turns the lot about the upright through it
k.g('steer');
k.box(-1.6, 20.0, 4.0, 1.6, 20.6, 5.8, RAIL);
k.box(-3.0, 15.4, 2.6, 3.0, 18.4, 6.8, P);
k.slab(-3.0, 3.0, 18.4, 6.8, 20.0, 4.0, 0.6, RAIL, -1);
k.box(-1.4, 18.6, 3.0, 1.4, 19.8, 3.2, C.DIAL);
k.box(-2.6, 17.0, 2.5, -1.8, 17.8, 2.6, '#C0201C');                     // the stop button
k.box(1.8, 17.0, 2.5, 2.6, 17.8, 2.6, '#2E8B3E');                       // the start button
k.beams([2.8, 18.4, 4.8], [6.4, 19.0, 4.4], 0.8, 0.8, C.BUMPER);
k.sym(() => {
    k.box(6.2, 18.4, 3.8, 8.6, 19.6, 5.0, C.BLACK);                     // the grip
    k.box(5.6, 18.2, 4.8, 6.4, 19.8, 5.6, RAIL);
});
k.beam([-6.0, 19.4, 5.2], [-7.6, 19.0, 6.4], 0.5, 0.5, RAIL);           // the throttle lever
