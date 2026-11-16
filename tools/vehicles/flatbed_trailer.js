// A twin-axle car transporter: a low steel deck between two side rails, the
// wheels tucked under it, loading ramps slid in under the tail, a headboard
// with a winch, and a long A-frame out to the coupling.
const P = 'paint', PD = 'paint_dark', PL = 'paint_light';
const GALV = C.BUMPER, PLATE = '#3E4044', TREAD = '#4A4D52', RAMP = '#8A8D91';
const BACK = -50, FRONT = 46, DECK = 12.8;

// ---- the frame --------------------------------------------------------------------------
k.g('frame');
k.pair(14.5, 9.5, BACK, 17, 13, FRONT, P);
k.pair(6.0, 9.4, BACK + 1, 7.6, 11.8, FRONT, PD);
for (let z = -46; z <= 38; z += 12) {
    k.box(-14.5, 10.2, z, 14.5, 11.8, z + 1.5, PD);
}
// the axles on their beams
for (const z of [-14, -3]) {
    k.box(-13, 3.8, z - 0.7, 13, 5.2, z + 0.7, C.TRIM);
    k.pair(5.6, 5.2, z - 0.9, 8.0, 9.5, z + 0.9, PD);
}
k.pair(5.8, 7.6, -16.5, 7.8, 8.6, -0.5, C.TRIM);

// ---- the deck ---------------------------------------------------------------------------
k.g('deck');
k.box(-14.5, 11.8, BACK, 14.5, DECK, FRONT, PLATE);
// a strip of chequer plate under each line of wheels, lashing rings beside them
k.pair(5.6, DECK, BACK + 0.6, 13.4, DECK + 0.08, FRONT - 2.4, TREAD);
for (let z = -44; z <= 40; z += 6) {
    k.pair(5.6, DECK + 0.08, z, 13.4, DECK + 0.14, z + 0.5, PLATE);
}
for (const z of [-40, -18, 6, 30]) {
    k.pair(13.6, DECK, z - 0.6, 14.3, DECK + 0.3, z + 0.6, C.CHROME);
}
// a kerb along each side, stake pockets and marker lamps on the rails
k.pair(16.6, 13, BACK, 17.6, 14, FRONT, PL);
for (let z = -48; z <= 32; z += 16) {
    k.pair(17.6, 10, z, 18.2, 12.5, z + 1.2, C.BLACK);
}
for (const z of [-40, -8, 24]) {
    k.pair(17, 10.6, z, 17.15, 11.6, z + 1.6, C.AMBER_SIDE);
}

// ---- the tail ---------------------------------------------------------------------------
k.box(-17, 9.2, BACK - 1, 17, 11.6, BACK, PD);
k.sym(() => {
    k.box(10.2, 9.4, BACK - 1.15, 16.7, 11.4, BACK - 1, C.BLACK);
    k.box(14.6, 9.6, BACK - 1.3, 16.5, 11.2, BACK - 1.15, C.AMBER);
    k.box(12.0, 9.6, BACK - 1.3, 14.6, 11.2, BACK - 1.15, C.TAIL);
    k.box(12.5, 9.9, BACK - 1.36, 14.1, 10.9, BACK - 1.3, C.TAIL_GLOW);
    k.box(10.4, 9.6, BACK - 1.3, 12.0, 11.2, BACK - 1.15, C.REVERSE);
    // the ramp in its slot, a handle on the end of it
    k.box(4.8, 8.4, BACK - 0.6, 13.2, 9.5, -30, PD);
    k.box(5.2, 8.6, BACK - 0.9, 12.8, 9.3, -30.4, RAMP);
    k.box(8.0, 8.7, BACK - 1.3, 10.0, 9.2, BACK - 0.9, C.BLACK);
});
k.plate(-3.5, 9.4, 3.5, 11.4, BACK - 1, false);

// ---- the headboard and the winch --------------------------------------------------
k.g('headboard');
k.box(-15, 13, 44, 15, 15, FRONT, P);
k.pair(3.5, 15, 44, 5.5, 23, FRONT, P);
k.box(-5.5, 22, 44, 5.5, 24, FRONT, P);
k.sym(() => k.beam([14.0, 15.0, 45.0], [5.6, 22.6, 45.0], 1.4, 1.4, P));
k.box(-3.5, 16.2, 42.8, 3.5, 21.0, 44.0, C.TRIM);
k.disc(-2.4, 2.4, 18.6, 42.0, 1.9, RAMP);
k.pair(2.4, 16.6, 40.2, 2.9, 20.6, 43.6, C.DARK_STEEL);
k.box(2.9, 18.3, 41.7, 4.6, 18.9, 42.3, C.CHROME);
k.box(4.4, 18.9, 41.6, 5.0, 20.6, 42.4, C.BLACK);
k.box(-0.3, 16.9, 39.2, 0.3, 17.5, 40.6, C.CHROME);

// ---- the drawbar ------------------------------------------------------------------------
k.g('drawbar');
k.sym(() => k.beam([14.2, 10.5, FRONT], [1.6, 10.5, 70.6], 1.8, 1.8, P));
k.box(-2, 9.6, 66, 2, 11.4, 73, P);
k.box(-9.6, 9.8, 52.4, 9.6, 11.2, 53.8, PD);
// a locker for the straps
k.box(-5.6, 11.2, 54.4, 5.6, 15.4, 60.0, C.TRIM);
k.box(-5.8, 15.0, 54.2, 5.8, 15.6, 60.2, C.BLACK);
k.box(-0.8, 13.6, 54.2, 0.8, 14.3, 54.4, C.CHROME);
k.coupler(75.6, 8.6);
k.jockey(3.7, 62.1, 13.4, 3.6);
k.box(-0.3, 8.8, 66.4, 0.3, 9.6, 71.6, C.CHROME);
