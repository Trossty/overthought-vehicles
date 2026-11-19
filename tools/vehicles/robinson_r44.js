// Robinson R44, 1990s: a four-seat light helicopter. A rounded cabin with a
// raked windscreen split by a post, chin windows in the nose, four doors
// with big windows; the mast fairing over the roof, the engine bay open
// behind the cabin over its tube frame, and a slim tail cone out to the
// fins and the tail rotor on its left. Two skids on bowed cross tubes.
// The main rotor turns about the upright through its hub, the tail rotor
// about the line across through its own: each hub is its group's first box.
const P = 'paint', PD = 'paint_dark';
const STRIPE = '#E8E8E4', SEAT = '#8C7B66', SEAT_BASE = '#4A4A50', PANEL = '#2A2A2C', ENGINE = '#5A5A5E';
const BLADE = '#C9CCD0', TIP = '#C42620', TUBE = C.CHROME;

// the windscreen's foot and head on the middle line, and how wide it is at each
const WS = {y0: 20.0, z0: 31.6, y1: 39.6, z1: 12.6, x0: 9.2, x1: 7.4};
const lerp = (a, b, t) => a + (b - a) * t;

k.g('body');
// ---- the cabin: the floor, the lower sides with a stripe, the back wall -----------------------------
k.box(-9.2, 9.4, -8.6, 9.2, 11.0, 26.0, PD);
k.box(-8.8, 11.0, -7.6, 8.8, 11.2, 26.0, C.CARPET);
k.leans(-8.6, 26.0, 9.2, 11.0, 10.0, 19.4, 0.8, P);
k.leans(-8.6, 26.0, 9.82, 16.6, 9.94, 17.8, 0.12, STRIPE, 1);
k.box(-9.4, 11.0, -8.6, 9.4, 39.6, -7.6, P);
// the door seams and handles
k.sym(() => {
    for (const z of [-7.4, 11.0, 25.6]) {
        k.lean(z, z + 0.2, 9.25, 11.4, 10.05, 19.3, 0.08, C.BLACK, 1);
    }
    k.box(9.9, 17.8, 8.6, 10.2, 18.4, 10.2, C.BLACK);
    k.box(9.9, 17.8, -5.6, 10.2, 18.4, -4.0, C.BLACK);
});

// ---- the nose: rounded under the windscreen, the chin windows, the landing lamps ---------------------
const NOSE = [[25.6, 9.6], [29.6, 10.2], [32.2, 12.0], [33.4, 14.8], [33.2, 18.0], [WS.z0, WS.y0 + 0.4]];
k.loft(-6.8, 6.8, NOSE, 0.8, P);
k.sym(() => {
    k.wall(9.6, 20.4, 9.8, 25.6, 6.8, 32.8, 0.8, P);
    k.wall(19.6, 20.6, 9.8, 25.6, 7.4, 31.8, 0.8, P);
    k.lean(25.6, 30.4, 9.2, 11.0, 9.8, 13.0, 0.8, P);
});
k.g('glass');
k.sym(() => k.wall(12.0, 17.6, 9.84, 26.6, 7.6, 31.8, 0.06, C.GLASS, 1));    // the chin windows
k.g('body');
k.sym(() => k.lamp(1.4, 15.4, 3.6, 16.8, 33.4, C.LAMP, true, {depth: 0.3}));
k.box(-0.9, 12.2, 32.6, 0.9, 13.4, 33.0, C.BLACK);                            // the air vent

// ---- the windscreen in two panes, the post between, the pillars round it ----------------------------
k.g('glass');
for (let i = 0; i < 8; i++) {
    const ta = i / 8, tb = (i + 1) / 8;
    const half = Math.min(lerp(WS.x0, WS.x1, ta), lerp(WS.x0, WS.x1, tb));
    const ya = lerp(WS.y0, WS.y1, ta), za = lerp(WS.z0, WS.z1, ta);
    const yb = lerp(WS.y0, WS.y1, tb), zb = lerp(WS.z0, WS.z1, tb);
    k.slab(0.35, half, ya, za, yb, zb, 0.06, C.GLASS, 0);
    k.slab(-half, -0.35, ya, za, yb, zb, 0.06, C.GLASS, 0);
}
k.g('body');
k.beam([0, WS.y0, WS.z0], [0, WS.y1, WS.z1], 0.7, 0.7, P);
k.planks([WS.x0 + 0.3, WS.y0, WS.z0], [WS.x1 + 0.2, WS.y1, WS.z1], [1, 0, 0.35], 1.4, 0.7, P, 0);
k.box(-WS.x0, WS.y0 - 0.6, WS.z0 - 1.0, WS.x0, WS.y0 + 0.4, WS.z0 + 0.4, P);   // its foot

// ---- the doors' windows and the pillars between them --------------------------------------------------
k.g('glass');
k.sideGlass({xLow: 10.0, yLow: 19.4, xHigh: 7.6, yHigh: 39.4, rearLow: 11.6, rearHigh: 11.6,
    frontLow: 30.6, frontHigh: 13.0, bands: 8});
k.sideGlass({xLow: 10.0, yLow: 19.4, xHigh: 7.6, yHigh: 39.4, rearLow: -7.6, rearHigh: -7.6,
    frontLow: 10.8, frontHigh: 10.8, bands: 4});
k.g('body');
k.leans(10.6, 11.8, 10.0, 19.4, 7.6, 39.4, 0.7, P, 1);                         // the middle pillar
k.leans(-8.6, -7.4, 10.0, 19.4, 7.6, 39.4, 0.7, P, 1);                         // the back pillar
k.leans(-8.6, 26.0, 10.1, 19.0, 10.2, 19.8, 0.4, P, 1);                        // the sill under them

// ---- the roof and the mast fairing ---------------------------------------------------------------------
k.box(-7.6, 39.4, -8.6, 7.6, 40.6, 13.4, P);
k.leans(-8.6, 13.4, 7.6, 39.4, 6.8, 40.6, 0.6, P, 1);
k.box(-5.2, 40.6, -14.0, 5.2, 45.0, 5.0, P);
k.slab(-5.2, 5.2, 40.6, 9.0, 45.0, 5.0, 0.8, P, -1);
k.leans(-14.0, 5.0, 5.2, 40.6, 4.2, 45.0, 0.8, P);
k.box(-4.2, 45.0, -10.0, 4.2, 45.6, 3.0, P);
k.box(4.6, 42.0, -2.0, 5.3, 43.4, -0.4, C.BLACK);                             // the fuel cap
// the mast, the swashplate on it
k.box(-1.0, 45.6, -1.0, 1.0, 52.6, 1.0, C.STEEL);
k.box(-2.6, 47.5, -2.6, 2.6, 49.0, 2.6, C.BUMPER);
k.box(-1.8, 46.4, -1.8, 1.8, 47.5, 1.8, C.DARK_STEEL);

// ---- behind the cabin: the cowl over the engine, the engine, its frame -----------------------------------
// the cowl's top falls from the fairing to the tail cone, its sides close in
k.slab(-5.2, 5.2, 45.0, -14.0, 33.4, -32.6, 0.8, P, -1);
// the broad cowl under that ridge, in short lengths so its sides follow the falling top
const cowlX = z => lerp(9.4, 4.4, (-8.6 - z) / 24.0), cowlY = z => lerp(40.6, 33.4, (-8.6 - z) / 24.0);
for (let z = -8.6; z > -32.59; z -= 3.0) {
    const zb = z - 3.0, xa = cowlX(z), xb = cowlX(zb);
    k.walls(26.4, (cowlY(z) + cowlY(zb)) / 2, xa, z, xb, zb, 0.8, P);
    k.slab(-Math.min(xa, xb), Math.min(xa, xb), cowlY(z), z, cowlY(zb), zb, 0.8, P, -1);
}
k.walls(25.6, 26.6, 9.6, -8.6, 4.6, -32.6, 0.4, PD, 1);
k.sym(() => {
    for (let i = 0; i < 4; i++) {
        k.wall(31.0 + i * 1.4, 31.6 + i * 1.4, cowlX(-12.0), -12.0, cowlX(-20.0), -20.0, 0.1, C.BLACK, 1);   // the louvres
    }
});
k.box(-6.4, 13.0, -24.0, 6.4, 22.0, -10.0, ENGINE);
k.sym(() => {
    for (let i = 0; i < 3; i++) {
        k.box(6.4, 14.0 + i * 2.6, -22.0, 7.8, 15.8 + i * 2.6, -12.0, '#8E9297');          // the cylinders
    }
});
k.disc(-5.6, 5.6, 22.4, -9.2, 5.0, PANEL);                                    // the cooling fan's scroll
k.box(-4.4, 22.0, -26.0, 4.4, 26.6, -14.0, PANEL);                             // the gearbox under the mast
k.beam([-5.6, 14.0, -22.0], [-7.4, 12.6, -31.0], 1.4, 1.4, C.EXHAUST);
k.beams([8.4, 11.0, -8.6], [3.2, 27.4, -33.0], 0.6, 0.6, TUBE);
k.beams([8.4, 25.6, -8.6], [3.4, 30.8, -33.0], 0.6, 0.6, TUBE);
k.beams([8.4, 11.0, -8.6], [6.0, 22.4, -24.0], 0.6, 0.6, TUBE);
k.box(-8.6, 10.4, -9.4, 8.6, 11.4, -8.6, TUBE);

// ---- the tail cone, tapering out to the tail ------------------------------------------------------------
for (let i = 0; i < 10; i++) {
    const z2 = -32.6 - i * 6.8, z1 = z2 - 7.0;
    const t = (i + 0.5) / 10;
    const y = lerp(29.6, 31.2, t), h = lerp(3.6, 1.5, t);
    k.box(-h, y - h * 0.62, z1, h, y + h * 0.62, z2, P);
    k.box(-h * 0.62, y - h, z1, h * 0.62, y + h, z2, P);
}
k.box(-2.0, 32.9, -42.0, 2.0, 33.6, -38.0, PD);
k.box(-0.8, 33.6, -40.6, 0.8, 34.4, -39.2, C.TAIL);                           // the beacon on its top
// the fins up and down, the guard under the lower one, the tail gearbox
k.beam([0, 32.0, -98.0], [0, 44.4, -105.6], 0.8, 5.4, P);
k.beam([0, 29.0, -98.4], [0, 19.4, -103.4], 0.8, 4.2, P);
k.beam([0, 22.4, -99.6], [0, 17.6, -102.4], 0.5, 0.5, TUBE);
k.box(-0.4, 17.2, -103.8, 0.4, 17.8, -101.6, TUBE);
k.box(-0.7, 44.4, -107.8, 0.7, 45.4, -105.8, C.TAIL);
k.box(-0.8, 29.6, -104.6, 3.3, 32.4, -101.4, ENGINE);
k.box(-1.0, 30.4, -101.4, 1.0, 31.8, -98.0, ENGINE);

// ---- the skids on their bowed cross tubes ---------------------------------------------------------------
k.sym(() => {
    k.box(16.6, 0.0, -18.0, 18.4, 1.8, 26.0, TUBE);
    k.beam([17.5, 0.9, 25.6], [17.5, 4.6, 30.6], 1.8, 1.8, TUBE);
    k.box(16.8, 0.2, -19.2, 18.2, 1.6, -18.0, C.BLACK);
    for (const z of [17.0, -12.0]) {
        k.beam([17.5, 1.6, z], [15.6, 7.0, z], 1.4, 1.4, TUBE);
        k.beam([15.6, 7.0, z], [11.0, 9.2, z], 1.4, 1.4, TUBE);
        k.box(16.4, 0.0, z - 1.2, 18.6, 2.2, z + 1.2, C.DARK_STEEL);             // the clamp on the skid
    }
    k.box(16.2, 1.8, 10.0, 18.8, 2.4, 14.0, C.BLACK);                            // the step
});
for (const z of [17.0, -12.0]) {
    k.box(-11.0, 8.6, z - 0.7, 11.0, 9.8, z + 0.7, TUBE);
}

// ---- inside: the console, the cyclic, the pedals, four seats ----------------------------------------------
k.box(-3.4, 11.2, 22.0, 3.4, 21.0, 27.0, PANEL);
k.box(-4.6, 21.0, 21.4, 4.6, 22.4, 27.4, PANEL);                               // the glare shield
for (const [x, y] of [[-2.2, 18.4], [0, 18.4], [2.2, 18.4], [-1.1, 15.8], [1.1, 15.8]]) {
    k.box(x - 0.8, y - 0.8, 21.94, x + 0.8, y + 0.8, 22.0, C.DIAL);
}
k.box(-2.6, 13.2, 21.9, 2.6, 14.2, 22.0, C.BLACK);                             // the radios
k.beam([0, 11.2, 18.0], [0, 20.0, 16.6], 0.8, 0.8, PANEL);                     // the cyclic and its T bar
k.box(-5.6, 20.0, 16.2, 5.6, 20.6, 17.0, PANEL);
k.sym(() => k.box(4.0, 20.0, 16.0, 6.0, 21.2, 17.2, C.BLACK));
k.beam([-0.6, 11.2, 13.0], [-1.2, 15.2, 18.0], 0.5, 0.5, PANEL);               // the collective
for (const x of [-6.0, -3.0, 3.0, 6.0]) {
    k.box(x - 0.6, 11.2, 23.4, x + 0.6, 13.8, 24.0, C.DARK_STEEL);             // the pedals
}
k.sym(() => {
    const x = 4.5;
    // a front seat: a box base, a cushion, a back with its foot clear of the legs behind
    k.box(x - 4.0, 11.2, 9.0, x + 4.0, 13.6, 16.4, SEAT_BASE);
    k.box(x - 3.8, 13.6, 9.4, x + 3.8, 15.24, 16.2, SEAT);
    k.slab(x - 3.8, x + 3.8, 16.6, 10.0, 32.0, 9.0, 1.0, SEAT, -1);
    k.slab(x - 3.9, x + 3.9, 16.4, 9.0, 32.2, 8.0, 0.3, SEAT_BASE, -1);
    // a rear seat against the back wall
    k.box(x - 4.0, 11.2, -7.6, x + 4.0, 14.0, 1.6, SEAT_BASE);
    k.box(x - 3.8, 14.0, -7.2, x + 3.8, 15.74, 1.4, SEAT);
    k.slab(x - 3.8, x + 3.8, 16.0, -6.2, 33.0, -7.6, 1.0, SEAT, -1);
});

// ---- the main rotor: the hub first, so it turns about the upright through it -------------------------------
k.g('rotor');
k.box(-2.5, 52.5, -2.5, 2.5, 55.0, 2.5, PANEL);
k.box(-1.5, 53.0, -8.0, 1.5, 54.5, 8.0, ENGINE);                              // the blade grips
k.box(-0.6, 55.0, -1.4, 0.6, 55.8, 1.4, C.STEEL);                             // the teeter hinge
k.box(-0.3, 49.0, 3.0, 0.3, 53.0, 3.6, C.STEEL);                              // the pitch links
k.box(-0.3, 49.0, -3.6, 0.3, 53.0, -3.0, C.STEEL);
k.box(-2.0, 53.4, 8.0, 2.0, 54.0, 80.0, BLADE);
k.box(-2.0, 53.4, -80.0, 2.0, 54.0, -8.0, BLADE);
k.box(-2.0, 53.38, 76.0, 2.0, 54.02, 79.0, TIP);
k.box(-2.0, 53.38, -79.0, 2.0, 54.02, -76.0, TIP);

// ---- the tail rotor on the left of the tail: its hub first, it turns about the line across -------------------
k.g('tail_rotor');
k.box(3.3, 29.8, -104.2, 4.8, 32.2, -101.8, PANEL);
k.box(3.7, 21.0, -103.6, 4.4, 41.0, -102.4, BLADE);
k.box(3.68, 21.0, -103.62, 4.42, 23.0, -102.38, TIP);
k.box(3.68, 39.0, -103.62, 4.42, 41.0, -102.38, TIP);
