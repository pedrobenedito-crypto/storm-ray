#!/usr/bin/env python3
"""
Synthesises the Storm Ray's original sound effects (no samples, no third-party audio).
Run from the repository root:  python3 tools/generate_sounds.py
Requires: numpy, scipy, ffmpeg (with libvorbis) on PATH.
Output: mono 44.1 kHz Ogg Vorbis files in assets/stormray/sounds, plus sounds.json.
"""
import json
import os
import subprocess
import tempfile

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 44100
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "stormray")
SOUNDS = os.path.join(ASSETS, "sounds")
rng = np.random.default_rng(4242)


# --------------------------------------------------------------------------- building blocks

def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def noise(seconds):
    return rng.standard_normal(int(seconds * SR))


def brown(seconds):
    n = np.cumsum(noise(seconds))
    n = signal.lfilter([1, -1], [1, -0.995], n)
    return n / (np.max(np.abs(n)) + 1e-9)


def lowpass(x, cutoff, order=4):
    b, a = signal.butter(order, cutoff / (SR / 2), "low")
    return signal.lfilter(b, a, x)


def highpass(x, cutoff, order=2):
    b, a = signal.butter(order, cutoff / (SR / 2), "high")
    return signal.lfilter(b, a, x)


def bandpass(x, low, high, order=2):
    b, a = signal.butter(order, [low / (SR / 2), high / (SR / 2)], "band")
    return signal.lfilter(b, a, x)


def sweep_bandpass(x, centers, q=3.0, block=1024):
    """Time-varying band-pass by processing short overlapping blocks."""
    out = np.zeros_like(x)
    win = np.hanning(block * 2)
    for start in range(0, len(x), block):
        seg = x[start:start + block * 2]
        if len(seg) < 16:
            break
        c = float(np.clip(centers[min(start, len(centers) - 1)], 40, SR / 2.3))
        bw = c / q
        lo, hi = max(20.0, c - bw / 2), min(SR / 2.2, c + bw / 2)
        b, a = signal.butter(2, [lo / (SR / 2), hi / (SR / 2)], "band")
        y = signal.filtfilt(b, a, seg) if len(seg) > 30 else seg
        out[start:start + len(seg)] += y * win[:len(seg)]
    return out


def saw(freqs):
    phase = np.cumsum(freqs) / SR
    return 2.0 * (phase % 1.0) - 1.0


def sine(freqs):
    return np.sin(2 * np.pi * np.cumsum(freqs) / SR)


def env(seconds, points):
    """Piecewise-linear envelope from (time, value) points."""
    t = t_axis(seconds)
    times, values = zip(*points)
    return np.interp(t, times, values)


def reverb(x, seconds=1.6, mix=0.35, damp=2500):
    ir_len = int(seconds * SR)
    ir = rng.standard_normal(ir_len) * np.exp(-np.linspace(0, 7, ir_len))
    ir = lowpass(ir, damp)
    wet = signal.fftconvolve(x, ir)[: len(x) + ir_len // 2]
    wet /= np.max(np.abs(wet)) + 1e-9
    dry = np.concatenate([x, np.zeros(len(wet) - len(x))])
    return dry * (1 - mix) + wet * mix * np.max(np.abs(x))


def soft_clip(x, drive=1.5):
    return np.tanh(x * drive) / np.tanh(drive)


def normalize(x, peak=0.9):
    return x / (np.max(np.abs(x)) + 1e-9) * peak


def fade(x, fade_in=0.01, fade_out=0.08):
    n_in, n_out = int(fade_in * SR), int(fade_out * SR)
    y = x.copy()
    if n_in:
        y[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        y[-n_out:] *= np.linspace(1, 0, n_out)
    return y


def pad(x, seconds):
    n = int(seconds * SR)
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:n]


def place(canvas, x, at):
    i = int(at * SR)
    end = min(len(canvas), i + len(x))
    canvas[i:end] += x[: end - i]
    return canvas


# --------------------------------------------------------------------------- sounds

def growl(seconds, base, rough=24.0, formants=((320, 700), (750, 1300)), bright=1.0):
    t = t_axis(seconds)
    vib = 1 + 0.04 * np.sin(2 * np.pi * 4.5 * t) + 0.03 * lowpass(noise(seconds), 6) * 8
    f = base * vib
    voice = saw(f) + 0.6 * saw(f * 1.007) + 0.4 * saw(f * 0.5)
    am = 0.6 + 0.4 * np.abs(lowpass(noise(seconds), rough) * 6)
    voice *= np.clip(am, 0, 1.5)
    shaped = sum(bandpass(voice, lo, hi) for lo, hi in formants) + lowpass(voice, 180) * 0.8
    breath = bandpass(noise(seconds), 400, 3500) * 0.12 * bright
    return soft_clip(shaped * 1.2 + breath, 2.0)


def make_ambient(i):
    seconds = [3.0, 2.6, 3.4][i]
    base = [48, 62, 40][i]
    x = growl(seconds, env(seconds, [(0, base), (seconds * 0.4, base * 1.25), (seconds, base * 0.8)]))
    x *= env(seconds, [(0, 0), (0.35, 1), (seconds * 0.7, 0.8), (seconds, 0)])
    return reverb(x, 1.8, 0.4)


def make_hurt(i):
    seconds = 1.2
    f = env(seconds, [(0, 420 + 60 * i), (0.15, 520 + 40 * i), (seconds, 160)])
    x = growl(seconds, f, rough=40, formants=((600, 1400), (1500, 3200)), bright=2.0)
    x *= env(seconds, [(0, 0), (0.03, 1), (0.4, 0.6), (seconds, 0)])
    return reverb(x, 1.0, 0.3, 4000)


def make_death():
    seconds = 6.0
    f = env(seconds, [(0, 340), (0.6, 380), (3.5, 90), (seconds, 45)])
    x = growl(seconds, f, rough=30, formants=((400, 1000), (1100, 2600)), bright=1.5)
    x *= env(seconds, [(0, 0), (0.1, 1), (3.0, 0.8), (5.0, 0.3), (seconds, 0)])
    for at in (2.4, 3.6, 4.4):
        place(x, make_boom(1.6, 55) * 0.8, at)
    return reverb(x, 2.5, 0.45)


def make_boom(seconds=2.0, start=70):
    t = t_axis(seconds)
    f = start * np.exp(-t * 1.6) + 25
    body = sine(f) * np.exp(-t * 2.2)
    hit = lowpass(noise(seconds), 900) * np.exp(-t * 18) * 3
    rumble = lowpass(brown(seconds), 120) * np.exp(-t * 1.3)
    return soft_clip(body * 1.2 + hit + rumble, 1.8)


def make_whoosh(seconds, f0, f1, f2):
    centers = env(seconds, [(0, f0), (seconds * 0.45, f1), (seconds, f2)])
    x = sweep_bandpass(noise(seconds), centers, q=2.5)
    return x * env(seconds, [(0, 0), (seconds * 0.45, 1), (seconds, 0)])


def make_tentacle(i):
    seconds = 1.0
    x = make_whoosh(seconds, 250 + 80 * i, 1800 + 300 * i, 350)
    slap_t = t_axis(0.25)
    slap = bandpass(noise(0.25), 200, 2600) * np.exp(-slap_t * 30) * 2.5
    wet = lowpass(noise(0.25), 500) * np.exp(-slap_t * 12)
    place(x, slap + wet, 0.55 + 0.05 * i)
    return reverb(normalize(x), 1.0, 0.3)


def crackle(seconds, density, bright=4000):
    x = np.zeros(int(seconds * SR))
    count = int(seconds * density)
    for _ in range(count):
        at = rng.random() * (seconds - 0.02)
        click = highpass(noise(0.01), bright) * np.exp(-np.linspace(0, 8, int(0.01 * SR)))
        place(x, click * rng.uniform(0.3, 1.0), at)
    return x


def make_charge():
    seconds = 4.2
    t = t_axis(seconds)
    f = env(seconds, [(0, 55), (seconds, 230)])
    trem = 0.6 + 0.4 * np.sin(2 * np.pi * np.cumsum(env(seconds, [(0, 3), (seconds, 22)])) / SR)
    tone = (saw(f) * 0.5 + saw(f * 1.5) * 0.3 + sine(f * 0.5)) * trem
    tone = lowpass(tone, 1200)
    riser = make_whoosh(seconds, 200, 1500, 4000) * env(seconds, [(0, 0.2), (seconds, 1.0)])
    sparks = crackle(seconds, 40) * env(seconds, [(0, 0.1), (seconds, 1.2)])
    x = tone * env(seconds, [(0, 0), (0.5, 0.6), (seconds - 0.1, 1.0), (seconds, 0)]) + riser * 0.6 + sparks * 0.5
    return reverb(soft_clip(x, 1.4), 1.4, 0.3)


def make_pulse():
    seconds = 3.5
    t = t_axis(seconds)
    boom = make_boom(seconds, 95)
    burst = bandpass(noise(seconds), 300, 6000) * np.exp(-t * 6) * 1.2
    ring = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, d in ((311, 2.5), (467, 3.2), (733, 4.0), (1093, 5.0))) * 0.25
    x = boom + burst + ring
    return reverb(soft_clip(x, 1.6), 2.4, 0.45)


def make_petrify(i):
    seconds = 1.3
    t = t_axis(seconds)
    grind = lowpass(noise(seconds), 900 + 200 * i) * (0.5 + 0.5 * np.abs(lowpass(noise(seconds), 40) * 10))
    grind *= env(seconds, [(0, 0), (0.05, 0.6), (0.8, 0.4), (seconds, 0)])
    cracks = np.zeros_like(t)
    for at in np.sort(rng.uniform(0.0, 0.9, 6 + i * 2)):
        n = int(0.04 * SR)
        click = bandpass(noise(0.04), 1500, 7000) * np.exp(-np.linspace(0, 10, n)) * rng.uniform(0.6, 1.4)
        thump = lowpass(noise(0.04), 300) * np.exp(-np.linspace(0, 6, n))
        place(cracks, click + thump * 0.8, at)
    return reverb(soft_clip(grind * 0.6 + cracks * 1.3, 1.5), 0.9, 0.25, 3500)


def make_thunder(i):
    seconds = 5.0
    t = t_axis(seconds)
    crack = highpass(noise(seconds), 1200) * np.exp(-t * 14) * 1.6
    roll_env = np.abs(lowpass(noise(seconds), 3 + i)) * 25
    roll = lowpass(brown(seconds), 260) * np.clip(roll_env, 0, 1.5) * np.exp(-t * (0.5 + 0.15 * i))
    sub = lowpass(brown(seconds), 70) * np.exp(-t * 0.8) * 1.2
    x = crack + roll * 1.4 + sub
    return reverb(soft_clip(x, 1.3), 2.0, 0.35, 1500)


def make_storm_loop():
    seconds = 12.0
    total = seconds + 2.0
    t = t_axis(total)
    wind = bandpass(noise(total), 150, 900) * (0.4 + 0.6 * np.abs(lowpass(noise(total), 0.4) * 60))
    wind = np.clip(wind, -3, 3)
    rumble = lowpass(brown(total), 90) * 1.5
    howl = sine(env(total, [(0, 180), (5, 230), (9, 160), (total, 180)])) * 0.05 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.11 * t))
    x = wind * 0.35 + rumble + howl
    for at in (2.0, 7.5):
        roll = make_thunder(0)[: int(4.0 * SR)]
        place(x, lowpass(roll, 300) * 0.5, at)
    # seamless loop: crossfade the extra two seconds back over the start
    n = int(seconds * SR)
    xf = int(2.0 * SR)
    loop = x[:n].copy()
    ramp = np.linspace(0, 1, xf)
    loop[:xf] = x[:xf] * ramp + x[n:n + xf] * (1 - ramp)
    return normalize(loop, 0.7)


def make_ritual():
    seconds = 3.2
    x = make_whoosh(seconds, 120, 900, 3000) * 0.8
    zap = crackle(0.6, 220, 3000) * 1.4
    place(x, zap, 1.2)
    place(x, make_boom(2.0, 80), 1.3)
    return reverb(soft_clip(x, 1.4), 2.0, 0.4)


def make_summon():
    seconds = 7.5
    t = t_axis(seconds)
    f = env(seconds, [(0, 32), (5.2, 58), (seconds, 50)])
    drone = lowpass(saw(f) + saw(f * 1.01) + sine(f * 0.5) * 1.5, 400)
    cluster = sum(sine(np.full_like(t, base) * (1 + 0.003 * np.sin(2 * np.pi * 0.3 * t + k))) for k, base in
                  enumerate((110, 117, 147, 156, 208)))
    cluster = lowpass(cluster, 900) * 0.25
    swell = make_whoosh(seconds, 80, 400, 2500) * env(seconds, [(0, 0), (5.2, 1), (5.4, 0), (seconds, 0)])
    x = (drone * 0.6 + cluster) * env(seconds, [(0, 0), (1.0, 0.4), (5.2, 1.0), (6.5, 0.6), (seconds, 0)]) + swell * 0.7
    place(x, make_boom(2.2, 85) * 1.3, 5.2)
    place(x, growl(1.8, env(1.8, [(0, 70), (0.6, 90), (1.8, 55)])) * env(1.8, [(0, 0), (0.2, 1), (1.8, 0)]) * 0.9, 5.4)
    return reverb(soft_clip(x, 1.4), 2.6, 0.45)


def make_portal_open():
    seconds = 4.5
    x = make_whoosh(seconds, 90, 700, 2400) * 0.6
    place(x, make_thunder(1)[: int(3.5 * SR)] * 0.9, 0.05)
    drone = lowpass(saw(env(seconds, [(0, 38), (seconds, 52)])) + sine(np.full(int(seconds * SR), 76.0)), 300)
    x += drone * env(seconds, [(0, 0), (0.6, 0.5), (3.5, 0.4), (seconds, 0)])
    return reverb(soft_clip(x, 1.4), 2.4, 0.45)


def make_portal_travel():
    seconds = 2.2
    x = make_whoosh(seconds, 2500, 500, 120)
    t = t_axis(seconds)
    x += sine(env(seconds, [(0, 220), (seconds, 55)])) * np.exp(-t * 1.5) * 0.4
    return reverb(x, 1.6, 0.45)


def make_portal_ambient():
    seconds = 3.5
    t = t_axis(seconds)
    hum = sine(np.full_like(t, 55.0)) + 0.5 * sine(np.full_like(t, 82.5)) + 0.25 * sine(np.full_like(t, 110.7))
    hum *= 0.7 + 0.3 * np.sin(2 * np.pi * 0.8 * t)
    hiss = bandpass(noise(seconds), 800, 3000) * 0.08
    x = (hum * 0.6 + hiss + crackle(seconds, 6, 2500) * 0.4) * env(seconds, [(0, 0), (0.6, 1), (2.8, 1), (seconds, 0)])
    return reverb(x, 1.2, 0.3)


# --------------------------------------------------------------------------- export

def export(name, data):
    path = os.path.join(SOUNDS, name + ".ogg")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = (fade(normalize(data, 0.92)) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wavfile.write(tmp.name, SR, pcm)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-ac", "1", "-c:a", "libvorbis",
                        "-q:a", "4", path], check=True)
    os.unlink(tmp.name)
    print(f"  {name}.ogg  {len(pcm) / SR:.1f}s")


def main():
    files = {
        "entity/storm_ray/summon": make_summon(),
        "entity/storm_ray/ambient1": make_ambient(0),
        "entity/storm_ray/ambient2": make_ambient(1),
        "entity/storm_ray/ambient3": make_ambient(2),
        "entity/storm_ray/hurt1": make_hurt(0),
        "entity/storm_ray/hurt2": make_hurt(1),
        "entity/storm_ray/death": make_death(),
        "entity/storm_ray/tentacle1": make_tentacle(0),
        "entity/storm_ray/tentacle2": make_tentacle(1),
        "entity/storm_ray/charge": make_charge(),
        "entity/storm_ray/pulse": make_pulse(),
        "entity/storm_ray/petrify1": make_petrify(0),
        "entity/storm_ray/petrify2": make_petrify(1),
        "environment/lightning1": make_thunder(0),
        "environment/lightning2": make_thunder(1),
        "environment/storm_loop": make_storm_loop(),
        "ritual/ignite": make_ritual(),
        "block/portal_open": make_portal_open(),
        "block/portal_travel": make_portal_travel(),
        "block/portal_ambient": make_portal_ambient(),
    }
    for name, data in files.items():
        export(name, data)

    def entry(subtitle, names, stream=False):
        return {"subtitle": subtitle, "sounds": [{"name": f"stormray:{n}", "stream": stream} for n in names]}

    sounds_json = {
        "entity.storm_ray.summon": entry("subtitles.stormray.summon", ["entity/storm_ray/summon"]),
        "entity.storm_ray.ambient": entry("subtitles.stormray.ambient", [f"entity/storm_ray/ambient{i}" for i in (1, 2, 3)]),
        "entity.storm_ray.hurt": entry("subtitles.stormray.hurt", ["entity/storm_ray/hurt1", "entity/storm_ray/hurt2"]),
        "entity.storm_ray.death": entry("subtitles.stormray.death", ["entity/storm_ray/death"]),
        "entity.storm_ray.tentacle": entry("subtitles.stormray.tentacle", ["entity/storm_ray/tentacle1", "entity/storm_ray/tentacle2"]),
        "entity.storm_ray.charge": entry("subtitles.stormray.charge", ["entity/storm_ray/charge"]),
        "entity.storm_ray.pulse": entry("subtitles.stormray.pulse", ["entity/storm_ray/pulse"]),
        "entity.storm_ray.petrify": entry("subtitles.stormray.petrify", ["entity/storm_ray/petrify1", "entity/storm_ray/petrify2"]),
        "environment.storm.lightning": entry("subtitles.stormray.lightning", ["environment/lightning1", "environment/lightning2"]),
        "environment.storm.loop": entry("subtitles.stormray.storm", ["environment/storm_loop"], stream=True),
        "ritual.ignite": entry("subtitles.stormray.ritual", ["ritual/ignite"]),
        "block.ashen_portal.open": entry("subtitles.stormray.portal_open", ["block/portal_open"]),
        "block.ashen_portal.travel": entry("subtitles.stormray.portal_travel", ["block/portal_travel"]),
        "block.ashen_portal.ambient": entry("subtitles.stormray.portal_ambient", ["block/portal_ambient"]),
    }
    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8") as f:
        json.dump(sounds_json, f, indent=2)


if __name__ == "__main__":
    main()
