#!/usr/bin/env python
import sys, urllib, base64
input_filename = sys.argv[1]
postwad_filename = input_filename + ".post"
datawad = base64.encodestring(file(input_filename, "rb").read())
postwad = urllib.urlencode({"filedata":datawad, "filename":input_filename})
file(postwad_filename, "wb").write(postwad)
print postwad_filename
