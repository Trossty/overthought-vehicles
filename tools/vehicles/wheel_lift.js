// The wheel lift a tow truck carries: a cross bar on the end of the boom,
// an L-shaped arm each side that cups a tyre, a strap over each, and an
// amber marker on each end.
const P = 'paint', PD = 'paint_dark';
const FORK = '#3B3B40', STRAP = '#E8C21B', LENS = '#E0F0A020';

// ---- the cross bar --------------------------------------------------------------------
k.g('bar');
k.box(-20, 4, -1.5, 20, 6, 1.5, P);
for (const x of [3.6, 6.2]) {
    k.pair(x, 4.0, -1.56, x + 1.3, 6.02, 1.56, C.BLACK);
}
k.pair(19, 5, -1, 20.4, 6.6, 1, LENS);
k.pair(18.4, 4.6, -1.2, 19.0, 6.9, 1.2, C.DARK_STEEL);

// ---- the arms -----------------------------------------------------------------------
k.g('arms');
k.sym(() => {
    k.box(17, 4, -9, 19, 6, 9, P);
    k.box(8, 4, -9, 17, 5.4, -7, FORK);
    k.box(8, 4, 7, 17, 5.4, 9, FORK);
    k.slab(8, 17, 5.4, -7.0, 4.2, -5.6, 0.5, FORK);
    k.slab(8, 17, 5.4, 7.0, 4.2, 5.6, 0.5, FORK);
    // the pin the arm swings on
    k.box(17.4, 6, -0.6, 18.6, 6.5, 0.6, C.CHROME);
    // the strap, the post its ratchet stands on
    k.box(9, 5.4, -1, 16, 6, 1, STRAP);
    k.box(12, 5.4, -8.6, 13, 12, -7.6, STRAP);
    k.box(11.7, 9.6, -8.9, 13.3, 11.2, -7.3, C.DARK_STEEL);
    k.box(13.3, 10.1, -8.4, 14.6, 10.6, -7.8, C.CHROME);
    k.box(7.6, 4.2, -8.8, 8.0, 5.2, -7.2, C.CHROME);
    k.box(7.6, 4.2, 7.2, 8.0, 5.2, 8.8, C.CHROME);
});

// ---- the boom -----------------------------------------------------------------------
k.g('boom');
k.box(-2, 4.4, 1.5, 2, 7.4, 18, PD);
k.box(-2.3, 4.2, 1.5, 2.3, 7.6, 3.4, P);
k.box(-1.5, 4.9, 8.0, 1.5, 6.9, 14.0, C.CHROME);
k.pair(2.0, 5.4, 9.6, 2.3, 6.4, 10.6, C.CHROME);
k.box(-2.6, 7.4, 14, 2.6, 9.4, 20, C.CHROME);
k.box(-1.9, 9.4, 15.0, 1.9, 9.9, 19.0, C.DARK_STEEL);
