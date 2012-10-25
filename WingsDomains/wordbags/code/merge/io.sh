#!/bin/bash

# -----------------------------------------------
# Option Parsing function for:
# -i<1..n> [files.. ] -o<1..n> [files.. ]
# 
# **** IMPORTANT ****
# - Please pass 2 Arguments to this script
#   - Arg1: Number of Input Data expected
#   - Arg1: Number of Input Parameters expected
#   - Arg2: Number of Output Data expected
#
# (c) Varun Ratnakar
# -----------------------------------------------

INUM=$1; shift
PNUM=$1; shift
ONUM=$1; shift

set_variables()
{
    for ((i=1; i<=INUM; i++)); do typeset ICOUNT$i=0; done
    for ((i=1; i<=PNUM; i++)); do typeset PCOUNT$i=0; done
    for ((i=1; i<=ONUM; i++)); do typeset OCOUNT$i=0; done
}

IFLAG=();
PFLAG=();
OFLAG=();
reset_flags()
{
    for ((j=1; j<=INUM; j++)); do IFLAG[$j]='0'; done
    for ((k=1; k<=PNUM; k++)); do PFLAG[$k]='0'; done
    for ((l=1; l<=ONUM; l++)); do OFLAG[$l]='0'; done
}

set_variables
reset_flags

while [ $# -gt 0 ]
do
    case "$1" in
        -i*) in=$(echo $1 | cut -di -f2); reset_flags; IFLAG[$in]='1';;
        -p*) ip=$(echo $1 | cut -dp -f2); reset_flags; PFLAG[$ip]='1';;
        -o*) op=$(echo $1 | cut -do -f2); reset_flags; OFLAG[$op]='1';;
        --) shift; break;;
        -*)
            echo >&2 \
            "usage: $0 -i<1..$INUM> [files.. ] -o<1..$ONUM> [files.. ]"
            exit 1;;
        *)  for((ind=1; ind<=INUM; ind++)); do
                if [ "${IFLAG[$ind]}" = "1" ] 
                then 
                    x=""
                    if [ "${INPUTS[$ind]}" != "" ]; then x="|"; fi
                    INPUTS[$ind]="${INPUTS[$ind]}$x$1"
                fi
            done
            for((ind=1; ind<=PNUM; ind++)); do
                if [ "${PFLAG[$ind]}" = "1" ] 
                then 
                    x=""
                    if [ "${PARAMS[$ind]}" != "" ]; then x="|"; fi
                    PARAMS[$ind]="${PARAMS[$ind]}$x$1"
                fi
            done
            for((ind=1; ind<=ONUM; ind++)); do
                if [ "${OFLAG[$ind]}" = "1" ] 
                then 
                    x=""
                    if [ "${OUTPUTS[$ind]}" != "" ]; then x="|"; fi
                    OUTPUTS[$ind]="${OUTPUTS[$ind]}$x$1"
                fi
            done;;
    esac
    shift
done

IFS='|'
for ((i=1; i<=INUM; i++)); do typeset INPUTS$i=$(echo ${INPUTS[$i]}); done
for ((i=1; i<=PNUM; i++)); do typeset PARAMS$i=$(echo ${PARAMS[$i]}); done
for ((i=1; i<=ONUM; i++)); do typeset OUTPUTS$i=$(echo ${OUTPUTS[$i]}); done
IFS=' '
