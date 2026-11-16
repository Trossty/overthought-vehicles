// An aircraft tow bar on two small wheels: a cradle that takes the nose
// wheel, a pin through each cheek to hold it, and a long striped bar with
// an eye on the end for the tug.
const P = 'paint', PD = 'paint_dark';
const PAN = '#3E4044', RAMP = '#8A8D91';
const WY = 2.5, WX = 9.5;

// ---- the axle -----------------------------------------------------------------------
k.g('frame');
k.box(-WX, 2, -0.6, WX, 3, 0.6, C.TRIM);
k.pair(4.2, 2.4, -1.0, 6.0, 3.6, 1.0, PD);
k.sym(() => {
    const pts = [];
    for (const a of [20, 55, 90, 125, 160]) {
        pts.push([3.4 * Math.cos(a * Math.PI / 180), WY + 3.4 * Math.sin(a * Math.PI / 180)]);
    }
    k.loft(8.0, 11.0, pts, 0.4, PD, 1);
    k.box(7.6, 3.0, -0.4, 8.2, 5.8, 0.4, PD);
});

// ---- the cradle ---------------------------------------------------------------------
k.g('cradle');
k.box(-2.6, 2.6, -4.5, 2.6, 4, 4.5, PAN);
k.pair(2.6, 2.6, -4.5, 4.2, 7.5, 4.5, P);
k.box(-2.6, 4, 3.6, 2.6, 6.5, 4.5, P);
k.slab(-2.6, 2.6, 3.3, -4.5, 2.2, -7.5, 0.6, RAMP);
k.sym(() => {
    // the pin and its ring, a chamfer on the cheek
    k.box(4.2, 5.2, -1.2, 4.6, 6.4, 1.2, C.CHROME);
    k.box(4.6, 5.5, -0.3, 5.3, 6.1, 0.3, C.CHROME);
    k.slab(2.6, 4.2, 7.5, -3.0, 5.6, -4.5, 0.5, P);
    k.box(2.3, 4.0, -4.0, 2.6, 4.4, 3.6, C.RUBBER);
});

// ---- the bar --------------------------------------------------------------------------
k.g('bar');
k.box(-1, 3.4, 4.5, 1, 5, 36, P);
for (const z of [10, 18, 26]) {
    k.box(-1.05, 3.35, z, 1.05, 5.05, z + 2, C.BLACK);
}
k.sym(() => k.beam([2.4, 4.0, 4.6], [1.0, 4.0, 14.0], 1.2, 1.2, P));
// the handle to steer it by
k.box(-1.2, 4, 5, 1.2, 8.5, 7.4, PD);
k.box(-0.3, 8.5, 5.9, 0.3, 9.6, 6.5, C.CHROME);
k.box(-2.4, 9.4, 5.9, 2.4, 10, 6.5, C.CHROME);
k.pair(1.6, 9.3, 5.8, 2.5, 10, 6.6, C.BLACK);
// the eye for the tug's pin
k.box(-1.4, 3.6, 36, 1.4, 5.0, 37.0, C.DARK_STEEL);
k.pair(1.5, 3.2, 36.6, 2.4, 5.4, 39.6, C.CHROME);
k.box(-1.5, 3.2, 38.7, 1.5, 5.4, 39.6, C.CHROME);
k.box(-1.6, 5.4, 36.6, 1.6, 6.2, 39.0, C.BLACK);
