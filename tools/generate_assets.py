#!/usr/bin/env python3
"""
Procedurally generates the Storm Ray's original assets:
  - GeckoLib geometry (geo.json) and animations
  - entity texture + glowmask, particle sprites, storm clouds, stone overlays
Run from the repository root:  python3 tools/generate_assets.py
Requires: numpy, pillow
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "stormray")
RNG = random.Random(1337)
NP_RNG = np.random.default_rng(1337)

TEX = 512                    # entity texture size
GLOW_ZONE = (0, 448, 64, 64)     # x, y, w, h  -> fully emissive red
VEIN_ZONE = (64, 448, 192, 64)   # partially emissive veins
ORGANIC_MAX_Y = 440


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)


# --------------------------------------------------------------------------- noise

def tileable_noise(size, cells, rng):
    """Smooth value noise that wraps around on both axes."""
    grid = rng.random((cells, cells))
    coords = np.arange(size) / size * cells
    i0 = np.floor(coords).astype(int)
    f = coords - i0
    f = f * f * (3 - 2 * f)
    i1 = (i0 + 1) % cells
    i0 = i0 % cells
    a = grid[np.ix_(i0, i0)]
    b = grid[np.ix_(i0, i1)]
    c = grid[np.ix_(i1, i0)]
    d = grid[np.ix_(i1, i1)]
    fx = f[None, :]
    fy = f[:, None]
    top = a * (1 - fx) + b * fx
    bottom = c * (1 - fx) + d * fx
    return top * (1 - fy) + bottom * fy


def fbm(size, base_cells, octaves, rng, persistence=0.5):
    total = np.zeros((size, size))
    amp = 1.0
    norm = 0.0
    cells = base_cells
    for _ in range(octaves):
        total += tileable_noise(size, cells, rng) * amp
        norm += amp
        amp *= persistence
        cells *= 2
    return total / norm


# --------------------------------------------------------------------------- geometry

class Geo:
    def __init__(self, identifier="geometry.storm_ray", tex=None, glow_zone=None, vein_zone=None, organic_max_y=None,
                 max_uv=160.0, bounds=(80, 30, 8), min_uv=4.0, vein_max=(120.0, 56.0)):
        self.bones = []
        self.identifier = identifier
        self.tex = tex or TEX
        self.glow_zone = glow_zone or GLOW_ZONE
        self.vein_zone = vein_zone or VEIN_ZONE
        self.organic_max_y = organic_max_y or ORGANIC_MAX_Y
        self.max_uv = max_uv
        self.bounds = bounds
        self.min_uv = min_uv
        self.vein_max = vein_max

    def bone(self, name, parent, pivot, rotation=None):
        b = {"name": name, "pivot": [round(v, 3) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = rotation
        self.bones.append(b)
        return b

    def cube(self, bone, origin, size, zone="organic", inflate=0.0, rotation=None, pivot=None):
        faces = {}
        w, h, d = size
        dims = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        for face, (fw, fh) in dims.items():
            faces[face] = {"uv": self._uv(zone, fw, fh), "uv_size": self._uv_size(zone, fw, fh)}
        cube = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": faces}
        if inflate:
            cube["inflate"] = inflate
        if rotation:
            cube["rotation"] = rotation
            cube["pivot"] = pivot
        bone["cubes"].append(cube)
        return cube

    def _uv_size(self, zone, fw, fh):
        if zone == "glow":
            g = max(2, min(8, self.glow_zone[2] // 4))
            return [g, g]
        density = 2.0
        uw = max(self.min_uv, min(self.max_uv, fw * density))
        uh = max(self.min_uv, min(self.max_uv, fh * density))
        if zone == "vein":
            uw, uh = min(uw, self.vein_max[0]), min(uh, self.vein_max[1])
        return [round(uw, 2), round(uh, 2)]

    def _uv(self, zone, fw, fh):
        uw, uh = self._uv_size(zone, fw, fh)
        if zone == "glow":
            x, y, gw, gh = self.glow_zone
            return [x + RNG.randint(2, gw - 12), y + RNG.randint(2, gh - 12)] if gw >= 16 else [x + 1, y + 1]
        if zone == "vein":
            x, y, vw, vh = self.vein_zone
            return [round(x + RNG.uniform(0, vw - uw), 2), round(y + RNG.uniform(0, vh - uh), 2)]
        return [round(RNG.uniform(0, self.tex - uw), 2), round(RNG.uniform(0, self.organic_max_y - uh), 2)]

    def to_json(self):
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": self.identifier,
                    "texture_width": self.tex,
                    "texture_height": self.tex,
                    "visible_bounds_width": self.bounds[0],
                    "visible_bounds_height": self.bounds[1],
                    "visible_bounds_offset": [0, self.bounds[2], 0],
                },
                "bones": self.bones,
            }],
        }


def mirror(origin, size):
    """Mirror a cube across the X axis."""
    return [-origin[0] - size[0], origin[1], origin[2]]


TENDRILS = [(10, -16, 1.0), (-10, -16, 0.95), (15, -2, 1.1), (-15, -2, 1.05),
            (11, 12, 0.9), (-11, 12, 1.0), (4, 20, 0.85), (-4, 20, 0.92)]
WING_SEGMENTS = [  # x start, length, depth, thickness, z start
    (20, 32, 50, 5.0, -26),
    (52, 28, 38, 4.0, -15),
    (80, 24, 24, 3.0, -3),
    (104, 18, 12, 2.0, 7),
]
TAIL_SEGMENTS = [  # z start, length, width, height
    (30, 22, 12, 7), (52, 22, 10, 6), (74, 24, 8, 5), (98, 24, 6, 4), (122, 24, 4, 3), (146, 20, 3, 2)]


def build_geometry():
    g = Geo()
    root = g.bone("root", None, [0, 0, 0])
    body = g.bone("body", "root", [0, 50, 0])
    g.cube(body, [-20, 46, -30], [40, 8, 60])
    g.cube(body, [-8, 54, -24], [16, 4, 46])
    g.cube(body, [-14, 43, -22], [28, 3, 44])
    for z in (-18, -9, 0, 9, 18):
        g.cube(body, [-1, 58, z - 1.5], [2, 4 + RNG.random() * 2, 3])
    g.cube(body, [-7, 42.5, -26], [14, 1, 3], zone="glow")             # maw
    for z in (-12, -6, 0, 6, 12):                                          # gill slits
        g.cube(body, [7, 42.6, z], [6, 0.5, 1.2], zone="glow")
        g.cube(body, [-13, 42.6, z], [6, 0.5, 1.2], zone="glow")
    for x in (-15, -9, 9, 15):                                             # dorsal veins
        g.cube(body, [x - 0.5, 54, -26], [1, 0.6, 50], zone="vein")

    head = g.bone("head", "body", [0, 50, -30])
    g.cube(head, [-14, 46, -40], [28, 7, 10])
    g.cube(head, [-10, 52, -38], [20, 3, 8])
    for x in (4, 8, 12):
        g.cube(head, [x - 1, 49, -40.8], [2, 2, 1], zone="glow")
        g.cube(head, [-x - 1, 49, -40.8], [2, 2, 1], zone="glow")
    for side, name in ((1, "horn_l"), (-1, "horn_r")):
        hx = 12 * side
        horn = g.bone(name, "head", [hx, 48, -40])
        o, s = [10, 44, -52], [5, 6, 12]
        g.cube(horn, o if side > 0 else mirror(o, s), s)
        tip = g.bone(name + "_tip", name, [hx, 44, -52])
        o, s = [10.5, 36, -56], [4, 10, 5]
        g.cube(tip, o if side > 0 else mirror(o, s), s)
        o, s = [11.5, 34.5, -55], [2, 2, 2]
        g.cube(tip, o if side > 0 else mirror(o, s), s, zone="glow")

    for side, prefix in ((1, "wing_l"), (-1, "wing_r")):
        parent = "body"
        for i, (x0, length, depth, thick, z0) in enumerate(WING_SEGMENTS, start=1):
            name = f"{prefix}{i}"
            pivot_x = x0 * side
            seg = g.bone(name, parent, [pivot_x, 50, 0])
            y0 = 50 - thick / 2
            o, s = [x0, y0, z0], [length, thick, depth]
            g.cube(seg, o if side > 0 else mirror(o, s), s)
            # rib / vein running along the wing
            o, s = [x0, y0 + thick, -2], [length, 0.8, 1.2]
            g.cube(seg, o if side > 0 else mirror(o, s), s, zone="vein")
            o, s = [x0, y0 + thick, z0 + depth * 0.25], [length * 0.8, 0.6, 1.0]
            g.cube(seg, o if side > 0 else mirror(o, s), s, zone="vein")
            # tattered trailing edge
            fringe_x = x0 + 1
            while fringe_x < x0 + length - 3:
                fw = RNG.uniform(3, 6)
                fl = RNG.uniform(3, 8 + 12 * (1 - i / 5)) if RNG.random() > 0.25 else RNG.uniform(1, 3)
                o, s = [fringe_x, y0 + 0.4, z0 + depth - 0.5], [fw, max(0.8, thick * 0.35), fl]
                g.cube(seg, o if side > 0 else mirror(o, s), s)
                fringe_x += fw + RNG.uniform(0.5, 2.5)
            if i == len(WING_SEGMENTS):
                o, s = [x0 + length, y0, z0 - 2], [7, thick, 4]
                g.cube(seg, o if side > 0 else mirror(o, s), s)
                o, s = [x0 + length + 6, y0, z0 - 2], [2, thick, 2]
                g.cube(seg, o if side > 0 else mirror(o, s), s, zone="glow")
            parent = name

    parent = "body"
    for i, (z0, length, width, height) in enumerate(TAIL_SEGMENTS, start=1):
        name = f"tail_{i}"
        seg = g.bone(name, parent, [0, 50, z0])
        g.cube(seg, [-width / 2, 50 - height / 2, z0], [width, height, length])
        if i <= 3:
            g.cube(seg, [-0.75, 50 + height / 2, z0 + length * 0.4], [1.5, 3 - i * 0.5, 3])
        g.cube(seg, [-0.4, 50 + height / 2, z0], [0.8, 0.5, length], zone="vein")
        parent = name
    spike = g.bone("tail_spike", parent, [0, 50, 166])
    g.cube(spike, [-1, 49, 166], [2, 2, 14])
    for z in (170, 176):
        g.cube(spike, [-4, 49.5, z], [8, 1, 2])
        g.cube(spike, [-5, 49.5, z], [1, 1, 1], zone="glow")
        g.cube(spike, [4, 49.5, z], [1, 1, 1], zone="glow")
    g.cube(spike, [-0.5, 49.5, 179], [1, 1, 2], zone="glow")

    for idx, (x, z, scale) in enumerate(TENDRILS):
        top = 43.0
        l1, l2, l3 = 14 * scale, 14 * scale, 13 * scale
        t1 = g.bone(f"tendril_{idx}_1", "body", [x, top, z])
        g.cube(t1, [x - 1.25, top - l1, z - 1.25], [2.5, l1, 2.5])
        y2 = top - l1
        t2 = g.bone(f"tendril_{idx}_2", f"tendril_{idx}_1", [x, y2, z])
        g.cube(t2, [x - 1, y2 - l2, z - 1], [2, l2, 2])
        g.cube(t2, [x - 0.3, y2 - l2, z - 1.05], [0.6, l2, 0.1], zone="vein")
        y3 = y2 - l2
        t3 = g.bone(f"tendril_{idx}_3", f"tendril_{idx}_2", [x, y3, z])
        g.cube(t3, [x - 0.75, y3 - l3, z - 0.75], [1.5, l3, 1.5])
        g.cube(t3, [x - 1, y3 - l3 - 2, z - 1], [2, 2, 2], zone="glow")
    return g


# --------------------------------------------------------------------------- animations

def vec(x=0, y=0, z=0):
    return [x, y, z]


def build_animations():
    wings = [f"wing_{s}{i}" for s in "lr" for i in range(1, 5)]
    tails = [f"tail_{i}" for i in range(1, 7)] + ["tail_spike"]
    tendrils = [(i, j) for i in range(len(TENDRILS)) for j in (1, 2, 3)]
    anims = {}

    def wing_sign(name):
        return 1 if name.startswith("wing_l") else -1

    # IDLE: slow organic flapping, tail undulation, drifting filaments. 4s loop, seamless (90 deg/s).
    bones = {"root": {"position": vec(0, "math.sin(query.anim_time * 90) * 1.5", 0)},
             "body": {"rotation": vec("math.sin(query.anim_time * 90 + 40) * 2", 0, "math.sin(query.anim_time * 90) * 1.5")},
             "head": {"rotation": vec("math.sin(query.anim_time * 90 - 30) * 2", 0, 0)},
             "horn_l": {"rotation": vec("math.sin(query.anim_time * 180) * 4", 0, 0)},
             "horn_r": {"rotation": vec("math.sin(query.anim_time * 180 + 60) * 4", 0, 0)}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        bones[w] = {"rotation": vec(0, 0, f"{sgn} * (math.sin(query.anim_time * 90 - {i * 28}) * {6 + i * 3})")}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": vec(f"math.sin(query.anim_time * 90 - {k * 40}) * 3",
                                    f"math.sin(query.anim_time * 180 - {k * 45}) * {3 + k}", 0)}
    for i, j in tendrils:
        ph = i * 47 + j * 30
        bones[f"tendril_{i}_{j}"] = {"rotation": vec(f"math.sin(query.anim_time * 180 + {ph}) * {4 + j * 3}", 0,
                                                     f"math.cos(query.anim_time * 90 + {ph}) * {3 + j * 2}")}
    anims["animation.storm_ray.idle"] = {"loop": True, "animation_length": 4.0, "bones": bones}

    # SWOOP: wings swept back, nose down, filaments trailing. 2s loop.
    bones = {"body": {"rotation": vec(-14, 0, 0)},
             "root": {"position": vec(0, "math.sin(query.anim_time * 180) * 1", 0)}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        bones[w] = {"rotation": vec(0, f"{sgn} * {-8 - i * 4}", f"{sgn} * (math.sin(query.anim_time * 360 - {i * 30}) * {4 + i * 2} - 4)")}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": vec(4, f"math.sin(query.anim_time * 360 - {k * 50}) * {2 + k}", 0)}
    for i, j in tendrils:
        bones[f"tendril_{i}_{j}"] = {"rotation": vec(f"{18 + j * 8} + math.sin(query.anim_time * 360 + {i * 40}) * 4", 0, 0)}
    anims["animation.storm_ray.swoop"] = {"loop": True, "animation_length": 2.0, "bones": bones}

    # LASH: filaments whip forward and down toward the target. 1.2s, play once.
    bones = {"body": {"rotation": {"0.0": vec(-14, 0, 0), "0.2": vec(6, 0, 0), "0.5": vec(-4, 0, 0), "1.2": vec(0, 0, 0)}}}
    for i, j in tendrils:
        side = 1 if TENDRILS[i][0] > 0 else -1
        amp = [0, -75, -40, -25][j]
        bones[f"tendril_{i}_{j}"] = {"rotation": {
            "0.0": vec(30, 0, 0),
            f"{0.12 + j * 0.05:.2f}": vec(amp, 0, side * 12),
            f"{0.45 + j * 0.05:.2f}": vec(amp * 0.4, 0, side * 6),
            "1.2": vec(0, 0, 0)}}
    for w in wings:
        sgn = wing_sign(w)
        bones[w] = {"rotation": {"0.0": vec(0, 0, 0), "0.2": vec(0, 0, sgn * 18), "0.6": vec(0, 0, sgn * -6), "1.2": vec(0, 0, 0)}}
    anims["animation.storm_ray.lash"] = {"loop": False, "animation_length": 1.2, "bones": bones}

    # CHARGE: wings curl upward around the body, everything trembles, filaments splay out. 2s loop.
    bones = {"root": {"position": vec(0, "2 + math.sin(query.anim_time * 180) * 0.6", 0)},
             "body": {"rotation": vec("math.sin(query.anim_time * 1440) * 0.8", 0, "math.cos(query.anim_time * 1440) * 0.8")},
             "head": {"rotation": vec(-10, 0, 0)}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        bones[w] = {"rotation": vec(0, 0, f"{sgn} * ({10 + i * 9} + math.sin(query.anim_time * 1440 + {i * 20}) * 2)")}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": vec(f"{-6 - k} + math.sin(query.anim_time * 720 - {k * 30}) * 2", 0, 0)}
    for i, j in tendrils:
        x, z, _ = TENDRILS[i]
        ang = math.degrees(math.atan2(z, x))
        rx = round(-math.sin(math.radians(ang)) * 28 / j, 2)
        rz = round(math.cos(math.radians(ang)) * 28 / j, 2)
        bones[f"tendril_{i}_{j}"] = {"rotation": vec(f"{rx} + math.sin(query.anim_time * 1440 + {i * 30}) * 3", 0,
                                                     f"{rz} + math.cos(query.anim_time * 1440 + {i * 30}) * 3")}
    anims["animation.storm_ray.charge"] = {"loop": True, "animation_length": 2.0, "bones": bones}

    # RELEASE: wings snap open, filaments flung outward, then settle. 1.5s.
    bones = {"root": {"position": {"0.0": vec(0, 2, 0), "0.15": vec(0, 4, 0), "1.5": vec(0, 0, 0)}},
             "body": {"rotation": {"0.0": vec(0, 0, 0), "0.12": vec(8, 0, 0), "0.6": vec(-3, 0, 0), "1.5": vec(0, 0, 0)}},
             "head": {"rotation": {"0.0": vec(-10, 0, 0), "0.15": vec(-25, 0, 0), "1.5": vec(0, 0, 0)}}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        bones[w] = {"rotation": {"0.0": vec(0, 0, sgn * (10 + i * 9)), "0.12": vec(0, 0, sgn * (-14 - i * 3)),
                                 "0.7": vec(0, 0, sgn * 5), "1.5": vec(0, 0, 0)}}
    for i, j in tendrils:
        x, z, _ = TENDRILS[i]
        ang = math.atan2(z, x)
        rx = round(-math.sin(ang) * 70 / j, 2)
        rz = round(math.cos(ang) * 70 / j, 2)
        bones[f"tendril_{i}_{j}"] = {"rotation": {"0.0": vec(0, 0, 0), "0.15": vec(rx, 0, rz),
                                                  "0.8": vec(rx * 0.3, 0, rz * 0.3), "1.5": vec(0, 0, 0)}}
    anims["animation.storm_ray.release"] = {"loop": False, "animation_length": 1.5, "bones": bones}

    # SUMMON: emerges folded and dormant, unfurls, then roars with wings spread. 7s.
    bones = {"root": {"position": {"0.0": vec(0, -6, 0), "5.0": vec(0, 0, 0), "7.0": vec(0, 0, 0)}},
             "body": {"rotation": {"0.0": vec(-35, 0, 0), "3.0": vec(-15, 0, 0), "5.2": vec(10, 0, 0),
                                   "5.8": vec(-12, 0, 0), "7.0": vec(0, 0, 0)}},
             "head": {"rotation": {"0.0": vec(30, 0, 0), "5.0": vec(10, 0, 0), "5.6": vec(-30, 0, 0), "7.0": vec(0, 0, 0)}}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        folded = sgn * (60 + i * 18)
        bones[w] = {"rotation": {"0.0": vec(0, 0, folded), "2.0": vec(0, 0, folded * 0.9),
                                 f"{3.5 + i * 0.3:.1f}": vec(0, 0, sgn * 10), "5.4": vec(0, 0, sgn * -20),
                                 "6.2": vec(0, 0, sgn * 6), "7.0": vec(0, 0, 0)}}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": {"0.0": vec(25 + k * 6, 0, 0), "4.0": vec(8, 0, 0), "5.5": vec(-6, 0, 0), "7.0": vec(0, 0, 0)}}
    for i, j in tendrils:
        bones[f"tendril_{i}_{j}"] = {"rotation": {"0.0": vec(0, 0, 0), "3.0": vec(10 * j, 0, 0),
                                                  "5.5": vec(-20, 0, 0), "7.0": vec(0, 0, 0)},
                                     "scale": {"0.0": vec(0.2, 0.2, 0.2), "3.5": vec(1, 1, 1)}}
    anims["animation.storm_ray.summon"] = {"loop": False, "animation_length": 7.0, "bones": bones}

    # DEATH: spasms, then the wings droop, the body rolls over and everything goes limp. 5s, hold.
    bones = {"root": {"position": {"0.0": vec(0, 0, 0), "5.0": vec(0, -10, 0)}},
             "body": {"rotation": {"0.0": vec(0, 0, 0), "0.4": vec(-12, 0, 6), "0.8": vec(8, 0, -8),
                                   "1.4": vec(-6, 0, 10), "3.0": vec(18, 0, 24), "5.0": vec(30, 0, 38)}},
             "head": {"rotation": {"0.0": vec(0, 0, 0), "0.6": vec(-35, 0, 0), "2.0": vec(10, 0, 0), "5.0": vec(28, 0, 0)}}}
    for w in wings:
        i = int(w[-1])
        sgn = wing_sign(w)
        bones[w] = {"rotation": {"0.0": vec(0, 0, 0), "0.3": vec(0, 0, sgn * 25), "0.7": vec(0, 0, sgn * -15),
                                 "1.2": vec(0, 0, sgn * 20), "3.5": vec(0, 0, sgn * (-25 - i * 6)),
                                 "5.0": vec(0, 0, sgn * (-35 - i * 8))}}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": {"0.0": vec(0, 0, 0), "0.5": vec(-10, 15, 0), "1.2": vec(10, -15, 0), "5.0": vec(14, 0, 0)}}
    for i, j in tendrils:
        bones[f"tendril_{i}_{j}"] = {"rotation": {"0.0": vec(0, 0, 0), "0.5": vec(-40 / j, 0, 20 / j),
                                                  "1.5": vec(20 / j, 0, -15 / j), "5.0": vec(0, 0, 0)}}
    anims["animation.storm_ray.death"] = {"loop": "hold_on_last_frame", "animation_length": 5.0, "bones": bones}

    return {"format_version": "1.8.0", "animations": anims, "geckolib_format_version": 2}


# --------------------------------------------------------------------------- textures

def entity_textures():
    base = fbm(TEX, 8, 6, NP_RNG)
    detail = fbm(TEX, 32, 4, NP_RNG)
    ridge = 1.0 - np.abs(fbm(TEX, 6, 5, NP_RNG) - 0.5) * 2.0          # 1 on vein lines
    veins = np.clip((ridge - 0.955) / 0.045, 0, 1) ** 1.3
    wrinkles = 1.0 - np.abs(fbm(TEX, 16, 4, NP_RNG) - 0.5) * 2.0
    folds = np.clip((wrinkles - 0.88) / 0.12, 0, 1)
    purple = np.clip((fbm(TEX, 4, 3, NP_RNG) - 0.5) * 3.0, 0, 1)

    shade = (0.3 + 0.6 * base * (0.7 + 0.3 * detail)) * (1.0 - 0.45 * folds)
    r = 10 + 30 * shade + 18 * purple * shade
    g = 7 + 18 * shade
    b = 14 + 30 * shade + 26 * purple * shade
    # pores / specks
    specks = NP_RNG.random((TEX, TEX)) > 0.985
    r = np.where(specks, r * 0.4, r)
    g = np.where(specks, g * 0.4, g)
    b = np.where(specks, b * 0.4, b)
    # red veins
    veins = veins * (0.55 + 0.45 * np.clip(purple * 2.0 + detail - 0.4, 0, 1))
    r = r * (1 - veins) + (150 + 60 * detail) * veins
    g = g * (1 - veins) + 14 * veins
    b = b * (1 - veins) + 32 * veins

    img = np.stack([r, g, b, np.full_like(r, 255)], axis=-1)
    glow = np.zeros((TEX, TEX, 4))
    strong = np.clip((veins - 0.6) / 0.4, 0, 1)
    glow[..., 0] = 230 * strong
    glow[..., 1] = 25 * strong
    glow[..., 2] = 40 * strong
    glow[..., 3] = 255 * strong

    # emissive zone: eyes, maw, filament tips
    x, y, w, h = GLOW_ZONE
    zone = fbm(64, 4, 3, NP_RNG)
    img[y:y + h, x:x + w, 0] = 210 + 45 * zone
    img[y:y + h, x:x + w, 1] = 30 + 50 * zone
    img[y:y + h, x:x + w, 2] = 40 + 20 * zone
    glow[y:y + h, x:x + w, 0] = 255
    glow[y:y + h, x:x + w, 1] = 60 + 90 * zone
    glow[y:y + h, x:x + w, 2] = 60 + 40 * zone
    glow[y:y + h, x:x + w, 3] = 255

    # vein zone: dark flesh with dense veins, half of them glowing
    x, y, w, h = VEIN_ZONE
    vz = 1.0 - np.abs(fbm(256, 8, 4, NP_RNG)[:h, :w] - 0.5) * 2.0
    vmask = np.clip((vz - 0.7) / 0.3, 0, 1)
    img[y:y + h, x:x + w, 0] = 25 + 170 * vmask
    img[y:y + h, x:x + w, 1] = 8 + 12 * vmask
    img[y:y + h, x:x + w, 2] = 20 + 30 * vmask
    glow[y:y + h, x:x + w, 0] = 220 * vmask
    glow[y:y + h, x:x + w, 1] = 20 * vmask
    glow[y:y + h, x:x + w, 2] = 35 * vmask
    glow[y:y + h, x:x + w, 3] = 255 * np.clip(vmask * 1.2 - 0.2, 0, 1)

    out = os.path.join(ASSETS, "textures", "entity")
    os.makedirs(out, exist_ok=True)
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "storm_ray.png"))
    Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "storm_ray_glowmask.png"))


def particle_sprites():
    out = os.path.join(ASSETS, "textures", "particle")
    os.makedirs(out, exist_ok=True)
    size = 8
    yy, xx = np.mgrid[0:size, 0:size]
    d = np.sqrt((xx - 3.5) ** 2 + (yy - 3.5) ** 2)
    for frame in range(4):
        radius = 3.6 - frame * 0.6
        core = np.clip(1 - d / radius, 0, 1)
        img = np.zeros((size, size, 4))
        img[..., 0] = 255
        img[..., 1] = 60 + 140 * core ** 2
        img[..., 2] = 40 + 60 * core ** 3
        img[..., 3] = 255 * np.clip(core * 1.6, 0, 1)
        Image.fromarray(img.astype(np.uint8), "RGBA").save(os.path.join(out, f"storm_ember_{frame}.png"))

        img = np.zeros((size, size, 4))
        speck = NP_RNG.random((size, size)) * np.clip(1 - d / (3.8 - frame * 0.5), 0, 1)
        mask = speck > 0.25
        grey = 120 + 70 * NP_RNG.random((size, size))
        img[..., 0] = grey
        img[..., 1] = grey
        img[..., 2] = grey + 8
        img[..., 3] = np.where(mask, 230, 0)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, f"petrify_dust_{frame}.png"))

        img = np.zeros((size, size, 4))
        arm = 3 - frame * 0.6
        cross = ((np.abs(xx - 3.5) < 0.6) & (np.abs(yy - 3.5) <= arm)) | ((np.abs(yy - 3.5) < 0.6) & (np.abs(xx - 3.5) <= arm))
        diag = (np.abs(np.abs(xx - 3.5) - np.abs(yy - 3.5)) < 0.6) & (d < arm * 0.8)
        mask = cross | diag
        img[..., 0] = 255
        img[..., 1] = np.where(d < 1.2, 200, 50)
        img[..., 2] = np.where(d < 1.2, 190, 70)
        img[..., 3] = np.where(mask, 255, 0)
        Image.fromarray(img.astype(np.uint8), "RGBA").save(os.path.join(out, f"ritual_rune_{frame}.png"))

    pdir = os.path.join(ASSETS, "particles")
    for name in ("storm_ember", "petrify_dust", "ritual_rune"):
        write_json(os.path.join(pdir, f"{name}.json"), {"textures": [f"stormray:{name}_{i}" for i in range(4)]})


def environment_textures():
    out = os.path.join(ASSETS, "textures", "environment")
    os.makedirs(out, exist_ok=True)
    n = fbm(256, 4, 6, NP_RNG, persistence=0.55)
    n = (n - n.min()) / (n.max() - n.min())
    alpha = np.clip((n - 0.32) / 0.45, 0, 1) ** 1.2
    shade = 140 + 115 * n
    img = np.stack([shade, shade * 0.92, shade * 0.95, 255 * alpha], axis=-1)
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "storm_clouds.png"))

    misc = os.path.join(ASSETS, "textures", "misc")
    os.makedirs(misc, exist_ok=True)
    s = 64
    stone = fbm(s, 4, 4, NP_RNG)
    crack = 1.0 - np.abs(fbm(s, 3, 3, NP_RNG) - 0.5) * 2.0
    crack_mask = np.clip((crack - 0.9) / 0.1, 0, 1)
    grey = 105 + 70 * stone
    grey = grey * (1 - 0.65 * crack_mask)
    img = np.stack([grey, grey, grey * 1.03, np.full_like(grey, 255)], axis=-1)
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(misc, "petrified_stone.png"))

    # full-screen crack vignette: cracks creep in from the edges
    w, h = 512, 288
    ov = np.zeros((h, w, 4))
    yy, xx = np.mgrid[0:h, 0:w]
    edge = np.minimum.reduce([xx / w, (w - xx) / w, yy / h * 0.8, (h - yy) / h * 0.8])
    edge_factor = np.clip(1 - edge / 0.32, 0, 1)
    big = fbm(512, 5, 5, NP_RNG)[:h, :w]
    lines = 1.0 - np.abs(big - 0.5) * 2.0
    crack_lines = np.clip((lines - 0.93) / 0.07, 0, 1) * np.clip(edge_factor * 1.4, 0, 1)
    vignette = edge_factor ** 2 * 0.55
    a = np.clip(vignette + crack_lines * 0.9, 0, 1)
    tone = 70 + 40 * big
    ov[..., 0] = tone * (1 - crack_lines * 0.7)
    ov[..., 1] = tone * (1 - crack_lines * 0.7)
    ov[..., 2] = tone * (1 - crack_lines * 0.7) + 6
    ov[..., 3] = 255 * a
    Image.fromarray(np.clip(ov, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(misc, "petrify_overlay.png"))


def portal_and_soot_textures():
    """Animated portal surface (swirling blood-red void) and soot flakes."""
    out = os.path.join(ASSETS, "textures", "block")
    os.makedirs(out, exist_ok=True)
    frames = 32
    size = 16
    big = 64
    base = fbm(big, 4, 4, NP_RNG)
    yy, xx = np.mgrid[0:size, 0:size]
    sheet = np.zeros((size * frames, size, 4))
    for f in range(frames):
        t = f / frames
        # sample the tileable noise along a slowly rotating, scrolling path
        u = (xx * 2 + int(t * big)) % big
        v = (yy * 2 + int(t * big * 0.5)) % big
        n = base[v, u]
        swirl = 0.5 + 0.5 * np.sin((xx + yy) * 0.6 + t * 2 * np.pi + n * 6)
        glow = np.clip(n * 0.9 + swirl * 0.4 - 0.25, 0, 1)
        frame = np.zeros((size, size, 4))
        frame[..., 0] = 40 + 200 * glow
        frame[..., 1] = 4 + 30 * glow ** 2
        frame[..., 2] = 10 + 40 * glow ** 3
        frame[..., 3] = 170 + 80 * glow
        sheet[f * size:(f + 1) * size] = frame
    Image.fromarray(np.clip(sheet, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "ashen_portal.png"))
    write_json(os.path.join(out, "ashen_portal.png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})

    pout = os.path.join(ASSETS, "textures", "particle")
    for frame in range(4):
        img = np.zeros((8, 8, 4))
        cx, cy = 3.5 + NP_RNG.uniform(-0.6, 0.6), 3.5 + NP_RNG.uniform(-0.6, 0.6)
        d = np.sqrt((np.mgrid[0:8, 0:8][1] - cx) ** 2 + ((np.mgrid[0:8, 0:8][0] - cy) * 1.6) ** 2)
        mask = (d < 2.6 - frame * 0.35) & (NP_RNG.random((8, 8)) > 0.15)
        tone = 22 + 30 * NP_RNG.random((8, 8))
        img[..., 0] = tone + 6
        img[..., 1] = tone
        img[..., 2] = tone
        img[..., 3] = np.where(mask, 235, 0)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(pout, f"soot_{frame}.png"))
    write_json(os.path.join(ASSETS, "particles", "soot.json"), {"textures": [f"stormray:soot_{i}" for i in range(4)]})


# --------------------------------------------------------------------------- Phantom X (soot wraith)

PX_TEX = 128
PX_GLOW = (0, 112, 16, 16)
PX_VEIN = (16, 112, 48, 16)
PX_ORGANIC_MAX_Y = 108


def build_phantom_geometry():
    g = Geo("geometry.phantom_x", PX_TEX, PX_GLOW, PX_VEIN, PX_ORGANIC_MAX_Y, max_uv=48.0, bounds=(3, 4, 1.3),
            min_uv=2.0, vein_max=(PX_VEIN[2] - 1.0, PX_VEIN[3] - 1.0))
    g.bone("root", None, [0, 0, 0])
    body = g.bone("body", "root", [0, 24, 0])
    g.cube(body, [-4, 24, -2], [8, 9, 4])
    g.cube(body, [-3, 18, -1.5], [6, 6, 3])
    g.cube(body, [-5.5, 30, -2.8], [11, 3, 5.6])             # ragged mantle
    for x in (-4.6, -2.2, 1.4, 3.6):
        g.cube(body, [x, 28.5, -3.0], [1.2, 2.0, 0.4])
    for y in (25.5, 27.5, 29.5):                                # faint ember ribs
        g.cube(body, [-3, y, -2.15], [6, 0.4, 0.2], zone="vein")

    head = g.bone("head", "body", [0, 33, 0])
    g.cube(head, [-4, 33, -4], [8, 9, 8])
    g.cube(head, [-1.5, 41, -2], [3, 2, 4])
    g.cube(head, [-4.4, 32, -4.4], [8.8, 3, 1])                 # drooping hood rim
    g.cube(head, [-2.5, 34, -4.25], [5, 6, 0.3])               # hollow face
    for o, sz in (([-0.7, 38.3], [0.9, 2.4]), ([-0.2, 36.4], [0.9, 2.2]), ([-0.8, 34.5], [0.9, 2.2]),
                  ([0.6, 38.9], [1.1, 0.6]), ([-1.7, 36.9], [1.1, 0.6]), ([0.5, 35.0], [0.8, 0.6])):
        g.cube(head, [o[0], o[1], -4.45], [sz[0], sz[1], 0.2], zone="glow")  # jagged glowing crack

    for side, name in ((-1, "arm_r"), (1, "arm_l")):
        sx = 5 * side
        upper = g.bone(name, "body", [sx, 31, 0])
        o, s = [5, 21, -1], [2, 10, 2]
        g.cube(upper, o if side > 0 else mirror(o, s), s)
        lower = g.bone(name + "_lower", name, [6 * side, 21, 0])
        o, s = [5.25, 10, -0.75], [1.5, 11, 1.5]
        g.cube(lower, o if side > 0 else mirror(o, s), s)
        for k, cx in enumerate((5.1, 5.85, 6.6)):
            o, s = [cx, 4, -0.6 + k * 0.4], [0.5, 6, 0.5]
            g.cube(lower, o if side > 0 else mirror(o, s), s)
            o, s = [cx, 3.2, -0.6 + k * 0.4], [0.5, 0.8, 0.5]
            g.cube(lower, o if side > 0 else mirror(o, s), s, zone="glow")

    # tattered cloak: four panels, each with a swaying lower fringe
    panels = {
        "cloak_front": ([0, 20, -2], [-4.5, 9, -2.6], [9, 11, 1], "z", -2.6),
        "cloak_back": ([0, 22, 2], [-5, 7, 1.6], [10, 15, 1], "z", 1.6),
        "cloak_left": ([4.2, 21, 0], [4, 8, -2.2], [1, 13, 4.4], "x", 4.0),
        "cloak_right": ([-4.2, 21, 0], [-5, 8, -2.2], [1, 13, 4.4], "x", -5.0),
    }
    for name, (pivot, origin, size, axis, plane) in panels.items():
        panel = g.bone(name, "body", pivot)
        g.cube(panel, origin, size)
        low = g.bone(name + "_low", name, [pivot[0], origin[1], pivot[2]])
        span = size[0] if axis == "z" else size[2]
        start = origin[0] if axis == "z" else origin[2]
        cursor = start
        while cursor < start + span - 0.5:
            w = RNG.uniform(1.0, 2.2)
            length = RNG.uniform(3.0, 7.5)
            if axis == "z":
                g.cube(low, [cursor, origin[1] - length, plane + 0.1], [min(w, start + span - cursor), length, 0.8])
            else:
                g.cube(low, [plane + 0.1, origin[1] - length, cursor], [0.8, length, min(w, start + span - cursor)])
            cursor += w + RNG.uniform(0.1, 0.5)
    return g


def build_phantom_animations():
    cloak = ["cloak_front", "cloak_back", "cloak_left", "cloak_right"]
    lows = [c + "_low" for c in cloak]
    anims = {}

    bones = {"root": {"position": vec(0, "1.5 + math.sin(query.anim_time * 120) * 1", 0)},
             "body": {"rotation": vec("math.sin(query.anim_time * 120 + 40) * 2", 0, "math.sin(query.anim_time * 120) * 1.5")},
             "head": {"rotation": vec("math.sin(query.anim_time * 120 - 30) * 3", "math.sin(query.anim_time * 120) * 6", 0)},
             "arm_r": {"rotation": vec("math.sin(query.anim_time * 120) * 4", 0, "4 + math.sin(query.anim_time * 240) * 2")},
             "arm_l": {"rotation": vec("math.sin(query.anim_time * 120 + 60) * 4", 0, "-4 - math.sin(query.anim_time * 240) * 2")},
             "arm_r_lower": {"rotation": vec("-6 + math.sin(query.anim_time * 120) * 4", 0, 0)},
             "arm_l_lower": {"rotation": vec("-6 + math.sin(query.anim_time * 120 + 60) * 4", 0, 0)}}
    for k, c in enumerate(cloak):
        sign = 1 if c in ("cloak_front", "cloak_left") else -1
        axis_x = c in ("cloak_front", "cloak_back")
        expr = f"{sign} * math.sin(query.anim_time * 240 - {k * 50}) * 4"
        bones[c] = {"rotation": vec(expr, 0, 0) if axis_x else vec(0, 0, expr)}
        expr2 = f"{sign} * math.sin(query.anim_time * 240 - {k * 50 + 70}) * 9"
        bones[lows[k]] = {"rotation": vec(expr2, 0, 0) if axis_x else vec(0, 0, expr2)}
    anims["animation.phantom_x.idle"] = {"loop": True, "animation_length": 3.0, "bones": bones}

    bones = {"root": {"position": vec(0, "1.5 + math.sin(query.anim_time * 360) * 0.6", 0)},
             "body": {"rotation": vec(16, 0, "math.sin(query.anim_time * 360) * 3")},
             "head": {"rotation": vec(-12, 0, 0)},
             "arm_r": {"rotation": vec("28 + math.sin(query.anim_time * 360) * 6", 0, 6)},
             "arm_l": {"rotation": vec("28 + math.sin(query.anim_time * 360 + 90) * 6", 0, -6)},
             "arm_r_lower": {"rotation": vec(12, 0, 0)},
             "arm_l_lower": {"rotation": vec(12, 0, 0)},
             "cloak_front": {"rotation": vec(-24, 0, 0)},
             "cloak_front_low": {"rotation": vec("-10 + math.sin(query.anim_time * 720) * 6", 0, 0)},
             "cloak_back": {"rotation": vec(28, 0, 0)},
             "cloak_back_low": {"rotation": vec("18 + math.sin(query.anim_time * 720 + 40) * 8", 0, 0)},
             "cloak_left": {"rotation": vec(0, 0, "8 + math.sin(query.anim_time * 720) * 4")},
             "cloak_right": {"rotation": vec(0, 0, "-8 - math.sin(query.anim_time * 720) * 4")}}
    anims["animation.phantom_x.move"] = {"loop": True, "animation_length": 1.0, "bones": bones}

    # four alternating slashes, hits land at 0.15 / 0.40 / 0.65 / 0.90 s
    def slash(start):
        return {f"{start:.2f}": vec(-130, 0, 0), f"{start + 0.15:.2f}": vec(35, 0, 0), f"{start + 0.25:.2f}": vec(10, 0, 0)}
    # keys always formatted with 2 decimals: GeckoLib rejects two keyframes at the same time ("0.0" and "0.00")
    right = {"0.00": vec(0, 0, 0)}
    left = {"0.00": vec(0, 0, 0)}
    for i, start in enumerate((0.0, 0.25, 0.5, 0.75)):
        (right if i % 2 == 0 else left).update(slash(start))
    right["1.05"] = vec(0, 0, 0)
    left["1.05"] = vec(0, 0, 0)
    bones = {"arm_r": {"rotation": right}, "arm_l": {"rotation": left},
             "arm_r_lower": {"rotation": {"0.0": vec(0, 0, 0), "0.1": vec(-30, 0, 0), "0.6": vec(-30, 0, 0), "1.05": vec(0, 0, 0)}},
             "arm_l_lower": {"rotation": {"0.0": vec(0, 0, 0), "0.35": vec(-30, 0, 0), "0.85": vec(-30, 0, 0), "1.05": vec(0, 0, 0)}},
             "body": {"rotation": {"0.0": vec(0, 0, 0), "0.15": vec(12, -22, 0), "0.40": vec(12, 22, 0),
                                   "0.65": vec(14, -24, 0), "0.90": vec(14, 24, 0), "1.05": vec(0, 0, 0)}},
             "head": {"rotation": {"0.0": vec(0, 0, 0), "0.1": vec(-20, 0, 0), "0.95": vec(-15, 0, 0), "1.05": vec(0, 0, 0)}},
             "root": {"position": {"0.0": vec(0, 1.5, 0), "0.15": vec(0, 1.5, -2), "0.9": vec(0, 1.5, -2), "1.05": vec(0, 1.5, 0)}}}
    anims["animation.phantom_x.combo"] = {"loop": False, "animation_length": 1.05, "bones": bones}

    bones = {"arm_r": {"rotation": {"0.0": vec(0, 0, 0), "0.12": vec(-20, 0, 45), "0.5": vec(0, 0, 0)}},
             "arm_l": {"rotation": {"0.0": vec(0, 0, 0), "0.12": vec(-20, 0, -45), "0.5": vec(0, 0, 0)}},
             "head": {"rotation": {"0.0": vec(0, 0, 0), "0.12": vec(-25, 0, 0), "0.5": vec(0, 0, 0)}},
             "root": {"position": {"0.0": vec(0, 1.5, 0), "0.12": vec(0, 3, 0), "0.5": vec(0, 1.5, 0)}}}
    for c in cloak:
        out = {"cloak_front": vec(-30, 0, 0), "cloak_back": vec(30, 0, 0), "cloak_left": vec(0, 0, 30), "cloak_right": vec(0, 0, -30)}[c]
        bones[c] = {"rotation": {"0.0": vec(0, 0, 0), "0.12": out, "0.5": vec(0, 0, 0)}}
    anims["animation.phantom_x.reveal"] = {"loop": False, "animation_length": 0.5, "bones": bones}

    bones = {"root": {"position": {"0.0": vec(0, 1.5, 0), "0.3": vec(0, 3, 0), "1.5": vec(0, -7, 0)}},
             "body": {"rotation": {"0.0": vec(0, 0, 0), "0.3": vec(-20, 0, 0), "1.5": vec(45, 0, 0)}},
             "head": {"rotation": {"0.0": vec(0, 0, 0), "0.3": vec(-35, 0, 0), "1.5": vec(30, 0, 0)}},
             "arm_r": {"rotation": {"0.0": vec(0, 0, 0), "0.3": vec(-60, 0, 30), "1.5": vec(10, 0, 5)}},
             "arm_l": {"rotation": {"0.0": vec(0, 0, 0), "0.3": vec(-60, 0, -30), "1.5": vec(10, 0, -5)}}}
    for c in cloak:
        bones[c] = {"rotation": {"0.0": vec(0, 0, 0), "1.5": {"cloak_front": vec(-50, 0, 0), "cloak_back": vec(50, 0, 0),
                                                              "cloak_left": vec(0, 0, 50), "cloak_right": vec(0, 0, -50)}[c]}}
    anims["animation.phantom_x.death"] = {"loop": "hold_on_last_frame", "animation_length": 1.5, "bones": bones}
    return {"format_version": "1.8.0", "animations": anims, "geckolib_format_version": 2}


def phantom_textures():
    rng = np.random.default_rng(777)
    n = PX_TEX
    base = fbm(n, 4, 5, rng)
    streak = tileable_noise(n, 32, rng)
    streak = np.repeat(streak[:, :1], n, axis=1) * 0 + tileable_noise(n, 48, rng).mean(axis=0, keepdims=True)
    fibers = np.clip(streak * 0.6 + fbm(n, 16, 3, rng) * 0.4, 0, 1)
    holes = fbm(n, 8, 3, rng)
    shade = 0.25 + 0.55 * base * (0.6 + 0.4 * fibers)
    shade = shade * (1 - 0.5 * np.clip((holes - 0.68) / 0.1, 0, 1))
    r = 16 + 40 * shade
    g = 14 + 34 * shade
    b = 18 + 38 * shade
    embers = rng.random((n, n)) > 0.993
    r = np.where(embers, 150, r)
    g = np.where(embers, 30, g)
    b = np.where(embers, 25, b)
    img = np.stack([r, g, b, np.full_like(r, 255)], axis=-1)
    glow = np.zeros((n, n, 4))
    glow[..., 0] = np.where(embers, 200, 0)
    glow[..., 1] = np.where(embers, 30, 0)
    glow[..., 3] = np.where(embers, 160, 0)

    x, y, w, h = PX_GLOW
    img[y:y + h, x:x + w] = [255, 70, 40, 255]
    glow[y:y + h, x:x + w] = [255, 90, 50, 255]
    x, y, w, h = PX_VEIN
    vz = 1.0 - np.abs(fbm(64, 4, 3, rng)[:h, :w] - 0.5) * 2.0
    vmask = np.clip((vz - 0.75) / 0.25, 0, 1)
    img[y:y + h, x:x + w, 0] = 22 + 150 * vmask
    img[y:y + h, x:x + w, 1] = 14 + 10 * vmask
    img[y:y + h, x:x + w, 2] = 18 + 10 * vmask
    glow[y:y + h, x:x + w, 0] = 200 * vmask
    glow[y:y + h, x:x + w, 1] = 25 * vmask
    glow[y:y + h, x:x + w, 3] = 255 * vmask
    out = os.path.join(ASSETS, "textures", "entity")
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "phantom_x.png"))
    Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "phantom_x_glowmask.png"))


# --------------------------------------------------------------------------- Phantom Sea (drowned wraith)

def build_sea_geometry():
    g = Geo("geometry.phantom_sea", PX_TEX, PX_GLOW, PX_VEIN, PX_ORGANIC_MAX_Y, max_uv=48.0, bounds=(4, 2, 0.5),
            min_uv=2.0, vein_max=(PX_VEIN[2] - 1.0, PX_VEIN[3] - 1.0))
    g.bone("root", None, [0, 0, 0])
    body = g.bone("body", "root", [0, 8, 0])
    g.cube(body, [-3.5, 5, -6], [7, 6, 10])                      # hunched torso, swimming flat
    g.cube(body, [-4.2, 9.5, -5.5], [8.4, 2, 6])                  # ragged shoulder mantle
    for z in (-3.5, -1.5, 0.5):                                   # glowing gill slits
        g.cube(body, [3.45, 7, z], [0.2, 2.2, 0.6], zone="glow")
        g.cube(body, [-3.65, 7, z], [0.2, 2.2, 0.6], zone="glow")
    for z in (-4, -1, 2):
        g.cube(body, [-0.5, 11, z], [1, 2.2 - abs(z) * 0.2, 1.6])  # dorsal spines
    head = g.bone("head", "body", [0, 8, -6])
    g.cube(head, [-3.5, 4.5, -13], [7, 7, 7])                     # drowned hood
    g.cube(head, [-3.8, 4, -13.4], [7.6, 1.6, 7.8])               # sodden hood rim
    g.cube(head, [-2.2, 5.5, -13.25], [4.4, 5, 0.3])              # hollow face
    for o, sz in (([-0.6, 8.3], [0.8, 2.2]), ([-0.1, 6.6], [0.8, 1.9]), ([-0.7, 5.6], [0.8, 1.2]),
                  ([0.5, 9.0], [1.0, 0.5]), ([-1.6, 7.2], [1.0, 0.5])):
        g.cube(head, [o[0], o[1], -13.45], [sz[0], sz[1], 0.2], zone="glow")
    for k, x in enumerate((-2.5, -0.5, 1.5)):                      # drifting strands under the hood
        strand = g.bone(f"strand_{k}", "head", [x + 0.5, 4.5, -11])
        g.cube(strand, [x, 0.5 - k * 0.6, -11.5], [1, 4 + k * 0.6, 0.6])
    for side, name in ((-1, "arm_r"), (1, "arm_l")):
        upper = g.bone(name, "body", [3.6 * side, 7, -5])
        o, s = [3.5, 6.2, -13], [1.5, 1.5, 8]
        g.cube(upper, o if side > 0 else mirror(o, s), s)
        lower = g.bone(name + "_lower", name, [4.25 * side, 6.9, -13])
        o, s = [3.65, 6.3, -21], [1.2, 1.2, 8]
        g.cube(lower, o if side > 0 else mirror(o, s), s)
        for k, cy in enumerate((6.0, 6.7, 7.4)):
            o, s = [3.9 + (k - 1) * 0.35, cy, -25], [0.4, 0.4, 4]
            g.cube(lower, o if side > 0 else mirror(o, s), s)
            o, s = [3.9 + (k - 1) * 0.35, cy, -25.6], [0.4, 0.4, 0.6]
            g.cube(lower, o if side > 0 else mirror(o, s), s, zone="glow")
    for side, name in ((-1, "fin_r"), (1, "fin_l")):
        fin = g.bone(name, "body", [3.5 * side, 9, -1])
        o, s = [3.5, 8.6, -5], [5, 0.6, 7]
        g.cube(fin, o if side > 0 else mirror(o, s), s)
        z = -4.5
        while z < 2.0:                                            # tattered trailing edge
            w = RNG.uniform(1.0, 1.8)
            o, s = [3.5 + RNG.uniform(0, 1.5), 8.7, z], [RNG.uniform(3.5, 6.5), 0.4, w]
            g.cube(fin, o if side > 0 else mirror(o, s), s)
            z += w + RNG.uniform(0.1, 0.4)
    parent = "body"
    segments = [(4, 8, 5, 5), (12, 8, 4, 4), (20, 8, 3, 3), (28, 8, 2, 2)]
    for i, (z0, length, w, h) in enumerate(segments, start=1):
        seg = g.bone(f"tail_{i}", parent, [0, 8, z0])
        g.cube(seg, [-w / 2, 8 - h / 2, z0], [w, h, length])
        g.cube(seg, [-0.3, 8 + h / 2, z0], [0.6, 0.2, length], zone="vein")
        parent = f"tail_{i}"
    fluke = g.bone("tail_fin", parent, [0, 8, 36])
    g.cube(fluke, [-3.5, 7.8, 36], [7, 0.4, 5])
    g.cube(fluke, [-4.5, 7.8, 39], [2, 0.4, 3])
    g.cube(fluke, [2.5, 7.8, 39], [2, 0.4, 3])
    return g


def build_sea_animations():
    tails = ["tail_1", "tail_2", "tail_3", "tail_4", "tail_fin"]
    strands = ["strand_0", "strand_1", "strand_2"]
    anims = {}

    def swim(speed, amp, length):
        bones = {"root": {"position": vec(0, f"math.sin(query.anim_time * {speed}) * 0.6", 0)},
                 "body": {"rotation": vec(0, f"math.sin(query.anim_time * {speed} + 90) * {amp * 0.25}", 0)},
                 "head": {"rotation": vec(f"math.sin(query.anim_time * {speed}) * 3", f"math.sin(query.anim_time * {speed} - 60) * {amp * 0.3}", 0)},
                 "arm_r": {"rotation": vec(f"math.sin(query.anim_time * {speed}) * 6", f"-6 + math.sin(query.anim_time * {speed} + 40) * 5", 0)},
                 "arm_l": {"rotation": vec(f"math.sin(query.anim_time * {speed} + 90) * 6", f"6 - math.sin(query.anim_time * {speed} + 40) * 5", 0)},
                 "fin_r": {"rotation": vec(0, 0, f"math.sin(query.anim_time * {speed}) * {amp}")},
                 "fin_l": {"rotation": vec(0, 0, f"-1 * math.sin(query.anim_time * {speed}) * {amp}")}}
        for k, t in enumerate(tails):
            bones[t] = {"rotation": vec(0, f"math.sin(query.anim_time * {speed} - {k * 45}) * {amp * (0.6 + k * 0.25):.1f}", 0)}
        for k, st in enumerate(strands):
            bones[st] = {"rotation": vec(f"20 + math.sin(query.anim_time * {speed} - {k * 60}) * 10", 0, 0)}
        return {"loop": True, "animation_length": length, "bones": bones}

    anims["animation.phantom_sea.idle"] = swim(120, 8, 3.0)
    anims["animation.phantom_sea.swim"] = swim(360, 16, 1.0)

    bones = {"head": {"rotation": vec(f"25 + math.sin(query.anim_time * 720) * 4", 0, 0)},
             "body": {"rotation": vec(18, 0, f"math.sin(query.anim_time * 720) * 4")},
             "arm_r": {"rotation": vec("55 + math.sin(query.anim_time * 720) * 6", 14, 0)},
             "arm_l": {"rotation": vec("55 + math.sin(query.anim_time * 720 + 90) * 6", -14, 0)},
             "arm_r_lower": {"rotation": vec(40, 0, 0)},
             "arm_l_lower": {"rotation": vec(40, 0, 0)},
             "fin_r": {"rotation": vec(0, 0, "math.sin(query.anim_time * 720) * 25")},
             "fin_l": {"rotation": vec(0, 0, "-1 * math.sin(query.anim_time * 720) * 25")}}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": vec(f"-8 - {k * 4}", f"math.sin(query.anim_time * 720 - {k * 50}) * {14 + k * 6}", 0)}
    anims["animation.phantom_sea.grab"] = {"loop": True, "animation_length": 1.0, "bones": bones}

    bones = {"arm_r": {"rotation": {"0.00": vec(-40, 30, 0), "0.15": vec(-50, 40, 0), "0.30": vec(50, 10, 0), "0.50": vec(55, 14, 0)}},
             "arm_l": {"rotation": {"0.00": vec(-40, -30, 0), "0.15": vec(-50, -40, 0), "0.30": vec(50, -10, 0), "0.50": vec(55, -14, 0)}},
             "head": {"rotation": {"0.00": vec(-20, 0, 0), "0.30": vec(25, 0, 0), "0.50": vec(25, 0, 0)}},
             "root": {"position": {"0.00": vec(0, 0, 0), "0.30": vec(0, 0, -3), "0.50": vec(0, 0, 0)}}}
    anims["animation.phantom_sea.lunge"] = {"loop": False, "animation_length": 0.5, "bones": bones}

    bones = {"root": {"position": {"0.00": vec(0, 0, 0), "1.50": vec(0, -4, 0)}},
             "body": {"rotation": {"0.00": vec(0, 0, 0), "0.40": vec(-20, 0, 30), "1.50": vec(10, 0, 160)}},
             "arm_r": {"rotation": {"0.00": vec(0, 0, 0), "1.50": vec(30, 20, 0)}},
             "arm_l": {"rotation": {"0.00": vec(0, 0, 0), "1.50": vec(30, -20, 0)}}}
    for k, t in enumerate(tails):
        bones[t] = {"rotation": {"0.00": vec(0, 0, 0), "0.40": vec(0, 25 - k * 10, 0), "1.50": vec(6, 0, 0)}}
    anims["animation.phantom_sea.death"] = {"loop": "hold_on_last_frame", "animation_length": 1.5, "bones": bones}
    return {"format_version": "1.8.0", "animations": anims, "geckolib_format_version": 2}


def sea_textures():
    rng = np.random.default_rng(909)
    n = PX_TEX
    base = fbm(n, 4, 5, rng)
    ripple = fbm(n, 12, 3, rng)
    scales = 1.0 - np.abs(fbm(n, 24, 2, rng) - 0.5) * 2.0
    shade = 0.25 + 0.5 * base * (0.7 + 0.3 * ripple)
    shade = shade * (1 - 0.35 * np.clip((scales - 0.85) / 0.15, 0, 1))
    r = 10 + 24 * shade
    g = 16 + 34 * shade
    b = 26 + 52 * shade
    img = np.stack([r, g, b, np.full_like(r, 255)], axis=-1)
    glow = np.zeros((n, n, 4))
    x, y, w, h = PX_GLOW
    img[y:y + h, x:x + w] = [255, 70, 40, 255]
    glow[y:y + h, x:x + w] = [255, 90, 50, 255]
    x, y, w, h = PX_VEIN
    vz = 1.0 - np.abs(fbm(64, 4, 3, rng)[:h, :w] - 0.5) * 2.0
    vmask = np.clip((vz - 0.75) / 0.25, 0, 1)
    img[y:y + h, x:x + w, 0] = 18 + 140 * vmask
    img[y:y + h, x:x + w, 1] = 16 + 10 * vmask
    img[y:y + h, x:x + w, 2] = 30 + 10 * vmask
    glow[y:y + h, x:x + w, 0] = 190 * vmask
    glow[y:y + h, x:x + w, 1] = 25 * vmask
    glow[y:y + h, x:x + w, 3] = 255 * vmask
    out = os.path.join(ASSETS, "textures", "entity")
    Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "phantom_sea.png"))
    Image.fromarray(np.clip(glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out, "phantom_sea_glowmask.png"))


def main():
    geo = build_geometry()
    write_json(os.path.join(ASSETS, "geo", "entity", "storm_ray.geo.json"), geo.to_json())
    write_json(os.path.join(ASSETS, "animations", "entity", "storm_ray.animation.json"), build_animations())
    entity_textures()
    particle_sprites()
    environment_textures()
    portal_and_soot_textures()
    phantom = build_phantom_geometry()
    write_json(os.path.join(ASSETS, "geo", "entity", "phantom_x.geo.json"), phantom.to_json())
    write_json(os.path.join(ASSETS, "animations", "entity", "phantom_x.animation.json"), build_phantom_animations())
    phantom_textures()
    print(f"phantom_x: {len(phantom.bones)} bones, {sum(len(b['cubes']) for b in phantom.bones)} cubes")
    sea = build_sea_geometry()
    write_json(os.path.join(ASSETS, "geo", "entity", "phantom_sea.geo.json"), sea.to_json())
    write_json(os.path.join(ASSETS, "animations", "entity", "phantom_sea.animation.json"), build_sea_animations())
    sea_textures()
    print(f"phantom_sea: {len(sea.bones)} bones, {sum(len(b['cubes']) for b in sea.bones)} cubes")
    bones = geo.bones
    print(f"geometry: {len(bones)} bones, {sum(len(b['cubes']) for b in bones)} cubes")


if __name__ == "__main__":
    main()
