#!/usr/bin/env python3
"""
Checks GeckoLib animation and model files with the same rules GeckoLib applies when the game
loads them, so a broken file fails the build instead of crashing Minecraft at startup.

Usage: python3 tools/validate_animations.py   (exit code 1 on any problem)
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "stormray")
ALLOWED_TOKEN = re.compile(r"^(?:math\.(?:sin|cos|abs|min|max|clamp|floor|ceil|round|sqrt|pow|lerp|random)|query\.anim_time|"
                           r"[0-9]*\.?[0-9]+|[-+*/(),?:<>=!&| ]+)$")
errors = []


def check_value(where, value):
    if isinstance(value, (int, float)):
        return
    if not isinstance(value, str):
        errors.append(f"{where}: value must be a number or molang string, got {value!r}")
        return
    if value.count("(") != value.count(")"):
        errors.append(f"{where}: unbalanced parentheses in {value!r}")
    for token in re.findall(r"[A-Za-z_][A-Za-z_0-9.]*|[0-9]*\.?[0-9]+|[^A-Za-z_0-9.\s]+", value):
        if not ALLOWED_TOKEN.match(token):
            errors.append(f"{where}: unsupported molang token {token!r} in {value!r}")


def check_vector(where, vector):
    if not isinstance(vector, list) or len(vector) != 3:
        errors.append(f"{where}: keyframe must be a list of 3 values, got {vector!r}")
        return
    for axis, value in zip("xyz", vector):
        check_value(f"{where}.{axis}", value)


def check_channel(where, channel):
    if isinstance(channel, (int, float, str)):
        return check_value(where, channel)
    if isinstance(channel, list):
        return check_vector(where, channel)
    if not isinstance(channel, dict):
        errors.append(f"{where}: invalid channel {channel!r}")
        return
    if "vector" in channel:
        return check_vector(where, channel["vector"])
    times = []
    for key, frame in channel.items():
        try:
            t = float(key)
        except ValueError:
            errors.append(f"{where}: keyframe time {key!r} is not a number")
            continue
        # GeckoLib: "Invalid keyframe data - multiple starting keyframes?"
        if t == 0 and times:
            errors.append(f"{where}: more than one keyframe at time 0 ({key!r})")
        if t in times:
            errors.append(f"{where}: two keyframes at the same time {t}")
        times.append(t)
        if isinstance(frame, dict) and "vector" not in frame:
            if "post" not in frame and "pre" not in frame:
                errors.append(f"{where}@{key}: keyframe object needs vector, pre or post")
            for part in ("pre", "post"):
                if part in frame:
                    check_vector(f"{where}@{key}.{part}", frame[part] if isinstance(frame[part], list) else frame[part].get("vector"))
        elif isinstance(frame, dict):
            check_vector(f"{where}@{key}", frame["vector"])
        else:
            check_vector(f"{where}@{key}", frame)


def bone_names(geo_path):
    with open(geo_path, encoding="utf-8") as f:
        geo = json.load(f)
    names = set()
    for geometry in geo["minecraft:geometry"]:
        for bone in geometry["bones"]:
            if bone["name"] in names:
                errors.append(f"{geo_path}: duplicate bone {bone['name']}")
            names.add(bone["name"])
            parent = bone.get("parent")
            if parent and parent not in {b["name"] for b in geometry["bones"]}:
                errors.append(f"{geo_path}: bone {bone['name']} has unknown parent {parent}")
    return names


def main():
    for anim_path in sorted(glob.glob(os.path.join(ASSETS, "animations", "**", "*.json"), recursive=True)):
        name = os.path.basename(anim_path).replace(".animation.json", "")
        geo_path = os.path.join(ASSETS, "geo", "entity", name + ".geo.json")
        bones = bone_names(geo_path) if os.path.exists(geo_path) else None
        with open(anim_path, encoding="utf-8") as f:
            data = json.load(f)
        for anim_name, anim in data.get("animations", {}).items():
            loop = anim.get("loop", False)
            if loop not in (True, False, "hold_on_last_frame", "play_once", "loop"):
                errors.append(f"{anim_name}: invalid loop value {loop!r}")
            if not isinstance(anim.get("animation_length", 0), (int, float)):
                errors.append(f"{anim_name}: animation_length must be a number")
            for bone, channels in anim.get("bones", {}).items():
                if bones is not None and bone not in bones:
                    errors.append(f"{anim_name}: unknown bone {bone}")
                for channel_name, channel in channels.items():
                    if channel_name not in ("rotation", "position", "scale"):
                        errors.append(f"{anim_name}.{bone}: unknown channel {channel_name}")
                        continue
                    check_channel(f"{os.path.basename(anim_path)}:{anim_name}.{bone}.{channel_name}", channel)
    if errors:
        print("Animation validation FAILED:")
        for e in errors:
            print("  - " + e)
        sys.exit(1)
    print("Animation validation passed.")


if __name__ == "__main__":
    main()
