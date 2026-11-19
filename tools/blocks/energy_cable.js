// Power cable. As with the pipe, the blockstate turns the arm drawn here,
// which runs north, towards each neighbour: a round black cable with a
// collar at the block's face and its copper showing at the cut. The
// junction is a copper box with steel corners, and where a cable meets a
// machine it ends in a bolted plate with its plug pushed into the machine.
// The glowing strips of a live cable are models of their own, laid along
// the top and bottom of these, so the cable stays four across and the box
// six.
const CABLE = '#1E1E20';

function arm(length) {
    k.round('z', 8, 8, length / 2, 2, length, CABLE, {caps: C.COPPER});
    k.round('z', 8, 8, 0.6, 2.35, 1.2, C.DARK_STEEL);
}

function box() {
    k.box(5, 5, 5, 11, 11, 11, C.COPPER);
    for (const x of [5, 10.2]) {
        for (const y of [5, 10.2]) {
            for (const z of [5, 10.2]) {
                k.box(x, y, z, x + 0.8, y + 0.8, z + 0.8, C.DARK_STEEL);
            }
        }
    }
}

k.model('energy_cable_arm', {particle: 'overthought_vehicles:block/energy_cable'}, () => arm(8));
k.model('energy_cable_core', {particle: 'overthought_vehicles:block/energy_cable'}, box);

k.model('energy_cable_plate', {particle: 'overthought_vehicles:block/energy_cable'}, () => {
    k.box(3, 3, 0, 13, 13, 1.2, C.STEEL);
    for (const [x, y] of [[3.6, 3.6], [11.6, 3.6], [3.6, 11.6], [11.6, 11.6]]) {
        k.box(x, y, 1.2, x + 0.8, y + 0.8, 1.5, C.CHROME);
    }
    k.round('z', 8, 8, 2.1, 3, 1.8, C.COPPER);
    k.box(5.5, 5.5, -3.5, 10.5, 10.5, 0, C.DARK_STEEL);
});

k.model('energy_cable_inventory', {particle: 'overthought_vehicles:block/energy_cable'}, () => {
    arm(16);
    k.round('z', 8, 8, 15.4, 2.35, 1.2, C.DARK_STEEL);
    box();
});
