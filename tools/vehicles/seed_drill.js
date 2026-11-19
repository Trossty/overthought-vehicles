// A trailed seed drill, 3 m: a long hopper across the frame, the metering
// housing under it feeding a row of black seed tubes down to the disc
// coulters, a harrow of tines behind them, a land wheel at each end, and an
// A-frame drawbar out to the hitch with a jockey stand. The row markers
// stand folded up at the ends; a step and a ladder at the back for filling.
const P = 'paint';
const FRAME = C.DARK_STEEL, LID = '#3A3B3F', TUBE = '#1B1B1D';
const ROWS = [];
for (let x = -19.2; x <= 19.21; x += 3.2) {
    ROWS.push(x);
}

k.g('body');
// ---- the frame: the toolbar across, the end plates the wheels hang on ------------------------------
k.box(-22.4, 8.6, 2.4, 22.4, 10.0, 3.8, FRAME);
k.box(-22.4, 8.6, -6.4, 22.4, 9.6, -5.2, FRAME);
k.pair(21.0, 7.0, -6.4, 22.4, 12.8, 3.8, P);
k.pair(22.0, 5.8, -1.0, 22.6, 7.2, 1.0, C.STEEL);                    // the hubs' stub axles
k.pair(10.0, 8.6, -6.4, 11.2, 9.6, 3.8, FRAME);

// ---- the hopper: wider at the top, a lid in two halves, a walkway behind ----------------------------
k.box(-21.0, 14.0, -3.0, 21.0, 18.0, 3.0, P);
k.slab(-21.0, 21.0, 18.0, 3.0, 26.0, 6.0, 0.8, P, 1);
k.slab(-21.0, 21.0, 18.0, -3.0, 26.0, -6.0, 0.8, P, 1);
k.pair(20.2, 18.0, -6.0, 21.0, 26.0, 6.0, P);
k.box(-21.4, 26.0, -6.6, 21.4, 26.8, 6.6, P);                         // the rim
k.box(-20.6, 26.8, -6.0, -0.2, 27.6, 6.0, LID);
k.box(0.2, 26.8, -6.0, 20.6, 27.6, 6.0, LID);
k.box(-20.6, 27.6, -0.4, 20.6, 27.9, 0.4, FRAME);                      // the hinge along its top
k.sym(() => k.box(9.0, 26.6, 6.6, 12.0, 27.4, 7.0, C.BLACK));         // the lid's catches
k.sym(() => {
    // the level window and the stickers on the front
    k.box(3.0, 20.0, 4.95, 6.0, 23.6, 5.3, '#55A9D3');
    k.box(14.0, 20.4, 5.0, 18.0, 22.4, 5.3, C.PLATE);
});
k.box(-8.4, 12.2, -9.4, 8.4, 12.8, -6.4, '#2E3033');                  // the walkway
k.pair(8.0, 12.8, -9.4, 8.4, 17.6, -9.0, FRAME);
k.box(-8.4, 17.2, -9.4, 8.4, 17.6, -9.0, FRAME);
k.sym(() => k.box(1.6, 1.0, -9.6, 2.2, 12.2, -9.0, FRAME));            // the ladder
for (let y = 2.8; y < 12.0; y += 2.4) {
    k.box(-1.6, y, -9.5, 1.6, y + 0.5, -9.1, FRAME);
}

// ---- the metering housing, the shaft out to the land wheel, the seed tubes -------------------------
k.box(-21.0, 11.4, -2.6, 21.0, 14.0, 2.6, LID);
k.box(-22.4, 12.2, -0.5, 22.4, 13.2, 0.5, C.STEEL);
k.box(21.6, 6.0, -0.8, 22.4, 13.4, 0.8, C.BLACK);                       // the drive chain's case
k.box(-22.6, 10.4, -1.6, -21.0, 14.6, 1.6, '#C2A23C');                  // the gearbox and its dial
for (const x of ROWS) {
    k.beam([x, 11.4, 0.0], [x, 3.4, -4.0], 0.6, 0.6, TUBE);
    // the coulter: an arm from the toolbar, a disc, a press wheel behind
    k.beam([x + 0.6, 9.0, 3.0], [x + 0.6, 3.0, -4.6], 0.5, 0.8, FRAME);
    k.disc(x - 0.3, x + 0.3, 2.4, -4.6, 2.4, C.STEEL);
    k.beam([x + 0.6, 3.4, -4.6], [x + 0.6, 1.2, -7.4], 0.3, 0.3, FRAME); // the press wheel's arm
    k.box(x - 0.5, 0.4, -8.2, x + 0.5, 2.0, -6.6, C.RUBBER);
}

// ---- the harrow behind: a bar and a row of tines ------------------------------------------------------
k.box(-21.0, 6.6, -9.8, 21.0, 7.4, -9.0, FRAME);
k.pair(10.0, 6.6, -9.0, 10.8, 8.6, -6.4, FRAME);
for (let x = -20.0; x <= 20.01; x += 2.0) {
    k.beam([x, 6.6, -9.4], [x, 0.4, -11.4], 0.3, 0.3, C.STEEL);
}

// ---- the row markers, folded up at the ends ------------------------------------------------------------
k.sym(() => {
    k.box(20.6, 12.8, -5.2, 21.6, 14.8, -4.0, FRAME);
    k.beam([21.0, 14.8, -4.6], [20.4, 30.0, -4.6], 0.7, 0.7, P);
    k.disc(19.8, 20.2, 30.0, -4.6, 1.8, C.STEEL);
    // a tail lamp and a reflector on each rear corner, a side reflector
    k.lamp(17.4, 9.8, 19.8, 11.0, -6.6, C.TAIL, false, {depth: 0.3});
    k.box(14.6, 9.8, -6.9, 16.6, 11.0, -6.6, C.AMBER);
    k.box(21.0, 9.8, 2.0, 21.2, 11.0, 3.4, C.AMBER);
});

// ---- the A-frame drawbar, its hitch eye, the jockey stand, the hoses -------------------------------------
k.beams([11.6, 9.2, 3.8], [1.2, 6.0, 29.4], 1.2, 1.2, P);
k.box(-1.8, 5.2, 27.8, 1.8, 7.0, 29.6, FRAME);
k.box(-1.2, 5.0, 29.6, 1.2, 6.4, 32.0, FRAME);
k.box(-0.5, 5.0, 32.0, 0.5, 6.4, 32.6, C.STEEL);
k.box(-5.0, 7.4, 16.0, 5.0, 8.2, 17.0, FRAME);                           // the cross brace
k.jockey(3.4, 21.0, 9.6, 0);
k.beam([-1.0, 13.6, 3.0], [-1.6, 7.4, 27.6], 0.4, 0.4, C.BLACK);
k.beam([-0.4, 13.6, 3.0], [-1.0, 7.4, 27.6], 0.4, 0.4, C.BLACK);
