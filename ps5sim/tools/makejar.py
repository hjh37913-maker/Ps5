#!/usr/bin/env python3
"""Pack a compiled classes directory into a jar (deterministic order)."""
import os
import sys
import zipfile

root, out = sys.argv[1], sys.argv[2]
files = []
for dp, _, fs in os.walk(root):
    for f in fs:
        if f.endswith(".class"):
            full = os.path.join(dp, f)
            files.append((os.path.relpath(full, root).replace(os.sep, "/"), full))
files.sort()
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for arc, full in files:
        z.write(full, arc)
print(f"classes.jar: {len(files)} classes")
