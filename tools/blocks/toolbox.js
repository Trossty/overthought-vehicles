// A roller cabinet with a top chest on it: red steel, a full-width aluminium
// pull along the top of every drawer, a lid with two latches and a push
// handle on one side. The drawers face north.
const RED = C.RED;

k.model('toolbox', {particle: 'overthought_vehicles:block/shop_red'}, () => {
    k.g('castors');
    for (const [x, z] of [[2.2, 3.4], [12.6, 3.4], [2.2, 11.6], [12.6, 11.6]]) {
        k.box(x - 0.1, 1.6, z - 0.2, x + 1.3, 2, z + 1.2, C.DARK_STEEL);
        k.box(x, 0.7, z + 0.1, x + 0.2, 1.6, z + 0.9, C.STEEL);
        k.box(x + 1, 0.7, z + 0.1, x + 1.2, 1.6, z + 0.9, C.STEEL);
        k.round('x', x + 0.6, 0.75, z + 0.5, 0.78, 0.6, C.RUBBER);
    }

    // ---- the roller cabinet -------------------------------------------------
    k.g('cabinet');
    k.box(1, 2, 2.6, 15, 9.4, 14, RED);
    k.box(1, 2, 2.3, 1.8, 9.4, 2.6, RED);
    k.box(14.2, 2, 2.3, 15, 9.4, 2.6, RED);
    k.box(1.8, 2, 2.3, 14.2, 2.5, 2.6, RED);
    k.box(1.8, 9, 2.3, 14.2, 9.4, 2.6, RED);
    k.box(1.8, 2.5, 2.52, 14.2, 9, 2.6, C.BLACK);
    k.g('cabinet/drawers');
    for (const [y1, y2] of [[2.6, 4.9], [5.05, 6.6], [6.75, 7.8], [7.95, 8.9]]) {
        k.box(1.9, y1, 2.3, 14.1, y2, 2.52, RED);
        k.box(1.9, y2 - 0.45, 2.05, 14.1, y2, 2.3, C.ALU);
    }
    // the bumper round its foot and the mat on its top
    k.g('cabinet');
    k.box(0.8, 2, 2.1, 15.2, 2.45, 14.2, C.BLACK);
    k.box(0.8, 9.4, 2.2, 15.2, 9.9, 14.2, C.BLACK);

    // ---- the push handle ----------------------------------------------------
    k.g('handle');
    k.box(15, 7.5, 4.4, 15.9, 8.3, 5.1, C.STEEL);
    k.box(15, 7.5, 11, 15.9, 8.3, 11.7, C.STEEL);
    k.round('z', 15.75, 7.9, 8.05, 0.42, 7.8, C.CHROME);

    // ---- the top chest ------------------------------------------------------
    k.g('chest');
    k.box(1.6, 9.9, 3.2, 14.4, 12.5, 13.6, RED);
    k.box(2.1, 10.05, 3.12, 13.9, 12.4, 3.2, C.BLACK);
    for (const [y1, y2] of [[10.15, 11.15], [11.3, 12.3]]) {
        k.box(2.2, y1, 2.95, 13.8, y2, 3.12, RED);
        k.box(2.2, y2 - 0.4, 2.75, 13.8, y2, 2.95, C.ALU);
    }
    k.g('chest/lid');
    k.box(1.4, 12.5, 3, 14.6, 14.8, 13.8, RED);
    k.box(1.4, 12.5, 2.9, 14.6, 12.9, 3, C.ALU);
    for (const x of [4, 11]) {
        k.box(x, 12.1, 2.7, x + 1, 13.5, 2.9, C.CHROME);
        k.box(x + 0.2, 13.5, 2.75, x + 0.8, 13.8, 2.9, C.STEEL);
    }
    k.box(7.6, 13.6, 2.85, 8.4, 14.3, 3, C.CHROME);
    k.box(5.4, 14.8, 8, 6, 15.4, 8.7, C.STEEL);
    k.box(10, 14.8, 8, 10.6, 15.4, 8.7, C.STEEL);
    k.box(5.4, 15.2, 8.05, 10.6, 15.6, 8.65, C.CHROME);
});
