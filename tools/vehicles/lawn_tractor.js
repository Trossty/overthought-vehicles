// John Deere X350, 2015: a ride-on mower. A green bonnet that falls away
// to a black nose with two lamps set in it and the yellow stripe down each
// side; black footrests either side; a big rounded rear wing with the
// high-backed seat on top; the cutting deck slung under the middle with
// its chute out to the right.
const P = 'paint';
const DEG = Math.PI / 180;
const YELLOW = '#F2C200', VINYL = '#1F1F22', DECK = '#2C2E30';
const RW = {y: 4.0, z: -8.0};

// ---- the frame and the front axle -----------------------------------------------------------
k.g('body');
k.box(-3.4, 2.4, -13.0, 3.4, 4.0, 17.6, C.BLACK);
k.box(-5.0, 2.2, 10.8, 5.0, 3.4, 12.2, C.DARK_STEEL);
k.pair(4.0, 2.0, 11.0, 5.0, 4.2, 12.0, C.DARK_STEEL);
k.box(-1.0, 3.4, 10.6, 1.0, 4.6, 12.4, C.DARK_STEEL);

// ---- the bonnet: level over the engine, rounding down to the nose ------------------------------
const HOOD = [[2.6, 10.8], [11.0, 10.7], [14.8, 10.2], [17.0, 9.2], [18.2, 7.6], [18.6, 5.8]];
k.loft(-4.0, 4.0, HOOD, 0.8, P);
k.sym(() => {
    k.loft(4.0, 4.9, HOOD.map(([z, y]) => [z - 0.1, y - 0.3]), 0.8, P);
    k.lean(2.6, 17.4, 4.9, 4.4, 4.9, 10.2, 0.8, P);
    k.box(4.8, 7.6, 3.4, 4.96, 8.2, 16.4, YELLOW);                 // the stripe
    k.box(4.8, 6.6, 12.6, 4.96, 7.0, 16.0, YELLOW);
    // the vents behind the stripe
    for (let i = 0; i < 3; i++) {
        k.box(4.8, 8.8 + i * 0.5, 13.0, 4.96, 9.1 + i * 0.5, 16.2, C.BLACK);
    }
});
// the nose: a black grille, the two lamps over it, a black bumper under
k.box(-4.4, 3.2, 17.4, 4.4, 6.2, 18.6, C.BLACK);
for (let y = 3.6; y < 5.9; y += 0.7) {
    k.box(-3.8, y, 18.6, 3.8, y + 0.3, 18.8, DECK);
}
k.sym(() => k.lamp(1.2, 6.6, 3.4, 7.8, 18.2, C.LAMP, true, {depth: 0.3}));
k.box(-0.6, 7.0, 18.4, 0.6, 8.0, 18.7, YELLOW);                   // the badge
k.box(-5.0, 2.0, 18.6, 5.0, 3.6, 20.0, C.BLACK);
k.pair(4.4, 2.0, 16.8, 5.0, 3.6, 18.6, C.BLACK);

// ---- the dash under the wheel -------------------------------------------------------------------
k.box(-3.6, 5.0, 0.8, 3.6, 10.6, 2.8, C.BLACK);
k.box(-3.2, 10.6, 1.2, 3.2, 11.0, 2.8, VINYL);
k.box(-0.6, 10.2, 0.4, 0.6, 10.6, 0.8, C.STEEL);                  // the key
k.box(1.6, 9.4, 0.5, 2.6, 10.4, 0.8, C.DIAL);                     // the hour meter
k.beam([-2.0, 10.6, 1.8], [-2.4, 12.0, 1.4], 0.4, 0.4, C.BLACK); // the throttle
k.box(-2.8, 11.8, 1.0, -2.0, 12.4, 1.8, YELLOW);
k.pedals(1.6, 3.4, 4.8);

// ---- the footrests --------------------------------------------------------------------------------
k.pair(3.4, 3.6, -1.6, 8.8, 4.4, 7.4, P);
k.pair(3.6, 4.4, -1.0, 8.4, 4.8, 7.0, C.BLACK);
k.sym(() => {
    for (let z = -0.4; z < 6.6; z += 1.4) {
        k.box(4.0, 4.8, z, 8.0, 5.0, z + 0.6, DECK);
    }
});
k.pair(8.4, 3.6, 7.0, 8.8, 5.6, 7.6, P);

// ---- the rear wing ---------------------------------------------------------------------------------
k.box(-8.8, 9.0, -13.8, 8.8, 9.8, -1.6, P);
k.slab(-8.8, 8.8, 9.8, -1.6, 4.6, -0.4, 0.8, P, -1);
k.slab(-8.8, 8.8, 9.8, -13.8, 4.0, -14.8, 0.8, P, -1);
k.sym(() => {
    // the skirt over each wheel, rounded, and a wall inside it
    const arc = [];
    for (let i = 0; i <= 10; i++) {
        const a = (14 + 152 * i / 10) * DEG;
        arc.push([RW.z - Math.cos(a) * 5.4, RW.y + Math.sin(a) * 5.4]);
    }
    k.loft(8.0, 9.6, arc, 0.8, P, 1);
    k.loft(8.4, 9.6, arc.map(([z, y]) => [z, y - 0.2]), 0.3, C.WELL);
    k.box(4.4, 4.4, -13.6, 4.9, 9.0, -1.8, P);
    // the deck lift lever by the driver's left hand
});
k.beam([6.2, 9.8, -2.6], [6.8, 13.4, -3.6], 0.5, 0.5, C.BLACK);
k.box(6.4, 13.2, -4.2, 7.4, 14.0, -3.2, YELLOW);
k.box(-7.4, 9.8, -4.8, -5.6, 10.6, -2.8, C.BLACK);                 // the cup holder
k.box(-7.0, 10.0, -4.4, -6.0, 10.62, -3.2, DECK);
// the hitch plate and the reflectors on the back
k.box(-2.4, 1.6, -15.4, 2.4, 4.4, -14.6, C.BLACK);
k.box(-1.2, 2.6, -16.2, 1.2, 3.2, -15.4, C.BLACK);
k.box(-0.3, 2.0, -15.9, 0.3, 3.8, -15.5, C.STEEL);
k.sym(() => k.box(6.2, 7.0, -14.95, 7.8, 7.8, -14.75, C.TAIL));

// ---- the seat ----------------------------------------------------------------------------------------
k.box(-3.6, 9.8, -9.0, 3.6, 12.0, -4.0, C.DARK_STEEL);
k.box(-4.4, 12.0, -9.8, 4.4, 14.3, -3.0, VINYL);
k.slab(-4.4, 4.4, 14.3, -3.0, 12.6, -2.2, 0.8, VINYL, -1);
k.slab(-4.4, 4.4, 14.0, -9.6, 22.4, -11.4, 1.6, VINYL, -1);
k.slab(-4.6, 4.6, 13.0, -10.6, 21.6, -12.4, 0.4, C.BLACK, 1);
k.box(-0.2, 15.4, -9.82, 0.2, 21.4, -9.6, DECK);                  // its seam
k.pair(4.4, 14.0, -9.6, 5.0, 17.6, -5.4, VINYL);                   // the side bolsters

// ---- the cutting deck --------------------------------------------------------------------------------
k.box(-8.0, 1.0, -1.6, 8.0, 3.0, 6.8, DECK);
k.leans(-1.6, 6.8, 8.0, 1.0, 8.6, 2.6, 0.6, DECK);
k.slab(-8.0, 8.0, 3.0, 6.8, 1.0, 7.8, 0.6, DECK, -1);
k.slab(-8.0, 8.0, 3.0, -1.6, 1.0, -2.6, 0.6, DECK, -1);
k.disc(-4.4, -2.2, 3.0, 2.6, 2.2, C.BLACK);                       // the spindle housings on top
k.box(-3.6, 3.0, 1.8, -3.0, 3.6, 3.4, C.DARK_STEEL);
k.box(3.0, 3.0, 1.8, 3.6, 3.6, 3.4, C.DARK_STEEL);
k.box(-8.6, 1.0, 0.8, -8.0, 2.8, 5.4, DECK);
k.box(-10.4, 1.2, 0.6, -8.6, 2.6, 5.6, C.BLACK);                   // the chute
k.slab(-10.4, -8.6, 2.6, 0.6, 3.2, 5.6, 0.3, C.BLACK, 1);
k.sym(() => {
    k.box(7.2, 0.6, 7.2, 8.0, 1.6, 8.2, C.BLACK);                  // the anti-scalp rollers
    k.box(-0.6, 3.0, -1.0, 0.6, 3.6, 6.0, YELLOW);
});

// ---- the steering column: a black shroud from under the game's wheel down to the bonnet, where a
// boot takes it in; the wheel lies back as a mower's does, so the shroud runs down between the feet
k.g('body');
const col = k.column();
const shroudT = (col.end[1] - 10.9) / -col.dir[1];
k.beam(col.end, col.end.map((v, i) => v + col.dir[i] * shroudT), 1.2, 1.2, C.BLACK);
const foot = col.end.map((v, i) => v + col.dir[i] * shroudT);
k.box(-1.1, 10.8, foot[2] - 1.0, 1.1, 11.5, foot[2] + 0.9, VINYL);
