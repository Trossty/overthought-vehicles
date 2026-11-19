// A low steel drain pan, the kind slid under a car on a lift: a yellow tray
// with a rolled rim, a splash grid over the old oil, a carrying handle at
// each end and a capped spout on the north side.
const TRAY = C.YELLOW, RIM = C.YELLOW_DARK;

k.model('drain_pan', {particle: 'minecraft:block/yellow_concrete'}, () => {
    k.g('tray');
    k.box(1, 0, 1, 15, 0.6, 15, TRAY);
    k.box(1, 0.6, 1, 15, 2.6, 2, TRAY);
    k.box(1, 0.6, 14, 15, 2.6, 15, TRAY);
    k.box(1, 0.6, 2, 2, 2.6, 14, TRAY);
    k.box(14, 0.6, 2, 15, 2.6, 14, TRAY);
    // the rolled rim
    k.box(0.7, 2.6, 0.7, 15.3, 3, 2.2, RIM);
    k.box(0.7, 2.6, 13.8, 15.3, 3, 15.3, RIM);
    k.box(0.7, 2.6, 2.2, 2.2, 3, 13.8, RIM);
    k.box(13.8, 2.6, 2.2, 15.3, 3, 13.8, RIM);

    k.g('oil');
    k.box(2, 0.6, 2, 14, 1.5, 14, C.OIL, {only: ['up']});
    k.box(9.5, 1.5, 4, 12, 1.56, 6.5, '#3A2E1E', {only: ['up']});

    // ---- the splash grid ------------------------------------------------------
    k.g('grid');
    for (const x of [3.6, 5.8, 8, 10.2, 12.4]) {
        k.box(x - 0.15, 2.1, 2, x + 0.15, 2.4, 14, C.DARK_STEEL);
    }
    for (const z of [5, 8, 11]) {
        k.box(2, 2.05, z - 0.15, 14, 2.35, z + 0.15, C.DARK_STEEL);
    }

    // ---- the handles and the spout ----------------------------------------------
    k.g('handles');
    k.sym(() => {
        k.box(0.3, 1.4, 5.4, 1, 1.9, 5.9, C.STEEL);
        k.box(0.3, 1.4, 10.1, 1, 1.9, 10.6, C.STEEL);
        k.box(0.2, 1.3, 5.4, 0.6, 2, 10.6, C.BLACK);
    });
    k.g('spout');
    k.box(6.9, 0.5, -0.6, 9.1, 2.1, 1, TRAY);
    k.round('z', 8, 1.3, -1, 0.95, 0.8, C.BLACK);
    k.box(7.6, 0.9, -1.7, 8.4, 1.7, -1.4, C.DARK_STEEL);
});
