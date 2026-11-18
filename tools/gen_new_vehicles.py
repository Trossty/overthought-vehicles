"""The vehicles added after GenVehicles: their definitions and their item icons.
Run: python3 tools/gen_new_vehicles.py

Each vehicle is drawn here in boxes, in the definition's own units (1/16 of a
block, which is a metre at the scale everything is drawn: x to the left, y up,
z to the front), and written twice: as the data definition the game builds
the vehicle from, and as the item model that stands for it in the hand.

The icons share GenVehicles' palette. This never rewrites it: colours it
already has keep their cells, new ones go in the free cells after them, so
the icons GenVehicles drew keep their colours (see the palette trap in the
notes). Running this again changes nothing.
"""
import json
import math
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'src', 'main', 'resources')
DEFINITIONS = os.path.join(RES, 'data', 'car', 'car', 'vehicle')
ICONS = os.path.join(RES, 'assets', 'car', 'models', 'item')
PALETTE = os.path.join(RES, 'assets', 'car', 'textures', 'item', 'vehicle_palette.png')

GLASS = '#55A9D3E6'
HEAD_LAMP = '#FFFDF2'
TAIL_LAMP = '#EE4A3C'
TYRE = '#202023'
HUB = '#C4C8CD'
TINTS = {'paint': 0, 'paint_dark': 1, 'paint_light': 2}

ICON_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.72, 0.72, 0.72]},
    'ground': {'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
    'fixed': {'rotation': [0, 90, 0], 'scale': [0.7, 0.7, 0.7]},
    'head': {'scale': [0.8, 0.8, 0.8]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.35, 0.35, 0.35]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.35, 0.35, 0.35]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3.5, 0], 'scale': [0.24, 0.24, 0.24]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 3.5, 0], 'scale': [0.24, 0.24, 0.24]},
}


def n(v):
    """A number as the definitions write them: short, and whole when it is."""
    v = round(float(v), 2)
    return int(v) if v == int(v) else v


class Vehicle:
    def __init__(self, name, header):
        self.name = name
        self.header = header
        self.seats = []
        self.wheels = []
        self.shape = []

    def box(self, f, t, color='paint', icon=True, **turns):
        """A box. {@code icon=False} leaves it out of the item; 'only' puts it in the item alone."""
        b = {'from': [n(c) for c in f], 'to': [n(c) for c in t], 'color': color}
        for key in ('tilt', 'roll', 'yaw'):
            if turns.get(key):
                b[key] = n(turns[key])
        if turns.get('steers'):
            b['steers'] = True
        if turns.get('spins'):
            b['spins'] = True
            if turns.get('axis', 'z') != 'z':
                b['spin_axis'] = turns['axis']
        b['_icon'] = icon
        self.shape.append(b)
        return b

    def pair(self, f, t, color='paint', icon=True, **turns):
        """A box on the left and its mirror on the right."""
        self.box(f, t, color, icon, **turns)
        mirrored = dict(turns)
        for key in ('roll', 'yaw'):
            if key in mirrored:
                mirrored[key] = -mirrored[key]
        self.box((-t[0], f[1], f[2]), (-f[0], t[1], t[2]), color, icon, **mirrored)

    def seat(self, x, y, z):
        self.seats.append([n(x), n(y), n(z)])

    def wheel(self, x, z, radius, width, steers=False, pair=True):
        self.wheels.append({'at': [n(x), n(radius), n(z)], 'radius': n(radius), 'width': n(width), 'steers': steers})
        if pair and x:
            self.wheels.append({'at': [n(-x), n(radius), n(z)], 'radius': n(radius), 'width': n(width),
                                'steers': steers})

    # ---- the definition ----

    def definition(self):
        lines = ['{']
        for key, value in self.header.items():
            lines.append('  "%s": %s,' % (key, inline(value)))
        lines.append('  "seats": [')
        lines.append(',\n'.join('    ' + inline(s) for s in self.seats))
        lines.append('  ],')
        lines.append('  "wheels": [')
        lines.append(',\n'.join('    ' + inline(w) for w in self.wheels))
        lines.append('  ],')
        lines.append('  "shape": [')
        boxes = [{k: v for k, v in b.items() if not k.startswith('_')} for b in self.shape if b['_icon'] != 'only']
        lines.append(',\n'.join('    ' + inline(b) for b in boxes))
        lines.append('  ]')
        lines.append('}')
        return '\n'.join(line for line in lines if line) + '\n'

    # ---- the icon ----

    def icon(self, palette):
        drawn = [b for b in self.shape if b['_icon'] and len(b['color']) != 9]  # glass is left out
        solids = [(b['from'], b['to']) for b in drawn]
        for w in self.wheels:
            x, y, z = w['at']
            solids.append(((x, 0, z - w['radius']), (x, 2 * w['radius'], z + w['radius'])))
        back = min(min(f[2], t[2]) for f, t in solids)
        front = max(max(f[2], t[2]) for f, t in solids)
        top = max(max(f[1], t[1]) for f, t in solids)
        scale = 22.0 / (front - back)
        middle = (front + back) / 2
        lift = 8 - top * scale / 2
        elements = []

        def element(f, t, color, tilt=0.0):
            index = palette.cell(color)
            u, v = index % 16 + 0.25, index // 16 + 0.25
            face = {'uv': [u, v, u + 0.5, v + 0.5], 'texture': '#p'}
            if color in TINTS:
                face['tintindex'] = TINTS[color]
            # An item model has north as -z, which is where the nose goes.
            e = {'from': [n(8 + f[0] * scale), n(lift + f[1] * scale), n(8 - (t[2] - middle) * scale)],
                 'to': [n(8 + t[0] * scale), n(lift + t[1] * scale), n(8 - (f[2] - middle) * scale)]}
            snapped = max(-45.0, min(45.0, round(tilt / 22.5) * 22.5))
            if snapped:
                e['rotation'] = {'origin': [n(8 + (f[0] + t[0]) / 2 * scale), n(lift + (f[1] + t[1]) / 2 * scale),
                                            n(8 - ((f[2] + t[2]) / 2 - middle) * scale)],
                                 'axis': 'x', 'angle': n(snapped)}
            e['faces'] = {side: dict(face) for side in ('north', 'south', 'east', 'west', 'up', 'down')}
            elements.append(e)

        for b in drawn:
            element(b['from'], b['to'], b['color'], b.get('tilt', 0.0))
        for w in self.wheels:
            x, y, z = w['at']
            r, h = w['radius'], w['width'] / 2
            # Round, the way the vehicle's own wheels are: eight slabs about the axle.
            reach, side = r * math.cos(math.pi / 16), r * math.sin(math.pi / 16)
            for turn in (-45, -22.5, 0, 22.5):
                element((x - h, y - reach, z - side), (x + h, y + reach, z + side), TYRE, turn)
                element((x - h, y - side, z - reach), (x + h, y + side, z + reach), TYRE, turn)
            rim = r * 0.58
            element((x - h - 0.3, y - rim * 0.92, z - rim * 0.38), (x + h + 0.3, y + rim * 0.92, z + rim * 0.38), HUB)
            element((x - h - 0.3, y - rim * 0.38, z - rim * 0.92), (x + h + 0.3, y + rim * 0.38, z + rim * 0.92), HUB)
            element((x - h - 0.3, y - rim * 0.7, z - rim * 0.7), (x + h + 0.3, y + rim * 0.7, z + rim * 0.7), HUB)
        lines = ['{',
                 '  "textures": { "particle": "car:item/vehicle_palette", "p": "car:item/vehicle_palette" },',
                 '  "elements": [',
                 ',\n'.join('    ' + inline(e) for e in elements),
                 '  ],',
                 '  "display": ' + json.dumps(ICON_DISPLAY, indent=4).replace('\n', '\n  '),
                 '}']
        return '\n'.join(lines) + '\n'

    def write(self, palette):
        with open(os.path.join(DEFINITIONS, self.name + '.json'), 'w') as out:
            out.write(self.definition())
        with open(os.path.join(ICONS, 'vehicle_' + self.name + '.json'), 'w') as out:
            out.write(self.icon(palette))
        print('wrote', self.name, '(%d boxes)' % sum(1 for b in self.shape if b['_icon'] != 'only'))


def inline(value):
    """JSON on one line, spaced the way the hand-kept files are."""
    if isinstance(value, dict):
        return '{ ' + ', '.join('"%s": %s' % (k, inline(v)) for k, v in value.items()) + ' }'
    if isinstance(value, list):
        if value and all(isinstance(v, (int, float)) for v in value):
            return '[' + ', '.join(json.dumps(v) for v in value) + ']'
        return '[ ' + ', '.join(inline(v) for v in value) + ' ]'
    return json.dumps(value)


class Palette:
    """GenVehicles' 16 by 16 palette. Its unused cells repeat the last colour it used."""

    def __init__(self, path):
        self.path = path
        image = Image.open(path).convert('RGBA')
        self.cells = [image.getpixel((i % 16, i // 16))[:3] for i in range(256)]
        last = self.cells[-1]
        used = 256
        while used > 1 and self.cells[used - 2] == last:
            used -= 1
        self.used = used
        self.added = 0

    def cell(self, color):
        # Paint is drawn white and tinted in the game by the item's paint.
        rgb = (255, 255, 255) if color in TINTS else tuple(int(color[i:i + 2], 16) for i in (1, 3, 5))
        for i in range(self.used):
            if self.cells[i] == rgb:
                return i
        if self.used >= 256:
            raise SystemExit('the vehicle palette is full')
        self.cells[self.used] = rgb
        self.used += 1
        self.added += 1
        return self.used - 1

    def save(self):
        image = Image.new('RGBA', (16, 16))
        for i in range(256):
            image.putpixel((i % 16, i // 16), self.cells[min(i, self.used - 1)] + (255,))
        image.save(self.path)
        print('palette: %d colours, %d new' % (self.used, self.added))


# ---------------------------------------------------------------------------
# Robinson R44: a four-seat light helicopter. Bubble cabin on skids, a mast
# fairing over the engine behind it, a thin tail boom, a two-blade main rotor
# ten metres across and a two-blade tail rotor on the left of the tail.
# ---------------------------------------------------------------------------

def raked_half(inner_bottom, inner_top, outer_x, flare):
    """
    The left half of a windscreen that rakes back and also opens out to the
    side: its inner edge runs up the middle from {@code inner_bottom} to
    {@code inner_top}, and it swings out about that edge by {@code flare}
    degrees, so that seen from above the two halves meet in a point. Returns
    the box's corners and its turns, and the points its outer edge runs
    between, for whatever has to meet it.
    """
    by, bz = inner_bottom[1], inner_bottom[2]
    ty, tz = inner_top[1], inner_top[2]
    height = math.hypot(ty - by, tz - bz)
    up, back = (ty - by) / height, (bz - tz) / height        # the inner edge: (0, up, -back)
    s, c = math.sin(math.radians(flare)), math.cos(math.radians(flare))
    across = (c, -s * back, -s * up)                         # out along the bottom edge
    width = outer_x / c
    yaw_sin = s * up
    yaw_cos = math.sqrt(1 - yaw_sin * yaw_sin)
    yaw = math.degrees(math.asin(yaw_sin))
    roll = math.degrees(math.atan2(-s * back / yaw_cos, c / yaw_cos))
    tilt = math.degrees(math.asin(back / yaw_cos))
    middle = [inner_bottom[i] + across[i] * width / 2 + (0, up, -back)[i] * height / 2 for i in range(3)]
    f = (middle[0] - width / 2, middle[1] - height / 2, middle[2] - 0.3)
    t = (middle[0] + width / 2, middle[1] + height / 2, middle[2] + 0.3)
    outer_bottom = [inner_bottom[i] + across[i] * width for i in range(3)]
    outer_top = [inner_top[i] + across[i] * width for i in range(3)]
    return f, t, {'tilt': tilt, 'yaw': yaw, 'roll': roll}, outer_bottom, outer_top


def robinson_r44():
    v = Vehicle('robinson_r44', {
        'chassis': 'helicopter', 'width': 2.0, 'height': 2.9, 'color': 'red',
        'performance': {'speed': 1.3, 'acceleration': 0.8, 'fuel_use': 1.3, 'braking': 0.8, 'steering': 0.8,
                        'offroad': 0.3},
        'factory': {'engine': 'v6', 'fuel': 'petrol', 'tyres': 'road'},
        'tank': 176,
        'lights': {'length': 14, 'spread': 2.5},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 10}, {'item': 'minecraft:glass_pane', 'count': 4},
                      {'item': 'minecraft:white_wool', 'count': 2}],
    })
    # The pilot flies from the right front seat. The seats sit low, and the
    # roof stands high enough over them for a head to fit under it.
    v.seat(-4.5, 13, 12)
    v.seat(4.5, 13, 12)
    v.seat(-4.5, 13.5, -2)
    v.seat(4.5, 13.5, -2)

    alu, dark, darker, metal, blade = '#B4B8BD', '#2A2A2C', '#1B1B1D', '#5A5A5E', '#C9CCD0'

    # Skids: two tubes on the ground, turned up at the front, on two arched
    # cross tubes that meet the belly.
    v.pair((16.6, 0, -24), (18.4, 1.8, 21.5), alu)
    v.pair((16.6, 2.0, 21.5), (18.4, 3.8, 28.5), alu, tilt=30)
    for z0 in (11, -13):
        v.box((-9.5, 7.6, z0), (9.5, 9.0, z0 + 2), alu)
        v.pair((8.35, 4.3, z0), (18.65, 5.8, z0 + 2), alu, roll=-39.1)

    # The bubble. The windscreen comes down nearly to the floor and goes up
    # into the roof in one sweep, in two halves that meet in a point over a
    # post up the middle; seen from above the cabin is a teardrop.
    bottom, top = (0, 15.5, 35), (0, 38.5, 18.5)
    f, t, turns, low, high = raked_half(bottom, top, 10, 20)
    v.pair(f, t, GLASS, **turns)
    rake = math.degrees(math.atan2(bottom[2] - top[2], top[1] - bottom[1]))
    length = math.hypot(top[1] - bottom[1], bottom[2] - top[2])
    mid_y, mid_z = (top[1] + bottom[1]) / 2, (top[2] + bottom[2]) / 2
    v.box((-0.6, mid_y - length / 2, mid_z - 0.6), (0.6, mid_y + length / 2, mid_z + 0.6), tilt=rake)
    # The pillar down each side of it.
    pillar_y, pillar_z = (low[1] + high[1]) / 2, (low[2] + high[2]) / 2
    v.pair((9.4, pillar_y - length / 2, pillar_z - 0.6), (10.3, pillar_y + length / 2, pillar_z + 0.6), tilt=rake)

    def edge(x):
        """Where the bottom of the windscreen is, a way out from the middle."""
        share = x / 10.0
        return bottom[1] + (low[1] - bottom[1]) * share, bottom[2] + (low[2] - bottom[2]) * share

    # Under it, the nose, rounded under the chin, with the landing lights.
    for x0 in (0, 2, 4, 6, 8):
        y, z = edge(x0 + 1)
        v.pair((x0, 11, 24), (x0 + 2, y, z))
        v.pair((x0, 9, 24), (x0 + 2, 11, z - 2.5))
    y, z = edge(3)
    v.pair((2, 12, z), (4, 13.4, z + 0.3), HEAD_LAMP)

    # The floor, the bulkhead behind the back seats, and the roof, which falls
    # away to the sides to meet the top of the windscreen and the doors.
    v.box((-9.4, 9, -8), (9.4, 11, 24))
    v.box((-9.4, 11, -8), (9.4, 12, 24), dark, icon=False)
    v.box((-10, 11, -8), (10, high[1], -6))
    fall = (top[1] - high[1]) / 10.0
    for x0 in (0, 2.5, 5, 7.5):
        middle = x0 + 1.25
        y = top[1] - fall * middle
        z = top[2] + (high[2] - top[2]) * middle / 10.0
        v.pair((x0 - 0.1, y, -8), (x0 + 2.6, y + 2, z), roll=-math.degrees(math.atan(fall)))

    # The sides: door skins below the windows, a big front door window and a
    # smaller back one, the posts between, and below the windscreen's edge
    # the skin ahead of the front door, stepped up under the pillar.
    v.pair((9.4, 11, -8), (10, 17, 26))
    v.pair((9.4, 11, 26), (10, 14, 29))
    v.pair((10, 14.5, -8), (10.05, 15.5, 25), 'paint_dark')
    rail = high[1] - 0.8
    v.pair((9.6, 17, 1), (9.8, rail, 14), GLASS)
    v.pair((9.6, 17, -6), (9.8, rail, 0), GLASS)
    v.pair((9.3, 17, 0), (10.1, rail, 1))
    v.pair((9.3, 17, -7), (10.1, rail, -6))
    v.pair((9.3, rail, -8), (10.1, high[1], 15))
    slope = (high[1] - low[1]) / (low[2] - high[2])
    for z0 in range(14, 27):
        reach = low[1] + (low[2] - (z0 + 1)) * slope
        if reach > 17:
            v.pair((9.4, 17, z0), (10, reach, z0 + 1))
    v.pair((10, 19, 10), (10.2, 19.6, 12), alu)
    v.pair((10, 19, -4), (10.2, 19.6, -2), alu)

    # Inside: four seats, the instrument panel on its pedestal up against the
    # windscreen, and the T-bar cyclic both front seats can reach.
    seat = '#4A4A50'
    v.pair((1, 12, 9.5), (8, 13, 16), seat, icon=False)
    v.pair((1, 13, 8), (8, 27, 9.5), seat, icon=False)
    v.pair((1, 12, -4.5), (8, 13.5, 1.5), seat, icon=False)
    v.pair((1, 13.5, -6), (8, 28, -4.5), seat, icon=False)
    v.box((-3, 12, 24), (3, 21, 28.5), dark, icon=False)
    v.box((-3.5, 21, 23.5), (3.5, 23, 26.5), darker, icon=False)
    v.box((-0.4, 12, 21.5), (0.4, 20, 22.5), darker, icon=False)
    v.box((-6, 20, 21.5), (6, 21, 22.5), darker, icon=False)

    # Over the back of the cabin, the round fairing about the mast.
    roof = top[1] + 2
    v.box((-7, roof - 1.5, -8), (7, roof + 2, 8))
    v.box((-5.5, roof + 2, -6), (5.5, roof + 4.5, 5))
    v.box((-3.5, roof + 4.5, -3), (3.5, roof + 5.5, 3))

    # Behind the cabin the body narrows and drops towards the tail cone; the
    # engine sits low in it, its underside and the frame open to see, the
    # cooling intake on the left, the exhaust out low on the right.
    v.box((-8, 14, -15), (8, roof - 1.5, -8))
    v.box((-7, 16, -21), (7, roof - 5, -15))
    v.box((-5.5, 18, -27), (5.5, roof - 8.5, -21))
    v.box((-4, 20, -32), (4, roof - 12, -27))
    v.box((7.9, 18, -14), (8.1, 27, -9), dark)
    for y in (19.5, 22, 24.5):
        v.box((8.1, y, -13.5), (8.2, y + 0.8, -9.5), metal)
    v.box((-5, 11, -20), (5, 14, -9), darker)
    v.box((-4, 12, -24), (4, 16, -20), metal)
    v.pair((6.5, 9, -9.5), (7.3, 14, -8.7), metal)
    v.pair((5.5, 9, -18), (6.3, 16, -17.2), metal)
    v.box((-7.5, 13, -26), (-5.5, 15, -14), metal)

    # The tail cone: thin, tapering and rising a little all the way back.
    slices = 10
    for i in range(slices):
        share = i / (slices - 1)
        half = 3.0 - 1.7 * share
        y = 24 + 4.5 * share
        z = -32 - 7 * i
        v.box((-half, y - half, z - 7), (half, y + half, z))
    # The tail: a small stabiliser, a fin above and a longer one below, swept
    # back, a guard under the lower one, a beacon on top.
    v.pair((1.3, 28, -98), (6.5, 29, -92))
    v.box((-0.5, 30, -108), (0.5, 41, -102), tilt=20)
    v.box((-0.5, 18, -106), (0.5, 27.5, -101), tilt=20)
    v.box((-0.4, 16.8, -103), (0.4, 17.8, -95), alu)
    v.box((-0.7, 40.2, -108), (0.7, 41.4, -105.6), TAIL_LAMP)
    # The tail rotor, on the left: its gearbox, then the rotor, which turns
    # about its hub, so the hub comes first.
    v.box((1.3, 27, -105), (3.3, 30.5, -101), metal)
    v.box((3.3, 27.25, -104.5), (4.8, 30.25, -101.5), dark, spins=True, axis='x')
    v.box((3.7, 18.95, -104), (4.4, 38.55, -102), blade, spins=True, axis='x')
    v.box((3.65, 16.95, -104.05), (4.45, 18.95, -101.95), '#C42620', spins=True, axis='x')
    v.box((3.65, 38.55, -104.05), (4.45, 40.55, -101.95), '#C42620', spins=True, axis='x')

    # The main rotor: the mast and its swashplate, then the hub, which the
    # blades turn about, and the two blades, ten metres from tip to tip.
    hub = roof + 12
    v.box((-1.2, roof + 5.5, -1.2), (1.2, hub, 1.2), metal)
    v.box((-2.5, roof + 7, -2.5), (2.5, roof + 8.5, 2.5), '#8C9197')
    v.box((-2.5, hub, -2.5), (2.5, hub + 2.5, 2.5), dark, spins=True, axis='y')
    v.box((-1.5, hub + 0.5, 2.5), (1.5, hub + 2, 8), metal, spins=True, axis='y')
    v.box((-1.5, hub + 0.5, -8), (1.5, hub + 2, -2.5), metal, spins=True, axis='y')
    v.box((-2, hub + 0.9, 8), (2, hub + 1.5, 80), blade, spins=True, axis='y')
    v.box((-2, hub + 0.9, -80), (2, hub + 1.5, -8), blade, spins=True, axis='y')
    return v


GREY = '#4A4B4F'
DARK = '#2A2A2C'
STEEL = '#8C9197'


def massey_ferguson_240():
    v = Vehicle('massey_ferguson_240', {
        'chassis': 'car', 'width': 1.8, 'height': 1.9, 'color': 'red',
        'performance': {'speed': 0.45, 'acceleration': 0.85, 'fuel_use': 0.9, 'braking': 0.9, 'steering': 0.85,
                        'offroad': 0.95},
        'factory': {'engine': 'inline4', 'fuel': 'diesel', 'tyres': 'dirt'},
        'tank': 47,
        'lights': {'length': 12, 'spread': 2},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 8}, {'item': 'minecraft:black_wool', 'count': 1}],
    })
    v.seat(0, 17, -12)
    # Big drive wheels at the back, small ones that steer at the front.
    v.wheel(11.4, -10, 10, 5.5)
    v.wheel(10.5, 19, 6, 2.5, steers=True)

    # Underneath: the cast front bolster and the axle beam across it, the
    # engine block, and the gearbox and rear axle housing it bolts to.
    v.box((-4, 4, 22), (4, 12, 30), GREY)
    v.box((-9.2, 5, 18), (9.2, 7, 20), GREY)
    v.box((-4.5, 7, 6), (4.5, 14, 22), GREY)
    v.box((-5, 6, -16), (5, 15, 6), GREY)
    v.pair((5, 8.5, -12), (8.7, 11.5, -8), GREY)
    # The bonnet, its nose with the grille and the lamps either side of it,
    # and the louvres in its flanks.
    v.box((-5.5, 14, 7), (5.5, 20, 30))
    v.box((-5.5, 12, 30), (5.5, 20, 31))
    v.box((-3.2, 12.6, 31), (3.2, 19.2, 31.3), DARK)
    v.pair((3.6, 16.6, 31), (5.1, 18.4, 31.4), HEAD_LAMP)
    v.pair((5.5, 15, 23), (5.6, 19, 29), DARK)
    # The exhaust stack up the right of the bonnet, the air cleaner on the left.
    v.box((-4.8, 20, 19.7), (-2.7, 26, 21.8), DARK)
    v.box((-4.4, 26, 20.1), (-3.1, 32, 21.4), DARK)
    v.box((2.9, 20, 21.9), (4.6, 24, 23.6), DARK)
    v.box((2.6, 24, 21.6), (4.9, 25, 23.9), DARK)
    # The dash between the bonnet and the driver, the steering column out of
    # its top, the footplates either side, and the seat on its spring.
    v.box((-4, 14, 0), (4, 23.5, 7), '#3A3A3E')
    v.box((-0.8, 23.5, 0.4), (0.8, 24.8, 2.6), DARK)
    v.pair((5, 9, -6), (8.4, 10, 5), GREY)
    v.box((-1.5, 15, -15), (1.5, 15.5, -11), GREY)
    v.box((-4.5, 15.5, -17), (4.5, 17, -9.5), DARK)
    v.box((-4.5, 17, -18), (4.5, 23, -16.5), DARK)
    # The flat-topped rear wings over the drive wheels, and the tail lamps on
    # their backs.
    v.pair((8, 21.5, -16), (15, 22.5, -4))
    v.pair((8, 19.1, -5), (15, 20.1, 1), tilt=-45)
    v.pair((8, 19.1, -21), (15, 20.1, -15), tilt=45)
    v.pair((8, 13, 0.2), (15, 17.8, 1.2))
    v.pair((8, 13, -21.2), (15, 17.8, -20.2))
    v.pair((12.5, 15, -21.5), (14.5, 16.5, -21.2), TAIL_LAMP)
    # At the back: the lift arms of the three-point linkage and the drawbar.
    v.pair((4, 9, -24), (5, 10.5, -14), GREY)
    v.box((-1.5, 7, -26), (1.5, 8.5, -16), GREY)
    return v


def seed_drill():
    v = Vehicle('seed_drill', {
        'chassis': 'trailer', 'implement': 'seeder', 'width': 3.2, 'height': 1.75, 'color': 'blue',
        'performance': {}, 'factory': {}, 'tank': 0,
        'materials': [{'item': 'minecraft:hopper', 'count': 1}, {'item': 'minecraft:iron_ingot', 'count': 4}],
    })
    v.wheel(24, 0, 6.5, 3)
    # The seed box across the whole width, narrowing to its outlets, and its lid.
    v.box((-21, 14, -6), (21, 26, 6))
    v.box((-21, 10, -4), (21, 14, 4))
    v.box((-21.5, 26, -6.5), (21.5, 27, 6.5), 'paint_dark')
    # The frame under it, and the arms out to the wheels.
    v.box((-22, 8, -5.5), (22, 10, -3.5), GREY)
    v.box((-22, 8, 3.5), (22, 10, 5.5), GREY)
    v.pair((21, 5.5, -1), (22.5, 7.5, 1), GREY)
    # A tube down from every outlet to the disc that opens the drill for it.
    for i in range(8):
        x = -18.9 + i * 5.4
        v.box((x - 0.4, 3.5, -0.8), (x + 0.4, 10, 0), DARK)
        v.box((x - 0.4, 0, -2.5), (x + 0.4, 4.5, 1.5), STEEL)
    # The step to fill it from behind, the tail lamps either end.
    v.box((-20, 9, -8), (20, 9.6, -6), GREY)
    v.pair((18.5, 12, -6.3), (20.5, 13.5, -6), TAIL_LAMP)
    # The A-frame drawbar to the hitch eye, and the stand it rests on unhitched.
    v.pair((6.75, 7, 4.3), (8.25, 8.5, 30.7), GREY, yaw=-29.5)
    v.box((-1.5, 7, 28), (1.5, 8.5, 32), GREY)
    v.box((-3, 7, 25), (3, 8.5, 27), GREY)
    v.box((-0.6, 0, 25.5), (0.6, 7, 26.7), GREY)
    return v


def fertilizer_spreader():
    v = Vehicle('fertilizer_spreader', {
        'chassis': 'trailer', 'implement': 'fertilizer', 'width': 2.4, 'height': 1.95, 'color': 'orange',
        'performance': {}, 'factory': {}, 'tank': 0,
        'materials': [{'item': 'minecraft:hopper', 'count': 1}, {'item': 'minecraft:iron_ingot', 'count': 4}],
    })
    v.wheel(17, 0, 7, 4)
    # The hopper, tapering in steps to its outlet, with a rim round the top.
    v.box((-14, 24, -10), (14, 30, 10))
    v.box((-12, 21, -9), (12, 24, 9))
    v.box((-10, 18, -7.5), (10, 21, 7.5))
    v.box((-7.5, 15, -6), (7.5, 18, 6))
    v.box((-14.5, 30, -10.5), (14.5, 31, 10.5), 'paint_dark')
    # The chute from the outlet back to the two spinning discs that throw it.
    v.box((-6, 12, -9), (6, 15, -4), 'paint_dark')
    v.pair((1.5, 10.6, -13), (9.5, 11.2, -5), '#B4B8BD')
    v.pair((5.2, 11.2, -9.5), (5.8, 12, -8.5), STEEL)
    # The frame and axle under it all.
    v.pair((11, 9, -13), (13, 11, 11), GREY)
    v.box((-13, 9, -13.5), (13, 10.5, -11.5), GREY)
    v.box((-13, 9, 9), (13, 10.5, 11), GREY)
    v.box((-15, 6, -1), (15, 8, 1), GREY)
    v.box((-5, 10.5, -6), (5, 15, 6), GREY)
    # Mudguards over the wheels.
    v.pair((15, 15.5, -6), (19, 16.5, 6), 'paint_dark')
    v.pair((15, 13.4, 5.2), (19, 14.4, 9.8), 'paint_dark', tilt=-45)
    v.pair((15, 13.4, -9.8), (19, 14.4, -5.2), 'paint_dark', tilt=45)
    v.pair((11.5, 11, -13.8), (13.5, 12.5, -13.5), TAIL_LAMP)
    # The drawbar to the hitch eye, and its stand.
    v.box((-1.25, 8, 10), (1.25, 9.5, 28), GREY)
    v.box((-1.5, 8, 28), (1.5, 9.5, 31.5), GREY)
    v.box((-0.6, 0, 24), (0.6, 8, 25.2), GREY)
    return v


def john_deere_x350():
    v = Vehicle('john_deere_x350', {
        'chassis': 'car', 'implement': 'mower', 'width': 1.1, 'height': 1.2, 'color': 'green',
        # The wheel is low, down by the rider's belly, not up at a car's.
        'steering_wheel_rise': 6,
        'performance': {'speed': 0.3, 'acceleration': 0.9, 'fuel_use': 0.3, 'braking': 1.0, 'steering': 1.4,
                        'offroad': 0.4},
        'factory': {'engine': 'inline4', 'fuel': 'petrol', 'tyres': 'road'},
        'tank': 9,
        'lights': {'length': 8, 'spread': 1.5},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 4}, {'item': 'minecraft:shears', 'count': 1}],
    })
    v.seat(0, 12, -6.5)
    v.wheel(7, -8, 4, 4)
    v.wheel(6, 11.5, 3, 2.4, steers=True)
    yellow = '#F2C200'

    # The frame, and the cutting deck slung under it with its chute on the right.
    v.box((-4, 3, -14), (4, 6, 14), DARK)
    v.box((-8.5, 1.2, -4), (8.5, 3.6, 7), 'paint_dark')
    v.box((-8, 3.6, -3.5), (8, 4.2, 6.5), 'paint_dark')
    v.box((-11, 1.5, -1), (-8.5, 3.4, 4), DARK)
    # The bonnet over the engine, its grille and lamps, a yellow stripe down each side.
    v.box((-5, 6, 4), (5, 13, 17))
    v.box((-4.5, 6.5, 17), (4.5, 12.5, 18))
    v.box((-2.8, 7.2, 18), (2.8, 11.6, 18.3), DARK)
    v.pair((3.1, 10, 18), (4.3, 11.4, 18.4), HEAD_LAMP)
    v.pair((5, 9, 5), (5.1, 9.8, 16), yellow)
    # The steering tower at the back of the bonnet, between the rider's knees,
    # its dash on top where the wheel's column goes in; the footrests.
    v.box((-2.5, 6, 1), (2.5, 13, 6), DARK)
    v.box((-3, 13, 2.5), (3, 15, 7), DARK)
    v.box((-2.5, 15, 4.5), (2.5, 16.5, 7.5), DARK)
    v.pair((4, 4.5, -3), (9, 5.5, 4), DARK)
    # The rear body over the drive wheels, the seat on it, and the hitch plate.
    v.box((-9.5, 8, -14), (9.5, 10, -2))
    v.box((-9.5, 6, -14.5), (9.5, 10, -14))
    v.box((-4.5, 10, -10), (4.5, 12, -3), DARK)
    v.box((-4.5, 12, -11), (4.5, 17, -9.5), DARK)
    v.box((-2, 4, -16), (2, 6, -14), DARK)
    v.pair((6.5, 8.4, -14.6), (8, 9.4, -14.4), TAIL_LAMP)
    return v


def honda_fourtrax_rancher():
    v = Vehicle('honda_fourtrax_rancher', {
        'chassis': 'car', 'handlebars': True, 'width': 1.2, 'height': 1.25, 'color': 'red',
        'performance': {'speed': 0.8, 'acceleration': 1.2, 'fuel_use': 0.6, 'braking': 1.0, 'steering': 1.3,
                        'offroad': 0.9},
        'factory': {'engine': 'inline4', 'fuel': 'petrol', 'tyres': 'dirt'},
        'tank': 14,
        'lights': {'length': 10, 'spread': 2},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 4}, {'item': 'minecraft:leather', 'count': 1}],
    })
    v.seat(0, 13.4, -4)
    v.wheel(6.8, 10, 4.9, 3.2, steers=True)
    v.wheel(6.6, -10, 4.9, 4)

    # The frame with its skid plate, the engine under the seat, the footrests.
    v.box((-4, 2.5, -15), (4, 5, 15), DARK)
    v.box((-4.5, 5, -7), (4.5, 11, 5), GREY)
    v.pair((4, 5.5, -4.5), (9.5, 6.5, 4.5), DARK)
    # The bodywork: the nose and a wing over each front wheel, the tank
    # cover, the tail and a wing over each rear wheel.
    v.box((-4.5, 8, 10), (4.5, 12.5, 20))
    v.pair((4.5, 10.5, 4.5), (10, 11.8, 15.5))
    v.pair((4.5, 9.4, 14.8), (10, 10.6, 19.2), tilt=-45)
    v.pair((4.5, 9.4, 0.8), (10, 10.6, 5.2), tilt=45)
    v.box((-4, 11, 0), (4, 14, 10))
    v.box((-4.5, 8, -16), (4.5, 11.5, -7))
    v.pair((4.5, 10.5, -15.5), (10, 11.8, -4.5))
    v.pair((4.5, 9.4, -19.2), (10, 10.6, -14.8), tilt=45)
    v.pair((4.5, 9.4, -5.2), (10, 10.6, -0.8), tilt=-45)
    v.pair((2, 9.6, 20), (4, 11.2, 20.3), HEAD_LAMP)
    v.box((-1.5, 9.6, -16.3), (1.5, 10.8, -16), TAIL_LAMP)
    # The seat, the racks front and back, the bumper, hitch and exhaust.
    v.box((-3.8, 11, -15), (3.8, 13.4, 0), DARK)
    for z0, z1 in ((11, 19), (-19, -11)):
        v.pair((8.4, 12.5, z0), (9, 13.2, z1), DARK)
        for z in (z0, (z0 + z1) / 2 - 0.3, z1 - 0.6):
            v.box((-9, 12.5, z), (9, 13.2, z + 0.6), DARK)
        v.pair((5, 10.6, z0 + 1), (5.6, 12.5, z0 + 2), DARK)
        v.pair((5, 10.6, z1 - 2), (5.6, 12.5, z1 - 1), DARK)
    v.box((-5, 4, 19.5), (5, 9, 20.5), DARK)
    v.box((-1, 4, -17), (1, 5.5, -15), GREY)
    v.box((-6.2, 8.6, -18), (-4.6, 10.2, -12), DARK)
    # The bars and the lamp on them, which turn with the front wheels.
    v.box((-1.2, 13, 7), (1.2, 18.6, 9), DARK, steers=True)
    v.box((-9, 18.2, 7.6), (9, 19, 8.4), STEEL, steers=True)
    v.pair((6.5, 17.9, 7.3), (9.6, 19.3, 8.7), DARK, steers=True)
    v.box((-2.5, 16, 8.8), (2.5, 18.4, 9.8), steers=True)
    v.box((-1.5, 16.4, 9.8), (1.5, 18, 10.1), HEAD_LAMP, steers=True)
    v.box((-2.5, 18.6, 7.2), (2.5, 19.8, 8.8), DARK, steers=True)
    return v


def tony_kart_racer_401():
    v = Vehicle('tony_kart_racer_401', {
        'chassis': 'car', 'width': 1.4, 'height': 0.95, 'color': 'lime',
        'performance': {'speed': 0.85, 'acceleration': 1.5, 'fuel_use': 0.5, 'braking': 1.3, 'steering': 1.6,
                        'offroad': 0.0},
        'factory': {'engine': 'inline4', 'fuel': 'petrol', 'tyres': 'sport'},
        'tank': 8,
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 3}, {'item': 'minecraft:black_wool', 'count': 1}],
    })
    v.seat(0, 3, -8)
    v.wheel(9.5, -8, 4.5, 2.9)
    v.wheel(8.4, 8.6, 4, 1.8, steers=True)
    chrome = '#B4B8BD'

    # The tube frame and the tray under the driver's legs.
    v.pair((6.5, 2, -14), (7.4, 2.9, 12), chrome)
    v.box((-7.4, 2, 7), (7.4, 2.9, 7.9), chrome)
    v.box((-7.4, 2, -9), (7.4, 2.9, -8.1), chrome)
    v.box((-6, 1.2, -6), (6, 1.8, 11), DARK)
    v.box((-8, 4, -8.5), (8, 5, -7.5), STEEL)
    # The bodywork: the nose, the panel in front of the legs, the side pods
    # between the wheels, and the bumper behind.
    v.box((-6, 1.5, 16.5), (6, 5, 21))
    v.box((-6, 3.8, 11.5), (6, 5, 17), tilt=-12)
    v.box((-5, 3, 9.8), (5, 11, 11), tilt=20)
    v.pair((7.4, 1.5, -2.6), (11, 4.8, 4.2))
    v.box((-10, 2, -16.5), (10, 3.5, -15.5), chrome)
    v.pair((6.5, 2, -15.5), (7.4, 2.9, -14), chrome)
    # The seat, and the fuel tank between the knees.
    v.box((-4.5, 1.8, -12), (4.5, 3, -4), DARK)
    v.box((-4.5, 3, -13), (4.5, 10, -11.5), DARK, tilt=15)
    v.pair((4.5, 1.8, -12.5), (5.3, 6, -4), DARK)
    v.box((-2, 2, 1.5), (2, 5, 5), '#D8D8D8')
    # The engine at the driver's left, its finned head and the silencer.
    v.box((5.3, 2, -11), (7.9, 9, -6), GREY)
    v.box((5.6, 9, -10), (7.6, 10.5, -7), STEEL)
    v.box((5.5, 6, -16), (7.5, 8, -11), DARK)
    # The steering column, down from the wheel to the front of the tray.
    v.box((-0.5, 1.75, 6.05), (0.5, 10.65, 7.05), STEEL, tilt=19)
    return v


def meyers_manx():
    v = Vehicle('meyers_manx', {
        'chassis': 'car', 'width': 1.55, 'height': 1.55, 'color': 'orange',
        'performance': {'speed': 1.0, 'acceleration': 1.1, 'fuel_use': 0.9, 'braking': 0.95, 'steering': 1.1,
                        'offroad': 0.85},
        'factory': {'engine': 'inline4', 'fuel': 'petrol', 'tyres': 'dirt'},
        'tank': 40,
        'lights': {'length': 12, 'spread': 3},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 5}, {'item': 'minecraft:leather', 'count': 2}],
    })
    v.seat(5, 7, -4)
    v.seat(-5, 7, -4)
    v.wheel(10, 18, 5, 2.6, steers=True)
    v.wheel(9.9, -14.5, 5.8, 4.5)

    # The floor pan with the backbone tunnel up it, and the open tub on it:
    # sides cut low, a bulkhead behind the seats, a hood over the front and
    # the nose under it, with a bumper bar.
    v.box((-9, 3.5, -12), (9, 4.5, 16), DARK)
    v.box((-1.5, 4.5, -9), (1.5, 6.5, 14), DARK)
    v.pair((8.5, 4, -9), (9.1, 11, 11))
    v.box((-8.5, 4, -12), (8.5, 11, -9))
    v.pair((8.4, 8, -21), (9, 13.5, -9))
    v.box((-8.5, 10.5, 11), (8.5, 12, 27))
    v.box((-8.5, 4, 14), (8.5, 10.5, 27))
    v.box((-8, 4.5, 27), (8, 9.5, 29))
    v.box((-9, 6, 29), (9, 7, 30), STEEL)
    v.pair((3.6, 10.6, 26), (6.9, 13.9, 27.5), DARK)
    v.pair((4, 11, 27.5), (6.5, 13.5, 27.8), HEAD_LAMP)
    # Round wings over all four wheels.
    v.pair((8.5, 12, 13), (12.5, 13, 23))
    v.pair((8.5, 9.6, 22), (12.5, 10.6, 26.5), tilt=-40)
    v.pair((8.5, 9.6, 9.5), (12.5, 10.6, 14), tilt=40)
    v.pair((9, 13.2, -20.5), (13, 14.2, -8.5))
    v.pair((9, 10.8, -9.5), (13, 11.8, -4), tilt=-35)
    v.pair((9, 10.8, -25), (13, 11.8, -19.5), tilt=35)
    v.pair((9, 8, -25.8), (12.5, 10.6, -24.5))
    v.pair((10, 8.6, -26.1), (12, 10, -25.8), TAIL_LAMP)
    # Inside: the dash, two low bucket seats, and the roll bar behind them.
    v.box((-8.5, 9, 9), (8.5, 14.5, 11), DARK)
    v.pair((2, 4.5, -8), (8, 7, -1), DARK)
    v.pair((2, 7, -9.5), (8, 16, -8), DARK, tilt=12)
    v.pair((8.2, 10, -11), (9, 24, -10), STEEL)
    v.box((-9, 23.2, -11), (9, 24, -10), STEEL)
    # The flat-four out in the open at the back, its air cleaner and pipes.
    v.box((-7, 5, -26), (7, 12, -12), GREY)
    v.box((-6.5, 6, -27), (6.5, 11.5, -26), DARK)
    v.box((-2, 12, -22), (2, 14.5, -18), STEEL)
    v.pair((3, 4.5, -28.5), (5, 6.5, -26), STEEL)
    return v


def club_car_precedent():
    v = Vehicle('club_car_precedent', {
        'chassis': 'car', 'width': 1.2, 'height': 2.1, 'color': 'white',
        'performance': {'speed': 0.5, 'acceleration': 0.9, 'fuel_use': 0.6, 'braking': 1.0, 'steering': 1.2,
                        'offroad': 0.3},
        'factory': {'engine': 'inline4', 'fuel': 'electric', 'tyres': 'road'},
        'tank': 20,
        'lights': {'length': 8, 'spread': 2},
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 4}, {'item': 'minecraft:redstone', 'count': 4},
                      {'item': 'minecraft:white_wool', 'count': 2}],
    })
    v.seat(5, 10, -5)
    v.seat(-5, 10, -5)
    v.wheel(7, 12, 3.7, 3.4, steers=True)
    v.wheel(7.8, -14, 3.7, 3.4)
    seat = '#3A3A3E'

    v.box((-8.5, 3, -21), (8.5, 4.5, 21), DARK)
    # The cowl over the front wheels: its nose, a wing over each wheel, and
    # the corners below the wings ahead of them, the lamps in those.
    v.box((-5, 4, 6.5), (5, 12, 21))
    v.pair((5, 8.5, 6.5), (9.5, 12, 21))
    v.pair((5, 4, 16.5), (9.5, 8.5, 21))
    v.pair((5.4, 9, 21), (7.6, 10.5, 21.3), HEAD_LAMP)
    v.box((-9, 3.5, 21), (9, 5.5, 22.5), DARK)
    # The dash and the column up to the wheel, the windscreen in one pane
    # between the front posts, the posts back to the canopy.
    v.box((-9, 9, 4), (9, 15, 6.5), DARK)
    v.box((4.2, 12, 5.5), (5.8, 18.2, 9), DARK)
    v.box((-8.3, 15, 5.7), (8.3, 30.5, 6.3), GLASS)
    v.pair((8.3, 12, 5.5), (9.1, 32, 6.5), DARK)
    v.pair((8.3, 13, -17.5), (9.1, 32, -16.5), DARK)
    v.box((-10, 32, -21), (10, 33.5, 8))
    # The body under the bench: wings over the rear wheels, the bench and
    # its back, the bag well behind with its strap bar, the tail lamps.
    v.box((-6, 4, -21), (6, 8.5, -1))
    v.pair((6, 8, -18.5), (9.5, 9.5, -9.5))
    v.pair((6, 4, -9.5), (9.5, 8.5, -1))
    v.pair((6, 4, -21), (9.5, 9.5, -18.5))
    v.box((-8, 8.5, -10), (8, 10, -1), seat)
    v.box((-8, 10, -12), (8, 19, -10.5), seat, tilt=8)
    v.box((-9, 9.5, -24), (9, 10.5, -18), DARK)
    v.pair((8, 10.5, -23.5), (9, 16, -22.5), DARK)
    v.box((-9, 15, -23.5), (9, 16, -22.5), DARK)
    v.pair((6.5, 6.5, -21.3), (8.5, 8, -21), TAIL_LAMP)
    v.box((-9, 3.5, -22.5), (9, 5.5, -21), DARK)
    return v


def yamaha_waverunner_vx():
    v = Vehicle('yamaha_waverunner_vx', {
        'chassis': 'boat', 'handlebars': True, 'width': 1.25, 'height': 1.2, 'color': 'blue',
        'performance': {'speed': 1.15, 'acceleration': 1.4, 'fuel_use': 0.9, 'braking': 0.7, 'steering': 1.4,
                        'offroad': 0},
        'factory': {'engine': 'inline4', 'fuel': 'petrol', 'tyres': 'road'},
        'tank': 70,
        'materials': [{'item': 'minecraft:iron_ingot', 'count': 3}, {'item': 'minecraft:white_wool', 'count': 1}],
    })
    v.seat(0, 13, -6)
    v.seat(0, 13, -14)
    hull = '#F2F2F0'

    # The hull, a vee under the stern widening to the chines, and the bow
    # narrowing and rising to its point.
    v.box((-4, 0, -26), (4, 1.5, 18), hull)
    v.box((-7.5, 1.5, -27), (7.5, 4, 18), hull)
    v.box((-9.5, 4, -27), (9.5, 7.5, 18), hull)
    for i in range(6):
        z = 18 + 2 * i
        half = 9.5 - 1.35 * i
        v.box((-half, 1.5 + 1.1 * i, z), (half, 7.5 + 0.25 * i, z + 2), hull)
    v.box((-10, 7, -27), (10, 8, 20), DARK)
    # On deck: the hood over the bow with its mirrors, the footwells and the
    # gunwales either side, the seat on its pedestal, the boarding platform.
    v.box((-6.5, 8, 2), (6.5, 13, 18))
    v.box((-5.5, 8, 17), (5.5, 11.5, 27), tilt=-12)
    v.box((-3, 13, 2), (3, 16, 7))
    v.pair((5.5, 13, 9.5), (7.5, 14.5, 10.2), DARK)
    v.pair((5.5, 7.5, -22), (9.5, 8.2, 2), DARK)
    v.pair((9.5, 7.5, -22), (10, 10, 2))
    v.box((-4.5, 8, -23), (4.5, 10.5, 2))
    v.box((-4, 10.5, -22), (4, 13, 1), DARK)
    v.box((-9.5, 7.5, -27), (9.5, 8.6, -23), DARK)
    v.box((-1.5, 2, -28.5), (1.5, 4.5, -27), STEEL)
    # The bars, which turn the jet.
    v.box((-1, 16, 4.2), (1, 18.6, 6), DARK, steers=True)
    v.box((-8.5, 18, 4.6), (8.5, 18.9, 5.6), STEEL, steers=True)
    v.pair((6, 17.7, 4.5), (9, 19.1, 5.7), DARK, steers=True)
    v.box((-1.2, 18.6, 4.4), (1.2, 19.8, 5.8), DARK, steers=True)
    return v


VEHICLES = [robinson_r44, massey_ferguson_240, seed_drill, fertilizer_spreader, john_deere_x350,
            honda_fourtrax_rancher, tony_kart_racer_401, meyers_manx, club_car_precedent, yamaha_waverunner_vx]


def main():
    palette = Palette(PALETTE)
    for make in VEHICLES:
        make().write(palette)
    palette.save()


if __name__ == '__main__':
    main()
