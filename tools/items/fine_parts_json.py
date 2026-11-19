"""
Writes the item models of the finer engine-bay and running-gear parts (water
pump, thermostat, tank pump, injectors, ignition coils, brake calipers, ball
joints, silencer) the way the Blockbench kit (tools/blocks/kit.js) would: box()
and round() with the kit's palette UVs, each drawing centred in its block, and
the display block worked out the kit's way from the drawing's size. Prints each
model's lowest and highest pixel, which PartLayout.BAY wants for the bay parts.

    python3 tools/items/fine_parts_json.py
"""
import json
import math
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'car')
PALETTE = json.load(open(os.path.join(ROOT, 'tools', 'blocks', 'palette.json')))

SIDES = ['north', 'south', 'east', 'west', 'up', 'down']
AXES = 'xyz'
FACING = [['west', 'east'], ['down', 'up'], ['north', 'south']]
STEEL, DARK_STEEL, IRON, ALU, CHROME = '#5B6168', '#3A3F45', '#7C8288', '#A9AEB4', '#C6CACE'
PLASTIC, RUBBER, BRASS, RED, CLOTH = '#2A2A2C', '#1E1E20', '#B8933F', '#B02E26', '#E6E1CF'
CAST, GREY = '#9EA3A9', '#4A4E54'


def r3(v):
    n = round(v * 1000) / 1000
    return 0 if n == 0 else n


def auto_uv(f, t, side):
    fx, fy, fz = f
    tx, ty, tz = t
    uv = {
        'down': [fx, 16 - tz, tx, 16 - fz], 'up': [fx, fz, tx, tz],
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy], 'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy], 'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
    }[side]

    def fold(a, b):
        if b - a >= 16:
            return [0, 16]
        shift = math.floor(a / 16) * 16
        a -= shift
        b -= shift
        return [a - (b - 16), 16] if b > 16 else [a, b]

    u1, u2 = fold(uv[0], uv[2])
    v1, v2 = fold(uv[1], uv[3])
    return [r3(u1), r3(v1), r3(u2), r3(v2)]


def cell_uv(i, f, t, side):
    u, v = i % 16, i // 16
    r = auto_uv(f, t, side)

    def at(cell, x):
        return round((cell + x / 16) * 10000) / 10000

    return [at(u, r[0]), at(v, r[1]), at(u, r[2]), at(v, r[3])]


def turn(p, axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    x, y, z = p
    if axis == 'x':
        return [x, y * c - z * s, y * s + z * c]
    if axis == 'y':
        return [x * c + z * s, y, -x * s + z * c]
    return [x * c - y * s, x * s + y * c, z]


class Drawing:
    """One model's boxes, as the kit keeps them until it writes the file."""

    def __init__(self):
        self.boxes = []

    def box(self, lo, hi, paint, rot=None, open_sides=()):
        f = [min(a, b) for a, b in zip(lo, hi)]
        t = [max(a, b) for a, b in zip(lo, hi)]
        self.boxes.append({'from': f, 'to': t, 'paint': paint, 'rot': rot, 'open': open_sides})

    def rnd(self, axis, cx, cy, cz, r, length, paint, sides=8):
        reach, half = r * math.cos(math.pi / sides), r * math.sin(math.pi / sides)
        a = AXES.index(axis)
        u, v = (a + 1) % 3, (a + 2) % 3
        c = [cx, cy, cz]
        turns = [-45, -22.5, 0, 22.5] if sides == 16 else [0, 45]
        for angle in turns:
            for across, along in [(u, v), (v, u)]:
                lo, hi = c[:], c[:]
                lo[a] -= length / 2
                hi[a] += length / 2
                lo[across] -= reach
                hi[across] += reach
                lo[along] -= half
                hi[along] += half
                self.box(lo, hi, paint, (axis, angle, c) if angle else None, FACING[along])

    def corners(self):
        for b in self.boxes:
            for i in range(8):
                p = [b['to'][k] if i >> k & 1 else b['from'][k] for k in range(3)]
                if b['rot']:
                    axis, angle, o = b['rot']
                    p = [v + o[k] for k, v in enumerate(turn([v - o[k] for k, v in enumerate(p)], axis, angle))]
                yield p

    def extent(self):
        pts = list(self.corners())
        return [min(p[a] for p in pts) for a in range(3)], [max(p[a] for p in pts) for a in range(3)]

    def centre(self):
        lo, hi = self.extent()
        shift = [8 - (lo[a] + hi[a]) / 2 for a in range(3)]
        for b in self.boxes:
            b['from'] = [v + shift[k] for k, v in enumerate(b['from'])]
            b['to'] = [v + shift[k] for k, v in enumerate(b['to'])]
            if b['rot']:
                axis, angle, o = b['rot']
                b['rot'] = (axis, angle, [v + shift[k] for k, v in enumerate(o)])

    def display(self, gui=(30, 225, 0)):
        lo3, hi3 = self.extent()
        largest = max(hi3[a] - lo3[a] for a in range(3))
        lo, hi = [math.inf, math.inf], [-math.inf, -math.inf]
        for p in self.corners():
            v = turn(turn(turn([x - 8 for x in p], 'z', gui[2]), 'y', gui[1]), 'x', gui[0])
            for a in range(2):
                lo[a] = min(lo[a], v[a])
                hi[a] = max(hi[a], v[a])
        fit = min(1.6, 15 / max(hi[0] - lo[0], hi[1] - lo[1]))
        held = max(0.35, min(0.75, 6.5 / largest))
        framed, third, first = min(1, held * 2), held * 0.9, held * 0.6

        def s(v):
            return [r3(v)] * 3

        return {
            'gui': {'rotation': list(gui), 'translation': [r3(-(lo[0] + hi[0]) / 2 * fit),
                                                           r3(-(lo[1] + hi[1]) / 2 * fit), 0], 'scale': s(fit)},
            'ground': {'translation': [0, 3, 0], 'scale': s(held)},
            'fixed': {'scale': s(framed)},
            'head': {'scale': s(framed)},
            'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': s(third)},
            'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': s(third)},
            'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3.5, 0], 'scale': s(first)},
            'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 3.5, 0], 'scale': s(first)},
        }

    def elements(self):
        out = []
        for b in self.boxes:
            f, t = [r3(v) for v in b['from']], [r3(v) for v in b['to']]
            e = {'from': f, 'to': t}
            if b['rot']:
                axis, angle, o = b['rot']
                e['rotation'] = {'origin': [r3(v) for v in o], 'axis': axis, 'angle': angle}
            e['faces'] = {side: {'uv': cell_uv(PALETTE.index(b['paint']), f, t, side), 'texture': '#p'}
                          for side in SIDES if side not in b['open']}
            out.append(e)
        return out


def water_pump(d):
    d.rnd('z', 8, 8, 9.2, 3.8, 2.6, ALU)
    d.box([3.4, 3.6, 10.5], [12.6, 12.4, 11.5], CAST)
    for x, y in [(4.2, 4.4), (11.8, 4.4), (4.2, 11.6), (11.8, 11.6)]:
        d.rnd('z', x, y, 11.7, 0.5, 0.5, CHROME)
    d.rnd('z', 8, 8, 7.0, 1.3, 2.2, IRON)
    d.rnd('z', 8, 8, 5.2, 3.2, 0.6, STEEL)
    d.rnd('z', 8, 8, 4.4, 2.5, 1.0, DARK_STEEL)
    d.rnd('z', 8, 8, 3.6, 3.2, 0.6, STEEL)
    d.rnd('x', 12.6, 9.6, 9.2, 1.0, 3.2, ALU)


def thermostat(d):
    d.rnd('y', 8, 9.2, 8, 2.3, 3.0, ALU)
    d.rnd('x', 10.8, 9.6, 8, 1.3, 3.6, ALU)
    d.rnd('y', 8, 7.3, 8, 3.2, 0.8, CAST)
    for x in [4.6, 11.4]:
        d.box([x - 0.7, 6.9, 7.3], [x + 0.7, 7.7, 8.7], CAST)
        d.rnd('y', x, 7.9, 8, 0.45, 0.4, CHROME)
    d.rnd('y', 8, 6.6, 8, 2.7, 0.5, BRASS)
    d.box([7.6, 3.6, 5.6], [8.4, 6.4, 6.2], BRASS)
    d.box([7.6, 3.6, 9.8], [8.4, 6.4, 10.4], BRASS)
    d.rnd('y', 8, 5.2, 8, 1.3, 2.2, STEEL)
    d.rnd('y', 8, 3.8, 8, 0.8, 1.0, BRASS)


def tank_pump(d):
    d.rnd('y', 8, 12.2, 8, 4.0, 0.8, PLASTIC)
    d.rnd('y', 8, 13.1, 8, 1.8, 1.0, PLASTIC)
    d.rnd('x', 10.4, 13.2, 8, 0.55, 2.4, STEEL)
    d.box([4.8, 12.6, 6.9], [6.4, 13.8, 9.1], GREY)
    d.rnd('y', 8, 8.0, 8, 2.0, 6.6, STEEL)
    d.box([10.4, 4.4, 7.6], [11.0, 11.8, 8.4], STEEL)
    d.box([4.9, 4.4, 7.6], [5.5, 11.8, 8.4], STEEL)
    d.box([5.4, 3.0, 5.4], [10.6, 4.6, 10.6], CLOTH)
    d.box([11.0, 6.6, 7.8], [13.2, 7.0, 8.2], STEEL)
    d.box([12.6, 5.6, 7.2], [14.2, 6.8, 8.8], PLASTIC)


CYLINDERS = [3.4, 6.5, 9.5, 12.6]


def injectors(d):
    d.box([1.8, 10.4, 6.8], [14.2, 12.0, 8.4], ALU)
    d.rnd('x', 1.2, 11.2, 7.6, 0.6, 1.4, STEEL)
    for x in CYLINDERS:
        d.box([x - 0.5, 12.0, 7.1], [x + 0.5, 12.4, 8.1], CHROME)
        d.rnd('y', x, 9.8, 7.6, 0.85, 1.2, ALU)
        d.rnd('y', x, 7.9, 7.6, 0.75, 2.6, PLASTIC)
        d.box([x - 0.6, 7.6, 8.3], [x + 0.6, 8.8, 9.7], GREY)
        d.rnd('y', x, 5.8, 7.6, 0.45, 1.6, STEEL)
        d.rnd('y', x, 4.7, 7.6, 0.25, 0.6, CHROME)


def ignition_coils(d):
    for x in CYLINDERS:
        d.box([x - 1.2, 11.0, 6.2], [x + 1.2, 12.6, 9.8], PLASTIC)
        d.box([x - 0.7, 11.4, 9.8], [x + 0.7, 12.4, 10.9], GREY)
        d.box([x - 0.4, 11.6, 4.8], [x + 0.4, 12.0, 6.2], STEEL)
        d.rnd('y', x, 8.0, 8, 0.8, 6.0, RUBBER)
        d.rnd('y', x, 4.1, 8, 0.65, 1.8, DARK_STEEL)


def brake_calipers(d):
    d.box([4.0, 5.0, 9.0], [12.0, 11.0, 10.6], RED)
    d.box([4.4, 5.0, 5.0], [11.6, 11.0, 7.4], RED)
    d.box([4.0, 10.0, 7.4], [12.0, 11.6, 9.0], RED)
    for x in [6.4, 9.6]:
        d.rnd('z', x, 8.0, 4.6, 1.5, 0.8, RED)
    d.box([2.6, 3.8, 7.2], [13.4, 5.0, 9.2], DARK_STEEL)
    for x in [3.3, 12.7]:
        d.box([x - 0.7, 5.0, 7.2], [x + 0.7, 8.4, 9.2], DARK_STEEL)
        d.rnd('z', x, 7.6, 6.6, 0.55, 1.2, CHROME)
    d.rnd('y', 10.8, 12.0, 6.2, 0.35, 0.9, STEEL)
    d.box([5.0, 5.4, 7.4], [11.0, 10.0, 7.8], PLASTIC)
    d.box([5.0, 5.4, 8.6], [11.0, 10.0, 9.0], PLASTIC)


def ball_joints(d):
    d.box([4.0, 4.8, 4.6], [12.0, 5.8, 11.4], STEEL)
    for x, z in [(5.0, 5.6), (11.0, 5.6), (8.0, 10.6)]:
        d.rnd('y', x, 6.1, z, 0.55, 0.6, CHROME)
    d.rnd('y', 8, 6.9, 8, 2.6, 2.2, IRON)
    d.rnd('y', 8, 8.8, 8, 2.2, 1.6, RUBBER)
    d.rnd('y', 8, 9.9, 8, 1.4, 0.6, RUBBER)
    d.rnd('y', 8, 11.3, 8, 0.95, 2.2, STEEL)
    d.rnd('y', 8, 12.9, 8, 0.6, 1.0, CHROME)
    d.rnd('y', 8, 4.2, 8, 0.4, 1.2, '#E8B620')


def muffler(d):
    d.rnd('x', 8, 8, 8, 3.6, 8.6, STEEL)
    d.rnd('x', 3.6, 8, 8, 3.3, 0.4, DARK_STEEL)
    d.rnd('x', 12.4, 8, 8, 3.3, 0.4, DARK_STEEL)
    d.rnd('x', 8, 8, 8, 3.7, 0.4, DARK_STEEL)
    d.rnd('x', 1.8, 8.8, 8, 0.9, 3.4, IRON)
    d.rnd('x', 14.2, 7.0, 8, 0.9, 3.4, IRON)
    d.box([7.6, 11.4, 7.6], [8.4, 12.4, 8.4], DARK_STEEL)
    d.box([7.0, 12.4, 7.2], [9.0, 13.4, 8.8], RUBBER)


MODELS = {
    'water_pump': water_pump, 'thermostat': thermostat, 'tank_pump': tank_pump,
    'injectors': injectors, 'ignition_coils': ignition_coils, 'brake_calipers': brake_calipers,
    'ball_joints': ball_joints, 'muffler': muffler,
}

for name, draw in MODELS.items():
    d = Drawing()
    draw(d)
    d.centre()
    lo, hi = d.extent()
    textures = {'particle': 'car:item/materials', 'p': 'car:block/palette'}
    elements = d.elements()
    with open(os.path.join(ASSETS, 'models', 'item', name + '.json'), 'w') as out:
        out.write('{\n  "textures": ' + json.dumps(textures) + ',\n  "elements": [\n')
        out.write(',\n'.join('    ' + json.dumps(e, separators=(', ', ': ')) for e in elements))
        out.write('\n  ],\n  "display": ' + json.dumps(d.display()) + '\n}\n')
    print(f'{name}: {len(elements)} elements, y {lo[1]:.2f}..{hi[1]:.2f}')
