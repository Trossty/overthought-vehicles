// A cabinet of batteries on a skid: a door on the front with a window the
// four charge bars show through, louvres down both sides, and the two
// terminals on the roof with their leads running off the back. One model a
// level of charge, with that many bars lit.
const CASE = C.STEEL, DOOR = C.ALU;

function bank(level) {
    k.g('skid');
    k.sym(() => {
        k.box(1, 0, 1, 4, 1, 15, C.BLACK);
    });
    k.box(4, 0.3, 2, 12, 1, 14, C.DARK_STEEL);

    k.g('case');
    k.box(1.5, 1, 2, 14.5, 14, 15, CASE);
    k.box(1.1, 14, 1.6, 14.9, 14.6, 15.4, C.DARK_STEEL);
    k.sym(() => {
        // the corner posts the door shuts between
        k.box(1.2, 1, 1.6, 2.4, 14, 2.6, C.DARK_STEEL);
        for (const y of [4, 5.6, 7.2, 8.8, 10.4]) {
            k.box(1.44, y, 5, 1.5, y + 0.6, 12, C.BLACK, {only: ['west']});
        }
        k.box(1.4, 11.6, 2.9, 1.5, 13.2, 4.5, C.YELLOW, {only: ['west']});
    });

    // ---- the door -----------------------------------------------------------------
    k.g('door');
    k.box(2.5, 1.6, 1.6, 13.5, 13.4, 2, DOOR);
    k.box(2.3, 3.4, 1.4, 2.9, 4.8, 1.8, C.BLACK);
    k.box(2.3, 10.4, 1.4, 2.9, 11.8, 1.8, C.BLACK);
    k.box(12.3, 6.4, 1.2, 12.9, 9.4, 1.6, C.CHROME);
    k.box(3.8, 4.3, 1.5, 12.2, 12.2, 1.6, C.BLACK);
    [5.1, 6.85, 8.6, 10.35].forEach((y, i) => {
        const on = i < level;
        k.box(4.8, y, 1.4, 11.2, y + 1.15, 1.5, on ? C.LED_GREEN : C.GREEN_DARK, {shade: !on});
    });
    k.box(5.6, 2.2, 1.4, 10.4, 3.6, 1.6, C.YELLOW);
    k.box(6.2, 2.5, 1.35, 9.8, 3.3, 1.4, C.BLACK, {only: ['north']});

    // ---- the roof -----------------------------------------------------------------
    k.g('roof');
    for (let x = 5.6; x < 10.5; x += 1.5) {
        k.box(x, 14.6, 10.6, x + 0.7, 14.9, 14.2, C.BLACK);
    }
    [[4.5, C.RED], [11.5, C.BLACK]].forEach(([x, colour]) => {
        k.box(x - 1.1, 14.6, 4.9, x + 1.1, 15.3, 7.1, colour);
        k.round('y', x, 15.65, 6, 0.6, 0.7, C.BRASS);
        // the lead, off over the back
        k.box(x - 0.4, 14.6, 7.1, x + 0.4, 15.2, 15.4, colour);
        k.box(x - 0.4, 11.4, 15, x + 0.4, 14.6, 15.4, colour);
    });
}

for (let level = 0; level <= 4; level++) {
    k.model('battery_bank_' + level, {particle: 'minecraft:block/gray_concrete'}, () => bank(level));
}
