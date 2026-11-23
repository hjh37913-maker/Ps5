"""Re-pack an aapt2 APK with classes.dex, keeping resources.arsc uncompressed
and 4-byte aligned (required for targetSdk >= 30)."""
import struct, sys, zipfile

src, dex, dst = sys.argv[1:4]
ALIGN_ID = 0xd935

with zipfile.ZipFile(src, 'r') as zin, zipfile.ZipFile(dst, 'w') as zout:
    for old in zin.infolist():
        data = zin.read(old.filename)
        zi = zipfile.ZipInfo(old.filename, date_time=old.date_time)
        zi.compress_type = old.compress_type
        zi.external_attr = old.external_attr
        zi.internal_attr = old.internal_attr
        zi.create_system = old.create_system
        zi.comment = old.comment
        zi.extra = old.extra
        zi.flag_bits = old.flag_bits & ~0x0008
        # resources.arsc must be STORED + 4-byte aligned on Android 11+
        if old.filename == 'resources.arsc':
            zi.compress_type = zipfile.ZIP_STORED
        if zi.compress_type == zipfile.ZIP_STORED:
            base = zout.fp.tell() + 30 + len(zi.filename.encode('utf-8')) + len(zi.extra)
            pad = (4 - base % 4) % 4
            if pad:
                zi.extra = zi.extra + struct.pack('<HH', ALIGN_ID, pad) + b'\x00' * pad
        zout.writestr(zi, data)
    zi = zipfile.ZipInfo('classes.dex', date_time=(2026, 11, 23, 0, 0, 0))
    zi.compress_type = zipfile.ZIP_DEFLATED
    zi.external_attr = 0o644 << 16
    with open(dex, 'rb') as f:
        zout.writestr(zi, f.read())

# verify alignment and content
with zipfile.ZipFile(dst, 'r') as z:
    ok = True
    for i in z.infolist():
        if i.compress_type == zipfile.ZIP_STORED:
            off = i.header_offset + 30 + len(i.filename.encode('utf-8')) + len(i.extra)
            if off % 4 != 0:
                print('MISALIGNED', i.filename, off)
                ok = False
    print('entries:', len(z.infolist()), 'aligned:', ok,
          'arsc stored:', z.getinfo('resources.arsc').compress_type == zipfile.ZIP_STORED)
