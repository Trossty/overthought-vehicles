// A forecourt pump, two blocks tall and cut into its two halves: a red
// cabinet with white corner posts on a concrete plinth, a sight glass low on
// the front with the petrol standing behind it, the display above, and a lit
// sign on the roof. The nozzle hangs in a boot on the west side with its
// hose looped under it; the pump without its nozzle keeps only the boot and
// the outlet the hose is drawn from.
//
// What FuelPumpRenderer and FuelPumpBlock.hoseOutlet count on: the hollow
// behind the glass is x 5..11, y 2.5..9, z 4..9; the display's face is at
// z 3.5, centred 23 up; the hose leaves at x 2.1, y 20.5, z 9.5.
const RED = C.RED, WHITE = C.WHITE, GLASS = 'car:block/pump_glass';

function pump(nozzle) {
    // ---- the plinth -------------------------------------------------------------
    k.g('plinth');
    k.box(2, 0, 3, 14, 1, 13, C.CONCRETE);
    k.box(2.5, 1, 3.5, 13.5, 1.5, 12.5, C.DARK_STEEL);

    // ---- the cabinet, built round the hollow -------------------------------------
    k.g('cabinet');
    k.box(3, 1.5, 9, 13, 16, 12, RED, {faces: {north: WHITE}});
    k.box(3, 1.5, 4, 5, 16, 9, RED, {faces: {east: WHITE}});
    k.box(11, 1.5, 4, 13, 16, 9, RED, {faces: {west: WHITE}});
    k.box(5, 1.5, 4, 11, 2.5, 9, RED, {faces: {up: WHITE}});
    k.box(5, 9, 4, 11, 16, 9, RED, {faces: {down: WHITE}});
    k.box(3, 16, 4, 13, 27, 12, RED);
    // the corner posts
    k.sym(() => {
        k.box(3, 1.5, 3.5, 4.2, 27, 4, WHITE);
        k.box(2.6, 1.5, 4, 3, 27, 5.2, WHITE);
        k.box(3, 1.5, 12, 4.2, 27, 12.5, WHITE);
    });

    // ---- the sight glass --------------------------------------------------------
    k.g('glass');
    k.box(4.6, 9, 3.6, 11.4, 9.4, 4, C.CHROME);
    k.box(4.6, 2.1, 3.6, 11.4, 2.5, 4, C.CHROME);
    k.box(4.6, 2.5, 3.6, 5, 9, 4, C.CHROME);
    k.box(11, 2.5, 3.6, 11.4, 9, 4, C.CHROME);
    k.box(5, 2.5, 3.8, 11, 9, 3.85, WHITE, {only: ['north'], faces: {north: GLASS}, uv: {north: [0, 0, 16, 16]}});

    // ---- the front --------------------------------------------------------------
    k.g('front');
    k.box(4.2, 10.6, 3.85, 11.8, 14.6, 4, WHITE);
    k.box(4.2, 12.1, 3.8, 11.8, 13.1, 3.85, RED, {only: ['north']});
    // the grade buttons
    k.box(4.6, 16.6, 3.7, 11.4, 18.9, 4, C.ALU);
    [C.LED_GREEN, C.YELLOW, C.BLUE].forEach((colour, i) => {
        k.box(5.1 + i * 2.2, 17.1, 3.5, 6.5 + i * 2.2, 18.4, 3.7, colour);
    });
    // the display
    k.box(4.2, 19.6, 3.6, 11.8, 26.4, 4, C.BLACK);
    k.box(4.5, 20, 3.5, 11.5, 26, 3.6, C.SCREEN, {shade: false});

    // ---- the roof and its sign --------------------------------------------------
    k.g('roof');
    k.box(2.5, 27, 3.4, 13.5, 28.2, 12.6, C.DARK_STEEL);
    k.box(3, 28.2, 4.4, 13, 30, 11.6, WHITE, {shade: false});
    k.box(2.95, 28.75, 4.35, 13.05, 29.45, 11.65, RED, {no: ['up', 'down'], shade: false});

    // ---- the back: a service door and the pipe up from the ground -----------------
    k.g('back');
    k.box(4.6, 3.2, 12, 11.4, 14.8, 12.25, C.RED_DARK);
    k.box(4.9, 3.5, 12.25, 11.1, 14.5, 12.3, RED, {only: ['south']});
    for (const y of [11.4, 12.3, 13.2]) {
        k.box(6, y, 12.3, 10, y + 0.45, 12.35, C.BLACK, {only: ['south']});
    }
    k.box(9.6, 6.8, 12.25, 10.2, 9, 12.7, C.CHROME);
    k.box(6.6, 1.5, 12, 9.4, 3.2, 13.4, C.DARK_STEEL);
    k.box(4.2, 19.6, 12, 11.8, 26.4, 12.4, C.BLACK);
    k.box(4.5, 20, 12.4, 11.5, 26, 12.5, C.SCREEN);

    // ---- the east side: the stop button -------------------------------------------
    k.g('stop');
    k.box(13, 21.4, 7, 13.25, 23.6, 9, C.YELLOW);
    k.box(13.25, 22, 7.5, 13.7, 23, 8.5, C.LED_RED);

    // ---- the west side: the boot, the outlet, and what hangs from them ------------
    k.g('boot');
    k.box(2.4, 18.6, 5.8, 3, 24.6, 8.4, C.DARK_STEEL);
    k.box(0.9, 22.3, 5.3, 2.4, 24.6, 7, C.DARK_STEEL);
    k.box(1.2, 22.3, 5.6, 2.1, 22.35, 6.7, C.BLACK, {only: ['down']});
    k.box(1.5, 20, 9, 3, 21, 10, C.CHROME);
    if (nozzle) {
        k.g('nozzle');
        k.box(1.35, 20.6, 5.8, 1.95, 22.3, 6.5, C.CHROME);
        k.box(1.05, 19.4, 5.7, 2.25, 20.8, 8.2, C.BLACK);
        k.box(0.95, 17.6, 7, 2.35, 19.4, 8.4, RED);
        k.box(1.4, 18, 6.3, 1.9, 19.4, 6.6, C.CHROME);
        k.box(1.4, 17.6, 5.9, 1.9, 17.8, 7, C.STEEL);
        k.box(1.4, 17.8, 5.9, 1.9, 19.4, 6.05, C.STEEL);
        k.box(1.25, 17, 7.3, 2.05, 17.6, 8.1, C.CHROME);
        k.g('hose');
        k.box(1.3, 9.8, 7.35, 2, 17, 8.05, C.RUBBER);
        k.box(1.3, 9.1, 7.35, 2, 9.8, 9.85, C.RUBBER);
        k.box(1.3, 9.8, 9.15, 2, 20, 9.85, C.RUBBER);
    }
}

const HALVES = {_lower: [0, 0, 0], _upper: [0, 1, 0]};
const EMPTY = {_lower_empty: [0, 0, 0], _upper_empty: [0, 1, 0]};

k.model('fuel_pump', {cells: HALVES, particle: 'car:block/pump_red'}, () => pump(true));
k.model('fuel_pump', {cells: EMPTY, particle: 'car:block/pump_red'}, () => pump(false));
