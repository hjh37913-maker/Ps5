import os, sys, zipfile
root, out = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
    for dp, _, fs in os.walk(root):
        for f in fs:
            if f.endswith('.class'):
                full = os.path.join(dp, f)
                arc = os.path.relpath(full, root).replace(os.sep, '/')
                z.write(full, arc)
print('classes.jar:', sum(1 for dp,_,fs in os.walk(root) for f in fs if f.endswith('.class')), 'classes')
