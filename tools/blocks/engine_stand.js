// An engine stand: a T-shaped base on four castors, a post braced to it, and
// a head that turns in a sleeve at the top, with four arms bolted to the
// back of the engine and a bar to turn it over by. The engine hangs to the
// north of the head, its middle ten up; EngineStandRenderer puts it there.
const RED = C.RED;

function castor(x, z) {
    k.box(x - 0.8, 1.1, z - 0.8, x + 0.8, 1.4, z + 0.8, C.DARK_STEEL);
    k.box(x - 0.6, 0.5, z - 0.5, x - 0.4, 1.1, z + 0.5, C.STEEL);
    k.box(x + 0.4, 0.5, z - 0.5, x + 0.6, 1.1, z + 0.5, C.STEEL);
    k.round('x', x, 0.7, z, 0.72, 0.6, C.RUBBER);
}

k.model('engine_stand', {particle: 'car:block/shop_red'}, () => {
    k.g('castors');
    for (const [x, z] of [[2.2, 13.2], [13.8, 13.2], [3.8, 1.8], [12.2, 1.8]]) {
        castor(x, z);
    }

    // ---- the base ---------------------------------------------------------------
    k.g('base');
    k.box(7, 1.4, 1, 9, 2.9, 14.4, RED);
    k.box(1, 1.4, 12.2, 15, 2.9, 14.2, RED);
    k.box(2.6, 1.4, 0.8, 13.4, 2.9, 2.8, RED);
    for (const [x, z] of [[1, 12.2], [14.6, 12.2], [2.6, 0.8], [13, 0.8]]) {
        k.box(x, 1.5, z + 0.1, x + 0.4, 2.8, z + 1.9, C.BLACK, {only: [x < 8 ? 'west' : 'east']});
    }
    // the pin the front leg folds on
    k.box(6.7, 1.8, 3.4, 9.3, 2.5, 4.1, C.CHROME);

    // ---- the post and its brace ---------------------------------------------------
    k.g('post');
    k.box(7, 2.9, 11.8, 9, 11.6, 13.8, RED);
    k.box(7.4, 2.4, 7.8, 8.6, 8.4, 9, RED, {rot: ['x', -45, [8, 2.9, 11.8]]});
    k.box(6.8, 2.9, 11.6, 9.2, 3.5, 14, C.RED_DARK);

    // ---- the head -----------------------------------------------------------------
    k.g('head');
    k.round('z', 8, 10, 12, 1.5, 4.6, C.DARK_STEEL, {turn: 22.5});
    k.round('z', 8, 10, 9.4, 3.3, 0.8, C.STEEL, {sides: 16});
    k.round('z', 8, 10, 8.85, 1.1, 0.3, C.CHROME);
    // four arms, each with the bolt that goes into the engine
    for (const [dx, dy] of [[-1, 1], [1, 1], [-1, -1], [1, -1]]) {
        const x = 8 + dx * 2.3, y = 10 + dy * 2.3;
        k.box(x - 0.55, y - 0.55, 7.2, x + 0.55, y + 0.55, 9, C.DARK_STEEL);
        k.box(x - 0.3, y - 0.3, 6.6, x + 0.3, y + 0.3, 7.2, C.CHROME);
    }
    // the locking pin and the bar it is turned by
    k.box(7.6, 11.4, 12.2, 8.4, 12.6, 13, C.CHROME);
    k.box(7.3, 12.6, 11.9, 8.7, 13, 13.3, C.BLACK);
    k.round('x', 8, 10, 14.75, 0.4, 8, C.CHROME);
    k.box(3.4, 9.5, 14.25, 4.2, 10.5, 15.25, C.BLACK);
    k.box(11.8, 9.5, 14.25, 12.6, 10.5, 15.25, C.BLACK);
});
