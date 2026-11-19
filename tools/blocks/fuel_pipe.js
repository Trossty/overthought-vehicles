// Copper fuel pipe. The blockstate lays an arm towards every neighbour the
// pipe joins, turned from the one drawn here that runs north, and adds the
// fitting where a run bends or branches. An arm is half a block of round
// pipe with a steel flange at the block's face; the fitting is a cast
// block with its edges taken off.
function arm() {
    k.round('z', 8, 8, 4.5, 3, 7, C.COPPER, {sides: 16, caps: null});
    k.round('z', 8, 8, 0.5, 3.6, 1, C.STEEL, {sides: 16});
}

k.model('fuel_pipe_arm', {particle: 'overthought_vehicles:block/fuel_pipe'}, arm);

k.model('fuel_pipe_core', {particle: 'overthought_vehicles:block/fuel_pipe'}, () => {
    k.box(4.4, 5, 5, 11.6, 11, 11, C.DARK_STEEL);
    k.box(5, 4.4, 5, 11, 11.6, 11, C.DARK_STEEL);
    k.box(5, 5, 4.4, 11, 11, 11.6, C.DARK_STEEL);
});

k.model('fuel_pipe_inventory', {particle: 'overthought_vehicles:block/fuel_pipe'}, () => {
    k.round('z', 8, 8, 8, 3, 14, C.COPPER, {sides: 16, caps: null});
    k.round('z', 8, 8, 0.5, 3.6, 1, C.STEEL, {sides: 16});
    k.round('z', 8, 8, 15.5, 3.6, 1, C.STEEL, {sides: 16});
});
