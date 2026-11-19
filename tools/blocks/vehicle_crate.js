// The shipping crate a new vehicle leaves the workstation in, after a real
// export crate: a pallet of deck boards on three runners, four sides of
// boards framed in battens with a diagonal brace and steel corner plates, and
// a lid of boards with two battens across.
//
// What VehicleCrateRenderer counts on: the pallet's deck is 2 high; a side is
// drawn on the north, 0..14.5 across so the four of them, each turned a
// quarter about the middle, close round the crate like a pinwheel, and it
// falls over its bottom outside edge (y 2, z 0); the lid is 15..16 high and
// swings on its back top edge (y 16, z 0). Only the pallet is in the chunk.
const PLANK = C.WOOD, BATTEN = '#8A6A48', DECK = C.WOOD_DARK, PLATE = C.DARK_STEEL;

/** Draws, then turns what it drew a quarter turn q times about the middle, the way the renderer turns a side. */
function turned(q, draw) {
    const start = k.boxes.length;
    draw();
    const at = ([u, w]) => [[u, w], [-w, u], [-u, -w], [w, -u]][q];
    const point = p => {
        const [u, w] = at([p[0] - 8, p[2] - 8]);
        return [8 + u, p[1], 8 + w];
    };
    const side = {north: ['north', 'east', 'south', 'west'], east: ['east', 'south', 'west', 'north'],
        south: ['south', 'west', 'north', 'east'], west: ['west', 'north', 'east', 'south'], up: ['up', 'up', 'up', 'up'],
        down: ['down', 'down', 'down', 'down']};
    for (const b of k.boxes.slice(start)) {
        const a = point(b.from), c = point(b.to);
        b.from = [0, 1, 2].map(i => Math.min(a[i], c[i]));
        b.to = [0, 1, 2].map(i => Math.max(a[i], c[i]));
        const faces = {};
        for (const s in b.faces) {
            faces[side[s][q]] = b.faces[s];
        }
        b.faces = faces;
        if (b.rot) {
            // only a turn about z is drawn here: z goes to -x, -z, x in turn
            b.rot = {axis: q % 2 ? 'x' : 'z', angle: q === 0 || q === 3 ? b.rot.angle : -b.rot.angle, origin: point(b.rot.origin)};
        }
    }
}

function pallet() {
    // three runners under it, cut away between for the forks
    k.g('pallet/runners');
    for (const z of [1, 7, 13]) {
        k.box(0, 0.6, z, 16, 1.2, z + 2, DECK);
        for (const [x1, x2] of [[0, 3.2], [6.4, 9.6], [12.8, 16]]) {
            k.box(x1, 0, z, x2, 0.6, z + 2, DECK);
        }
    }
    // the deck: five boards across them, a finger's gap between
    k.g('pallet/deck');
    for (let i = 0; i < 5; i++) {
        const x = i * 3.25;
        k.box(x, 1.2, 0, x + 2.95 + (i === 4 ? 0.05 : 0), 2, 16, PLANK);
    }
}

function wall() {
    // four boards stacked, the seams between them dark
    k.g('side/boards');
    k.box(0, 2, 0.5, 14.5, 15, 1.5, PLANK);
    for (const y of [5.25, 8.5, 11.75]) {
        k.box(0, y - 0.08, 0.45, 14.5, y + 0.08, 0.5, DECK, {only: ['north']});
    }
    // the frame of battens round its outside, and the brace from corner to corner
    k.g('side/battens');
    k.box(0, 2, 0, 14.5, 3.4, 0.5, BATTEN);
    k.box(0, 13.6, 0, 14.5, 15, 0.5, BATTEN);
    k.box(0, 3.4, 0, 1.4, 13.6, 0.5, BATTEN);
    k.box(13.1, 3.4, 0, 14.5, 13.6, 0.5, BATTEN);
    k.box(0.05, 7.8, 0.05, 14.45, 9.2, 0.45, BATTEN, {rot: ['z', 45, [7.25, 8.5, 0.25]]});
    // steel plates over the frame's corners, each nailed four times
    k.g('side/plates');
    for (const [x, y] of [[0, 2], [12.7, 2], [0, 13.2], [12.7, 13.2]]) {
        k.box(x, y, -0.15, x + 1.8, y + 1.8, 0, PLATE, {no: ['south']});
        for (const [dx, dy] of [[0.45, 0.45], [1.35, 0.45], [0.45, 1.35], [1.35, 1.35]]) {
            k.box(x + dx - 0.15, y + dy - 0.15, -0.22, x + dx + 0.15, y + dy + 0.15, -0.15, C.CHROME, {no: ['south']});
        }
    }
}

function lid() {
    // boards from back to front, two battens across them
    k.g('lid/boards');
    k.box(0, 15, 0, 16, 16, 16, PLANK);
    for (const x of [4, 8, 12]) {
        k.box(x - 0.08, 15.95, 0, x + 0.08, 16, 16, DECK, {only: ['up']});
    }
    k.g('lid/battens');
    for (const z of [1.5, 12.5]) {
        k.box(0.5, 16, z, 15.5, 16.6, z + 2, BATTEN);
    }
    k.box(7.0, 16.6, 13.0, 9.0, 16.75, 14.0, PLATE);
}

k.model('vehicle_crate/base', {}, pallet);
k.model('vehicle_crate/wall', {}, wall);
k.model('vehicle_crate/lid', {}, lid);
k.model('vehicle_crate/closed', {}, () => {
    pallet();
    for (let q = 0; q < 4; q++) {
        turned(q, wall);
    }
    lid();
});
