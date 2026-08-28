#!/usr/bin/env python3
"""Builds the game's sound effects and background music into assets/sounds.

The upstream PvZ2 package this project extracts its art from carries no audio at all -- there is
not one .ogg/.mp3/.wav in RESOURCES.json, in ATLASES, or in resources/_source/pvz-assets.zip -- and
the game's own soundtrack is not ours to ship. So the sounds are synthesised here instead: no
sample is copied from anywhere, everything below is arithmetic over a sine/triangle/noise
generator, which keeps the repository free of third-party audio licensing.

Output is 16-bit mono PCM WAV at 22050 Hz, which every LibGDX desktop backend decodes without an
extra codec. Deterministic: the noise generator is seeded, so re-running this writes byte-identical
files and a rebuild does not show up as a diff.

    py tools/audio-gen/generate_audio.py

Same contract as tools/asset-extract/extract_assets.py: run it from the repository root.
"""

import math
import os
import random
import struct
import sys
import wave

RATE = 22050
AMPLITUDE = 0.62

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SFX_DIR = os.path.join(ROOT, "assets", "sounds", "sfx")
MUSIC_DIR = os.path.join(ROOT, "assets", "sounds", "music")


# --------------------------------------------------------------------------------------------
# generators
# --------------------------------------------------------------------------------------------

def silence(seconds):
    return [0.0] * int(RATE * seconds)


def sine(freq, seconds, phase=0.0):
    n = int(RATE * seconds)
    return [math.sin(2 * math.pi * freq * i / RATE + phase) for i in range(n)]


def triangle(freq, seconds):
    """Softer than a square but with more body than a sine, via the odd-harmonic series."""
    n = int(RATE * seconds)
    out = []
    for i in range(n):
        t = i / RATE
        value = 0.0
        for h in range(1, 8, 2):
            value += ((-1) ** ((h - 1) // 2)) * math.sin(2 * math.pi * freq * h * t) / (h * h)
        out.append(value * 8 / (math.pi ** 2))
    return out


def noise(seconds, seed):
    rng = random.Random(seed)
    return [rng.uniform(-1.0, 1.0) for _ in range(int(RATE * seconds))]


def sweep(start_freq, end_freq, seconds):
    """A frequency glide; the phase is integrated so the sweep has no clicks in it."""
    n = int(RATE * seconds)
    out = []
    phase = 0.0
    for i in range(n):
        freq = start_freq + (end_freq - start_freq) * (i / max(n - 1, 1))
        phase += 2 * math.pi * freq / RATE
        out.append(math.sin(phase))
    return out


# --------------------------------------------------------------------------------------------
# shaping
# --------------------------------------------------------------------------------------------

def envelope(samples, attack=0.01, decay=0.0, sustain=1.0, release=0.08):
    """A plain ADSR over the whole buffer, so nothing starts or stops on a discontinuity."""
    n = len(samples)
    a = max(int(RATE * attack), 1)
    d = int(RATE * decay)
    r = max(int(RATE * release), 1)
    s = max(n - a - d - r, 0)
    out = []
    for i, value in enumerate(samples):
        if i < a:
            gain = i / a
        elif i < a + d:
            gain = 1.0 - (1.0 - sustain) * ((i - a) / max(d, 1))
        elif i < a + d + s:
            gain = sustain
        else:
            gain = sustain * max(0.0, 1.0 - (i - a - d - s) / r)
        out.append(value * gain)
    return out


def lowpass(samples, alpha):
    """One-pole filter; takes the fizz off the noise bursts and the edge off the triangles."""
    out = []
    previous = 0.0
    for value in samples:
        previous += alpha * (value - previous)
        out.append(previous)
    return out


def mix(*layers):
    """Sums layers of different lengths, padding the short ones with silence."""
    length = max(len(layer) for layer in layers)
    out = [0.0] * length
    for layer in layers:
        for i, value in enumerate(layer):
            out[i] += value
    return out


def gain(samples, factor):
    return [value * factor for value in samples]


def at(offset_seconds, samples):
    """Places a layer at a start time inside a longer buffer."""
    return silence(offset_seconds) + samples


def normalise(samples, peak=AMPLITUDE):
    loudest = max((abs(value) for value in samples), default=0.0)
    if loudest == 0.0:
        return samples
    return [value * peak / loudest for value in samples]


def write(path, samples):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    frames = bytearray()
    for value in normalise(samples):
        clipped = max(-1.0, min(1.0, value))
        frames += struct.pack("<h", int(clipped * 32767))
    with wave.open(path, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(RATE)
        out.writeframes(bytes(frames))
    return len(frames) + 44


# --------------------------------------------------------------------------------------------
# the sound effects
# --------------------------------------------------------------------------------------------

def sfx_click():
    """A short blip for menu buttons: quiet and high, so it never competes with the music."""
    return envelope(triangle(880, 0.06), attack=0.002, release=0.05)


def sfx_plant():
    """Planting: a soft low thud, the sound of something being pressed into soil."""
    body = envelope(sine(150, 0.18), attack=0.004, release=0.16)
    soil = envelope(lowpass(noise(0.12, seed=11), 0.10), attack=0.002, release=0.11)
    return mix(body, gain(soil, 0.5))


def sfx_sun():
    """Collecting sun: a rising two-note chime, the reward sound of the game."""
    first = envelope(sine(784, 0.13), attack=0.004, release=0.12)
    second = at(0.08, envelope(sine(1175, 0.20), attack=0.004, release=0.19))
    return mix(first, gain(second, 0.85))


def sfx_shoot():
    """A pea leaving a shooter: a quick downward blip."""
    return envelope(sweep(900, 420, 0.09), attack=0.002, release=0.08)


def sfx_chomp():
    """A zombie biting a plant: two filtered noise bites."""
    bite = envelope(lowpass(noise(0.09, seed=23), 0.16), attack=0.003, release=0.08)
    return mix(bite, at(0.11, gain(bite, 0.8)))


def sfx_explode():
    """An explosion: a noise burst over a falling boom."""
    boom = envelope(sweep(180, 40, 0.55), attack=0.004, release=0.5)
    blast = envelope(lowpass(noise(0.45, seed=37), 0.25), attack=0.002, release=0.44)
    return mix(gain(boom, 0.9), gain(blast, 0.7))


def sfx_win():
    """A major arpeggio: C-E-G-C."""
    layers = []
    for index, freq in enumerate((523.25, 659.25, 783.99, 1046.50)):
        layers.append(at(index * 0.11, envelope(triangle(freq, 0.34),
                                                attack=0.006, release=0.3)))
    return mix(*layers)


def sfx_lose():
    """The same shape falling instead of rising, and detuned flat."""
    layers = []
    for index, freq in enumerate((622.25, 466.16, 369.99, 293.66)):
        layers.append(at(index * 0.13, envelope(triangle(freq, 0.42),
                                                attack=0.008, release=0.38)))
    return mix(*layers)


SFX = {
    "click": sfx_click,
    "plant": sfx_plant,
    "sun": sfx_sun,
    "shoot": sfx_shoot,
    "chomp": sfx_chomp,
    "explode": sfx_explode,
    "win": sfx_win,
    "lose": sfx_lose,
}


# --------------------------------------------------------------------------------------------
# the music
# --------------------------------------------------------------------------------------------

# Note names to frequency, over the octaves the two tracks use.
def note(name):
    semitones = {"C": 0, "C#": 1, "D": 2, "D#": 3, "E": 4, "F": 5, "F#": 6,
                 "G": 7, "G#": 8, "A": 9, "A#": 10, "B": 11}
    pitch = name[:-1]
    octave = int(name[-1])
    return 440.0 * (2 ** ((semitones[pitch] + (octave - 4) * 12 - 9) / 12))


def track(chords, lead, beat, bars, seed, with_pulse):
    """
    Lays a bass note and a chord pad under a lead line, one chord per bar.

    Loops cleanly because every layer is cut to the same whole number of bars and every note is
    enveloped down to zero, so the end of the buffer meets the start of it at silence.
    """
    bar = beat * 4
    total = silence(bar * bars)

    for index in range(bars):
        chord = chords[index % len(chords)]
        start = index * bar

        bass = envelope(triangle(note(chord[0]) / 2, bar * 0.92),
                        attack=0.02, decay=0.1, sustain=0.7, release=0.35)
        total = mix(total, at(start, gain(bass, 0.34)))

        for tone in chord[1:]:
            pad = envelope(sine(note(tone), bar * 0.9), attack=0.15, decay=0.2,
                           sustain=0.55, release=0.4)
            total = mix(total, at(start, gain(pad, 0.13)))

        if with_pulse:
            for step in range(4):
                tick = envelope(lowpass(noise(0.05, seed=seed + index * 4 + step), 0.3),
                                attack=0.002, release=0.045)
                total = mix(total, at(start + step * beat, gain(tick, 0.06)))

    for offset, name, length in lead:
        voice = envelope(triangle(note(name), length), attack=0.03, decay=0.12,
                         sustain=0.6, release=length * 0.4)
        total = mix(total, at(offset, gain(voice, 0.24)))

    return total


def music_menu():
    """Calm, slow, no percussion: it plays under every menu the player reads text on."""
    beat = 0.75
    chords = [
        ["C3", "E4", "G4"],
        ["A2", "C4", "E4"],
        ["F2", "A3", "C4"],
        ["G2", "B3", "D4"],
    ]
    lead = []
    melody = ["E5", "G5", "E5", "D5", "C5", "E5", "D5", "G4"]
    for index, name in enumerate(melody):
        lead.append((index * beat * 2, name, beat * 1.7))
    return track(chords, lead, beat, bars=4, seed=101, with_pulse=False)


def music_battle():
    """Faster, minor, with a quiet pulse on the beat: the lawn is under attack."""
    beat = 0.5
    chords = [
        ["A2", "C4", "E4"],
        ["A2", "C4", "E4"],
        ["F2", "A3", "C4"],
        ["E2", "G#3", "B3"],
    ]
    lead = []
    melody = ["A4", "C5", "E5", "C5", "A4", "F4", "G4", "E4",
              "A4", "C5", "E5", "D5", "C5", "B4", "A4", "E4"]
    for index, name in enumerate(melody):
        lead.append((index * beat, name, beat * 0.85))
    return track(chords, lead, beat, bars=4, seed=202, with_pulse=True)


MUSIC = {
    "menu": music_menu,
    "battle": music_battle,
}


def main():
    if not os.path.isdir(os.path.join(ROOT, "assets")):
        print("run this from the repository root", file=sys.stderr)
        return 1

    written = 0
    for name, build in sorted(SFX.items()):
        path = os.path.join(SFX_DIR, name + ".wav")
        size = write(path, build())
        written += size
        print("sfx   %-10s %7d bytes" % (name, size))

    for name, build in sorted(MUSIC.items()):
        path = os.path.join(MUSIC_DIR, name + ".wav")
        size = write(path, build())
        written += size
        print("music %-10s %7d bytes" % (name, size))

    print("total %d bytes" % written)
    return 0


if __name__ == "__main__":
    sys.exit(main())
