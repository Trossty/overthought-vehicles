// A mechanic's bench: a plank top on a red steel frame, a shelf low down,
// a bank of drawers hung under the east half, and on the top a vise bolted
// to the north-east corner, a tray of sockets and a spanner left out.
const RED = C.RED, VISE = C.BLUE_DARK;

k.model('vehicle_workbench', {particle: 'car:block/shop_wood'}, () => {
    // ---- the frame ------------------------------------------------------------
    k.g('frame');
    for (const [x, z] of [[0.6, 0.6], [13.6, 0.6], [0.6, 13.6], [13.6, 13.6]]) {
        k.box(x, 0.4, z, x + 1.8, 12.6, z + 1.8, RED);
        k.box(x - 0.2, 0, z - 0.2, x + 2, 0.4, z + 2, C.BLACK);
    }
    // the apron under the top
    k.box(2.4, 11.2, 0.8, 13.6, 12.6, 1.6, RED);
    k.box(2.4, 11.2, 14.4, 13.6, 12.6, 15.2, RED);
    k.box(0.8, 11.2, 2.4, 1.6, 12.6, 13.6, RED);
    k.box(14.4, 11.2, 2.4, 15.2, 12.6, 13.6, RED);
    // the shelf and the rails it lies on
    k.box(2.4, 2.2, 0.9, 13.6, 3.2, 1.5, RED);
    k.box(2.4, 2.2, 14.5, 13.6, 3.2, 15.1, RED);
    k.box(0.9, 2.2, 2.4, 1.5, 3.2, 13.6, RED);
    k.box(14.5, 2.2, 2.4, 15.1, 3.2, 13.6, RED);
    k.box(1.5, 2.6, 1.5, 14.5, 3.1, 14.5, C.STEEL);

    // ---- the top --------------------------------------------------------------
    k.g('top');
    k.box(0, 12.6, 0, 16, 13.1, 16, C.DARK_STEEL);
    k.box(0, 13.1, 0, 16, 16, 16, C.WOOD);
    for (const z of [3.9, 7.9, 11.9]) {
        k.box(0, 15.9, z, 16, 16, z + 0.2, C.WOOD_DARK, {only: ['up']});
    }
    for (const x of [0, 15.9]) {
        for (const z of [3.9, 7.9, 11.9]) {
            k.box(x, 13.1, z, x + 0.1, 16, z + 0.2, C.WOOD_DARK, {only: [x ? 'east' : 'west']});
        }
    }

    // ---- the drawers ----------------------------------------------------------
    k.g('drawers');
    k.box(8.4, 5.2, 1.7, 14.4, 12.6, 14.3, C.DARK_STEEL);
    k.box(8.7, 5.5, 1.62, 14.1, 12.3, 1.7, C.BLACK);
    for (const [y1, y2] of [[5.6, 8], [8.15, 10.2], [10.35, 12.2]]) {
        k.box(8.8, y1, 1.4, 14, y2, 1.62, RED);
        k.box(8.8, y2 - 0.45, 1.15, 14, y2, 1.4, C.ALU);
    }

    // ---- what is kept on the shelf ----------------------------------------------
    k.g('shelf');
    k.box(2.6, 3.1, 8.6, 6.8, 6.3, 13.2, C.GREEN_DARK);
    k.box(2.5, 6.3, 8.5, 6.9, 6.8, 13.3, C.GREEN);
    k.box(4.1, 6.8, 10.4, 5.3, 7.3, 11.4, C.BLACK);
    k.round('y', 4.4, 4.9, 4.6, 1.5, 3.6, C.BLUE);
    k.round('y', 4.4, 6.85, 4.6, 1.55, 0.3, C.DARK_STEEL);
    k.round('y', 4.4, 7.2, 4.6, 0.5, 0.4, C.BLACK);

    // ---- the vise ---------------------------------------------------------------
    k.g('vise');
    k.box(10.8, 16, 1, 14.8, 16.5, 5, VISE);
    for (const [x, z] of [[11.1, 1.3], [14, 1.3], [11.1, 4.2], [14, 4.2]]) {
        k.box(x, 16.5, z, x + 0.5, 16.7, z + 0.5, C.CHROME);
    }
    k.box(11.8, 16.5, 2, 13.8, 18.2, 4.6, VISE);
    k.box(11.4, 18.2, 3.2, 14.2, 19.9, 4.6, VISE);
    k.box(11.4, 18.9, 2.9, 14.2, 19.9, 3.2, C.STEEL);
    // the slide, and the jaw that rides on it
    k.box(12.1, 17, -0.2, 13.5, 18.1, 2, C.STEEL);
    k.box(11.4, 16.9, 0.4, 14.2, 19.9, 1.6, VISE);
    k.box(11.4, 18.9, 1.6, 14.2, 19.9, 1.9, C.STEEL);
    k.round('z', 12.8, 17.55, -0.5, 0.55, 0.7, C.DARK_STEEL);
    k.round('x', 12.8, 17.55, -0.55, 0.22, 4.6, C.CHROME);
    k.box(10.3, 17.25, -0.85, 10.7, 17.85, -0.25, C.BLACK);
    k.box(14.9, 17.25, -0.85, 15.3, 17.85, -0.25, C.BLACK);
    // the little anvil behind the fixed jaw
    k.box(11.9, 18.2, 4.6, 13.7, 18.9, 5.4, VISE);

    // ---- the socket tray --------------------------------------------------------
    k.g('sockets');
    k.box(1.6, 16, 9.6, 8.4, 16.5, 14.4, RED);
    k.box(1.9, 16.5, 9.9, 8.1, 16.55, 14.1, C.RED_DARK, {only: ['up']});
    [0.62, 0.55, 0.5, 0.44].forEach((r, i) => {
        k.round('y', 2.9 + i * 1.45, 16.5 + (1.5 - i * 0.2) / 2, 11, r, 1.5 - i * 0.2, C.CHROME);
        k.round('y', 2.9 + i * 1.45, 16.5 + (1.2 - i * 0.15) / 2, 13, r * 0.85, 1.2 - i * 0.15, C.CHROME);
    });

    // ---- the spanner and the ratchet --------------------------------------------
    k.g('tools');
    k.box(2.6, 16, 3.6, 7.4, 16.3, 4.3, C.CHROME);
    k.box(1.6, 16, 3.3, 2.6, 16.3, 3.65, C.CHROME);
    k.box(1.6, 16, 4.25, 2.6, 16.3, 4.6, C.CHROME);
    k.round('y', 7.9, 16.15, 3.95, 0.75, 0.3, C.CHROME);
    k.box(2.2, 16, 6.2, 6.6, 16.4, 6.8, C.BLACK);
    k.box(6.6, 16, 6.25, 8.2, 16.35, 6.75, C.CHROME);
    k.round('y', 8.6, 16.3, 6.5, 0.7, 0.6, C.CHROME);
    k.round('y', 8.6, 16.8, 6.5, 0.4, 0.5, C.STEEL);
});
