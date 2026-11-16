// A tow dolly: an axle with a pan for each front wheel of the car behind,
// a ramp up into each pan and a strap over it, mudguards with the lamps on,
// and a single beam forward to the coupling.
const P = 'paint', PD = 'paint_dark';
const PAN = '#3E4044', RAMP = '#8A8D91', STRAP = '#E8C21B';
const WY = 5.0, WX = 22.0;

// ---- the axle and the frame ---------------------------------------------------------
k.g('frame');
k.box(-WX, 4.2, -0.8, WX, 5.6, 0.8, C.TRIM);
k.box(-19.5, 5.6, -2, 19.5, 6.6, 2, P);
k.box(-8.5, 5.6, 6, 8.5, 7, 8, P);
k.pair(7.4, 5.6, 2, 8.5, 6.8, 6, PD);
// the turntable the pans swing on
k.box(-3.4, 6.6, -3.0, 3.4, 7.0, 3.0, C.DARK_STEEL);
k.box(-0.7, 7.0, -0.7, 0.7, 7.5, 0.7, C.CHROME);

// ---- the pans -----------------------------------------------------------------------
k.g('pans');
k.sym(() => {
    k.box(8.5, 6.6, -9, 18.5, 8, 9, PAN);
    for (const z of [-6.5, -3.5, 3.5, 6.5]) {
        k.box(9.0, 8, z - 0.35, 18.0, 8.12, z + 0.35, C.TRIM);
    }
    // a stop across the front, a lip along each side
    k.box(8.5, 8, 7.5, 18.5, 10.5, 9, P);
    k.slab(8.5, 18.5, 8, 6.2, 10.5, 7.5, 0.5, P);
    k.box(18.5, 8, -9, 19.3, 10, 9, P);
    k.box(7.7, 8, -9, 8.5, 10, 9, P);
    // the ramp, ribbed for grip
    k.slab(8.5, 18.5, 7.3, -9.0, 5.6, -13.0, 0.8, RAMP);
    for (const z of [-10.0, -11.3]) {
        k.slab(9.0, 18.0, 7.1, z, 6.85, z - 0.6, 0.12, PAN, 1);
    }
    // the strap over the tyre, its ratchet on the outside lip
    k.box(9.0, 8, -1.2, 18.0, 8.4, 1.2, STRAP);
    k.box(19.3, 8.4, -1.6, 20.0, 9.8, 1.6, C.DARK_STEEL);
    k.box(19.5, 9.8, -0.3, 19.9, 10.2, 2.4, C.CHROME);
    k.box(7.3, 8.6, -0.8, 7.7, 9.6, 0.8, C.CHROME);
});

// ---- the mudguards and the lamps ----------------------------------------------------
k.g('mudguards');
k.sym(() => {
    const pts = [];
    for (const a of [8, 36, 63, 90, 117, 144, 172]) {
        pts.push([6.5 * Math.cos(a * Math.PI / 180), WY + 6.5 * Math.sin(a * Math.PI / 180)]);
    }
    k.loft(19.5, 24.5, pts, 0.6, PD, 1);
    k.box(19.5, 6.6, -0.5, 20.3, 11.4, 0.5, PD);
    // the lamp board behind the wheel
    k.box(19.8, 8.6, -7.3, 24.2, 11.2, -6.6, C.BLACK);
    k.box(22.6, 9.0, -7.45, 23.9, 10.8, -7.3, C.AMBER);
    k.box(20.1, 9.0, -7.45, 22.6, 10.8, -7.3, C.TAIL);
    k.box(20.6, 9.4, -7.5, 22.1, 10.4, -7.45, C.TAIL_GLOW);
    k.box(24.2, 9.4, -3.0, 24.5, 10.2, -1.6, C.AMBER_SIDE);
});

// ---- the drawbar ------------------------------------------------------------------------
k.g('drawbar');
k.box(-1.2, 6, 8, 1.2, 8, 38, P);
k.sym(() => k.beam([7.6, 6.8, 8.0], [1.2, 6.8, 25.0], 1.4, 1.5, P));
// the lever that locks the tilting bed
k.beam([0, 8.0, 14.8], [0, 12.2, 13.6], 0.8, 0.8, PD);
k.box(-0.7, 11.8, 12.6, 0.7, 13.0, 14.0, C.BLACK);
// safety chains hung along the beam
k.pair(1.2, 6.4, 30.0, 1.5, 7.2, 36.0, C.CHROME);
k.coupler(41.4, 6.0);
