#!/usr/bin/env python3
"""
BeatPad Sample & Manifest Generator (Pure Python standard library)
Generates mono, 44.1 kHz, 16-bit WAV files and pack.json manifests for 8 genres:
Trap, Hip-Hop, EDM, House, Dubstep, Drum & Bass, Electro, Future Bass.
Each pack has 12 pads with ONE_SHOT and seamless 2-bar LOOPs, plus Side B variations.
"""

import os
import math
import struct
import wave
import json
import random

SAMPLE_RATE = 44100

NOTE_FREQS = {
    "C1": 32.70, "D1": 36.71, "E1": 41.20, "F1": 43.65, "G1": 49.00, "A1": 55.00, "B1": 61.74,
    "C2": 65.41, "D2": 73.42, "E2": 82.41, "F2": 87.31, "G2": 98.00, "A2": 110.00, "B2": 123.47,
    "C3": 130.81, "D3": 146.83, "E3": 164.81, "F3": 174.61, "G3": 196.00, "A3": 220.00, "B3": 246.94,
    "C4": 261.63, "D4": 293.66, "E4": 329.63, "F4": 349.23, "G4": 392.00, "A4": 440.00, "B4": 493.88,
    "C5": 523.25, "D5": 587.33, "E5": 659.25, "F5": 698.46, "G5": 783.99, "A5": 880.00, "B5": 987.77,
}

def write_wav(filepath, samples):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with wave.open(filepath, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit
        wav_file.setframerate(SAMPLE_RATE)
        # Soft clamp and pack
        frames = bytearray()
        for s in samples:
            clamped = max(-1.0, min(1.0, s))
            int_val = int(clamped * 32767.0)
            frames.extend(struct.pack('<h', int_val))
        wav_file.writeframes(frames)

def gen_kick(duration=0.45, start_freq=160.0, end_freq=45.0, decay=8.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    phase = 0.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        freq = start_freq * math.exp(-decay * progress) + end_freq
        phase += 2.0 * math.pi * freq / SAMPLE_RATE
        env = math.exp(-7.0 * progress)
        samples.append(0.95 * math.sin(phase) * env)
    return samples

def gen_snare(duration=0.25, tone_freq=200.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    phase = 0.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        phase += 2.0 * math.pi * tone_freq / SAMPLE_RATE
        tone = math.sin(phase) * math.exp(-15.0 * progress)
        noise = (random.random() * 2.0 - 1.0) * math.exp(-9.0 * progress)
        samples.append(0.5 * tone + 0.5 * noise)
    return samples

def gen_clap(duration=0.25):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        burst = 0.0
        for b_time in [0.0, 0.015, 0.030]:
            dt = t - b_time
            if 0 <= dt < 0.03:
                burst += (random.random() * 2.0 - 1.0) * math.exp(-35.0 * dt)
        tail = (random.random() * 2.0 - 1.0) * math.exp(-18.0 * t) if t > 0.03 else 0.0
        samples.append(0.85 * (burst + tail))
    return samples

def gen_hat(duration=0.08, open_hat=False):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    decay = 10.0 if open_hat else 35.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        noise = (random.random() * 2.0 - 1.0) * math.exp(-decay * progress)
        samples.append(0.7 * noise)
    return samples

def gen_tom(duration=0.35, start_freq=180.0, end_freq=80.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    phase = 0.0
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        freq = start_freq * (1.0 - progress) + end_freq * progress
        phase += 2.0 * math.pi * freq / SAMPLE_RATE
        env = math.exp(-8.0 * progress)
        samples.append(0.8 * math.sin(phase) * env)
    return samples

def gen_seamless_loop(bpm, pattern_func, bars=2):
    """Generates exactly 2 bars of seamless audio at specified BPM."""
    seconds_per_beat = 60.0 / bpm
    total_seconds = seconds_per_beat * 4 * bars
    num_samples = int(SAMPLE_RATE * total_seconds)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        sample = pattern_func(t, total_seconds, bpm)
        samples.append(sample)
    # Apply micro fade at loop boundary (first/last 200 samples) for seamless loop
    fade_len = min(200, num_samples // 4)
    for i in range(fade_len):
        samples[i] *= (i / fade_len)
        samples[num_samples - 1 - i] *= (i / fade_len)
    return samples

def gen_bass_loop(bpm, root_freq):
    def pattern(t, total_len, bpm):
        beat = (t / (60.0 / bpm)) % 8.0
        notes = [root_freq, root_freq, root_freq * 1.25, root_freq * 1.5]
        note_idx = int(beat // 2) % len(notes)
        freq = notes[note_idx]
        saw = (2.0 * ((t * freq) % 1.0) - 1.0) * 0.4
        sub = math.sin(2.0 * math.pi * (freq / 2.0) * t) * 0.5
        beat_prog = (t / (60.0 / bpm)) % 1.0
        env = 0.5 + 0.5 * math.cos(2.0 * math.pi * beat_prog)
        return (saw + sub) * env * 0.75
    return gen_seamless_loop(bpm, pattern)

def gen_synth_loop(bpm, root_freq):
    def pattern(t, total_len, bpm):
        beat = (t / (60.0 / bpm)) % 8.0
        notes = [root_freq * 2.0, root_freq * 2.5, root_freq * 3.0, root_freq * 2.25]
        freq = notes[int(beat // 2) % len(notes)]
        sine = math.sin(2.0 * math.pi * freq * t)
        saw = (2.0 * ((t * freq * 1.002) % 1.0) - 1.0) * 0.3
        return (sine + saw) * 0.65
    return gen_seamless_loop(bpm, pattern)

def gen_chord_loop(bpm, root_freq):
    def pattern(t, total_len, bpm):
        beat = (t / (60.0 / bpm)) % 8.0
        f1 = root_freq * 2.0
        f2 = root_freq * 2.4
        f3 = root_freq * 3.0
        chord = (math.sin(2.0 * math.pi * f1 * t) +
                 math.sin(2.0 * math.pi * f2 * t) +
                 math.sin(2.0 * math.pi * f3 * t)) / 3.0
        env = math.exp(-2.0 * ((t / (60.0 / bpm)) % 2.0))
        return chord * env * 0.7
    return gen_seamless_loop(bpm, pattern)

def gen_arp_loop(bpm, root_freq):
    def pattern(t, total_len, bpm):
        step_16th = int((t / (15.0 / bpm))) % 16
        multipliers = [1.0, 1.25, 1.5, 2.0, 2.5, 2.0, 1.5, 1.25]
        freq = root_freq * 2.0 * multipliers[step_16th % len(multipliers)]
        step_prog = (t / (15.0 / bpm)) % 1.0
        env = math.exp(-8.0 * step_prog)
        saw = (2.0 * ((t * freq) % 1.0) - 1.0)
        return saw * env * 0.6
    return gen_seamless_loop(bpm, pattern)

def gen_lead_loop(bpm, root_freq):
    def pattern(t, total_len, bpm):
        beat = (t / (60.0 / bpm)) % 8.0
        notes = [root_freq * 3.0, root_freq * 3.5, root_freq * 4.0, root_freq * 3.0]
        freq = notes[int(beat // 2) % len(notes)]
        pulse = 1.0 if ((t * freq) % 1.0) < 0.35 else -1.0
        return pulse * 0.55
    return gen_seamless_loop(bpm, pattern)

def gen_fx(duration=0.6):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        freq = 1200.0 * (1.0 - progress) + 100.0
        phase = 2.0 * math.pi * freq * t
        samples.append(0.7 * math.sin(phase) * (1.0 - progress))
    return samples

def gen_vocal_blip(duration=0.3, pitch=350.0):
    num_samples = int(SAMPLE_RATE * duration)
    samples = []
    for i in range(num_samples):
        t = i / SAMPLE_RATE
        progress = t / duration
        carrier = math.sin(2.0 * math.pi * pitch * t)
        formant = math.sin(2.0 * math.pi * (pitch * 3.2) * t) * 0.4
        env = math.sin(math.pi * progress)
        samples.append((carrier + formant) * env * 0.8)
    return samples

PACK_DEFS = [
    {
        "id": "trap",
        "name": "Neon Trap",
        "genre": "Trap",
        "bpm": 140,
        "key": "F Minor",
        "root": NOTE_FREQS["F1"],
        "primaryColor": "#FF8A3D",
        "secondaryColor": "#FF4D8D",
        "isNew": True
    },
    {
        "id": "dubstep",
        "name": "Cyber Dubstep",
        "genre": "Dubstep",
        "bpm": 145,
        "key": "E Minor",
        "root": NOTE_FREQS["E1"],
        "primaryColor": "#9D4EDD",
        "secondaryColor": "#00F5D4",
        "isNew": True
    },
    {
        "id": "edm",
        "name": "Electro Festival",
        "genre": "EDM",
        "bpm": 128,
        "key": "A Minor",
        "root": NOTE_FREQS["A1"],
        "primaryColor": "#00B4D8",
        "secondaryColor": "#F72585",
        "isNew": False
    },
    {
        "id": "house",
        "name": "Deep Tech House",
        "genre": "House",
        "bpm": 124,
        "key": "G Minor",
        "root": NOTE_FREQS["G1"],
        "primaryColor": "#FF9E00",
        "secondaryColor": "#7209B7",
        "isNew": False
    },
    {
        "id": "dnb",
        "name": "Neuro Drum & Bass",
        "genre": "Drum & Bass",
        "bpm": 174,
        "key": "F# Minor",
        "root": NOTE_FREQS["F1"] * 1.059,
        "primaryColor": "#D90429",
        "secondaryColor": "#EF233C",
        "isNew": False
    },
    {
        "id": "hiphop",
        "name": "Lo-Fi Hip-Hop",
        "genre": "Hip-Hop",
        "bpm": 88,
        "key": "C Major",
        "root": NOTE_FREQS["C2"],
        "primaryColor": "#8338EC",
        "secondaryColor": "#3A86FF",
        "isNew": False
    },
    {
        "id": "electro",
        "name": "Retro Wave Electro",
        "genre": "Electro",
        "bpm": 118,
        "key": "D Minor",
        "root": NOTE_FREQS["D1"],
        "primaryColor": "#FB5607",
        "secondaryColor": "#FF006E",
        "isNew": False
    },
    {
        "id": "futurebass",
        "name": "Chroma Future Bass",
        "genre": "Future Bass",
        "bpm": 150,
        "key": "C# Minor",
        "root": NOTE_FREQS["C1"] * 1.059,
        "primaryColor": "#4CC9F0",
        "secondaryColor": "#F72585",
        "isNew": False
    }
]

PAD_TEMPLATES = [
    {"index": 0, "name": "Kick", "family": "DRUMS", "color": "#FF8A3D", "mode": "ONE_SHOT"},
    {"index": 1, "name": "Snare", "family": "DRUMS", "color": "#FF8A3D", "mode": "ONE_SHOT"},
    {"index": 2, "name": "Clap", "family": "DRUMS", "color": "#FF8A3D", "mode": "ONE_SHOT"},
    {"index": 3, "name": "Closed Hat", "family": "DRUMS", "color": "#FF8A3D", "mode": "ONE_SHOT"},
    {"index": 4, "name": "Perc / Tom", "family": "DRUMS", "color": "#FF8A3D", "mode": "ONE_SHOT"},
    {"index": 5, "name": "Bass Loop", "family": "BASS", "color": "#FF4D8D", "mode": "LOOP"},
    {"index": 6, "name": "Synth Loop", "family": "SYNTH", "color": "#5CE05C", "mode": "LOOP"},
    {"index": 7, "name": "Chord Loop", "family": "CHORDS", "color": "#3DA5FF", "mode": "LOOP"},
    {"index": 8, "name": "Arp Loop", "family": "SYNTH", "color": "#5CE05C", "mode": "LOOP"},
    {"index": 9, "name": "Lead Loop", "family": "SYNTH", "color": "#5CE05C", "mode": "LOOP"},
    {"index": 10, "name": "Laser FX", "family": "FX_VOCAL", "color": "#FFD83D", "mode": "ONE_SHOT"},
    {"index": 11, "name": "Vocal Chop", "family": "FX_VOCAL", "color": "#FFD83D", "mode": "ONE_SHOT"},
]

def generate_pack(pack_def, output_base):
    pack_id = pack_def["id"]
    bpm = pack_def["bpm"]
    root = pack_def["root"]
    pack_dir = os.path.join(output_base, pack_id)
    os.makedirs(pack_dir, exist_ok=True)

    generator_map = [
        lambda: gen_kick(),
        lambda: gen_snare(),
        lambda: gen_clap(),
        lambda: gen_hat(0.08, False),
        lambda: gen_tom(),
        lambda: gen_bass_loop(bpm, root),
        lambda: gen_synth_loop(bpm, root),
        lambda: gen_chord_loop(bpm, root),
        lambda: gen_arp_loop(bpm, root),
        lambda: gen_lead_loop(bpm, root),
        lambda: gen_fx(),
        lambda: gen_vocal_blip()
    ]

    pads_json = []

    for idx, tmpl in enumerate(PAD_TEMPLATES):
        file_a = f"pad_{idx}.wav"
        file_b = f"pad_{idx}_b.wav"
        path_a = os.path.join(pack_dir, file_a)
        path_b = os.path.join(pack_dir, file_b)

        samples_a = generator_map[idx]()
        write_wav(path_a, samples_a)

        samples_b = generator_map[idx]()
        write_wav(path_b, samples_b)

        pad_info = {
            "index": idx,
            "name": tmpl["name"],
            "family": tmpl["family"],
            "file": file_a,
            "fileB": file_b,
            "color": tmpl["color"],
            "mode": tmpl["mode"]
        }
        pads_json.append(pad_info)

    manifest = {
        "id": pack_def["id"],
        "name": pack_def["name"],
        "genre": pack_def["genre"],
        "bpm": pack_def["bpm"],
        "key": pack_def["key"],
        "producer": "BeatPad Studio",
        "primaryColor": pack_def["primaryColor"],
        "secondaryColor": pack_def["secondaryColor"],
        "isNew": pack_def["isNew"],
        "pads": pads_json
    }

    manifest_path = os.path.join(pack_dir, "pack.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

def main():
    target_dir = os.path.join("app", "src", "main", "assets", "packs")
    os.makedirs(target_dir, exist_ok=True)
    for pack_def in PACK_DEFS:
        generate_pack(pack_def, target_dir)

if __name__ == "__main__":
    main()
