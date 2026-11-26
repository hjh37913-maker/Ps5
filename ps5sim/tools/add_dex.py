#!/usr/bin/env python3
"""Merge classes.dex into the aapt2-built APK.

resources.arsc must stay STORED and 4-byte aligned for targetSdk >= 30, and all
STORED entries must be aligned. Writes a verification summary at the end.
"""
import struct
import sys
import zipfile

src, resources_apk_entries_from, dex_files, dst = None, None, None, None

if len(sys.argv) < 4:
    sys.exit("usage: add_dex.py <base.apk> <out.apk> <dex1> [dex2 ...]")

base = sys.argv[1]
out = sys.argv[2]
dexes = sys.argv[3:]
ALIGN_ID = 0xD935

with zipfile.ZipFile(base, "r") as zin:
    entries = [(i.filename, zin.read(i.filename), i) for i in zin.infolist()]

with zipfile.ZipFile(out, "w") as zout:
    for name, data, old in entries:
        zi = zipfile.ZipInfo(name, date_time=old.date_time)
        zi.compress_type = old.compress_type
        zi.external_attr = old.external_attr
        zi.internal_attr = old.internal_attr
        zi.create_system = 3
        zi.comment = old.comment
        zi.flag_bits = old.flag_bits & ~0x0008
        if name == "resources.arsc":
            zi.compress_type = zipfile.ZIP_STORED
        if zi.compress_type == zipfile.ZIP_STORED:
            base_off = zout.fp.tell() + 30 + len(name.encode("utf-8")) + len(zi.extra)
            pad = (4 - base_off % 4) % 4
            if pad:
                zi.extra = zi.extra + struct.pack("<HH", ALIGN_ID, pad) + b"\x00" * pad
        zout.writestr(zi, data)
    for i, dex in enumerate(dexes):
        name = "classes.dex" if i == 0 else f"classes{i + 1}.dex"
        zi = zipfile.ZipInfo(name, date_time=(2026, 11, 26, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_DEFLATED
        zi.external_attr = 0o644 << 16
        with open(dex, "rb") as f:
            zout.writestr(zi, f.read())

with zipfile.ZipFile(out, "r") as z:
    bad = []
    for i in z.infolist():
        if i.compress_type == zipfile.ZIP_STORED:
            off = i.header_offset + 30 + len(i.filename.encode("utf-8")) + len(i.extra)
            if off % 4:
                bad.append((i.filename, off))
    print(f"entries: {len(z.infolist())}, stored-misaligned: {len(bad)}")
    if bad:
        for b in bad[:10]:
            print("  MISALIGNED", b)
        sys.exit(1)
    print("arsc stored:", z.getinfo("resources.arsc").compress_type == zipfile.ZIP_STORED)
