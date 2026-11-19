// A gate valve in a run of pipe: copper pipe in and out along north-south
// with a flange at either end, a cast body between two bolted flanges, a
// bonnet standing on it and a handwheel on a rising stem. Open, the wheel
// is yellow and stands high; shut, it is red and screwed down.
function valve(open) {
    const WHEEL = open ? C.YELLOW : C.RED, lift = open ? 1 : 0;

    k.g('pipe');
    k.round('z', 8, 8, 8, 3, 14, C.COPPER, {sides: 16, caps: null});
    for (const z of [0.5, 15.5]) {
        k.round('z', 8, 8, z, 3.6, 1, C.STEEL, {sides: 16});
    }

    // ---- the body -------------------------------------------------------------
    k.g('body');
    k.round('z', 8, 8, 8, 3.7, 4.4, C.DARK_STEEL, {sides: 16});
    for (const z of [5.4, 10.6]) {
        k.round('z', 8, 8, z, 4, 0.8, C.STEEL, {sides: 16});
    }
    k.box(5.6, 10.6, 5.8, 10.4, 11.8, 10.2, C.DARK_STEEL);
    k.box(5.2, 11.8, 5.4, 10.8, 12.4, 10.6, C.STEEL);
    for (const [x, z] of [[5.4, 5.6], [9.9, 5.6], [5.4, 9.7], [9.9, 9.7]]) {
        k.box(x, 12.4, z, x + 0.7, 12.7, z + 0.7, C.CHROME);
    }

    // ---- the bonnet, the stem and the wheel -----------------------------------
    k.g('bonnet');
    k.round('y', 8, 13, 8, 1.5, 1.2, C.DARK_STEEL);
    k.round('y', 8, 13.9 + lift / 2, 8, 0.5, 0.6 + lift, C.CHROME);
    k.g('wheel');
    k.round('y', 8, 14.5 + lift, 8, 3.5, 0.6, WHEEL, {sides: 16});
    k.round('y', 8, 14.55 + lift, 8, 2.6, 0.72, C.BLACK, {sides: 16});
    k.box(5.4, 14.2 + lift, 7.6, 10.6, 14.9 + lift, 8.4, WHEEL);
    k.box(7.6, 14.2 + lift, 5.4, 8.4, 14.9 + lift, 10.6, WHEEL);
    k.round('y', 8, 14.6 + lift, 8, 0.8, 0.9, C.CHROME);
}

k.model('fuel_valve', {particle: 'overthought_vehicles:block/fuel_valve'}, () => valve(true));
k.model('fuel_valve_shut', {particle: 'overthought_vehicles:block/fuel_valve'}, () => valve(false));
