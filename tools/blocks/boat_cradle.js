// A boat cradle: a galvanised frame on four feet, two carpeted bunks the
// hull lies on, each on a pair of screw posts, and a rubber roller low at
// the north end for the keel. The bunks' tops are eight up, where
// ServiceStandBlock holds the boat.
const FRAME = C.STEEL, CARPET = '#5E6A75', CARPET_DARK = '#4A545E';

k.model('boat_cradle', {particle: 'overthought_vehicles:block/lift_housing'}, () => {
    k.g('frame');
    for (const z of [2, 11.5]) {
        k.box(1, 0.6, z, 15, 2.6, z + 2.5, FRAME);
        k.sym(() => {
            k.box(0.6, 0, z - 0.3, 3, 0.6, z + 2.8, C.DARK_STEEL);
        });
    }
    k.sym(() => {
        k.box(1.2, 0.9, 4.5, 2.8, 2.3, 11.5, FRAME);
    });
    k.box(7.2, 0.9, 4.5, 8.8, 2.3, 11.5, FRAME);

    // ---- the bunks ----------------------------------------------------------------
    k.g('bunks');
    k.sym(() => {
        for (const z of [3.25, 12.75]) {
            k.box(3.6, 2.6, z - 0.9, 5.4, 4.2, z + 0.9, C.DARK_STEEL);
            k.round('y', 4.5, 5, z, 0.5, 1.8, C.CHROME);
            k.box(3.9, 5.5, z - 0.7, 5.1, 6, z + 0.7, C.DARK_STEEL);
            // the brace out to the end of the beam
            k.box(1.6, 2.2, z - 0.3, 2.2, 5.4, z + 0.3, FRAME, {rot: ['z', -45, [1.9, 2.6, z]]});
        }
        k.box(2.6, 6, 0, 6.2, 6.6, 16, C.WOOD);
        k.box(2.5, 6.6, 0, 6.3, 8, 16, CARPET);
        for (const z of [0, 15.6]) {
            k.box(2.45, 6.5, z, 6.35, 8.05, z + 0.4, CARPET_DARK);
        }
    });

    // ---- the keel roller ------------------------------------------------------------
    k.g('roller');
    k.box(6.3, 2.6, 2.4, 6.8, 5.2, 4.1, C.DARK_STEEL);
    k.box(9.2, 2.6, 2.4, 9.7, 5.2, 4.1, C.DARK_STEEL);
    k.round('x', 8, 4.4, 3.25, 0.95, 2.4, C.RUBBER);
    k.round('x', 8, 4.4, 3.25, 0.3, 3.8, C.CHROME);
});
