// An engine crane: two long legs on castors that run under the nose of the
// car to the north, a mast at the back braced to them, a boom with an
// extension pinned inside it, the ram that lifts it, and a chain and hook
// that hang over the engine bay.
const RED = C.RED;

function castor(x, z) {
    k.box(x - 0.9, 0.8, z - 0.9, x + 0.9, 1.1, z + 0.9, C.DARK_STEEL);
    k.box(x - 0.6, 0.4, z - 0.4, x - 0.4, 0.9, z + 0.4, C.STEEL);
    k.box(x + 0.4, 0.4, z - 0.4, x + 0.6, 0.9, z + 0.4, C.STEEL);
    k.round('x', x, 0.55, z, 0.55, 0.6, C.RUBBER);
}

k.model('engine_hoist', {particle: 'overthought_vehicles:block/shop_red'}, () => {
    // ---- the legs ---------------------------------------------------------------
    k.g('legs');
    k.sym(() => {
        k.box(1, 1.1, -12, 3, 2.6, 14, RED);
        k.box(1.2, 1.3, -12.3, 2.8, 2.4, -12, C.BLACK);
        castor(2, -11);
        castor(2, 13);
        // the pin a leg folds up on
        k.box(0.7, 1.5, 9.6, 3.3, 2.2, 10.3, C.CHROME);
    });
    k.box(3, 1.1, 11.6, 13, 2.6, 14, RED);

    // ---- the mast ---------------------------------------------------------------
    k.g('mast');
    k.box(6.5, 2.6, 10.5, 9.5, 26.4, 13.5, RED);
    k.box(6.2, 2.6, 10.2, 9.8, 3.4, 13.8, C.RED_DARK);
    k.sym(() => {
        k.box(3.4, 1.6, 12, 4.6, 9.6, 13, RED, {rot: ['z', -22.5, [4, 2.6, 12.5]]});
    });
    // the bracket the boom swings on
    k.box(6.1, 24.6, 10.8, 6.5, 28.2, 13.2, C.DARK_STEEL);
    k.box(9.5, 24.6, 10.8, 9.9, 28.2, 13.2, C.DARK_STEEL);
    k.round('x', 8, 26.9, 12, 0.5, 4.4, C.CHROME);

    // ---- the boom ---------------------------------------------------------------
    k.g('boom');
    k.box(6.9, 25.8, -4, 9.1, 28, 13.2, RED);
    k.box(7.3, 26.2, -11.4, 8.7, 27.6, -4, C.STEEL);
    k.box(6.7, 25.6, -4.4, 9.3, 28.2, -3.6, C.RED_DARK);
    k.box(6.6, 26.5, -2.6, 9.4, 27.2, -1.9, C.CHROME);
    for (const z of [-9.6, -7.6, -5.6]) {
        k.box(7.25, 26.6, z, 8.75, 27.2, z + 0.6, C.BLACK);
    }
    k.box(7.1, 25.4, -11.2, 8.9, 26.2, -9.6, C.DARK_STEEL);

    // ---- the ram ----------------------------------------------------------------
    k.g('ram');
    k.box(7, 4.2, 6.2, 9, 15.4, 8.2, C.DARK_STEEL, {rot: ['x', -22.5, [8, 6, 11]]});
    k.box(7.4, 15.4, 6.6, 8.6, 25, 7.8, C.CHROME, {rot: ['x', -22.5, [8, 6, 11]]});
    k.box(6.9, 14.8, 6.1, 9.1, 15.6, 8.3, C.STEEL, {rot: ['x', -22.5, [8, 6, 11]]});
    k.box(7, 4.6, 9.4, 9, 6.6, 10.5, C.DARK_STEEL);
    // the pump handle, left in its socket
    k.box(7.6, 8, 13.5, 8.4, 9, 14.4, C.DARK_STEEL);
    k.round('z', 8, 8.5, 16.4, 0.4, 4, C.CHROME);
    k.round('z', 8, 8.5, 18.6, 0.55, 1.2, C.BLACK);

    // ---- the chain and the hook -------------------------------------------------
    k.g('chain');
    for (let i = 0; i < 6; i++) {
        const y = 23.6 - i * 1.7;
        if (i % 2 === 0) {
            k.box(7.75, y, -10.9, 8.25, y + 2, -9.9, C.STEEL);
        } else {
            k.box(7.5, y, -10.65, 8.5, y + 2, -10.15, C.STEEL);
        }
    }
    k.g('hook');
    k.box(7.3, 14.4, -11.1, 8.7, 15.6, -9.7, C.DARK_STEEL);
    k.box(7.6, 12.4, -10.3, 8.4, 14.4, -9.6, C.CHROME);
    k.box(7.6, 11.6, -11.3, 8.4, 12.4, -9.6, C.CHROME);
    k.box(7.6, 12.4, -11.3, 8.4, 13.2, -10.6, C.CHROME);
});
