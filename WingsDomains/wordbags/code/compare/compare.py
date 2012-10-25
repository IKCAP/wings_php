#!/usr/bin/env python

import sys

dicts=[{},{}]

# ---------------------------------------------------------

def add(dict, filename):
    f = open(filename)
    for l in f:
        [word, count] = l.split(" ");
        dicts[dict][word] = int(count)
    f.close()

# ---------------------------------------------------------

#print sys.argv


add(0, sys.argv[1])
add(1, sys.argv[2])
    
dist = 0.0; # count of mismatches
count = 0.0;
for k in dicts[0]:
    if (k in dicts[1]):
       dist += abs(int(dicts[1][k]) - int(dicts[0][k]))
    else:
        dist += int(dicts[0][k])
    count += 1
    
for k in dicts[1]:
    if (k in dicts[0]):
       pass
    else:
        dist += int(dicts[1][k])
    count += 1

print dist/count
