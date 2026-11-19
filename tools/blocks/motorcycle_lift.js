// A scissor table for a bike: a long checker-plate deck with a wheel vise
// at the south end, over a floor frame it folds down on to. Raised, two
// pairs of legs cross under it and a ram pushes between them; the foot
// pump stands out on the east side. The deck's top is where
// MotorcycleLiftBlock holds the bike: two up, and three and three quarters
// more a level.
const RED = C.RED, HAZARD = 'car:block/shop_hazard', PLATE = 'car:block/lift_plate';

function lift(level) {
    const top = 2 + 3.75 * level, under = top - 1.2;

    // ---- the floor frame --------------------------------------------------------
    k.g('frame');
    k.sym(() => {
        k.box(4.3, 0, -6, 5.5, 0.8, 22, C.DARK_STEEL);
        k.box(3.6, 0, -5.4, 4.3, 0.5, -3.6, C.BLACK);
        k.box(3.6, 0, 19.6, 4.3, 0.5, 21.4, C.BLACK);
    });
    for (const z of [-6, 7.4, 20.8]) {
        k.box(5.5, 0, z, 10.5, 0.8, z + 1.2, C.DARK_STEEL);
    }

    // ---- the foot pump ----------------------------------------------------------
    k.g('pump');
    k.box(11.7, 0, 0.6, 14, 0.5, 4.4, C.DARK_STEEL);
    k.box(12.1, 0.5, 1, 13.5, 2.4, 2.6, RED);
    k.round('y', 12.8, 2.7, 1.8, 0.45, 0.6, C.CHROME);
    k.box(12.3, 1.9, 2.4, 13.3, 2.4, 7.2, C.STEEL, {rot: ['x', -22.5, [12.8, 2.2, 2.6]]});
    k.box(12, 1.85, 5.8, 13.6, 2.5, 7.4, C.BLACK, {rot: ['x', -22.5, [12.8, 2.2, 2.6]]});
    // the release
    k.box(13.5, 1, 1.5, 14.6, 1.4, 1.9, C.CHROME);
    k.box(14.6, 0.8, 1.3, 15.2, 1.6, 2.1, C.BLACK);

    // ---- the legs ---------------------------------------------------------------
    if (level) {
        k.g('legs');
        const gap = under - 0.8, steep = level > 2;
        const angle = steep ? 45 : 22.5;
        const run = gap / Math.tan(angle * Math.PI / 180), length = gap / Math.sin(angle * Math.PI / 180);
        const low = 0.8, mid = low + gap / 2;
        // one leg of each pair climbs south, the other north, on planes of their own
        k.sym(() => {
            k.box(4.5, low - 0.5, 8 - run / 2 - 0.6, 5.3, low + 0.5, 8 - run / 2 + length + 0.6, RED,
                {rot: ['x', -angle, [8, low, 8 - run / 2]]});
            k.box(5.5, low - 0.5, 8 + run / 2 - length - 0.6, 6.3, low + 0.5, 8 + run / 2 + 0.6, C.RED_DARK,
                {rot: ['x', angle, [8, low, 8 + run / 2]]});
        });
        k.round('x', 8, mid, 8, 0.4, 7.6, C.CHROME);
        // the ram, lying along the legs that climb north and pushing on the pin they cross at
        k.box(7.1, low - 0.75, 8 + run / 2 - length * 0.3, 8.9, low + 0.75, 8 + run / 2 - 0.2, C.DARK_STEEL,
            {rot: ['x', angle, [8, low, 8 + run / 2]]});
        k.box(7.5, low - 0.45, 8 + run / 2 - length * 0.5, 8.5, low + 0.45, 8 + run / 2 - length * 0.3, C.CHROME,
            {rot: ['x', angle, [8, low, 8 + run / 2]]});
        k.box(5.6, low - 0.35, 8 + run / 2 - 0.45, 10.4, low + 0.35, 8 + run / 2 + 0.25, C.STEEL);
    }

    // ---- the deck ---------------------------------------------------------------
    k.g('deck');
    k.box(4, under, -8, 12, top, 24, C.DARK_STEEL, {faces: {up: PLATE}});
    k.sym(() => {
        k.box(3.95, under + 0.1, -8, 4.5, top + 0.05, 24, HAZARD, {no: ['east']});
        // the loops the straps hook to
        for (const z of [-5, 9, 18]) {
            k.box(3.4, under + 0.3, z, 3.95, under + 0.6, z + 1.4, C.CHROME);
        }
    });
    k.box(4.4, under + 0.05, -8.4, 11.6, top - 0.05, -8, C.STEEL);

    // ---- the wheel vise ---------------------------------------------------------
    k.g('vise');
    k.box(5.2, top, 19.6, 10.8, top + 0.4, 23.8, C.DARK_STEEL);
    k.box(5.9, top + 0.4, 20, 6.5, top + 6.4, 23.6, RED);
    k.box(9.9, top + 0.4, 20, 10.5, top + 5.4, 23.6, RED);
    k.box(6.5, top + 0.4, 23.1, 9.9, top + 3.6, 23.6, C.RUBBER);
    k.round('x', 11.4, top + 3.4, 21.8, 0.3, 2, C.CHROME);
    k.round('x', 12.6, top + 3.4, 21.8, 0.75, 0.6, C.BLACK);
}

for (let level = 0; level <= 4; level++) {
    k.model(level ? 'motorcycle_lift_' + level : 'motorcycle_lift', {ao: false, particle: 'car:block/lift_plate'},
        () => lift(level));
}
