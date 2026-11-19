// A two-hundred-litre steel drum, round: two rolling hoops pressed into the
// body, a rim top and bottom, a lid sunk inside the top rim with its two
// bungs, and the hazard placard riveted on the front. The body takes the
// colour of what the drum holds (tint 0), so it is drawn near white.
const BODY = '#F2F2F2', HOOP = '#DADADA', RIM = '#C4C4C4';
const tinted = {sides: 16, tint: 0, caps: null};

k.model('fuel_drum', {particle: 'overthought_vehicles:block/drum_body'}, () => {
    k.g('body');
    k.round('y', 8, 8, 8, 6.6, 15, BODY, tinted);
    k.round('y', 8, 5.4, 8, 6.95, 0.9, HOOP, Object.assign({}, tinted, {caps: HOOP}));
    k.round('y', 8, 10.6, 8, 6.95, 0.9, HOOP, Object.assign({}, tinted, {caps: HOOP}));
    k.round('y', 8, 0.5, 8, 7, 1, RIM, Object.assign({}, tinted, {caps: RIM}));
    k.round('y', 8, 15.5, 8, 7, 1, RIM, Object.assign({}, tinted, {caps: RIM}));

    // ---- the lid and its bungs ------------------------------------------------
    k.g('lid');
    k.round('y', 8, 15.35, 8, 6.3, 0.3, C.ALU, {sides: 16});
    k.round('y', 10.6, 15.85, 4.6, 1.15, 0.7, C.DARK_STEEL);
    k.round('y', 10.6, 16.25, 4.6, 0.6, 0.3, C.STEEL);
    k.round('y', 5.2, 15.75, 11.2, 0.7, 0.5, C.DARK_STEEL);

    // ---- the placard ------------------------------------------------------------
    k.g('placard');
    k.box(6.5, 6.3, 1.2, 9.5, 9.7, 1.4, C.YELLOW, {faces: {north: 'overthought_vehicles:block/drum_label'}, uv: {north: [0, 0, 16, 16]}});
});
