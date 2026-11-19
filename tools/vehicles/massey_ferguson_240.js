// Massey Ferguson 240, 1980s: a small utility tractor. Red tinwork (the
// bonnet and the flat-top wings) over grey castings: the engine, the
// gearbox and the back axle are the frame. Two square lamps sit in the
// top of a black grille, the exhaust stands up through the bonnet on the
// right, the air cleaner on the left; the driver sits on a pan seat over
// the axle with the three-point linkage behind and a two-post frame over.
const P = 'paint';
const CAST = '#55575C', CAST_DARK = '#3A3B3F', VINYL = '#1F1F22', FRAME = '#1E1F22';

// ---- the front axle and its support -------------------------------------------------
k.g('body');
k.box(-4.4, 4.6, 21.0, 4.4, 11.0, 29.6, CAST);                    // the axle support casting
k.box(-5.6, 4.8, 29.6, 5.6, 9.8, 31.6, CAST_DARK);                 // the weight frame on its nose
k.box(-4.6, 5.4, 31.6, 4.6, 9.2, 32.2, CAST_DARK);
for (const x of [-3.2, 0, 3.2]) {
    k.box(x - 0.5, 8.4, 31.6, x + 0.5, 9.0, 31.9, C.STEEL);       // the bolts holding it
}
k.box(-1.0, 6.2, 32.2, 1.0, 8.2, 33.0, CAST_DARK);                // the towing eye
k.box(-0.5, 6.8, 33.0, 0.5, 7.6, 33.4, C.STEEL);
// the beam across, its pivot in the middle, the king pins and the track rod behind
k.box(-9.4, 5.0, 18.2, 9.4, 7.0, 19.8, C.DARK_STEEL);
k.box(-1.8, 4.4, 17.4, 1.8, 7.6, 20.6, CAST);
k.pair(8.2, 4.0, 18.3, 9.2, 8.6, 19.7, C.DARK_STEEL);
k.pair(8.0, 8.6, 18.6, 9.0, 9.2, 19.4, C.STEEL);
k.box(-8.6, 5.3, 16.6, 8.6, 5.9, 17.2, C.STEEL);
k.pair(8.0, 5.0, 16.4, 8.6, 6.2, 18.6, C.DARK_STEEL);

// ---- the engine --------------------------------------------------------------------------
k.box(-3.8, 6.5, 3.0, 3.8, 13.2, 21.0, CAST);
k.box(-3.2, 4.6, 5.0, 3.2, 6.5, 20.0, CAST_DARK);                  // the sump
// the injection pump on the left, the filter and the starter on the right
k.box(3.8, 8.0, 10.0, 5.0, 11.0, 15.0, CAST);
k.box(4.0, 11.0, 11.0, 4.8, 12.2, 14.0, CAST_DARK);
k.box(-5.0, 7.0, 14.2, -3.8, 10.2, 16.2, '#C2A23C');
k.box(-5.2, 6.6, 4.0, -3.8, 8.6, 9.0, C.BLACK);
k.box(-4.6, 11.0, 8.0, -3.8, 12.4, 20.0, C.EXHAUST);               // the manifold
// the fan behind the radiator
k.disc(-3.4, 3.4, 10.6, 23.8, 3.2, C.BLACK);

// ---- the bonnet ----------------------------------------------------------------------------
k.box(-4.4, 20.0, 2.4, 4.4, 20.8, 28.6, P);
k.leans(2.4, 28.6, 5.4, 19.6, 4.4, 20.8, 0.8, P, -1);
k.pair(4.6, 13.2, 2.4, 5.4, 19.8, 28.6, P);
// the louvres on its sides, near the front
k.sym(() => {
    for (let i = 0; i < 4; i++) {
        k.box(5.4, 14.6 + i * 1.2, 21.6, 5.5, 15.1 + i * 1.2, 27.2, C.BLACK);
    }
});
// the grille: a red surround, a black mesh with grey bars, the lamps in its top corners
k.box(-5.4, 19.4, 28.6, 5.4, 20.8, 29.4, P);
k.pair(4.6, 10.2, 28.6, 5.4, 19.4, 29.4, P);
k.box(-5.4, 9.6, 28.6, 5.4, 10.6, 29.4, CAST_DARK);
k.box(-4.6, 10.6, 28.2, 4.6, 19.4, 28.8, C.BLACK);
for (let y = 11.2; y < 15.8; y += 1.15) {
    k.box(-4.6, y, 28.8, 4.6, y + 0.45, 29.0, CAST_DARK);
}
k.box(-0.3, 10.6, 28.8, 0.3, 15.8, 29.0, CAST_DARK);
k.box(-4.6, 15.8, 28.8, 4.6, 16.2, 29.0, CAST);
k.sym(() => k.lamp(1.8, 16.8, 4.0, 18.8, 28.8, C.LAMP, true));
k.box(-0.6, 17.6, 28.8, 0.6, 18.6, 29.1, C.CHROME);                // the badge between them
// the filler cap, the exhaust through the right of the bonnet, the air cleaner on the left
k.box(-0.9, 20.8, 4.6, 0.9, 21.4, 6.4, C.BLACK);
k.box(-4.0, 20.8, 21.8, -2.0, 21.2, 23.8, C.BLACK);
k.box(-3.8, 21.2, 22.0, -2.2, 27.6, 23.6, C.EXHAUST);
k.box(-3.4, 27.6, 22.4, -2.6, 31.6, 23.2, C.EXHAUST);
k.slab(-3.6, -2.4, 31.6, 22.2, 32.4, 23.6, 0.3, C.BLACK, 1);      // its rain flap
k.box(2.6, 20.8, 18.4, 3.6, 22.4, 19.4, C.BLACK);
k.box(1.8, 22.4, 17.6, 4.4, 25.0, 20.2, '#2E3033');
k.box(1.6, 25.0, 17.4, 4.6, 25.6, 20.4, C.BLACK);

// ---- the gearbox and the back axle ---------------------------------------------------------
k.box(-4.6, 6.0, -17.0, 4.6, 15.0, 3.0, CAST);
k.box(-3.8, 15.0, -9.0, 3.8, 15.6, 2.4, CAST_DARK);
k.box(-4.2, 15.0, -18.4, 4.2, 17.4, -9.0, CAST_DARK);              // the lift cover
k.pair(4.6, 7.8, -12.4, 8.6, 12.2, -7.6, CAST);                    // the trumpets out to the hubs
k.pair(4.6, 8.4, -13.6, 6.2, 11.6, -6.4, CAST_DARK);               // the brake housings
k.pair(7.8, 7.2, -13.0, 8.6, 12.8, -7.0, CAST_DARK);
// the footplates, ribbed, from the gearbox out to the wings
k.pair(4.6, 10.0, -1.0, 8.6, 10.6, 9.0, CAST_DARK);
k.sym(() => {
    for (let z = 0; z < 8.6; z += 2) {
        k.box(4.8, 10.6, z, 8.4, 10.8, z + 0.6, C.BLACK);
    }
});
// the gear levers by the driver's right leg, the hydraulic quadrant by the seat
k.beam([-4.6, 13.4, -4.0], [-5.6, 19.6, -6.0], 0.6, 0.6, C.BLACK);
k.box(-6.1, 19.4, -6.6, -5.1, 20.4, -5.6, C.BLACK);
k.beam([-4.6, 13.4, -2.0], [-5.0, 17.6, -2.8], 0.5, 0.5, C.BLACK);
k.box(-5.4, 17.4, -3.2, -4.6, 18.2, -2.4, '#C0201C');
k.box(-5.6, 15.4, -16.0, -4.2, 18.6, -11.0, CAST_DARK);
k.beam([-5.6, 18.0, -14.6], [-6.4, 21.0, -13.0], 0.5, 0.5, C.BLACK);
k.beam([-5.6, 18.0, -12.6], [-6.4, 20.4, -11.4], 0.5, 0.5, C.BLACK);

// ---- the wings: flat tops, a slope down at each end, lamps on their corners ------------------
k.sym(() => {
    k.box(8.2, 21.6, -19.6, 14.8, 22.4, -2.4, P);
    k.slab(8.2, 14.8, 22.4, -2.6, 18.0, 1.6, 0.8, P, -1);
    k.box(8.2, 10.6, 1.0, 14.8, 18.2, 1.8, P);
    k.slab(8.2, 14.8, 22.4, -19.4, 17.8, -22.8, 0.8, P, -1);
    k.box(8.2, 12.6, -23.4, 14.8, 18.0, -22.6, P);
    k.box(7.6, 12.2, -22.6, 8.2, 22.4, 1.0, P);                    // the inner wall
    k.box(14.6, 20.8, -19.6, 15.0, 22.6, -2.4, P);                 // the rolled outer edge
    // an indicator and a side lamp on the front, a tail lamp on the back
    k.box(12.4, 18.6, 1.8, 14.2, 19.8, 2.2, C.AMBER);
    k.box(9.0, 18.6, 1.8, 10.8, 19.8, 2.2, C.LAMP);
    k.lamp(11.0, 15.2, 13.8, 16.8, -23.4, C.TAIL, false);
    k.box(9.0, 15.4, -23.8, 10.4, 16.6, -23.4, C.AMBER);
});
k.plate(9.4, 13.0, 13.4, 14.8, -23.4, false);

// ---- the dash and the seat ----------------------------------------------------------------------
// the column housing and the instrument panel on its back; the game hangs the wheel over it
k.box(-3.4, 13.0, 0.2, 3.4, 23.4, 3.2, CAST_DARK);
k.box(-3.0, 23.4, 0.6, 3.0, 23.8, 2.8, C.BLACK);
k.box(-2.8, 19.6, -0.1, 2.8, 22.8, 0.2, C.BLACK);
k.box(-2.4, 20.0, -0.15, -0.4, 22.0, -0.1, C.DIAL);
k.box(0.4, 20.0, -0.15, 2.4, 22.0, -0.1, C.DIAL);
k.box(-1.5, 20.8, -0.2, -1.3, 21.8, -0.15, C.NEEDLE);
k.box(1.3, 20.8, -0.2, 1.5, 21.8, -0.15, C.NEEDLE);
k.box(-0.6, 17.4, -0.2, 0.6, 18.4, 0.2, C.STEEL);                 // the key
// the pan seat on its post, a low back behind it
k.box(-1.0, 15.6, -14.0, 1.0, 16.8, -10.6, C.DARK_STEEL);
k.box(-4.2, 16.8, -15.8, 4.2, 17.4, -8.6, C.DARK_STEEL);
k.box(-4.0, 17.4, -15.6, 4.0, 19.2, -9.4, VINYL);
k.slab(-4.0, 4.0, 19.2, -9.4, 17.6, -8.6, 0.8, VINYL, -1);
k.slab(-4.0, 4.0, 19.0, -15.6, 25.0, -17.2, 1.2, VINYL, -1);
k.slab(-4.2, 4.2, 18.2, -16.4, 24.0, -17.8, 0.4, C.DARK_STEEL, 1);

// ---- the frame over the driver --------------------------------------------------------------------
k.sym(() => {
    k.box(6.8, 11.0, -22.6, 8.0, 36.6, -21.4, FRAME);
    k.beam([7.4, 36.0, -22.0], [6.0, 38.6, -22.0], 1.2, 1.2, FRAME);
    k.box(6.6, 11.0, -23.0, 8.2, 13.0, -21.0, CAST_DARK);          // where it bolts to the axle
    k.box(6.6, 24.6, -23.0, 8.2, 25.4, -21.0, C.DARK_STEEL);       // its fold hinge
});
k.box(-6.4, 37.8, -22.6, 6.4, 39.0, -21.4, FRAME);

// ---- the linkage, the PTO and the drawbar behind ----------------------------------------------------
k.beams([3.6, 8.0, -16.6], [4.4, 5.4, -27.0], 0.8, 1.0, C.DARK_STEEL);
k.pair(3.8, 4.6, -27.8, 5.0, 6.2, -26.4, C.DARK_STEEL);
k.beams([3.4, 17.0, -15.0], [3.6, 16.0, -21.4], 0.8, 1.2, CAST_DARK);
k.beams([3.6, 15.6, -21.0], [4.2, 7.2, -23.6], 0.6, 0.6, C.STEEL);
k.box(-1.4, 12.6, -18.6, 1.4, 15.0, -16.8, CAST);
k.beam([0, 14.0, -17.6], [0, 13.0, -26.6], 0.9, 0.9, C.STEEL);
k.box(-0.8, 13.0, -23.4, 0.8, 14.4, -21.0, C.DARK_STEEL);
k.disc(-0.7, 0.7, 9.0, -18.2, 0.8, C.STEEL);
k.box(-0.8, 8.2, -19.2, 0.8, 9.8, -17.0, C.STEEL);
k.box(-2.6, 10.4, -19.8, 2.6, 10.8, -17.0, CAST_DARK);             // the PTO guard
k.pair(2.2, 8.0, -19.8, 2.6, 10.8, -19.4, CAST_DARK);
k.box(-1.4, 4.6, -26.0, 1.4, 5.4, -15.0, C.DARK_STEEL);
k.box(-0.4, 5.4, -25.0, 0.4, 7.0, -24.2, C.STEEL);
k.pair(3.0, 4.6, -18.0, 3.8, 7.0, -16.0, C.DARK_STEEL);
