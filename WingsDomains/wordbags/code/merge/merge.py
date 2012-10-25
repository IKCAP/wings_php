#!/usr/bin/env python

import sys


dict={}

# ---------------------------------------------------------

def add(filename):
    f = open(filename)
    for l in f:
        [word, count] = l.split(" ");
        if word in dict:
            oldcount = dict[word]
            newcount = oldcount + int(count)
            dict[word] = newcount
        else:
            dict[word] = int(count)
    f.close()


# ---------------------------------------------------------

#print sys.argv
numberOfFiles = len(sys.argv)-1

for i in range(1,numberOfFiles+1):
#    print sys.argv[i]
    add(sys.argv[i])
    
for k in sorted(dict):
    val = dict[k]
    avg = val/numberOfFiles
    print k, avg
    
