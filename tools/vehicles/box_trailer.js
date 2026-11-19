// A single-axle tub trailer: a ribbed steel tub on a galvanised frame, a
// drop tailgate, round mudguards, an A-frame drawbar with a jockey wheel.
const P = 'paint', PD = 'paint_dark';
const GALV = C.BUMPER, WOOD = '#6E5536', WOOD_LINE = '#5C4529';
const X = 12.0, Y0 = 9.3, TOP = 18.8, BACK = -19.5, FRONT = 11.8;
const WZ = -3.0, WY = 5.9;

// ---- the frame ---------------------------------------------------------------------
k.g('frame');
k.pair(7.6, 8.2, BACK + 0.4, 9.0, Y0, FRONT, C.DARK_STEEL);
for (const z of [-17.5, -9.5, 3.5, 10.2]) {
    k.box(-7.6, 8.4, z - 0.5, 7.6, Y0, z + 0.5, C.DARK_STEEL);
}
k.box(-12.6, WY - 0.7, WZ - 0.8, 12.6, WY + 0.7, WZ + 0.8, C.TRIM);
k.pair(7.4, WY + 0.7, WZ - 3.4, 9.2, 8.2, WZ + 3.4, C.TRIM);
// the drawbar: two arms meeting at the coupling
k.sym(() => k.beam([8.3, 8.9, FRONT], [1.4, 8.9, 23.6], 1.5, 1.4, GALV));
k.box(-1.2, 8.2, 20.6, 1.2, 9.6, 26.4, GALV);
k.box(-6.4, 8.3, 14.6, 6.4, 9.5, 15.8, GALV);
k.coupler(29.63, 8.0);
k.jockey(2.9, 21.6, 12.2, 4.8);
k.box(-0.3, 7.4, 22.0, 0.3, 8.2, 25.6, C.CHROME);

// ---- the tub -------------------------------------------------------------------------
k.g('body');
k.box(-X, Y0, BACK, X, 10.8, FRONT, PD);
k.box(-11, 10.8, BACK + 0.9, 11, 11.2, FRONT - 0.9, WOOD);
for (const x of [-7.3, -3.65, 0, 3.65, 7.3]) {
    k.box(x - 0.06, 11.2, BACK + 0.9, x + 0.06, 11.24, FRONT - 0.9, WOOD_LINE);
}
k.pair(11, 10.8, BACK, X, TOP, FRONT, P);
k.box(-11, 10.8, FRONT - 0.9, 11, TOP, FRONT, P);
// pressed ribs, corner posts and a rolled rail round the top
for (const z of [-13.4, -8.9, 2.2, 6.9]) {
    k.pair(X, 11.6, z - 0.45, X + 0.25, 18.2, z + 0.45, PD);
}
for (const x of [-6.6, 0, 6.6]) {
    k.box(x - 0.45, 11.6, FRONT, x + 0.45, 18.2, FRONT + 0.25, PD);
}
k.pair(X - 0.9, 10.8, FRONT - 0.9, X + 0.2, TOP, FRONT + 0.2, GALV);
k.pair(X - 0.9, 10.8, BACK - 0.2, X + 0.2, TOP, BACK + 0.9, GALV);
k.pair(10.8, 18.4, BACK - 0.2, X + 0.35, 19.4, FRONT + 0.2, GALV);
k.box(-10.8, 18.4, FRONT - 1.0, 10.8, 19.4, FRONT + 0.35, GALV);
// lashing hooks under the rail
for (const z of [-15.6, -6.0, 5.0]) {
    k.pair(X + 0.25, 16.6, z - 0.4, X + 0.6, 17.4, z + 0.4, C.CHROME);
}
// the tailgate: hinged along the floor, a latch at each top corner
k.box(-11.1, 10.8, BACK, 11.1, 17.9, BACK + 0.85, P);
k.box(-11.1, 17.5, BACK - 0.15, 11.1, 18.3, BACK + 0.95, GALV);
for (const x of [-6.2, 0, 6.2]) {
    k.box(x - 0.45, 11.6, BACK - 0.22, x + 0.45, 17.5, BACK, PD);
}
k.sym(() => {
    k.box(7.6, 10.5, BACK - 0.3, 9.4, 11.3, BACK, C.DARK_STEEL);
    k.box(10.2, 16.4, BACK - 0.35, 11.6, 17.3, BACK, C.CHROME);
    k.box(10.5, 15.2, BACK - 0.5, 10.9, 16.6, BACK - 0.3, C.BLACK);
});

// ---- the mudguards ------------------------------------------------------------------
k.sym(() => {
    const pts = [];
    for (const a of [12, 40, 66, 90, 114, 140, 168]) {
        pts.push([WZ + 7.3 * Math.cos(a * Math.PI / 180), WY + 7.3 * Math.sin(a * Math.PI / 180)]);
    }
    k.loft(X, 16.2, pts, 0.6, C.BLACK, 1);
    k.box(X, 9.6, WZ - 0.5, 13.0, 12.9, WZ + 0.5, C.TRIM);
    k.box(15.6, 11.6, WZ + 7.3, 16.2, 12.3, WZ + 7.7, C.AMBER_SIDE);
});

// ---- the lamps and the plate -------------------------------------------------------
k.g('lamps');
k.box(-11.8, 8.3, BACK - 0.5, 11.8, 9.3, BACK + 0.4, C.BLACK);
k.sym(() => {
    k.box(6.9, 11.5, BACK - 0.35, 11.3, 14.5, BACK, C.BLACK);
    k.box(9.9, 11.8, BACK - 0.5, 11.0, 14.2, BACK - 0.35, C.AMBER);
    k.box(7.2, 11.8, BACK - 0.5, 9.9, 14.2, BACK - 0.35, C.TAIL);
    k.box(7.7, 12.4, BACK - 0.58, 9.4, 13.6, BACK - 0.5, C.TAIL_GLOW);
    k.box(8.2, 8.5, BACK - 0.58, 10.6, 9.1, BACK - 0.5, '#B5221E');
    k.box(11.2, 11.6, FRONT + 0.2, 12.0, 12.4, FRONT + 0.3, '#DADAD4');
});
k.box(-3.7, 11.7, BACK - 0.3, 3.7, 14.3, BACK, C.BLACK);
k.plate(-3.4, 11.9, 3.4, 14.1, BACK - 0.3, false);
