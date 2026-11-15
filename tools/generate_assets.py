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
    def __init__(self):
        self.bones = []
        self.uv_cursor = []

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

    @staticmethod
    def _uv_size(zone, fw, fh):
        if zone == "glow":
            return [8, 8]
        density = 2.0
        uw = max(4.0, min(160.0, fw * density))
        uh = max(4.0, min(160.0, fh * density))
        if zone == "vein":
            uw, uh = min(uw, 120.0), min(uh, 56.0)
        return [round(uw, 2), round(uh, 2)]

    def _uv(self, zone, fw, fh):
        uw, uh = self._uv_size(zone, fw, fh)
        if zone == "glow":
            x, y, gw, gh = GLOW_ZONE
            return [x + RNG.randint(2, gw - 12), y + RNG.randint(2, gh - 12)]
        if zone == "vein":
            x, y, vw, vh = VEIN_ZONE
            return [round(x + RNG.uniform(0, vw - uw), 2), round(y + RNG.uniform(0, vh - uh), 2)]
        return [round(RNG.uniform(0, TEX - uw), 2), round(RNG.uniform(0, ORGANIC_MAX_Y - uh), 2)]

    def to_json(self):
        return {
            "format_version": "1.12.0",
            "minecraft:geometry": [{
                "description": {
                    "identifier": "geometry.storm_ray",
                    "texture_width": TEX,
                    "texture_height": TEX,
                    "visible_bounds_width": 80,
                    "visible_bounds_height": 30,
                    "visible_bounds_offset": [0, 8, 0],
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


def main():
    geo = build_geometry()
    write_json(os.path.join(ASSETS, "geo", "entity", "storm_ray.geo.json"), geo.to_json())
    write_json(os.path.join(ASSETS, "animations", "entity", "storm_ray.animation.json"), build_animations())
    entity_textures()
    particle_sprites()
    environment_textures()
    bones = geo.bones
    print(f"geometry: {len(bones)} bones, {sum(len(b['cubes']) for b in bones)} cubes")


if __name__ == "__main__":
    main()
