"""Vehicle crate models. Run: python3 tools/gen_crate_wrench.py

The crate is a spruce box on a dark oak pallet. Its four sides are the same
side turned a quarter each time, laid out as a pinwheel (each side runs from
one corner to just short of the next) so they meet at the corners without
overlapping and each can fall away on its own. The renderer draws the sides
and the lid; only the pallet is a block model in the world, and the closed
crate is all of them together, for the item. The wrench that prises the lid
off has its own model, which this leaves alone.
"""
import json, os
from model_uv import fold_uvs

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'car')
M = os.path.join(R, 'models')

PLANKS = 'minecraft:block/spruce_planks'
BATTEN = 'minecraft:block/stripped_spruce_log'
DECK = 'minecraft:block/dark_oak_planks'
RUNNER = 'minecraft:block/stripped_dark_oak_log'


def box(f, t, tex, name, cull_down=False):
    faces = {}
    for face in ('north', 'south', 'east', 'west', 'up', 'down'):
        faces[face] = {'texture': '#' + tex}
        if cull_down and face == 'down':
            faces[face]['cullface'] = 'down'
    return {'name': name, 'from': list(f), 'to': list(t), 'faces': faces}


def turned(e, quarters):
    """The element turned clockwise (seen from above) about the block's middle."""
    (fx, fy, fz), (tx, ty, tz) = e['from'], e['to']
    for _ in range(quarters):
        fx, fz, tx, tz = 16 - tz, fx, 16 - fz, tx
        fx, tx = min(fx, tx), max(fx, tx)
        fz, tz = min(fz, tz), max(fz, tz)
    out = json.loads(json.dumps(e))
    out['from'], out['to'] = [fx, fy, fz], [tx, ty, tz]
    return out


def base():
    return [
        box((0, 1, 0), (16, 2, 16), 'deck', 'deck'),
        box((0, 0, 1), (16, 1, 3), 'runner', 'runner_back', True),
        box((0, 0, 7), (16, 1, 9), 'runner', 'runner_middle', True),
        box((0, 0, 13), (16, 1, 15), 'runner', 'runner_front', True),
    ]


def wall():
    # The north side; the others are this one turned.
    return [
        box((0, 2, 0), (14.5, 15, 1.5), 'planks', 'panel'),
        box((0.5, 3, -0.5), (14, 4.5, 0), 'batten', 'batten_low'),
        box((0.5, 12.5, -0.5), (14, 14, 0), 'batten', 'batten_high'),
        box((6.5, 4.5, -0.5), (8, 12.5, 0), 'batten', 'batten_upright'),
    ]


def lid():
    return [
        box((0, 15, 0), (16, 16, 16), 'planks', 'lid'),
        box((1, 16, 2), (15, 16.5, 3.5), 'batten', 'lid_batten_back'),
        box((1, 16, 12.5), (15, 16.5, 14), 'batten', 'lid_batten_front'),
    ]


CRATE_TEXTURES = {'particle': PLANKS, 'planks': PLANKS, 'batten': BATTEN, 'deck': DECK, 'runner': RUNNER}


def block_model(elements, parent=None):
    model = {}
    if parent:
        model['parent'] = parent
    model['textures'] = CRATE_TEXTURES
    model['elements'] = fold_uvs(elements) or elements
    return model


def write(path, model):
    full = os.path.join(M, path + '.json')
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, 'w') as out:
        json.dump(model, out, indent=2)
        out.write('\n')


def main():
    write('block/vehicle_crate/base', block_model(base()))
    write('block/vehicle_crate/wall', block_model(wall()))
    write('block/vehicle_crate/lid', block_model(lid()))
    closed = base() + lid()
    for quarters in range(4):
        closed += [turned(e, quarters) for e in wall()]
    write('block/vehicle_crate/closed', block_model(closed, 'minecraft:block/block'))
    write('item/vehicle_crate', {'parent': 'car:block/vehicle_crate/closed'})

    states = {'variants': {}}
    rotation = {'south': 0, 'west': 90, 'north': 180, 'east': 270}
    for facing, y in rotation.items():
        for piece, name in ((0, 'base'), (1, 'wall'), (2, 'lid')):
            # Only the pallet is ever in the world; the renderer turns the rest itself.
            variant = {'model': 'car:block/vehicle_crate/' + name}
            if piece == 0 and y:
                variant['y'] = y
            states['variants']['facing=%s,piece=%d' % (facing, piece)] = variant
    with open(os.path.join(R, 'blockstates', 'vehicle_crate.json'), 'w') as out:
        json.dump(states, out, indent=2)
        out.write('\n')


if __name__ == '__main__':
    main()
