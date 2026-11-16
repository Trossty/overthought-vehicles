// A charging post for the kerb: a white pedestal on a bolted foot, a head
// with a screen and a status light, a green cap, and the plug holstered on
// the east side with its cable hung in a loop. Charging, the screen and the
// light come on.
const BODY = C.WHITE, ACCENT = C.GREEN;

function post(lit) {
    k.g('foot');
    k.box(3.5, 0, 4.5, 12.5, 1, 11.5, C.DARK_STEEL);
    for (const [x, z] of [[4, 5], [11.4, 5], [4, 10.4], [11.4, 10.4]]) {
        k.box(x, 1, z, x + 0.6, 1.3, z + 0.6, C.CHROME);
    }
    k.box(4.8, 1, 5.8, 11.2, 1.8, 10.8, C.BLACK);

    k.g('pedestal');
    k.box(5.5, 1.8, 6.5, 10.5, 14, 10.5, BODY);
    k.box(7.2, 2.6, 6.44, 8.8, 13.4, 6.5, ACCENT, {only: ['north']});
    k.box(5, 13.4, 6, 11, 14, 11, C.BLACK);

    k.g('head');
    k.box(4.5, 14, 5.5, 11.5, 23, 11, BODY);
    k.box(4.2, 23, 5.2, 11.8, 23.8, 11.3, ACCENT);
    k.box(5.2, 16.9, 5.3, 10.8, 22.1, 5.5, C.BLACK);
    k.box(5.7, 17.4, 5.24, 10.3, 21.6, 5.3, lit ? C.LED_BLUE : C.SCREEN, {only: ['north'], shade: !lit});
    if (lit) {
        // a battery filling up
        k.box(6.6, 18.4, 5.2, 9.4, 20.6, 5.24, C.WHITE, {only: ['north'], shade: false});
        k.box(6.9, 18.7, 5.17, 8.3, 20.3, 5.2, C.GREEN, {only: ['north'], shade: false});
    }
    k.box(7.2, 15.1, 5.3, 8.8, 16.1, 5.5, lit ? C.LED_GREEN : C.GREEN_DARK, {shade: !lit});

    // ---- the plug and its cable ----------------------------------------------------
    k.g('holster');
    k.box(11.5, 9.4, 7, 12.9, 12.8, 9.6, C.DARK_STEEL);
    k.g('plug');
    k.box(11.8, 12.8, 7.5, 12.8, 15, 9.1, C.BLACK);
    k.box(11.6, 15, 7.3, 13, 15.7, 9.3, C.BLACK);
    k.box(12.8, 13.6, 7.9, 13, 14.4, 8.7, ACCENT);
    k.g('cable');
    k.box(11.9, 15.7, 8.6, 12.7, 16.5, 10.6, C.RUBBER);
    k.box(11.9, 4.2, 9.9, 12.7, 15.7, 10.6, C.RUBBER);
    k.box(10.5, 4.2, 9.7, 11.9, 5, 10.5, C.RUBBER);
}

k.model('charging_post', {particle: 'minecraft:block/white_concrete'}, () => post(false));
k.model('charging_post_lit', {particle: 'minecraft:block/white_concrete'}, () => post(true));
