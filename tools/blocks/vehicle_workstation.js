// The diagnostics trolley: a red cabinet on castors with a drawer over two
// louvred doors, a steel top with a mat, a keyboard and a mouse on it, and a
// monitor leant back on its stand. The screen is the one face that keeps a
// texture of its own, because it plays. A scan tool hangs on the east side.
const RED = C.RED;

k.model('vehicle_workstation', {particle: 'car:block/shop_red'}, () => {
    k.g('castors');
    for (const [x, z] of [[2.2, 3.2], [12.6, 3.2], [2.2, 12.6], [12.6, 12.6]]) {
        k.box(x - 0.1, 1.6, z - 0.2, x + 1.3, 2, z + 1.2, C.DARK_STEEL);
        k.box(x, 0.7, z + 0.1, x + 0.2, 1.6, z + 0.9, C.STEEL);
        k.box(x + 1, 0.7, z + 0.1, x + 1.2, 1.6, z + 0.9, C.STEEL);
        k.round('x', x + 0.6, 0.75, z + 0.5, 0.78, 0.6, C.RUBBER);
    }

    // ---- the cabinet ------------------------------------------------------------
    k.g('cabinet');
    k.box(1, 2, 2.6, 15, 12.6, 15, RED);
    k.box(1, 2, 2.3, 1.8, 12.6, 2.6, RED);
    k.box(14.2, 2, 2.3, 15, 12.6, 2.6, RED);
    k.box(1.8, 2, 2.3, 14.2, 2.5, 2.6, RED);
    k.box(1.8, 12.2, 2.3, 14.2, 12.6, 2.6, RED);
    k.box(1.8, 2.5, 2.52, 14.2, 12.2, 2.6, C.BLACK);
    k.box(0.8, 2, 2.1, 15.2, 2.45, 15.2, C.BLACK);
    // the drawer
    k.box(1.9, 9.9, 2.3, 14.1, 12.1, 2.52, RED);
    k.box(1.9, 11.65, 2.05, 14.1, 12.1, 2.3, C.ALU);
    // the doors, and the louvres the computer breathes through
    k.g('cabinet/doors');
    k.sym(() => {
        k.box(1.9, 2.6, 2.3, 7.9, 9.75, 2.52, RED);
        k.box(7.1, 6.4, 2.1, 7.5, 8, 2.3, C.CHROME);
        for (const y of [3.3, 4, 4.7]) {
            k.box(2.7, y, 2.26, 6.3, y + 0.3, 2.3, C.BLACK, {only: ['north']});
        }
    });

    // ---- the side handles ---------------------------------------------------------
    k.g('handles');
    k.sym(() => {
        k.box(0.3, 9.9, 5, 1, 10.5, 5.6, C.STEEL);
        k.box(0.3, 9.9, 11.4, 1, 10.5, 12, C.STEEL);
        k.round('z', 0.4, 10.2, 8.5, 0.38, 7.6, C.CHROME);
    });

    // ---- the top ------------------------------------------------------------------
    k.g('top');
    k.box(0.5, 12.6, 1.9, 15.5, 13.4, 15.5, C.STEEL);
    k.box(1.3, 13.4, 2.5, 14.7, 13.55, 8.4, C.BLACK);
    k.g('keyboard');
    k.box(3.2, 13.55, 3.2, 11.2, 13.95, 6.4, C.PLASTIC);
    for (const z of [3.5, 4.4, 5.3]) {
        k.box(3.5, 13.95, z, 10.9, 14.05, z + 0.65, C.TRIM, {no: ['down']});
    }
    k.box(12.4, 13.55, 4, 13.5, 13.95, 5.7, C.PLASTIC);
    k.box(12.85, 13.95, 4.1, 13.05, 14, 4.7, C.TRIM, {only: ['up']});

    // ---- the monitor ----------------------------------------------------------------
    k.g('monitor');
    k.box(5.4, 13.4, 10, 10.6, 13.8, 13.8, C.BLACK);
    k.box(7.3, 13.8, 11.7, 8.7, 18.4, 12.5, C.DARK_STEEL);
    const lean = ['x', 22.5, [8, 16, 10.5]];
    k.box(1.5, 15.5, 10.1, 14.5, 23.5, 11, C.BLACK, {rot: lean});
    k.box(5, 17, 11, 11, 21.6, 11.6, C.PLASTIC, {rot: lean});
    k.box(2, 16, 10, 14, 23, 10.1, C.BLACK, {
        rot: lean, shade: false, only: ['north'],
        faces: {north: 'car:block/vehicle_workstation_top'}, uv: {north: [0, 0, 16, 16]}
    });
    k.box(13.6, 15.62, 10.04, 14, 15.82, 10.1, C.LED_GREEN, {rot: lean, shade: false, only: ['north']});

    // ---- the scan tool on its hook ----------------------------------------------------
    k.g('scanner');
    k.box(15, 8.2, 6.6, 15.5, 8.6, 7, C.STEEL);
    k.box(15.05, 5, 5.6, 15.75, 8.4, 8, C.ORANGE);
    k.box(15.75, 6.6, 5.9, 15.8, 8.1, 7.7, C.SCREEN, {only: ['east'], shade: false});
    k.box(15.75, 5.4, 6, 15.8, 6.2, 7.6, C.BLACK, {only: ['east']});
    k.box(15.2, 3.4, 6.6, 15.5, 5, 7, C.BLACK);
    k.box(15.1, 2.8, 6.4, 15.6, 3.4, 7.2, C.TRIM);
});
