"""Keeps a block model's faces on their own texture.

A face with no "uv" takes it from where the element sits in the block, so an
element reaching past the block (a lift platform, a post's lamp) gets UVs past
the texture's edge, and 1.21.1 reads those straight off the neighbouring
sprites in the atlas. fold_uvs writes those UVs out, moved back inside 0..16.
"""


def _default(e, face):
    (fx, fy, fz), (tx, ty, tz) = e['from'], e['to']
    return {
        'down': [fx, 16 - tz, tx, 16 - fz],
        'up': [fx, fz, tx, tz],
        'north': [16 - tx, 16 - ty, 16 - fx, 16 - fy],
        'south': [fx, 16 - ty, tx, 16 - fy],
        'west': [fz, 16 - ty, tz, 16 - fy],
        'east': [16 - tz, 16 - ty, 16 - fz, 16 - fy],
    }[face]


def _fold(a, b):
    if b - a >= 16:
        return 0, 16
    shift = (a // 16) * 16
    a, b = a - shift, b - shift
    if b > 16:
        a, b = a - (b - 16), 16
    return a, b


def _num(v):
    return int(v) if v == int(v) else round(v, 4)


def fold_uvs(elements):
    for e in elements:
        for face, f in e['faces'].items():
            if 'uv' in f:
                continue
            u0, v0, u1, v1 = _default(e, face)
            if min(u0, v0, u1, v1) >= 0 and max(u0, v0, u1, v1) <= 16:
                continue
            u0, u1 = _fold(u0, u1)
            v0, v1 = _fold(v0, v1)
            f['uv'] = [_num(u0), _num(v0), _num(u1), _num(v1)]
    return elements
