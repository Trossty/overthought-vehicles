#!/usr/bin/env python3
"""The vehicles' item icons, made from their definitions.

Run from the repository root after a vehicle's shape changes:

    python3 tools/gen_vehicle_icons.py

For every data/overthought_vehicles/overthought_vehicles/vehicle/<name>.json it writes
assets/overthought_vehicles/models/item/vehicle_<name>.json, the vehicle in miniature, and
the one palette they all share, assets/overthought_vehicles/textures/item/vehicle_palette.png.

An item model is poorer than the vehicle it stands for, and the icon bends
to that: glass is left out (an item is solid), a box turns about one axis
only and in steps of 22.5 degrees, and the paint is drawn white for the
item's own tints to colour.
"""
import glob
import json
import math
import os

from PIL import Image

ROOT = 'src/main/resources/'
DEFINITIONS = ROOT + 'data/overthought_vehicles/overthought_vehicles/vehicle/'
MODELS = ROOT + 'assets/overthought_vehicles/models/item/'
PALETTE_FILE = ROOT + 'assets/overthought_vehicles/textures/item/vehicle_palette.png'

SPAN = 22.0          # how long the longest vehicle side is, in item pixels
LIFT = 0.012         # how far a trim piece stands off the panel it lies on
TINTS = {'paint': 0, 'paint_dark': 1, 'paint_light': 2}
CARGO = '#A9C6D3'    # the load is not known to an item: a pale sight glass
TYRE, HUB = '#202023', '#C4C8CD'
DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.72, 0.72, 0.72]},
    'ground': {'translation': [0, 3, 0], 'scale': [0.4, 0.4, 0.4]},
    'fixed': {'rotation': [0, 90, 0], 'scale': [0.7, 0.7, 0.7]},
    'head': {'scale': [0.8, 0.8, 0.8]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.35, 0.35, 0.35]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.35, 0.35, 0.35]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 3.5, 0], 'scale': [0.24, 0.24, 0.24]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 3.5, 0], 'scale': [0.24, 0.24, 0.24]},
}

palette = [0xFFFFFF]


def swatch(color):
    rgb = 0xFFFFFF if color in TINTS else int(color[-6:], 16)
    if rgb not in palette:
        palette.append(rgb)
    return palette.index(rgb)


def num(value):
    text = ('%.3f' % value).rstrip('0').rstrip('.')
    return '0' if text in ('-0', '') else text


def shares_face(a, b):
    """As DefinedVehicleModel.sharesFace: two colours fighting over one plane."""
    if a['color'] == b['color'] or a['tilt'] != b['tilt'] or a['roll'] != b['roll']:
        return False
    for axis in range(3):
        if abs(a['lo'][axis] - b['lo'][axis]) >= 1e-3 and abs(a['hi'][axis] - b['hi'][axis]) >= 1e-3:
            continue
        if all(min(a['hi'][o], b['hi'][o]) - max(a['lo'][o], b['lo'][o]) > 1e-3 for o in range(3) if o != axis):
            return True
    return False


def levels(boxes):
    """How many panels each box lies on, the smaller always on the bigger."""
    order = sorted(range(len(boxes)), key=lambda i: -math.prod(
        boxes[i]['hi'][k] - boxes[i]['lo'][k] for k in range(3)))
    level = [0] * len(boxes)
    for a, i in enumerate(order):
        for j in order[:a]:
            if level[j] + 1 > level[i] and shares_face(boxes[i], boxes[j]):
                level[i] = level[j] + 1
    return level


def element(lo, hi, color, turn=None):
    """One box, already in the item's own frame. turn is (axis, degrees)."""
    index = swatch(color)
    u, v = index % 16 + 0.25, index // 16 + 0.25
    face = '{"uv": [%s, %s, %s, %s], "texture": "#p"%s}' % (
        num(u), num(v), num(u + 0.5), num(v + 0.5),
        ', "tintindex": %d' % TINTS[color] if color in TINTS else '')
    rotation = ''
    if turn and turn[1]:
        rotation = '"rotation": {"origin": [%s], "axis": "%s", "angle": %s}, ' % (
            ', '.join(num((lo[k] + hi[k]) / 2) for k in range(3)), turn[0], num(turn[1]))
    return '{"from": [%s], "to": [%s], %s"faces": {%s}}' % (
        ', '.join(num(c) for c in lo), ', '.join(num(c) for c in hi), rotation,
        ', '.join('"%s": %s' % (side, face) for side in ('north', 'south', 'east', 'west', 'up', 'down')))


def turned(size, angles):
    """
    A box turned three ways, brought down to what an item model can hold: a
    quarter turn becomes a box with two sides swapped, and of what is left
    only the biggest turn is kept, to the nearest step.
    """
    size = list(size)
    rest = []
    for axis, angle in enumerate(angles):
        quarters = round(angle / 90.0)
        if quarters % 2:
            a, b = [k for k in range(3) if k != axis]
            size[a], size[b] = size[b], size[a]
        rest.append(angle - quarters * 90.0)
    axis = max(range(3), key=lambda k: abs(rest[k]))
    step = max(-45.0, min(45.0, round(rest[axis] / 22.5) * 22.5))
    return size, ('xyz'[axis], step)


def icon(name, definition):
    boxes = []
    for b in definition['shape']:
        lo = [min(b['from'][k], b['to'][k]) for k in range(3)]
        hi = [max(b['from'][k], b['to'][k]) for k in range(3)]
        boxes.append({'lo': lo, 'hi': hi, 'color': b.get('color', 'paint'),
                      'tilt': b.get('tilt', 0), 'yaw': b.get('yaw', 0), 'roll': b.get('roll', 0)})
    wheels = definition.get('wheels', [])

    reach_x = max([max(abs(b['lo'][0]), abs(b['hi'][0])) for b in boxes]
                  + [abs(w['at'][0]) + w['width'] / 2 for w in wheels])
    back = min([b['lo'][2] for b in boxes] + [w['at'][2] - w['radius'] for w in wheels])
    nose = max([b['hi'][2] for b in boxes] + [w['at'][2] + w['radius'] for w in wheels])
    top = max(b['hi'][1] for b in boxes)
    bottom = min([0.0] + [b['lo'][1] for b in boxes])
    scale = SPAN / max(nose - back, reach_x * 2)
    middle = (nose + back) / 2
    rise = 8 - (top + bottom) * scale / 2

    def place(centre, size, color, angles, grow=0.0):
        # The vehicle's frame has its nose at +z and its left at +x; an item
        # model has north, where the nose goes, at -z. Half a turn about the
        # upright takes one to the other, and a tilt, a yaw and a roll with it.
        size, turn = turned(size, (angles[0], angles[1], -angles[2]))
        mid = (8 - centre[0] * scale, rise + centre[1] * scale, 8 - (centre[2] - middle) * scale)
        half = [s * scale / 2 + grow for s in size]
        return element([mid[k] - half[k] for k in range(3)], [mid[k] + half[k] for k in range(3)], color, turn)

    out = []
    for b, level in zip(boxes, levels(boxes)):
        if len(b['color']) == 9:
            continue
        centre = [(b['lo'][k] + b['hi'][k]) / 2 for k in range(3)]
        size = [b['hi'][k] - b['lo'][k] for k in range(3)]
        color = CARGO if b['color'] == 'cargo' else b['color']
        out.append(place(centre, size, color, (b['tilt'], b['yaw'], b['roll']), level * LIFT))
    for w in wheels:
        at, r, width = w['at'], w['radius'], w['width']
        # Round, the way the vehicle's own wheels are: eight slabs about the axle.
        reach, side = r * math.cos(math.pi / 16), r * math.sin(math.pi / 16)
        for turn in (-45, -22.5, 0, 22.5):
            out.append(place(at, (width, reach * 2, side * 2), TYRE, (turn, 0, 0)))
            out.append(place(at, (width, side * 2, reach * 2), TYRE, (turn, 0, 0)))
        rim = r * 0.58
        for tall, long in ((0.92, 0.38), (0.38, 0.92), (0.7, 0.7)):
            out.append(place(at, (width + 0.6, rim * tall * 2, rim * long * 2), HUB, (0, 0, 0)))

    text = ('{\n  "textures": {"particle": "overthought_vehicles:item/vehicle_palette", "p": "overthought_vehicles:item/vehicle_palette"},\n'
            '  "elements": [\n    ' + ',\n    '.join(out) + '\n  ],\n  "display": '
            + json.dumps(DISPLAY) + '\n}\n')
    with open(MODELS + 'vehicle_' + name + '.json', 'w') as file:
        file.write(text)
    print('%-20s %4d elements' % (name, len(out)))


def main():
    for path in sorted(glob.glob(DEFINITIONS + '*.json')):
        with open(path) as file:
            icon(os.path.basename(path)[:-5], json.load(file))
    if len(palette) > 256:
        raise SystemExit('%d colours, and the palette holds 256' % len(palette))
    image = Image.new('RGBA', (16, 16))
    for i in range(256):
        rgb = palette[min(i, len(palette) - 1)]
        image.putpixel((i % 16, i // 16), (rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, 255))
    image.save(PALETTE_FILE)
    print('palette: %d colours' % len(palette))


main()
